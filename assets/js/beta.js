/**
 * Lógica de Solicitud de Acceso Beta y Envío de Feedback
 * Brynn Soundbox
 */

// Clave para guardar en almacenamiento local de respaldo
const LOCAL_FEEDBACK_KEY = 'brynn_beta_feedbacks_v1';
let currentRating = 5;
let hoveredRating = 0;

const RATING_TEXTS = {
    1: "1 / 5 - Muy deficiente",
    2: "2 / 5 - Necesita mejoras importantes",
    3: "3 / 5 - Regular, funciona a veces",
    4: "4 / 5 - Buena, estable y rápida",
    5: "5 / 5 - ¡Excelente experiencia!"
};

// ── Manejo de Categorías de Feedback ──
function selectFeedbackCategory(chipEl, catKey) {
    document.querySelectorAll('.cat-chip').forEach(c => c.classList.remove('selected'));
    chipEl.classList.add('selected');
    const input = document.getElementById('fbTypeInput');
    if (input) input.value = catKey;
}

// ── Manejo de Calificación por Estrellas ──
function setRating(val) {
    currentRating = val;
    const input = document.getElementById('fbRatingInput');
    if (input) input.value = val;
    updateStarsVisual(val);
    const label = document.getElementById('starTextLabel');
    if (label) label.textContent = RATING_TEXTS[val] || `${val} / 5`;
}

function hoverRating(val) {
    hoveredRating = val;
    updateStarsVisual(val);
    const label = document.getElementById('starTextLabel');
    if (label) label.textContent = RATING_TEXTS[val] || `${val} / 5`;
}

function resetRatingHover() {
    hoveredRating = 0;
    updateStarsVisual(currentRating);
    const label = document.getElementById('starTextLabel');
    if (label) label.textContent = RATING_TEXTS[currentRating] || `${currentRating} / 5`;
}

function updateStarsVisual(ratingVal) {
    const starItems = document.querySelectorAll('.star-item');
    starItems.forEach(star => {
        const itemVal = parseInt(star.getAttribute('data-val'), 10);
        if (itemVal <= ratingVal) {
            star.classList.add('active');
        } else {
            star.classList.remove('active');
        }
    });
}

// ── Envío de Feedback ──
async function handleFeedbackSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSubmitFeedback');
    const name = document.getElementById('fbName').value.trim();
    const device = document.getElementById('fbDevice').value.trim();
    const email = document.getElementById('fbEmail').value.trim();
    const message = document.getElementById('fbMessage').value.trim();
    const type = document.getElementById('fbTypeInput').value || 'general';
    const rating = parseInt(document.getElementById('fbRatingInput').value, 10) || 5;

    if (!message) {
        alert("Por favor detalla tu experiencia antes de enviar.");
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span style="display:inline-block;animation:spin 1s linear infinite;">⏳</span> Enviando...';

    const feedbackPayload = {
        name,
        deviceModel: device,
        email,
        message,
        feedbackType: type,
        rating,
        date: new Date().toISOString()
    };

    try {
        await fetch(window.BRYNN_CONFIG?.endpoints?.feedback || '/api/feedback', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(feedbackPayload)
        });
    } catch (err) {
        console.warn('API de feedback en modo local/fallback:', err);
    }

    saveFeedbackLocally(feedbackPayload);
    addFeedbackToDOM(feedbackPayload, true);

    // Preparar mensaje de WhatsApp
    const waText = `*Nuevo Feedback Brynn Beta*\n👤 *Nombre:* ${name}\n📱 *Teléfono:* ${device || 'No especificado'}\n⭐ *Calificación:* ${rating}/5\n💬 *Mensaje:* ${message}`;
    const waUrl = window.BRYNN_CONFIG ? window.BRYNN_CONFIG.getWhatsAppUrl(waText) : `https://wa.me/51910865359?text=${encodeURIComponent(waText)}`;
    
    const waLink = document.getElementById('fbWaDirectLink');
    if (waLink) waLink.href = waUrl;

    document.getElementById('betaFeedbackForm').style.display = 'none';
    const successCard = document.getElementById('fbSuccessCard');
    if (successCard) successCard.style.display = 'block';

    btn.disabled = false;
    btn.innerHTML = 'Enviar Comentario';
}

function saveFeedbackLocally(fb) {
    try {
        const stored = JSON.parse(localStorage.getItem(LOCAL_FEEDBACK_KEY) || '[]');
        stored.unshift(fb);
        localStorage.setItem(LOCAL_FEEDBACK_KEY, JSON.stringify(stored.slice(0, 30)));
    } catch (e) {}
}

function loadLocalFeedback() {
    try {
        const stored = JSON.parse(localStorage.getItem(LOCAL_FEEDBACK_KEY) || '[]');
        stored.forEach(fb => addFeedbackToDOM(fb, false));
        renderCommentsCount();
    } catch (e) {}
}

