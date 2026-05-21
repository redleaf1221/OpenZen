package shit.zen.gui.panel.setting;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.gui.PanelClickGui;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.TextGlow;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.BooleanSetting;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class BooleanSettingRenderer
extends ClientBase
implements SettingRenderer {
    private static final int COLOR_ON = new Color(76, 175, 80, 180).getRGB();
    private static final int COLOR_OFF = new Color(158, 158, 158, 150).getRGB();
    private final Map<BooleanSetting, Boolean> hoverStates = new HashMap<>();
    private final Map<BooleanSetting, Float> toggleAnimations = new HashMap<>();
    private final Map<BooleanSetting, Float> hoverAnimations = new HashMap<>();

    @Override
    public int render(GuiGraphics guiGraphics, Setting<?> setting, int n, int n2, int n3, int n4, int n5, float f, float f2) {
        int n6;
        if (!(setting instanceof BooleanSetting booleanSetting)) {
            return 0;
        }
        int n7 = Math.round(24.0f * f2);
        int n8 = n6 = Math.round(10.0f * f2);
        int n9 = Math.round(5.0f * f2);
        float f3 = this.toggleAnimations.getOrDefault(booleanSetting, Float.valueOf(booleanSetting.getValue() != false ? 1.0f : 0.0f)).floatValue();
        float f4 = booleanSetting.getValue() != false ? 1.0f : 0.0f;
        f3 = Math.abs(f4 - f3) > 0.01f ? LerpUtil.smoothLerp(f3, f4, 0.25f) : f4;
        this.toggleAnimations.put(booleanSetting, Float.valueOf(f3));
        int n10 = n6 * 2;
        int n11 = n + n3 - n10 - n9;
        int n12 = n2 + (n7 - n8) / 2;
        this.updateHoverState(booleanSetting, n11, n12, n4, n5, n6);
        float f5 = this.hoverAnimations.getOrDefault(booleanSetting, Float.valueOf(0.0f)).floatValue();
        float f6 = this.hoverStates.getOrDefault(booleanSetting, false) != false ? 1.0f : 0.0f;
        f5 = Math.abs(f6 - f5) > 0.01f ? LerpUtil.smoothLerp(f5, f6, 0.3f) : f6;
        this.hoverAnimations.put(booleanSetting, Float.valueOf(f5));
        FontRenderer fontRenderer = FontPresets.axiformaRegular(14.0f * f2);
        float f7 = (float)n2 + (float)n7 / 2.0f - fontRenderer.getMetrics().capHeight() / 2.0f;
        TextGlow.drawGlowText(booleanSetting.getName(), n, f7, fontRenderer, this.applyAlpha(-1, f), this.applyAlpha(new Color(255, 255, 255, 120).getRGB(), f), 6.0f * f2);
        this.drawToggle(guiGraphics, n11, n12, f3, f5, f, f2);
        return n7;
    }

    private int applyAlpha(int n, float f) {
        int n2 = n >> 24 & 0xFF;
        int n3 = (int)((float)n2 * f);
        return n3 << 24 | n & 0xFFFFFF;
    }

    private void drawToggle(GuiGraphics guiGraphics, int n, int n2, float f, float f2, float f3, float f4) {
        int n3 = Math.round(10.0f * f4);
        int n4 = n3 * 2;
        int n5 = n3;
        int n6 = this.lerpColor(COLOR_OFF, COLOR_ON, f);
        if (f2 > 0.0f) {
            float f5 = 1.0f + 0.3f * f2;
            n6 = this.brightenColor(n6, f5);
        }
        if (f > 0.01f) {
            int n7 = new Color(76, 175, 80, (int)(70.0f * f)).getRGB();
            RenderUtil.drawRoundedRect(guiGraphics.pose(), (float)n - f4, (float)n2 - f4, (float)n4 + 2.0f * f4, (float)n5 + 2.0f * f4, (float)n5 / 2.0f, this.applyAlpha(n7, f3));
        }
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n, n2, n4, n5, (float)n5 / 2.0f, this.applyAlpha(n6, f3));
        int n8 = Math.round(2.0f * f4);
        int n9 = n5 - n8 * 2;
        int n10 = n + n8 + Math.round((float)(n4 - n9 - n8 * 2) * f);
        int n11 = n2 + n8;
        int n12 = -1;
        if (f2 > 0.0f) {
            int n13 = (int)(50.0f * f2);
            int n14 = n13 << 24 | 0xFFFFFF;
            RenderUtil.drawRoundedRect(guiGraphics.pose(), n10 - 1, n11 - 1, n9 + 2, n9 + 2, (float)(n9 + 2) / 2.0f, this.applyAlpha(n14, f3));
        }
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n10, n11, n9, n9, (float)n9 / 2.0f, this.applyAlpha(n12, f3));
    }

    private void updateHoverState(BooleanSetting booleanSetting, int n, int n2, int n3, int n4, int n5) {
        boolean bl = n3 >= n && n3 <= n + n5 * 2 && n4 >= n2 && n4 <= n2 + n5;
        this.hoverStates.put(booleanSetting, bl);
    }

    private int brightenColor(int n, float f) {
        int n2 = n >> 24 & 0xFF;
        int n3 = n >> 16 & 0xFF;
        int n4 = n >> 8 & 0xFF;
        int n5 = n & 0xFF;
        n3 = Math.min(255, (int)((float)n3 * f));
        n4 = Math.min(255, (int)((float)n4 * f));
        n5 = Math.min(255, (int)((float)n5 * f));
        return n2 << 24 | n3 << 16 | n4 << 8 | n5;
    }

    private int lerpColor(int n, int n2, float f) {
        int n3 = n >> 24 & 0xFF;
        int n4 = n >> 16 & 0xFF;
        int n5 = n >> 8 & 0xFF;
        int n6 = n & 0xFF;
        int n7 = n2 >> 24 & 0xFF;
        int n8 = n2 >> 16 & 0xFF;
        int n9 = n2 >> 8 & 0xFF;
        int n10 = n2 & 0xFF;
        int n11 = (int)((float)n3 + (float)(n7 - n3) * f);
        int n12 = (int)((float)n4 + (float)(n8 - n4) * f);
        int n13 = (int)((float)n5 + (float)(n9 - n5) * f);
        int n14 = (int)((float)n6 + (float)(n10 - n6) * f);
        return n11 << 24 | n12 << 16 | n13 << 8 | n14;
    }

    @Override
    public boolean onClick(Setting<?> setting, int n, int n2, int n3, int n4, int n5, int n6, float f) {
        int n7;
        if (!(setting instanceof BooleanSetting booleanSetting)) {
            return false;
        }
        int n8 = Math.round(24.0f * f);
        int n9 = n7 = Math.round(10.0f * f);
        int n10 = Math.round(5.0f * f);
        int n11 = n7 * 2;
        int n12 = n + n3 - n11 - n10;
        int n13 = n2 + (n8 - n9) / 2;
        if (n6 == 0 && n4 >= n12 && n4 <= n12 + n11 && n5 >= n13 && n5 <= n13 + n9) {
            boolean bl = booleanSetting.getValue() == false;
            booleanSetting.setValue(bl);
            String string = bl ? "On" : "Off";
            PanelClickGui.panelClickGui.addToast(booleanSetting.getName() + " is " + string);
            return true;
        }
        return false;
    }

    @Override
    public boolean supports(Setting<?> setting) {
        return setting instanceof BooleanSetting;
    }

    @Override
    public int getHeight(Setting<?> setting, float f) {
        return Math.round(24.0f * f);
    }

    @Override
    public void onMouseRelease(double d, double d2, int n) {
    }
}