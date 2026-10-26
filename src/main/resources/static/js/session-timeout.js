/*
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : static.js
 * - File      : session-timeout.js
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Session timeout warning and redirect behavior.
 */

// iPOS session idle timeout (native JS, no dependencies).
// Redirects to /login?timeout=true after 30 minutes of inactivity, matching
// server.servlet.session.timeout so shared back-office workstations lock.
(function () {
    'use strict';

    if (!document.querySelector('.sidebar')) return;

    var TIMEOUT_MS = 30 * 60 * 1000;
    var loggedOut = false;
    var timer = null;

    function expire() {
        if (loggedOut) return;
        loggedOut = true;
        try {
            window.location.replace('/login?timeout=true');
        } catch (e) {
            window.location.href = '/login?timeout=true';
        }
    }

    function reset() {
        if (loggedOut) return;
        clearTimeout(timer);
        timer = setTimeout(expire, TIMEOUT_MS);
    }

    var events = ['mousemove', 'mousedown', 'keydown', 'click', 'scroll', 'touchstart'];
    events.forEach(function (eventName) {
        document.addEventListener(eventName, reset, { passive: true });
    });

    reset();
})();