function addFeedbackToDOM(fb, prepend = false) {
    const list = document.getElementById('testerCommentsList');
    if (!list) return;

    const initials = (fb.name || "T")
        .split(" ")
        .slice(0, 2)
        .map(w => w[0]?.toUpperCase())
        .join("") || "T";

    const starsString = "★".repeat(fb.rating || 5) + "☆".repeat(5 - (fb.rating || 5));
    const typeLabel = {
        general: "⭐ General",
        voice: "🔊 Bot de Voz",
        detection: "⚡ Detección",
        bug: "🐛 Reporte de Falla",
        idea: "💡 Sugerencia"
    }[fb.feedbackType] || "⭐ Experiencia";

    const item = document.createElement('div');
    item.className = 'tester-item';
    item.innerHTML = `
        <div class="tester-item-header">
            <div class="tester-user-info">
                <div class="tester-avatar">${escapeHtml(initials)}</div>
                <div>
                    <div class="tester-name">${escapeHtml(fb.name || "Comercio Tester")}</div>
                    <div class="tester-device">${escapeHtml(fb.deviceModel || "Android")}</div>
                </div>
            </div>
            <div class="tester-meta">
                <div class="tester-stars">${starsString}</div>
                <div class="tester-time">Reciente</div>
            </div>
        </div>
        <div class="tester-body">${escapeHtml(fb.message)}</div>
        <span class="tester-badge-tag">${typeLabel}</span>
    `;

    if (prepend && list.firstChild) {
        list.insertBefore(item, list.firstChild);
    } else {
        list.appendChild(item);
    }
}

function renderCommentsCount() {
    const list = document.getElementById('testerCommentsList');
    const badge = document.getElementById('testerCountBadge');
    if (!list || !badge) return;
    const count = list.querySelectorAll('.tester-item').length;
    badge.textContent = `${count} Comentarios Verificados`;
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// ── Validación en Vivo de Gmail ──
function validateGmailLive(input) {
    const errorEl = document.getElementById('gmailErrorMsg');
    const val = input.value.trim().toLowerCase();
    if (!val) {
        if (errorEl) errorEl.style.display = 'none';
        return;
    }
    const isGmail = val.endsWith('@gmail.com') || val.endsWith('@googlemail.com');
    if (val.includes('@') && !isGmail) {
        if (errorEl) errorEl.style.display = 'block';
    } else {
        if (errorEl) errorEl.style.display = 'none';
    }
}

// ── Envío de Solicitud de Tester & Redirección a WhatsApp ──
async function handleTesterRegisterSubmit(e) {
    e.preventDefault();
    const emailInput = document.getElementById('testerGmailInput');
    const nameInput = document.getElementById('testerNameInput');
    const btn = document.getElementById('btnSubmitTester');
    const errorEl = document.getElementById('gmailErrorMsg');

    const email = emailInput.value.trim().toLowerCase();
    const name = nameInput.value.trim();

    if (!name) {
        alert("Por favor ingresa tu nombre o negocio");
        nameInput.focus();
        return;
    }

    const isGmail = email.endsWith('@gmail.com') || email.endsWith('@googlemail.com');
    if (!isGmail) {
        if (errorEl) errorEl.style.display = 'block';
        emailInput.focus();
        return;
    }
    if (errorEl) errorEl.style.display = 'none';

    btn.disabled = true;
    btn.innerHTML = '<span style="display:inline-block;animation:spin 1s linear infinite;">⏳</span> Registrando solicitud...';

    try {
        const deviceHint = /Android/i.test(navigator.userAgent) ? "Android" : (/iPhone|iPad/i.test(navigator.userAgent) ? "iOS" : "Navegador Web");
        await fetch(window.BRYNN_CONFIG?.endpoints?.registerTester || '/api/register-tester', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, name, device: deviceHint })
        });
    } catch (err) {
        console.warn('Registro local de tester:', err);
    }

    try {
        localStorage.setItem('brynn_registered_tester_gmail', email);
        localStorage.setItem('brynn_registered_tester_name', name);
    } catch (e) {}

    // Mensaje automático por WhatsApp hacia el desarrollador con correo y nombre
    const waMsg = `Hola, he solicitado acceso a la prueba beta de Brynn con mi correo de Gmail: ${email} (Nombre/Negocio: ${name}). Por favor, habilítame en Google Play para comenzar a probar. ¡Muchas gracias!`;
    const waUrl = window.BRYNN_CONFIG ? window.BRYNN_CONFIG.getWhatsAppUrl(waMsg) : `https://wa.me/51910865359?text=${encodeURIComponent(waMsg)}`;

    showRegisteredTesterBox(email, waUrl);

    // Abrir WhatsApp automáticamente
    window.open(waUrl, '_blank');
}

function showRegisteredTesterBox(email, directWaUrl) {
    const form = document.getElementById('testerRegisterForm');
    const box = document.getElementById('testerLinkBox');
    const emailDisplay = document.getElementById('registeredEmailDisplay');
    const waLink = document.getElementById('waTesterConfirmLink');

    if (form) form.style.display = 'none';
    if (emailDisplay) emailDisplay.textContent = email;
    if (box) box.style.display = 'block';

    if (waLink) {
        const savedName = localStorage.getItem('brynn_registered_tester_name') || 'Tester';
        const fallbackMsg = `Hola, he solicitado acceso a la prueba beta de Brynn con mi correo de Gmail: ${email} (Nombre/Negocio: ${savedName}). Por favor, habilítame en Google Play para comenzar a probar. ¡Muchas gracias!`;
        const url = directWaUrl || (window.BRYNN_CONFIG ? window.BRYNN_CONFIG.getWhatsAppUrl(fallbackMsg) : `https://wa.me/51910865359?text=${encodeURIComponent(fallbackMsg)}`);
        waLink.href = url;
    }
}

function checkPreviousTesterRegistration() {
    try {
        const savedEmail = localStorage.getItem('brynn_registered_tester_gmail');
        if (savedEmail) {
            showRegisteredTesterBox(savedEmail);
        }
    } catch (e) {}
}

document.addEventListener('DOMContentLoaded', () => {
    loadLocalFeedback();
    checkPreviousTesterRegistration();
});
