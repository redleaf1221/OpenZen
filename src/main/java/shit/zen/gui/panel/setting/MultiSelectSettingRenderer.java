package shit.zen.gui.panel.setting;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.gui.PanelClickGui;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.TextGlow;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.MultiSelectSetting;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class MultiSelectSettingRenderer
extends ClientBase
implements SettingRenderer {
    private static final int COLOR_SELECTED = new Color(76, 175, 80).getRGB();
    private static final int COLOR_SELECTED_GLOW = new Color(76, 175, 80, 100).getRGB();
    private final Map<String, Float> hoverAnimations = new HashMap<>();
    private final Map<String, Float> selectedAnimations = new HashMap<>();

    @Override
    public int render(GuiGraphics guiGraphics, Setting<?> setting, int n, int n2, int n3, int n4, int n5, float f, float f2) {
        if (!(setting instanceof MultiSelectSetting multiSelectSetting)) {
            return 0;
        }
        FontRenderer fontRenderer = FontPresets.axiformaRegular(14.0f * f2);
        float f3 = (float)n2 + 20.0f * f2 / 2.0f - fontRenderer.getMetrics().capHeight() / 2.0f;
        TextGlow.drawGlowText(multiSelectSetting.getName(), n, f3, fontRenderer, this.applyAlpha(-1, f), this.applyAlpha(new Color(255, 255, 255, 120).getRGB(), f), 8.0f * f2);
        int n6 = n2 + Math.round(20.0f * f2);
        int n7 = Math.round(20.0f * f2);
        int n8 = Math.round(10.0f * f2);
        int n9 = Math.round(5.0f * f2);
        for (String string : multiSelectSetting.getOptions()) {
            int n10;
            boolean bl = multiSelectSetting.isSelected(string);
            boolean bl2 = n4 >= n && n4 <= n + n3 && n5 >= n6 && n5 <= n6 + n7;
            this.updateAnimation(this.hoverAnimations, string, bl2, 0.28f);
            this.updateAnimation(this.selectedAnimations, string, bl, 0.25f);
            float f4 = this.hoverAnimations.getOrDefault(string, Float.valueOf(0.0f)).floatValue();
            float f5 = this.selectedAnimations.getOrDefault(string, Float.valueOf(0.0f)).floatValue();
            int n11 = n + n3 - n8 - n9;
            int n12 = n6 + (n7 - n8) / 2;
            if (f5 > 0.01f) {
                n10 = this.applyAlpha(COLOR_SELECTED_GLOW, f * f5);
                RenderUtil.drawRoundedRect(guiGraphics.pose(), (float)n11 - f2, (float)n12 - f2, (float)n8 + 2.0f * f2, (float)n8 + 2.0f * f2, 2.0f * f2, n10);
            }
            RenderUtil.drawRoundedRect(guiGraphics.pose(), n11, n12, n8, n8, 2.0f * f2, this.applyAlpha(-5592406, f));
            if ((double)f5 > 0.01) {
                float f6 = (1.0f - f5) * ((float)n8 / 2.0f);
                RenderUtil.drawRoundedRect(guiGraphics.pose(), (float)n11 + f6, (float)n12 + f6, (float)n8 - f6 * 2.0f, (float)n8 - f6 * 2.0f, f2, this.applyAlpha(COLOR_SELECTED, f));
            }
            n10 = bl ? -1 : -5592406;
            int n13 = this.lerpColor(n10, -1, f4);
            FontRenderer fontRenderer2 = FontPresets.axiformaRegular(13.0f * f2);
            float f7 = (float)n6 + (float)n7 / 2.0f - fontRenderer2.getMetrics().capHeight() / 2.0f;
            if (f5 > 0.01f) {
                int n14 = this.applyAlpha(COLOR_SELECTED_GLOW, f * f5);
                TextGlow.drawGlowText(string, n + n9, f7, fontRenderer2, this.applyAlpha(n13, f), n14, 8.0f * f2 * f5);
            } else {
                GlHelper.drawText(string, n + n9, f7, fontRenderer2, this.applyAlpha(n13, f));
            }
            n6 += n7;
        }
        return (multiSelectSetting.getOptions().size() + 1) * n7;
    }

    private void updateAnimation(Map<String, Float> map, String string, boolean bl, float f) {
        float f2 = map.getOrDefault(string, Float.valueOf(0.0f)).floatValue();
        float f3 = bl ? 1.0f : 0.0f;
        f2 = Math.abs(f3 - f2) > 0.01f ? LerpUtil.smoothLerp(f2, f3, f) : f3;
        map.put(string, Float.valueOf(f2));
    }

    @Override
    public boolean onClick(Setting<?> setting, int n, int n2, int n3, int n4, int n5, int n6, float f) {
        if (!(setting instanceof MultiSelectSetting multiSelectSetting) || n6 != 0) {
            return false;
        }
        int n7 = n2 + Math.round(20.0f * f);
        int n8 = Math.round(20.0f * f);
        for (String string : multiSelectSetting.getOptions()) {
            if (n4 >= n && n4 <= n + n3 && n5 >= n7 && n5 <= n7 + n8) {
                if (multiSelectSetting.isSelected(string)) {
                    multiSelectSetting.getValue().remove(string);
                } else {
                    multiSelectSetting.getValue().add(string);
                }
                boolean bl = multiSelectSetting.isSelected(string);
                PanelClickGui.panelClickGui.addToast(string + (bl ? " enabled" : " disabled"));
                return true;
            }
            n7 += n8;
        }
        return false;
    }

    @Override
    public boolean supports(Setting<?> setting) {
        return setting instanceof MultiSelectSetting;
    }

    @Override
    public int getHeight(Setting<?> setting, float f) {
        if (!(setting instanceof MultiSelectSetting multiSelectSetting)) {
            return 0;
        }
        return Math.round((float)((multiSelectSetting.getOptions().size() + 1) * 20) * f);
    }

    @Override
    public void onMouseRelease(double d, double d2, int n) {
    }

    private int applyAlpha(int n, float f) {
        int n2 = n >> 24 & 0xFF;
        int n3 = (int)((float)n2 * f);
        return n3 << 24 | n & 0xFFFFFF;
    }

    private int lerpColor(int n, int n2, float f) {
        float f2 = 1.0f - f;
        int n3 = n >> 24 & 0xFF;
        int n4 = n >> 16 & 0xFF;
        int n5 = n >> 8 & 0xFF;
        int n6 = n & 0xFF;
        int n7 = n2 >> 24 & 0xFF;
        int n8 = n2 >> 16 & 0xFF;
        int n9 = n2 >> 8 & 0xFF;
        int n10 = n2 & 0xFF;
        int n11 = (int)((float)n3 * f2 + (float)n7 * f);
        int n12 = (int)((float)n4 * f2 + (float)n8 * f);
        int n13 = (int)((float)n5 * f2 + (float)n9 * f);
        int n14 = (int)((float)n6 * f2 + (float)n10 * f);
        return n11 << 24 | n12 << 16 | n13 << 8 | n14;
    }
}