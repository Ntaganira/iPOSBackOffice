/*
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : static.js
 * - File      : whatsapp-share.js
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Client-side WhatsApp sharing for receipts.
 */

(function () {
    'use strict';

    var STATUS_TIMEOUT = 6000;

    function notify(button, message, type) {
        if (typeof window.showToast === 'function') {
            window.showToast(message, type || 'info');
            return;
        }
        var status = document.querySelector('[data-whatsapp-status]');
        if (!status) return;
        status.textContent = message;
        status.dataset.type = type || 'info';
        status.hidden = false;
        clearTimeout(status._hideTimer);
        status._hideTimer = setTimeout(function () {
            status.hidden = true;
        }, STATUS_TIMEOUT);
    }

    function setBusy(button, busy) {
        if (busy) {
            button.dataset.label = button.innerHTML;
            button.disabled = true;
            button.textContent = button.getAttribute('data-busy-text') || 'Preparing PDF...';
        } else {
            button.disabled = false;
            if (button.dataset.label !== undefined) {
                button.innerHTML = button.dataset.label;
                delete button.dataset.label;
            }
        }
    }

    function pdfFileName(button) {
        return button.getAttribute('data-file-name') || 'Receipt.pdf';
    }

    function canShareFiles(file) {
        try {
            return typeof navigator.share === 'function'
                && typeof navigator.canShare === 'function'
                && navigator.canShare({ files: [file] });
        } catch (error) {
            return false;
        }
    }

    function fallbackUrl(button) {
        var url = button.getAttribute('data-whatsapp-url');
        if (url) return url;
        var endpoint = button.getAttribute('data-whatsapp-endpoint');
        if (!endpoint) return null;
        var selector = button.getAttribute('data-whatsapp-phone');
        var input = selector ? document.querySelector(selector) : null;
        var phone = input && input.value ? input.value.trim() : '';
        return endpoint + (phone ? '?phone=' + encodeURIComponent(phone) : '');
    }

    function download(blob, fileName) {
        var url = URL.createObjectURL(blob);
        var link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(function () {
            URL.revokeObjectURL(url);
        }, 10000);
    }

    async function fetchPdf(button) {
        var response = await fetch(button.getAttribute('data-pdf-url'), {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/pdf' }
        });
        if (!response.ok) {
            throw new Error('The receipt PDF could not be generated.');
        }
        var type = response.headers.get('Content-Type') || '';
        if (type.indexOf('application/pdf') === -1) {
            throw new Error('Your session may have expired. Refresh the page and try again.');
        }
        return response.blob();
    }

    function prepare(button) {
        if (!button || !button.hasAttribute('data-whatsapp-pdf')) return;
        if (!button.getAttribute('data-pdf-url')) return;
        button._pdfPromise = fetchPdf(button);
        button._pdfPromise.catch(function () {
            button._pdfPromise = null;
        });
    }

    window.iposPrepareWhatsAppPdf = prepare;

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-whatsapp-pdf]').forEach(prepare);
    });

    document.addEventListener('click', async function (event) {
        var button = event.target.closest('[data-whatsapp-pdf]');
        if (!button) return;
        event.preventDefault();
        if (button.disabled) return;

        var fileName = pdfFileName(button);
        var title = button.getAttribute('data-share-title') || document.title;
        var text = button.getAttribute('data-share-text') || '';
        var fallback = fallbackUrl(button);
        var probe = new File([new Blob([], { type: 'application/pdf' })], fileName, { type: 'application/pdf' });
        var supported = canShareFiles(probe);
        var fallbackWindow = null;

        if (!supported && fallback) {
            fallbackWindow = window.open(fallback, '_blank');
            if (fallbackWindow) {
                fallbackWindow.opener = null;
            } else {
                notify(button, 'Allow pop-ups for this site to open WhatsApp. The PDF will be downloaded so you can attach it.', 'warning');
            }
        }

        setBusy(button, true);
        try {
            var blob = await (button._pdfPromise || fetchPdf(button));
            var file = new File([blob], fileName, { type: 'application/pdf' });

            if (supported) {
                try {
                    await navigator.share({ files: [file], title: title, text: text });
                } catch (error) {
                    if (error && error.name === 'AbortError') return;
                    download(blob, fileName);
                    notify(button, 'The share sheet could not open. The receipt PDF was downloaded; attach it in WhatsApp.', 'warning');
                }
            } else {
                download(blob, fileName);
                if (fallbackWindow) {
                    notify(button, 'Receipt PDF downloaded. Attach it to the WhatsApp message that just opened.', 'info');
                } else {
                    notify(button, 'Receipt PDF downloaded. Open WhatsApp and attach it to your message.', 'info');
                }
            }
        } catch (error) {
            notify(button, error.message || 'The receipt PDF could not be shared.', 'error');
        } finally {
            setBusy(button, false);
        }
    });
})();
