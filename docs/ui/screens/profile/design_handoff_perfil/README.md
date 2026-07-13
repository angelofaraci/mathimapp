# Handoff: Perfil (MathimApp)

## Overview
Rediseño de la pantalla de **Perfil** de MathimApp como **hub de navegación**: identidad del usuario arriba, tarjetas de acceso a sub-pantallas (**Cuenta**, **Preferencias**, **Ayuda y soporte**, **Acerca de**), y **Cerrar sesión** anclado al pie del hub. La acción destructiva **Eliminar cuenta** vive dentro de la sub-pantalla Cuenta. Todo en **modo claro y oscuro** con los tokens del Sistema unificado Marea.

## About the Design Files
`Perfil v2.dc.html` es una **referencia de diseño hecha en HTML** — muestra aspecto y jerarquía buscados, **no** es código de producción. La tarea es **recrear estas pantallas en el proyecto MathimApp existente** (Kotlin / Android — Jetpack Compose recomendado) usando sus patrones, navegación y componentes ya montados. **No agregar funcionalidades nuevas**: solo reorganizar las existentes en este layout.

## Fidelity
**Alta fidelidad.** Colores, tipografía, espaciados y estados son finales. Medidas dadas sobre un lienzo de teléfono de **300 px de ancho** (mockup); escalar proporcionalmente al viewport real.

---

## Design Tokens
Idénticos al handoff de Auth (Sistema unificado Marea):

### Marca (constante en ambos modos)
| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| Coral (primario) | `#F2654B` | `#FF6E5A` | Icono de Cuenta, tab activo, botón editar avatar, iniciales, links |
| On-coral | `#FFFFFF` | `#2A0F0A` | Icono dentro del botón editar avatar |
| Verde azulado | `#0E9E8E` | `#2DD4BF` | Chip de rol, toggles activos, icono de Preferencias |
| Rosa | `#F0526A` | `#FF6F8A` | Eliminar cuenta, icono de Ayuda |

### Neutros
| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| app-bg → app-bg2 | `#FBF6EF → #F2E9DD` | `#16181D → #0E0F13` | Fondo de pantalla (gradiente vertical) |
| surface | `#FFFFFF` | `#1F232B` | Tarjetas, tab bar, botón cerrar sesión |
| surface2 | `#F6EFE6` | `#282D37` | Fondo de avatar, cajas de icono (42×42), botón volver |
| ink | `#26333B` | `#EEF1F5` | Texto principal |
| muted | `#7C8790` | `#8A93A3` | Texto secundario, iconos de fila, chevrons |
| line | `#EBE3D7` | `#2E333E` | Bordes de tarjeta, separadores |
| track | `#EADFD1` | `#282D37` | Toggle apagado |

### Tipografía (Sora)
- Nombre del usuario: **21px / 800**, letter-spacing `-0.01em`
- Correo bajo el nombre: **12px / 500**, muted
- Título de tarjeta de navegación: **14px / 700**; subtítulo: **11px / 500** muted
- Encabezado de sub-pantalla: **17px / 700** centrado
- Etiqueta de fila (label pequeño): **10px / 600** muted; valor: **13px / 600**
- Fila de preferencia: **13px / 600**
- Chips: **11px / 700**
- Caption de versión: **10px** muted

### Radios y componentes
- Tarjeta: radio `18px`, borde `1px line`, fondo surface
- Caja de icono en tarjeta de navegación: `42×42`, radio `13px`, fondo surface2
- Botón Cerrar sesión / Eliminar cuenta: radio `16px`; Eliminar con borde `1.5px rosa` y texto rosa
- Chips: pill (`99px`), fondo surface, borde line
- Toggle: `40×24`, pill; activo = teal con perilla blanca (claro) / `#05201C` (oscuro); apagado = track con perilla blanca
- Avatar: `92px` círculo, fondo surface2, borde `3px surface`, iniciales coral 32/800; badge editar `28px` coral, borde `3px app-bg`, abajo-derecha
- Filas de lista: padding `13–14px 16px`, separadores `1px line` con margen lateral `16px` (inset)
- Padding horizontal de pantalla: `20px`; gap entre secciones: `14–16px`

---

## Screens

### 1 · Perfil (hub)
Tab "Perfil" activo en la tab bar (coral).
1. **Identidad** (centrada): avatar con badge de editar → nombre → correo → chips [Estudiante (teal)] [Racha 12 días (coral, icono llama)].
2. **Tarjetas de navegación** (columna, gap 12px), cada una: caja de icono + título + subtítulo + chevron:
   - **Cuenta** — "Nombre, correo, contraseña" (icono usuario, coral)
   - **Preferencias** — "Notificaciones, sonidos, idioma" (icono engranaje, teal)
   - **Ayuda y soporte** — "FAQ, contacto, reportar un problema" (icono ?, rosa)
   - **Acerca de** — "Términos, privacidad, versión" (icono i, muted)
3. **Pie** (anclado abajo): botón **Cerrar sesión** (surface, icono + texto ink) y caption "MathimApp · versión X".

### 2 · Cuenta (sub-pantalla)
Header con botón volver (38×38, surface2, radio 12px) + título centrado "Cuenta".
- Tarjeta con 3 filas: **Nombre completo** (chevron), **Correo electrónico** (chevron), **Contraseña** (acción "Cambiar" en coral).
- Nota legal en muted (rol del usuario + aviso de confirmación por correo).
- **Anclado al pie: botón Eliminar cuenta** (borde y texto rosa, icono papelera) + caption "Esta acción es permanente y borra todo tu progreso." Debe pedir confirmación (diálogo destructivo estándar del sistema).

### 3 · Preferencias (sub-pantalla)
Mismo header con volver. Tarjeta con 4 filas:
- **Notificaciones** — toggle (activo)
- **Sonidos** — toggle (activo)
- **Modo oscuro** — toggle (refleja/controla el tema de la app)
- **Idioma** — valor actual ("Español") + chevron → selector

Ayuda y soporte / Acerca de no tienen mockup: usar listas simples con el mismo patrón de tarjeta y filas.

## Navegación
- Hub → sub-pantallas: push estándar con botón volver.
- Cerrar sesión: confirmación ligera opcional; vuelve al flujo Auth (ver handoff `design_handoff_auth`).
- Eliminar cuenta: diálogo de confirmación destructivo obligatorio.

## Reglas
- No inventar campos ni ajustes que la app no tenga; si un ítem mostrado no existe (p. ej. racha), omitir el chip.
- Mantener contraste AA en ambos modos usando exactamente los pares de tokens indicados.
