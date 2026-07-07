# MathimApp — icono & splash (logo 6d)

Assets del logo **6d** (infinito con el color integrado en el trazo: coral → tinta → verde).
Todos los iconos son PNG en cada densidad — es la vía robusta en Android porque 6d usa un
degradado sobre el trazo, algo que un VectorDrawable no soporta de forma nativa.
`logo_6d.svg` va incluido por si querés editar el vector.

## Contenido
```
res/
  mipmap-mdpi|hdpi|xhdpi|xxhdpi|xxxhdpi/
    ic_launcher.png            ← icono cuadrado (legacy)
    ic_launcher_round.png      ← icono redondo (legacy)
    ic_launcher_foreground.png ← capa foreground del icono adaptativo
  mipmap-anydpi-v26/
    ic_launcher.xml            ← icono adaptativo (API 26+)
    ic_launcher_round.xml
  values/
    ic_launcher_background.xml ← color de fondo del icono (#FFFFFF)
    themes_splash.xml          ← tema del splash (fusionar con tu themes.xml)
  drawable-nodpi/
    splash_logo.png            ← imagen de carga (splash)
ic_launcher-playstore.png      ← 512×512 para Google Play
logo_6d.svg                    ← vector editable
```

## 1) Icono
Copiá la carpeta `res/` dentro de `app/src/main/res/` (se fusiona con la que ya tenés).
En `AndroidManifest.xml` dejá el `<application>` apuntando al icono (suele venir así):
```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ... >
```
El fondo blanco se cambia en `res/values/ic_launcher_background.xml`.

## 2) Splash (imagen de carga) — SplashScreen API
Agregá la dependencia (retrocompatible hasta API 21):
```kotlin
// build.gradle.kts (module)
implementation("androidx.core:core-splashscreen:1.0.1")
```
Fusioná `Theme.App.Starting` de `res/values/themes_splash.xml` en tu `themes.xml`
(ajustá `postSplashScreenTheme` al tema real de tu app).

En el `<activity>` de arranque del manifest usá ese tema:
```xml
<activity android:name=".MainActivity" android:theme="@style/Theme.App.Starting" ... >
```

En tu Activity, antes de `setContentView`:
```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
}
```

En Android 12+ el sistema muestra `splash_logo` centrado sobre el fondo blanco.
Para mantener el splash mientras cargan tus datos:
```kotlin
val content: View = findViewById(android.R.id.content)
content.viewTreeObserver.addOnPreDrawListener(
    object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean =
            if (viewModel.isReady) { content.viewTreeObserver.removeOnPreDrawListener(this); true }
            else false
    }
)
```

## Regenerar / editar
El color de la marca vive en el degradado del trazo: coral `#F2654B`, tinta `#26333B`,
verde `#0E9E8E`. El fondo (icono y splash) es blanco `#FFFFFF` — cambialo donde prefieras.
