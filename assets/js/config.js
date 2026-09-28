/**
 * Configuración Global de Brynn Soundbox Web
 */
const BRYNN_CONFIG = {
    appName: "Brynn Soundbox",
    version: "v1.0.7",
    supportWhatsApp: "51910865359",
    supportEmail: "soporte@brynn.app",
    endpoints: {
        registerTester: "/api/register-tester",
        feedback: "/api/feedback",
        authConfig: "/api/auth-config",
        recoverPassword: "/api/recover-password"
    },
    /**
     * Genera un enlace directo a WhatsApp con mensaje codificado
     * @param {string} message 
     * @returns {string} URL completa de wa.me
     */
    getWhatsAppUrl: function(message) {
        if (!message) {
            return `https://wa.me/${this.supportWhatsApp}`;
        }
        return `https://wa.me/${this.supportWhatsApp}?text=${encodeURIComponent(message.trim())}`;
    }
};

window.BRYNN_CONFIG = BRYNN_CONFIG;
