package shit.zen.gui.panel.setting;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.gui.PanelClickGui;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.TextGlow;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.ModeSetting;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class ModeSettingRenderer
extends ClientBase
implements SettingRenderer {
    private static final int DROPDOWN_BG_COLOR = new Color(255, 255, 255, 15).getRGB();
    private final Map<ModeSetting, Boolean> openStates = new HashMap<>();
    private final Map<ModeSetting, Float> openAnimations = new HashMap<>();
    private final Map<ModeSetting, Map<String, Float>> itemHoverAnimations = new HashMap<>();

    @Override
    public int render(GuiGraphics guiGraphics, Setting<?> setting, int n, int n2, int n3, int n4, int n5, float f, float f2) {
        if (!(setting instanceof ModeSetting modeSetting)) {
            return 0;
        }
        this.updateOpenAnimation(modeSetting);
        String[] stringArray = Arrays.stream((Object[])modeSetting.getModes()).filter(string -> !Objects.equals(string, modeSetting.getValue())).toArray(String[]::new);
        float f3 = this.openAnimations.getOrDefault(modeSetting, Float.valueOf(0.0f)).floatValue();
        int n6 = Math.round(16.0f * f2);
        int n7 = Math.round((float)(stringArray.length * n6) * f3);
        int n8 = Math.round(24.0f * f2);
        try {
            int n9 = Math.round(75.0f * f2);
            int n10 = n + n3 - Math.round(80.0f * f2);
            int n11 = n2 + 1;
            RenderUtil.drawRoundedRect(guiGraphics.pose(), (float)n10, (float)n11, (float)n9, (float)(n6 + n7), 4.0f * f2, this.applyAlpha(DROPDOWN_BG_COLOR, f));
            FontRenderer fontRenderer = FontPresets.axiformaRegular(14.0f * f2);
            float f4 = (float)n2 + (float)n8 / 2.0f - fontRenderer.getMetrics().capHeight() / 2.0f;
            int n12 = new Color(255, 255, 255, 100).getRGB();
            TextGlow.drawGlowText(modeSetting.getName(), n, f4, fontRenderer, this.applyAlpha(-1, f), this.applyAlpha(n12, f), 8.0f * f2);
            FontRenderer fontRenderer2 = FontPresets.axiformaRegular(14.0f * f2);
            String string2 = modeSetting.getValue() != null ? modeSetting.getValue() : "None";
            float f5 = GlHelper.getStringWidth(string2, fontRenderer2);
            float f6 = (float)n10 + ((float)n9 - f5) / 2.0f;
            float f7 = (float)n11 + (float)n6 / 2.0f - fontRenderer2.getMetrics().capHeight() / 2.0f + 2.0f * f2;
            TextGlow.drawGlowText(string2, f6, f7, fontRenderer2, this.applyAlpha(-3355444, f), this.applyAlpha(new Color(255, 255, 255, 150).getRGB(), f), 10.0f * f2);
            float f8 = this.openAnimations.getOrDefault(modeSetting, Float.valueOf(0.0f)).floatValue();
            if ((double)f8 > 0.01) {
                this.itemHoverAnimations.putIfAbsent(modeSetting, new HashMap<>());
                Map<String, Float> map = this.itemHoverAnimations.get(modeSetting);
                for (int i = 0; i < stringArray.length; ++i) {
                    String string3 = stringArray[i];
                    float f9 = GlHelper.getStringWidth(string3, fontRenderer2);
                    float f10 = (float)n10 + ((float)n9 - f9) / 2.0f;
                    int n13 = n11 + n6 + i * n6;
                    float f11 = (float)n13 + (float)n6 / 2.0f - fontRenderer2.getMetrics().capHeight() / 2.0f + 2.0f * f2;
                    boolean bl = this.isMouseInBounds(n4, n5, n10, n13, n9, n6);
                    this.updateItemHoverAnim(map, string3, bl);
                    float f12 = map.getOrDefault(string3, 0.0f);
                    float f13 = Math.min(1.0f, f8);
                    if (!(f13 > 0.01f)) continue;
                    int n14 = (int)(255.0f * f13 * f);
                    int n15 = this.lerpColor(-3355444, -1, f12);
                    int n16 = n14 << 24 | n15 & 0xFFFFFF;
                    int n17 = (int)this.lerpFloat(80.0f, 150.0f, f12);
                    int n18 = new Color(255, 255, 255, (int)((float)n17 * f13 * f)).getRGB();
                    float f14 = this.lerpFloat(6.0f, 10.0f, f12);
                    TextGlow.drawGlowText(string3, f10, f11, fontRenderer2, n16, n18, f14 * f2);
                }
            }
        } catch (Exception exception) {
            // empty catch block
        }
        return n8 + n7;
    }

    private int applyAlpha(int n, float f) {
        int n2 = n >> 24 & 0xFF;
        int n3 = (int)((float)n2 * f);
        return n3 << 24 | n & 0xFFFFFF;
    }

    private void updateItemHoverAnim(Map<String, Float> map, String string, boolean bl) {
        float f = map.getOrDefault(string, Float.valueOf(0.0f)).floatValue();
        float f2 = bl ? 1.0f : 0.0f;
        f = Math.abs(f - f2) > 0.01f ? LerpUtil.smoothLerp(f, f2, 0.28f) : f2;
        map.put(string, Float.valueOf(f));
    }

    private float lerpFloat(float f, float f2, float f3) {
        return f + (f2 - f) * f3;
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

    private void updateOpenAnimation(ModeSetting modeSetting) {
        boolean bl = this.openStates.getOrDefault(modeSetting, false);
        float f = this.openAnimations.getOrDefault(modeSetting, Float.valueOf(0.0f)).floatValue();
        if (bl) {
            this.openAnimations.put(modeSetting, Float.valueOf(LerpUtil.lerp(f, 1.0f, 0.08f)));
        } else {
            this.openAnimations.put(modeSetting, Float.valueOf(LerpUtil.lerp(f, 0.0f, 0.16f)));
        }
    }

    @Override
    public boolean onClick(Setting<?> setting, int n, int n2, int n3, int n4, int n5, int n6, float f) {
        if (!(setting instanceof ModeSetting modeSetting)) {
            return false;
        }
        int n7 = Math.round(16.0f * f);
        int n8 = Math.round(75.0f * f);
        int n9 = n + n3 - Math.round(80.0f * f);
        int n10 = n2 - 2;
        boolean bl = this.openStates.getOrDefault(modeSetting, false);
        String[] stringArray = Arrays.stream((Object[])modeSetting.getModes()).filter(string -> !Objects.equals(string, modeSetting.getValue())).toArray(String[]::new);
        boolean bl2 = this.isMouseInBounds(n4, n5, n9, n10, n8, n7);
        if (bl2 && n6 == 1) {
            this.openStates.put(modeSetting, !bl);
            return true;
        }
        if (bl) {
            int n11 = n10 + n7;
            for (int i = 0; i < stringArray.length; ++i) {
                if (!this.isMouseInBounds(n4, n5, n9, n11 + i * n7, n8, n7) || n6 != 0) continue;
                String string2 = stringArray[i];
                modeSetting.setValue(string2);
                this.openStates.put(modeSetting, false);
                PanelClickGui.panelClickGui.addToast(modeSetting.getName() + " set to " + string2);
                return true;
            }
            if (!bl2) {
                this.openStates.put(modeSetting, false);
            }
        }
        return false;
    }

    private boolean isMouseInBounds(int n, int n2, int n3, int n4, int n5, int n6) {
        return n >= n3 && n <= n3 + n5 && n2 >= n4 && n2 <= n4 + n6;
    }

    @Override
    public boolean supports(Setting<?> setting) {
        return setting instanceof ModeSetting;
    }

    @Override
    public int getHeight(Setting<?> setting, float f) {
        if (!(setting instanceof ModeSetting modeSetting)) {
            return Math.round(24.0f * f);
        }
        String[] stringArray = Arrays.stream((Object[])modeSetting.getModes()).filter(string -> !Objects.equals(string, modeSetting.getValue())).toArray(String[]::new);
        float f2 = this.openAnimations.getOrDefault(modeSetting, Float.valueOf(0.0f)).floatValue();
        int n = Math.round(16.0f * f);
        int n2 = Math.round((float)(stringArray.length * n) * f2);
        return Math.round(24.0f * f) + n2;
    }

    @Override
    public void onMouseRelease(double d, double d2, int n) {
    }
}