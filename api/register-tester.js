// Vercel Serverless Function: Registro de Correos Gmail para Pruebas en Google Play
// Endpoint: POST /api/register-tester

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY;
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || SUPABASE_ANON_KEY;
const TEST_LINK = "https://play.google.com/apps/internaltest/4701652003026527529";

export default async function handler(req, res) {
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS,POST");
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
                // mantener como objeto o fallback
            }
        }

        const email = (body?.email || "").trim().toLowerCase();
        const name = (body?.name || "").trim() || "Tester Google Play";

        if (!email) {
            return res.status(400).json({
                success: false,
                error: "El correo electrónico es requerido."
            });
        }

        // Validación estricta: Solo cuentas de Gmail / Googlemail para Google Play Console
        const isGmail = email.endsWith("@gmail.com") || email.endsWith("@googlemail.com");
        if (!isGmail) {
            return res.status(400).json({
                success: false,
                error: "Debes ingresar una cuenta de Gmail (@gmail.com) registrada en Google Play Store."
            });
        }

        const record = {
            name,
            email,
            feedback_type: "tester_signup",
            rating: 5,
            device_model: body?.device || "Google Play Tester",
            message: `Solicitud de acceso a prueba interna Google Play para: ${email}`,
            created_at: new Date().toISOString()
        };

        // Si Supabase está disponible, registrar en beta_feedback o beta_testers
        if (SUPABASE_URL && SUPABASE_SERVICE_ROLE_KEY) {
            try {
                const supabaseEndpoint = `${SUPABASE_URL}/rest/v1/beta_feedback`;
                await fetch(supabaseEndpoint, {
                    method: "POST",
                    headers: {
                        "apikey": SUPABASE_SERVICE_ROLE_KEY,
                        "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
                        "Content-Type": "application/json",
                        "Prefer": "return=minimal"
                    },
                    body: JSON.stringify([record])
                });
            } catch (dbErr) {
                console.warn("Aviso al guardar tester en Supabase:", dbErr.message);
            }
        }

        return res.status(200).json({
            success: true,
            message: "¡Correo Gmail registrado con éxito!",
            testLink: TEST_LINK,
            email
        });

    } catch (error) {
        console.error("Error en register-tester handler:", error);
        return res.status(500).json({
            success: false,
            error: "Error interno al procesar la solicitud: " + (error.message || "Desconocido"),
            testLink: TEST_LINK
        });
    }
}
