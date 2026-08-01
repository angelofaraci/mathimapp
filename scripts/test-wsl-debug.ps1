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

Write-Host "Running wsl.exe..."
$output = @($probe | & wsl.exe -d FedoraLinux-43 -- /bin/bash -s)
Write-Host "Exit code: $LASTEXITCODE"
Write-Host "Output:"
$output | ForEach-Object { Write-Host "  [$_]" }
