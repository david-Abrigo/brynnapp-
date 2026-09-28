# Brynn Soundbox - Plataforma Web Oficial

Sitio web oficial, landing page, APIs serverless y documentos legales para la aplicación **Brynn Soundbox** (Parlante de Pagos inteligente para comercios).

Desplegado en **Vercel** con integración continua desde **GitHub**.

---

## 📁 Estructura del Proyecto

```text
notiyape/
├── api/                       # Funciones Serverless de Vercel (Backend)
│   ├── auth-config.js         # Endpoint seguro de configuración pública
│   ├── feedback.js            # Recepción y registro de feedback de testers
│   ├── recover-password.js    # Enrutamiento de restablecimiento de contraseña
│   └── register-tester.js     # Solicitud de acceso beta y registro de testers
│
├── assets/                    # Recursos estáticos modulares
│   ├── css/
│   │   ├── main.css           # Tokens de diseño, reset, navbar, footer y componentes comunes
│   │   ├── home.css           # Estilos de la landing page (hero, métricas, funciones, faq)
│   │   ├── beta.css           # Estilos de solicitud beta, chips, estrellas y muro
│   │   └── legal.css          # Estilos de páginas legales y formularios de cuenta
│   ├── js/
│   │   ├── config.js          # Constantes globales (WhatsApp, endpoints, versión)
│   │   └── beta.js            # Lógica cliente para solicitud de beta y feedback
│   └── images/                # Identidad visual, logos y capturas oficiales
│       ├── logo-app.png
│       └── ...
│
├── database/                  # Scripts SQL y migraciones de base de datos
│   └── supabase_migration_beta_testers.sql
│
├── docs/                      # Documentación técnica y plantillas auxiliares
│   ├── EMAIL_RESET_PASSWORD_TEMPLATE.html
│   ├── PRIVACY_POLICY.md
│   └── PROPUESTA_AJUSTES_UI_UX.md
│
├── public/                    # Archivos estáticos de acceso directo
│   ├── robots.txt
│   └── sitemap.xml
│
├── index.html                 # Página Principal (Landing Page)
├── beta.html                  # Solicitud de Acceso Beta & Feedback
├── terms.html                 # Términos y Condiciones de Uso
├── privacy.html               # Política de Privacidad y Tratamiento de Datos
├── reset-password.html        # Restablecimiento de Contraseña
├── delete-account.html        # Solicitud oficial de Eliminación de Cuenta
├── vercel.json                # Configuración de URLs limpias, rewrites y caché
├── robots.txt                 # Instrucciones para motores de búsqueda
└── sitemap.xml                # Mapa del sitio para SEO e indexación
```

---

## 🚀 Despliegue

Los cambios en la rama `main` se compilan y despliegan automáticamente a través de la infraestructura global de **Vercel**.
