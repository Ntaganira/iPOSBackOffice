package com.ntaganira.heritier.ipos.backoffice;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice
 * - File      : ThemePaletteTest.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Contrast tests for generated shop theme text colors.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.util.ThemePalette;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThemePaletteTest {

    private static final List<String[]> PRESETS = List.of(
            new String[]{"#BE5103", "#FFCE1B", "#069494", "#B7410E"},
            new String[]{"#D4AF37", "#BDB76B", "#FDFBD4", "#CE8946"},
            new String[]{"#FDFBD4", "#D47E30", "#8D5A2B", "#825E34"},
            new String[]{"#FFFF66", "#FFE566", "#D6D58B", "#B3B347"},
            new String[]{"#BE5103", "#8C4C1F", "#544823", "#332216"},
            new String[]{"#89F336", "#FFFC30", "#4FE0CB", "#00BF33"},
            new String[]{"#B8E3E9", "#93B1B5", "#4F7C82", "#0B2E33"},
            new String[]{"#FFDBBB", "#BADDFF", "#BAFFF5", "#496580"},
            new String[]{"#CBBD93", "#FFF5B8", "#FFB16E", "#CCA25A"},
            new String[]{"#B298E7", "#B8E3E9", "#F5B8D5", "#F9BEDD"}
    );

    @Test
    void everyPresetProducesReadableTextColors() {
        for (String[] preset : PRESETS) {
            ThemePalette palette = ThemePalette.from(preset[0], preset[1], preset[2], preset[3]);

            assertContrast(palette.backgroundInk(), palette.background(), 4.5);
            assertContrast(palette.backgroundMuted(), palette.background(), 4.6);
            assertContrast(palette.surfaceInk(), palette.surface(), 4.5);
            assertContrast(palette.surfaceMuted(), palette.surface(), 4.6);
            assertContrast(palette.surfaceSubtle(), palette.surface(), 3.2);
            assertContrast(palette.primaryInk(), palette.primary(), 4.5);
            assertContrast(palette.secondaryInk(), palette.secondary(), 4.5);
            assertContrast(palette.backgroundAccent(), palette.background(), 4.6);
            assertContrast(palette.surfaceAccent(), palette.surface(), 4.8);
        }
    }

    @Test
    void darkPresetUsesLightTextAndLightPrimaryUsesDarkButtonText() {
        ThemePalette winter = ThemePalette.from("#B8E3E9", "#93B1B5", "#4F7C82", "#0B2E33");
        assertThat(winter.surfaceInk()).isEqualTo("#FFFFFF");
        assertThat(winter.primaryInk()).isEqualTo("#0B1220");

        ThemePalette spiced = ThemePalette.from("#FDFBD4", "#D47E30", "#8D5A2B", "#825E34");
        assertThat(spiced.surfaceInk()).isEqualTo("#FFFFFF");
        assertThat(spiced.primaryInk()).isEqualTo("#0B1220");
    }

    @Test
    void brightPrimaryKeepsItsHueWhenAccentIsDarkened() {
        ThemePalette palette = ThemePalette.from("#FF0000", "#4FB8A8", "#FFFFFF", "#FFFFFF");

        assertContrast(palette.surfaceAccent(), palette.surface(), 4.8);
        assertThat(red(palette.surfaceAccent())).isGreaterThan(red(palette.surfaceInk()) + 100);
        assertThat(red(palette.surfaceAccent())).isGreaterThan(200);
        assertThat(red(palette.surfaceAccent())).isGreaterThan(green(palette.surfaceAccent()));
    }

    @Test
    void invalidValuesFallBackToTheDefaultPalette() {
        ThemePalette palette = ThemePalette.from("teal", null, "#12345", "#FAFAFA");

        assertThat(palette.primary()).isEqualTo("#268071");
        assertThat(palette.secondary()).isEqualTo("#4FB8A8");
        assertThat(palette.background()).isEqualTo("#FFFFFF");
        assertThat(palette.surface()).isEqualTo("#FAFAFA");
    }

    private void assertContrast(String foreground, String background, double minimum) {
        assertThat(ThemePalette.contrast(foreground, background))
                .as("%s on %s", foreground, background)
                .isGreaterThanOrEqualTo(minimum);
    }

    private int red(String hex) {
        return Integer.parseInt(hex.substring(1, 3), 16);
    }

    private int green(String hex) {
        return Integer.parseInt(hex.substring(3, 5), 16);
    }
}
