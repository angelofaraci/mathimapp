[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [ValidatePattern('^[A-Za-z0-9._-]+$')]
    [string]$DistroName = 'FedoraLinux-43'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Test-IsAdministrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Invoke-ElevatedSelf {
    $escapedScriptPath = $PSCommandPath.Replace('"', '`"')
    $escapedDistroName = $DistroName.Replace('"', '`"')
    $arguments = "-NoProfile -ExecutionPolicy Bypass -File `"$escapedScriptPath`" -DistroName `"$escapedDistroName`""
    $process = Start-Process -FilePath 'powershell.exe' -ArgumentList $arguments -Verb RunAs -Wait -PassThru

    if ($process.ExitCode -ne 0) {
        throw "Android connectivity setup failed in the elevated PowerShell process (exit code $($process.ExitCode))."
    }
}

function Convert-LittleEndianHexToBytes {
    param([Parameter(Mandatory)][string]$HexValue)

    if ($HexValue -notmatch '^[0-9A-Fa-f]{8}$') {
        throw "Invalid WSL route value: $HexValue"
    }

    [uint32]$value = [Convert]::ToUInt32($HexValue, 16)
    return [byte[]]@(
        ($value -band 0x000000FF),
        (($value -shr 8) -band 0x000000FF),
        (($value -shr 16) -band 0x000000FF),
        (($value -shr 24) -band 0x000000FF)
    )
}

function Test-AddressInNetwork {
    param(
        [Parameter(Mandatory)][string]$Address,
        [Parameter(Mandatory)][string]$NetworkHex,
        [Parameter(Mandatory)][string]$MaskHex
    )

    $addressBytes = [Net.IPAddress]::Parse($Address).GetAddressBytes()
    if ($addressBytes.Length -ne 4) {
        return $false
    }

    $networkBytes = Convert-LittleEndianHexToBytes $NetworkHex
    $maskBytes = Convert-LittleEndianHexToBytes $MaskHex

    for ($index = 0; $index -lt 4; $index++) {
        if (($addressBytes[$index] -band $maskBytes[$index]) -ne ($networkBytes[$index] -band $maskBytes[$index])) {
            return $false
        }
    }

    return $true
}

function Get-WslIpv4Address {
    $probe = @'
default_interface=""
while IFS=$' \t' read -r interface destination gateway flags rest; do
    interface="${interface%$'\r'}"
    destination="${destination%$'\r'}"
    gateway="${gateway%$'\r'}"
    if [[ "$destination" == "00000000" && "$gateway" != "00000000" ]]; then
        default_interface="$interface"
        break
    fi
done < /proc/net/route #

if [[ -z "$default_interface" ]]; then
    exit 11
fi

while IFS=$' \t' read -r interface destination gateway flags refcount use metric mask rest; do
    interface="${interface%$'\r'}"
    destination="${destination%$'\r'}"
    gateway="${gateway%$'\r'}"
    mask="${mask%$'\r'}"
    if [[ "$interface" == "$default_interface" && "$gateway" == "00000000" && "$destination" != "00000000" ]]; then
        printf 'NETWORK %s %s\n' "$destination" "$mask"
        break
    fi
done < /proc/net/route #

candidate=""
while IFS= read -r line; do
    line="${line%$'\r'}"
    case "$line" in
        *'|-- '*|*'/-- '*)
            candidate="${line##*-- }"
            ;;
        *'/32 host LOCAL'*)
            case "$candidate" in
                [0-9]*.[0-9]*.[0-9]*.[0-9]*)
                    printf 'ADDRESS %s\n' "$candidate"
                    ;;
            esac
            ;;
    esac
