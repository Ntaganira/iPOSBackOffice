package com.ntaganira.heritier.ipos.backoffice.config;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.config
 * - File      : ThemeInterceptor.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Loads the active shop palette into every rendered page.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.entity.ShopSetting;
import com.ntaganira.heritier.ipos.backoffice.service.SettingsService;
import com.ntaganira.heritier.ipos.backoffice.util.ThemePalette;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class ThemeInterceptor implements HandlerInterceptor {

    public static final String SHOP_SETTINGS_ATTRIBUTE = ThemeInterceptor.class.getName() + ".shopSettings";

    private final SettingsService settingsService;

    public ThemeInterceptor(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(SHOP_SETTINGS_ATTRIBUTE, settingsService.get());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        if (modelAndView == null || !modelAndView.hasView()
                || modelAndView.getViewName() == null
                || modelAndView.getViewName().startsWith("redirect:")) {
            return;
        }
        Object attribute = request.getAttribute(SHOP_SETTINGS_ATTRIBUTE);
        if (!(attribute instanceof ShopSetting settings)) {
            return;
        }
        ThemePalette palette = ThemePalette.from(
                settings.getThemePrimary(),
                settings.getThemeSecondary(),
                settings.getThemeBackground(),
                settings.getThemeSurface());
        modelAndView.addObject("themePalette", palette);
        modelAndView.addObject("colorPrimary", palette.primary());
        modelAndView.addObject("colorSecondary", palette.secondary());
        modelAndView.addObject("colorBackground", palette.background());
        modelAndView.addObject("colorSurface", palette.surface());
    }
}
