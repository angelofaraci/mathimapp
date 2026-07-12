# Handoff: Auth — Login y Register (MathimApp)

## Overview
Pantallas de autenticación para **MathimApp**, una app móvil de matemática gamificada. Cubre el **Login** (una pantalla) y el **Register** (flujo de 3 pasos, una pregunta por pantalla). Todo se entrega en **modo claro (Marea)** y **modo oscuro (Carbón)**.

## About the Design Files
Los archivos de este bundle son **referencias de diseño hechas en HTML** — prototipos que muestran el aspecto y el comportamiento buscado, **no** código de producción para copiar tal cual. La tarea es **recrear estos diseños en el entorno del proyecto MathimApp** (Kotlin / Android — Jetpack Compose o Views, según el proyecto ya existente) usando sus patrones y librerías. Si todavía no hay un sistema montado, elegir el enfoque más adecuado (se recomienda **Jetpack Compose**) e implementarlos ahí. El HTML es solo la fuente de verdad visual.

> Nota Android: el proyecto ya tiene el icono y el splash del logo (paquete `mathimapp_icon`). Reutilizar esos assets; no recrear el logo desde cero salvo para el mark inline de las pantallas (ver Assets).

## Fidelity
**Alta fidelidad (hifi).** Colores, tipografía, espaciados y estados son finales. Recrear la UI de forma fiel usando los componentes nativos del codebase. Las medidas están dadas sobre un lienzo de teléfono de **300 × 624 px** (mockup); escalar proporcionalmente al viewport real (son proporciones de un teléfono estándar ~9:19.5).

---

## Design Tokens

### Marca (constante en ambos modos)
| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| Coral (primario) | `#F2654B` | `#FF6E5A` | CTA, foco de campo, enlaces, progreso activo |
| On-coral (texto sobre coral) | `#FFFFFF` | `#2A0F0A` | Texto/íconos dentro del botón primario |
| Verde azulado | `#0E9E8E` | `#2DD4BF` | Éxito, medidor de fuerza de contraseña |
| Rosa | `#F0526A` | `#FF6F8A` | Vidas/errores (no usado en auth, parte del sistema) |

### Neutros
| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| app-bg | `#FBF6EF` | `#16181D` | Fondo de pantalla (tope del gradiente) |
| app-bg2 | `#F2E9DD` | `#0E0F13` | Fondo de pantalla (base del gradiente) |
| surface | `#FFFFFF` | `#1F232B` | Campo enfocado, botones sociales, tarjeta del mark |
| surface2 | `#F6EFE6` | `#282D37` | Botón de "volver", chips |
| field | `#FBF6EF` | `#1A1D24` | Fondo de campo en reposo |
| ink | `#26333B` | `#EEF1F5` | Texto principal |
| muted | `#7C8790` | `#8A93A3` | Texto secundario, placeholders, íconos inactivos |
| line | `#EBE3D7` | `#2E333E` | Bordes, separadores, segmentos de progreso vacíos |

### Tipografía
- Familia: **Sora** (400/500/600/700/800). Alternativa del sistema: Nunito.
- En Android: usar Sora como fuente descargable/empaquetada. Fallback sans-serif.
- Escala usada:
  - Título de pantalla: **27px / 800**, letter-spacing `-0.02em` (login y "Creá tu cuenta")
  - Pregunta de paso: **25px / 800**, letter-spacing `-0.02em`
  - Subtítulo: **14px / 500**, color muted, line-height 1.5
  - Valor/placeholder de campo: **15px** (500 placeholder muted, 600 valor ink)
  - Texto de botón: **16px / 700**
  - Wordmark "MathimApp": **22px / 800**, letter-spacing `-0.02em` ("Mathim" ink + "App" muted/500)
  - Labels pequeños (Paso X/3, fuerza, helper): **11–13px**, algunos en mono (JetBrains Mono) para "Paso X / 3"

