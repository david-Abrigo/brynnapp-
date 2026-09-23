// Vercel Serverless Function: Recepción y Registro de Comentarios de Testers Beta
// Endpoint: POST /api/feedback

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY;
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || SUPABASE_ANON_KEY;

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
                // mantener como objeto o string si no es parseable
            }
        }

        const name = (body?.name || "").trim() || "Tester anónimo";
        const email = (body?.email || "").trim();
        const feedbackType = (body?.feedbackType || "general").trim();
        const rating = parseInt(body?.rating, 10) || 5;
        const deviceModel = (body?.deviceModel || "").trim();
        const message = (body?.message || "").trim();

        if (!message) {
            return res.status(400).json({
                success: false,
                error: "Por favor escribe un mensaje o comentario detallando tu experiencia."
            });
        }

        const feedbackRecord = {
            name,
            email: email || null,
            feedback_type: feedbackType,
            rating,
            device_model: deviceModel || null,
            message,
            created_at: new Date().toISOString()
        };

        // Si Supabase está configurado, intentar persistir en la tabla 'beta_feedback'
        if (SUPABASE_URL && SUPABASE_SERVICE_ROLE_KEY) {
            try {
                const supabaseEndpoint = `${SUPABASE_URL}/rest/v1/beta_feedback`;
                const response = await fetch(supabaseEndpoint, {
                    method: "POST",
                    headers: {
                        "apikey": SUPABASE_SERVICE_ROLE_KEY,
                        "Authorization": `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
                        "Content-Type": "application/json",
                        "Prefer": "return=minimal"
                    },
                    body: JSON.stringify([feedbackRecord])
                });

                if (!response.ok) {
                    const errTxt = await response.text();
                    console.warn("Aviso: Supabase beta_feedback no pudo guardar directamente (la tabla puede requerir migración):", errTxt);
                }
            } catch (dbErr) {
                console.warn("Excepción al contactar Supabase para feedback:", dbErr.message);
            }
        } else {
            console.log("Feedback recibido (sin base de datos Supabase conectada):", feedbackRecord);
        }

        return res.status(200).json({
            success: true,
            message: "¡Comentario recibido exitosamente! Muchas gracias por colaborar en la fase de pruebas de Brynn.",
            data: feedbackRecord
        });

    } catch (error) {
        console.error("Error en feedback handler:", error);
        return res.status(500).json({
            success: false,
            error: "Error interno al procesar el comentario: " + (error.message || "Desconocido")
        });
    }
}
