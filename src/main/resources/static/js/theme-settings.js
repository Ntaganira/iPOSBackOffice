/*
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : static.js
 * - File      : theme-settings.js
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Theme preset selection and live POS preview interactions.
 */

document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('theme-settings-form');
    const preview = document.getElementById('theme-preview');
    const customCard = document.getElementById('theme-custom-card');
    const customBadge = document.getElementById('theme-custom-badge');
    if (!form || !preview) return;

    const keys = ['primary', 'secondary', 'background', 'surface'];
    const inputs = {};
    const pickers = {};
    const labels = {};
    const presets = Array.from(form.querySelectorAll('[data-theme-preset]'));

    keys.forEach(function (key) {
        inputs[key] = form.querySelector('[data-theme-input="' + key + '"]');
        pickers[key] = form.querySelector('[data-theme-color="' + key + '"]');
        labels[key] = form.querySelector('[data-theme-value="' + key + '"]');
    });

    const hex = function (value) {
        return /^#[0-9a-f]{6}$/i.test(value || '') ? value.toUpperCase() : null;
    };

    const readColors = function () {
        const colors = {};
        keys.forEach(function (key) {
            colors[key] = hex(inputs[key].value) || hex(pickers[key].value) || '#000000';
        });
        return colors;
    };

    const inkFor = function (value) {
        const raw = (value || '#FFFFFF').replace('#', '');
        const channel = function (offset) {
            const value = parseInt(raw.substring(offset, offset + 2), 16) / 255;
            return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
        };
        const luminance = (0.2126 * channel(0)) + (0.7152 * channel(2)) + (0.0722 * channel(4));
        return luminance > 0.46 ? '#10231F' : '#FFFFFF';
    };

    const applyPreview = function (colors) {
        preview.style.setProperty('--preview-primary', colors.primary);
        preview.style.setProperty('--preview-secondary', colors.secondary);
        preview.style.setProperty('--preview-background', colors.background);
        preview.style.setProperty('--preview-surface', colors.surface);
        preview.style.setProperty('--preview-ink', inkFor(colors.surface));
        preview.style.setProperty('--preview-bg-ink', inkFor(colors.background));
        preview.style.setProperty('--preview-primary-ink', inkFor(colors.primary));
        preview.style.setProperty('--preview-secondary-ink', inkFor(colors.secondary));
    };

    const updateActive = function () {
        const current = readColors();
        let matchedPreset = false;
        presets.forEach(function (button) {
            const active = keys.every(function (key) {
                return current[key] === hex(button.getAttribute('data-' + key));
            });
            if (active) matchedPreset = true;
            button.classList.toggle('is-active', active);
            button.setAttribute('aria-pressed', active ? 'true' : 'false');
        });
        if (customCard) customCard.classList.toggle('is-active', !matchedPreset);
        if (customBadge) customBadge.hidden = matchedPreset;
    };

    const setColors = function (colors) {
        keys.forEach(function (key) {
            const value = hex(colors[key]) || hex(pickers[key].value) || '#000000';
            inputs[key].value = value;
            pickers[key].value = value;
            if (labels[key]) labels[key].textContent = value;
        });
        applyPreview(readColors());
        updateActive();
    };

    presets.forEach(function (button) {
        const previewPreset = function () {
            applyPreview({
                primary: button.getAttribute('data-primary'),
                secondary: button.getAttribute('data-secondary'),
                background: button.getAttribute('data-background'),
                surface: button.getAttribute('data-surface')
            });
        };
        button.addEventListener('mouseenter', previewPreset);
        button.addEventListener('focus', previewPreset);
        button.addEventListener('mouseleave', function () { applyPreview(readColors()); });
        button.addEventListener('blur', function () { applyPreview(readColors()); });
        button.addEventListener('click', function () {
            setColors({
                primary: button.getAttribute('data-primary'),
                secondary: button.getAttribute('data-secondary'),
                background: button.getAttribute('data-background'),
                surface: button.getAttribute('data-surface')
            });
        });
    });

    keys.forEach(function (key) {
        pickers[key].addEventListener('input', function () {
            inputs[key].value = pickers[key].value.toUpperCase();
            if (labels[key]) labels[key].textContent = inputs[key].value;
            applyPreview(readColors());
            updateActive();
        });
    });

    setColors(readColors());
});