done < /proc/net/fib_trie #
'@

    # Feed the probe through standard input so WSL does not reinterpret Bash variables
    # while translating Windows command-line arguments.
    $output = @($probe | & wsl.exe -d $DistroName -- /bin/bash -s)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect networking in WSL distribution '$DistroName' (exit code $LASTEXITCODE)."
    }

    $networkLine = @($output | Where-Object { $_ -match '^NETWORK\s+[0-9A-Fa-f]{8}\s+[0-9A-Fa-f]{8}$' })
    if ($networkLine.Count -ne 1) {
        throw "Could not determine the IPv4 network for WSL distribution '$DistroName'."
    }

    $networkParts = $networkLine[0] -split '\s+'
    $candidates = @(
        $output |
            Where-Object { $_ -match '^ADDRESS\s+\d+\.\d+\.\d+\.\d+$' } |
            ForEach-Object { ($_ -split '\s+')[1] } |
            Sort-Object -Unique |
            Where-Object { Test-AddressInNetwork -Address $_ -NetworkHex $networkParts[1] -MaskHex $networkParts[2] }
    )

    if ($candidates.Count -ne 1) {
        throw "Expected one WSL IPv4 address in its default network; found $($candidates.Count)."
    }

    return $candidates[0]
}

function Find-AdbExecutable {
    $command = Get-Command 'adb.exe' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -ne $command) {
        return $command.Source
    }

    $sdkRoots = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME, (Join-Path $env:LOCALAPPDATA 'Android\Sdk')) |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Unique

    foreach ($sdkRoot in $sdkRoots) {
        $candidate = Join-Path $sdkRoot 'platform-tools\adb.exe'
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            return $candidate
        }
    }

    throw 'Could not find Windows adb.exe. Install Android SDK Platform-Tools or add adb.exe to the Windows PATH.'
}

function Get-AuthorizedDeviceSerial {
    param([Parameter(Mandatory)][string]$AdbPath)

    $devices = @(& $AdbPath devices)
    if ($LASTEXITCODE -ne 0) {
        throw "adb devices failed (exit code $LASTEXITCODE)."
    }

    $connected = @(
        $devices |
            Where-Object { $_ -match '^(\S+)\s+(device|unauthorized|offline)$' } |
            ForEach-Object {
                $parts = $_ -split '\s+'
                [PSCustomObject]@{ Serial = $parts[0]; State = $parts[1] }
            }
    )

    $authorized = @($connected | Where-Object State -eq 'device')
    if ($authorized.Count -ne 1) {
        $status = if ($connected.Count -eq 0) { 'no Android device was detected' } else { ($connected | ForEach-Object { "$($_.Serial)=$($_.State)" }) -join ', ' }
        throw "Expected exactly one USB-connected, authorized Android device; found $status."
    }

    return $authorized[0].Serial
}

if (-not (Test-IsAdministrator)) {
    if ($WhatIfPreference) {
        Write-Host 'WhatIf: UAC elevation would be requested to set the loopback-only port proxy.'
    }
    else {
        Invoke-ElevatedSelf
        return
    }
}

$wslAddress = Get-WslIpv4Address
$adbPath = Find-AdbExecutable

if ($WhatIfPreference) {
    Write-Host "WhatIf: would set 127.0.0.1:8080 -> $wslAddress`:8080 with netsh."
    Write-Host "WhatIf: would run '$adbPath -s <authorized-device> reverse tcp:8080 tcp:8080'."
    return
}

& netsh.exe interface portproxy set v4tov4 listenaddress=127.0.0.1 listenport=8080 connectaddress=$wslAddress connectport=8080 protocol=tcp
if ($LASTEXITCODE -ne 0) {
    throw "netsh interface portproxy failed (exit code $LASTEXITCODE)."
}

$deviceSerial = Get-AuthorizedDeviceSerial -AdbPath $adbPath
& $adbPath -s $deviceSerial reverse tcp:8080 tcp:8080
if ($LASTEXITCODE -ne 0) {
    throw "adb reverse failed (exit code $LASTEXITCODE)."
}

Write-Host "Android device $deviceSerial now reaches the WSL server through http://127.0.0.1:8080."
