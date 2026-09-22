// Vercel Serverless Function: API Backend para Recuperación de Contraseña
// Endpoint: POST /api/recover-password

const SUPABASE_URL = "https://sljznppbjxfbgzasjnyq.supabase.co";
const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNsanpucHBianhmYmd6YXNqbnlxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODgzOTM1NzQsImV4cCI6MjEwMzk2OTU3NH0.bIH3x2xsPjxgU0hTPWAg4IwFjKnsUW7RtdpnRM1DB1o";
const REDIRECT_URL = "https://brynnapp.vercel.app/reset-password";

export default async function handler(req, res) {
    // Configurar cabeceras CORS
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS,PATCH,DELETE,POST,PUT");
    res.setHeader(
        "Access-Control-Allow-Headers",
        "X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, Authorization, apikey"
    );

    if (req.method === "OPTIONS") {
        return res.status(200).end();
    }

    if (req.method !== "POST") {
        return res.status(405).json({
            success: false,
            error: "Método no permitido. Solo se acepta POST."
        });
    }

    try {
        let body = req.body;
        if (typeof body === "string") {
            try {
                body = JSON.parse(body);
            } catch (e) {
                // mantener como string si falla
            }
        }

        const email = body?.email?.trim();

        if (!email || !email.includes("@")) {
            return res.status(400).json({
                success: false,
                error: "Debes proporcionar un correo electrónico válido."
            });
        }

        // Llamar al endpoint de recuperación de Supabase Auth
        const supabaseEndpoint = `${SUPABASE_URL}/auth/v1/recover?redirect_to=${encodeURIComponent(REDIRECT_URL)}`;

        const response = await fetch(supabaseEndpoint, {
            method: "POST",
            headers: {
                "apikey": SUPABASE_ANON_KEY,
                "Authorization": `Bearer ${SUPABASE_ANON_KEY}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email: email })
        });

        if (!response.ok) {
            const errorText = await response.text();
            let errorMessage = "No se pudo enviar el correo de recuperación.";
            try {
                const parsed = JSON.parse(errorText);
                errorMessage = parsed.msg || parsed.error_description || parsed.message || errorMessage;
            } catch (e) {
                errorMessage = errorText || errorMessage;
            }

            return res.status(response.status).json({
                success: false,
                error: errorMessage
            });
        }

        return res.status(200).json({
            success: true,
            message: "Correo de recuperación enviado exitosamente a " + email,
            redirect_to: REDIRECT_URL
        });

    } catch (error) {
        console.error("Error en recover-password handler:", error);
        return res.status(500).json({
            success: false,
            error: "Error interno del servidor: " + (error.message || "Desconocido")
        });
    }
}