### Radios, sombras, espaciados
- Radio de teléfono (mockup): `44px`. En dispositivo real es el borde del sistema — ignorar.
- Radio de campo: `15px`. Radio de botón: `16px`. Radio de botón social: `14px`. Radio de checkbox: `7px`. Radio de botón "volver": `12px`. Radio de tarjeta del mark: `16px`.
- Borde de campo en reposo: `1.5px solid line`. Campo enfocado: `2px solid coral` + sombra `0 6px 18px -10px rgba(coral,0.42)`.
- Sombra de CTA primario: `0 12px 24px -10px rgba(coral, 0.42 claro / 0.5 oscuro)`.
- Padding horizontal de pantalla: `26px`. Gap entre campos: `11–12px`.
- Altura de barra de progreso segmentada: `5px`, radio `99px`, gap `6px`.

---

## Screens / Views

### 1. Login (`Login · Claro` / `Login · Oscuro`)
- **Purpose**: El usuario existente inicia sesión.
- **Layout** (de arriba a abajo, padding-x 26px):
  1. Status bar (9:41 + íconos) — es del sistema, no implementar.
  2. **Marca**: tarjeta 52×52 (surface, radio 16, ring interior 1px `line`) con el logo infinito 6d + wordmark "MathimApp". Gap 11px.
  3. **Título**: "Hola de nuevo" (27/800). **Subtítulo**: "Iniciá sesión para seguir tu racha." (14 muted).
  4. **Campo email** (reposo): ícono sobre a la izquierda, valor `ana@correo.com`.
  5. **Campo contraseña** (enfocado, coral): ícono candado coral, `••••••••`, ícono de ojo (toggle mostrar/ocultar) a la derecha.
  6. **Link** derecha: "¿Olvidaste tu contraseña?" (13/600 coral).
  7. **CTA primario**: "Iniciar sesión" (coral, full-width, 16px pad, texto on-coral).
  8. **Divisor**: línea — "o continuá con" — línea (12 muted).
  9. **Botones sociales** (fila, 2 iguales): Google (logo multicolor) y Apple (glifo ink). Fondo surface, borde line.
  10. **Pie** (anclado abajo, centrado): "¿No tenés cuenta? **Registrate**" (Registrate en coral/700) → navega a Register paso 1.
- **Estados**: campo enfocado = borde coral 2px + glow; ojo alterna visibilidad de la contraseña.

### 2. Register — flujo de 3 pasos
Una **pregunta por pantalla** para que el teclado nunca tape el campo ni el botón (ambos viven en la mitad superior). Header común en cada paso:
- Botón **volver** (38×38, surface2, radio 12, flecha ink) a la izquierda; a la derecha **"Paso N / 3"** (12px mono, muted).
- **Barra de progreso** segmentada de 3 tramos: los completados/actual en `coral`, los pendientes en `line`.

#### Paso 1 — Nombre (`Registro 1 Nombre · …`)
- Título: "¿Cómo te llamás?" (25/800). Subtítulo: "Así te vamos a saludar en la app."
- Campo **nombre** enfocado (coral) con ícono usuario, valor `Ana` y caret coral (2×19px).
- Helper: "Podés usar solo tu nombre." (12.5 muted).
- CTA: **"Continuar"** con flecha → (coral). Progreso: `1` lleno.

#### Paso 2 — Correo (`Registro 2 Correo · …`)
- Título: "¿Cuál es tu correo?" Subtítulo: "Lo usamos para guardar tu progreso."
- Campo **email** enfocado con valor `ana@correo.com` + caret.
- Helper: "Te enviaremos un código de verificación."
- CTA: "Continuar" →. Progreso: `1,2` llenos.

#### Paso 3 — Contraseña (`Registro 3 Contraseña · …`)
- Título: "Elegí una contraseña" Subtítulo: "Mínimo 8 caracteres."
- Campo **password** enfocado: candado coral, `••••••••`, ojo (toggle).
- **Medidor de fuerza**: 3 segmentos (5px) — 2 en `teal`, 1 en `line` — + etiqueta "Buena" (11/700 teal). Estado ejemplo = fuerza media/buena.
- **Checkbox términos**: cuadro 22×22 coral con check on-coral (marcado) + texto "Acepto los **Términos** y la **Privacidad**." (12.5 muted, palabras clave en ink/600).
- CTA: **"Crear cuenta"** (coral, sin flecha). Progreso: `1,2,3` llenos.

