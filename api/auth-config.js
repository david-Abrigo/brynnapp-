// Vercel Serverless Function: Configuración Pública Segura para Clientes Web
// Endpoint: GET /api/auth-config

export default function handler(req, res) {
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS");
    res.setHeader(
        "Access-Control-Allow-Headers",
        "X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, Authorization"
    );
    res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");

    if (req.method === "OPTIONS") {
        return res.status(200).end();
    }

    if (req.method !== "GET") {
        return res.status(405).json({
            success: false,
            error: "Método no permitido. Solo se acepta GET."
        });
    }

    const supabaseUrl = process.env.SUPABASE_URL;
    const supabaseAnonKey = process.env.SUPABASE_ANON_KEY;

    if (!supabaseUrl || !supabaseAnonKey) {
        console.error("Faltan variables de entorno SUPABASE_URL o SUPABASE_ANON_KEY en el servidor.");
        return res.status(500).json({
            success: false,
            error: "Configuración de autenticación no disponible en el servidor."
        });
    }

    return res.status(200).json({
        success: true,
        supabaseUrl,
        supabaseAnonKey
    });
}
