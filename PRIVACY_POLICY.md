# Política de Privacidad - Brynn: Parlante de Pagos

**Última actualización:** 21 de septiembre de 2026

La presente Política de Privacidad describe cómo **Brynn** ("nosotros", "nuestro" o "la aplicación") recopila, utiliza, almacena y protege la información del usuario en relación con nuestra aplicación móvil para Android.

---

## 1. Información que recopilamos

Para proporcionar las funciones del parlante de cobros y sincronización de ventas para negocios, recopilamos:
* **Información de cuenta:** Correo electrónico, nombre de perfil y credenciales de autenticación gestionadas de forma segura a través de Supabase / Google Sign-In.
* **Información de la tienda:** Nombre de la tienda, configuración de sucursales y códigos de vinculación para conectar terminales de trabajadores.
* **Datos de transacciones:** Monto cobrado, nombre o alias del pagador, fecha/hora y notas de la venta. *(Brynn NO recopila números de tarjeta, claves bancarias ni contraseñas financieras).*

---

## 2. Uso del Servicio de Notificaciones (Notification Listener Service)

> **Declaración de Transparencia de Google Play:** Brynn solicita acceso especial al servicio de escucha de notificaciones de Android (`NotificationListenerService`). Este permiso es estrictamente necesario para el funcionamiento principal de la aplicación.

* **Objetivo:** La aplicación analiza en tiempo real las notificaciones recibidas de la aplicación de billetera digital Yape para detectar la confirmación de un pago entrante y anunciarlo en voz alta.
* **Privacidad de mensajes personales:** Brynn **NUNCA** lee, almacena, analiza ni transmite notificaciones de mensajes personales, chats privados (WhatsApp, Telegram, SMS), correos electrónicos ni de ninguna aplicación ajena a las billeteras de pago soportadas.

---

## 3. Servicios de Terceros

* **Supabase:** Almacenamiento seguro en la nube de cuentas de usuario, configuración de sucursales e historial de ventas.
* **Firebase Cloud Messaging (Google FCM):** Transmisión en tiempo real de alertas de cobro entre dispositivos vinculados.
* **RevenueCat & Google Play Billing:** Procesamiento seguro de suscripciones (mensuales y anuales). Los datos financieros son gestionados exclusivamente por Google Play.

---

## 4. Seguridad de los Datos

Implementamos cifrado SSL/TLS en tránsito y políticas de seguridad a nivel de fila (RLS) en la base de datos para asegurar que los datos de tu negocio solo sean accesibles por ti y tus trabajadores autorizados.

---

## 5. Eliminación de Datos

Puedes solicitar la eliminación total de tu cuenta y de todo el historial de transacciones en cualquier momento enviando un correo a nuestro equipo de soporte.

---

## 6. Contacto

* **Correo electrónico de soporte:** soporte.brynn@gmail.com
* **Desarrollador:** Equipo de Desarrollo de Brynn