> En el mockup, la zona inferior de cada paso muestra un bloque tenue rotulado "ÁREA DEL TECLADO" — es solo una **anotación de diseño** para indicar dónde aparece el teclado. **No implementar** ese bloque; sirve para mostrar que campo + botón quedan por encima del teclado.

---

## Interactions & Behavior
- **Login → Register**: "Registrate" abre el paso 1. **Register → Login**: "Iniciá sesión" (pie del paso... en el diseño el pie de login lo enlaza; en register el retorno es la flecha de volver / o un enlace equivalente) vuelve al login.
- **Navegación entre pasos**: "Continuar" avanza al siguiente paso; la flecha "volver" retrocede uno (en el paso 1, vuelve a Login). Transición sugerida: slide horizontal (avance izquierda, retroceso derecha), ~250ms ease-out.
- **Foco de campo**: al enfocar, borde pasa a coral 2px + glow; al desenfocar vuelve a `line` 1.5px.
- **Toggle de contraseña**: el ícono de ojo alterna texto plano / oculto.
- **Barra de progreso**: refleja el paso actual (N tramos coral de 3).
- **CTA deshabilitado**: cuando el campo del paso está vacío/ inválido, el botón debería verse atenuado (reducir opacidad ~0.5) y no navegar. (No hay mock explícito del estado disabled — aplicar el patrón del codebase.)

### Validaciones (reglas sugeridas)
- Nombre: no vacío.
- Correo: formato email válido.
- Contraseña: mínimo 8 caracteres; el medidor refleja fuerza (débil/media/buena). Términos debe estar aceptado para habilitar "Crear cuenta".

## State Management
- `mode`: claro | oscuro (seguir el tema del sistema/app).
- `registerStep`: 1 | 2 | 3.
- Campos: `name`, `email`, `password`, `passwordVisible` (bool), `acceptedTerms` (bool).
- `passwordStrength`: derivado de `password`.
- Auth: disparadores para login con email/contraseña y con Google/Apple (OAuth). El backend/SDK lo define el codebase.

## Assets
- **Logo 6d (infinito)**: trazo con degradado horizontal — claro: `#F2654B → #26333B → #26333B → #0E9E8E` (stops 0 / 0.28 / 0.72 / 1); oscuro: `#FF6E5A → #EEF1F5 → #EEF1F5 → #2DD4BF`. Path SVG (viewBox 0 0 100 100), `stroke-width:7`, linejoin/linecap round:
  `M50 50 C62 34 90 36 90 50 C90 64 62 66 50 50 C38 34 10 36 10 50 C10 64 38 66 50 50 Z`
  El SVG editable también está en el paquete del icono: `mathimapp_icon/logo_6d.svg`.
- **Íconos** (line, stroke ~2px, 24px viewBox): sobre/mail, candado, ojo, usuario, flecha-volver, flecha-derecha, check. Reemplazar por el set de íconos del codebase (p. ej. Material Symbols) manteniendo el trazo.
- **Google**: logo multicolor oficial (4 colores). **Apple**: glifo monocromo (usa el color `ink`).
- **App icon / splash**: ya generados en `mathimapp_icon/` (adaptive icon, legacy, round, Play Store 512, splash_logo). Reutilizar.

## Files
- `Auth · Login y Register.dc.html` — prototipo de las 8 pantallas (Login + 3 pasos de Register × claro/oscuro). Abrir en navegador para inspeccionar medidas y copiar valores exactos.
- Referencia del sistema completo (colores/tokens/tipografía y otras pantallas de la app): `Sistema Unificado.dc.html` (en la raíz del proyecto).
- `mathimapp_icon/` — assets de icono y splash + `logo_6d.svg`.
