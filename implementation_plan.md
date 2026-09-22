# Rediseño de Paleta de Colores — Brynn Web

## Objetivo
Migrar el tema visual de la landing page y páginas HTML de un estilo **oscuro neón** a un estilo **claro moderno** inspirado en la imagen de referencia (Edu.ai dashboard).

## Paleta Nueva

| Variable CSS | Antes | Después | Rol |
|---|---|---|---|
| `--bg-color` | `#0d0f12` (negro) | `#F0F1F7` (gris lavanda) | Fondo de página |
| `--card-bg` | `#15181e` (gris oscuro) | `#FFFFFF` (blanco) | Fondo de tarjetas |
| `--card-border` | `#222631` | `#E5E7EF` (gris sutil) | Bordes |
| `--accent` | `#a6ff00` (neón verde) | `#4B0DCA` (morado profundo) | Color primario |
| `--accent-lime` | — | `#C8F135` (lima vibrante) | Acento secundario |
| `--accent-glow` | `rgba(166,255,0,0.25)` | `rgba(75,13,202,0.15)` | Sombras glow |
| `--text-main` | `#f3f4f6` (blanco) | `#111827` (casi negro) | Texto principal |
| `--text-sub` | `#9ca3af` (gris claro) | `#6B7280` (gris medio) | Texto secundario |

## Archivos a Modificar

### index.html
- Variables CSS en `:root`
- Colores hardcoded de texto blanco (#ffffff → #111827)
- CTA button: fondo morado, texto blanco
- Banner: gradiente claro
- Tooltip de WhatsApp: fondo blanco con borde

### reset-password.html
- Mismo sistema de variables

### delete-account.html
- Mismo sistema de variables

### terms.html / terms_of_service.html
- Mismo sistema de variables

### privacy.html / privacy_policy.html
- Mismo sistema de variables
