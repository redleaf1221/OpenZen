package shit.zen.gui.panel.setting;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.gui.PanelClickGui;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.GlyphMetrics;
import shit.zen.render.TextGlow;
import shit.zen.settings.Setting;
import shit.zen.settings.impl.NumberSetting;
import shit.zen.utils.render.RenderUtil;

public class NumberSettingRenderer
extends ClientBase
implements SettingRenderer {
    private static NumberSetting editingNumberSetting;
    private static String editingText;
    private static long lastInputTime;
    private final Map<NumberSetting, Long> editIconTimers = new HashMap<>();
    private final Map<NumberSetting, Boolean> plusButtonHover = new HashMap<>();
    private final Map<NumberSetting, Boolean> minusButtonHover = new HashMap<>();
    private final Map<NumberSetting, Boolean> editIconHover = new HashMap<>();

    @Override
    public int render(GuiGraphics guiGraphics, Setting<?> setting, int n, int n2, int n3, int n4, int n5, float f, float f2) {
        if (!(setting instanceof NumberSetting numberSetting)) {
            return 0;
        }
        boolean bl = numberSetting.equals(editingNumberSetting);
        int n6 = Math.round(24.0f * f2);
        int n7 = Math.round(12.0f * f2);
        int n8 = Math.round(8.0f * f2);
        FontRenderer fontRenderer = FontPresets.axiformaBold(14.0f * f2);
        String string = bl ? editingText : this.formatValue(numberSetting.getValue().doubleValue());
        float f3 = GlHelper.getStringWidth(string, fontRenderer);
        int n9 = n7 * 2 + (int)f3 + n8 * 2 - Math.round(2.0f * f2);
        int n10 = n + n3 - n9;
        int n11 = n6 - Math.round(14.0f * f2);
        float f4 = (float)n2 + (float)n6 / 2.0f;
        int n12 = Math.round(f4 - (float)n11 / 2.0f);
        int n13 = Math.round(16.0f * f2);
        int n14 = Math.round(4.0f * f2);
        int n15 = n10 - n13 - n14;
        int n16 = Math.round(f4 - (float)n13 / 2.0f) + Math.round(3.0f * f2);
        this.updateHoverStates(numberSetting, n4, n5, n10, n12, n9, n7, n11, n15, n16, n13);
        this.drawNumberWidget(guiGraphics, numberSetting, n, n2, n3, n10, n12, n9, n11, n15, n16, bl, f, f2);
        return n6;
    }

    @Override
    public boolean onClick(Setting<?> setting, int n, int n2, int n3, int n4, int n5, int n6, float f) {
        if (!(setting instanceof NumberSetting numberSetting) || n6 != 0) {
            return false;
        }
        int n7 = Math.round(24.0f * f);
        int n8 = Math.round(12.0f * f);
        int n9 = Math.round(8.0f * f);
        FontRenderer fontRenderer = FontPresets.axiformaBold(14.0f * f);
        String string = this.formatValue(numberSetting.getValue().doubleValue());
        float f2 = GlHelper.getStringWidth(string, fontRenderer);
        int n10 = n8 * 2 + (int)f2 + n9 * 2 - Math.round(2.0f * f);
        int n11 = n + n3 - n10;
        int n12 = n7 - Math.round(14.0f * f);
        float f3 = (float)n2 + (float)n7 / 2.0f;
        int n13 = Math.round(f3 - (float)n12 / 2.0f);
        int n14 = Math.round(16.0f * f);
        int n15 = Math.round(4.0f * f);
        int n16 = n11 - n14 - n15;
        int n17 = Math.round(f3 - (float)n14 / 2.0f);
        if (n4 >= n16 && n4 <= n16 + n14 && n5 >= n17 && n5 <= n17 + n14) {
            this.startEditing(numberSetting);
            return true;
        }
        if (n4 >= n11 && n4 <= n11 + n10 && n5 >= n13 && n5 <= n13 + n12) {
            if (n4 < n11 + n8) {
                this.decrementValue(numberSetting);
            } else if (n4 > n11 + n10 - n8) {
                this.incrementValue(numberSetting);
            }
            return true;
        }
        return false;
    }

    public static void clearEditing() {
        NumberSettingRenderer.cancelEdit();
    }

    public static boolean onKeyPress(int n, int n2, int n3) {
        if (editingNumberSetting == null) {
            return false;
        }
        if (n == 257 || n == 335) {
            NumberSettingRenderer.commitEdit();
            return true;
        }
        if (n == 256) {
            NumberSettingRenderer.cancelEdit();
            return true;
        }
        if (n == 259 && !editingText.isEmpty()) {
            editingText = editingText.substring(0, editingText.length() - 1);
            lastInputTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public static boolean onCharTyped(char c) {
        if (editingNumberSetting == null) {
            return false;
        }
        if (Character.isDigit(c) || c == '.' || c == '-' && editingText.isEmpty()) {
            if (c == '.' && editingText.contains(".")) {
                return true;
            }
            editingText = editingText + c;
            lastInputTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    @Override
    public void onMouseMove(double d, double d2) {
    }

    @Override
    public void onMouseRelease(double d, double d2, int n) {
    }

    private void startEditing(NumberSetting numberSetting) {
        editingNumberSetting = numberSetting;
        editingText = "";
        lastInputTime = System.currentTimeMillis();
    }

    private static void commitEdit() {
        if (editingNumberSetting == null || editingText.isEmpty()) {
            NumberSettingRenderer.cancelEdit();
            return;
        }
        try {
            double d = Double.parseDouble(editingText);
            double d2 = editingNumberSetting.getMin().doubleValue();
            double d3 = editingNumberSetting.getMax().doubleValue();
            d = Math.max(d2, Math.min(d3, d));
            NumberSettingRenderer.applyValueStatic(editingNumberSetting, d);
            PanelClickGui.panelClickGui.addToast(editingNumberSetting.getName() + " set to " + String.format(Locale.US, "%.1f", new Object[]{d}));
        } catch (NumberFormatException numberFormatException) {
            PanelClickGui.panelClickGui.addToast("Invalid input, edit cancelled");
        }
        NumberSettingRenderer.cancelEdit();
    }

    private static void cancelEdit() {
        editingNumberSetting = null;
        editingText = "";
    }

    private int applyAlpha(int n, float f) {
        int n2 = n >> 24 & 0xFF;
        int n3 = (int)((float)n2 * f);
        return n3 << 24 | n & 0xFFFFFF;
    }

    private void drawNumberWidget(GuiGraphics guiGraphics, NumberSetting numberSetting, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, boolean bl, float f, float f2) {
        String string;
        float f3 = 24.0f * f2;
        float f4 = (float)n2 + f3 / 2.0f;
        FontRenderer fontRenderer = FontPresets.axiformaRegular(14.0f * f2);
        FontRenderer fontRenderer2 = FontPresets.axiformaBold(14.0f * f2);
        FontRenderer fontRenderer3 = FontPresets.axiformaBold(12.0f * f2);
        GlyphMetrics glyphMetrics = fontRenderer.getMetrics();
        float f5 = f4 - (glyphMetrics.ascent() + glyphMetrics.descent()) / 2.0f + glyphMetrics.ascent() + 6.0f * f2;
        TextGlow.drawGlowText(numberSetting.getName(), n, f5, fontRenderer, this.applyAlpha(-1, f), this.applyAlpha(new Color(255, 255, 255, 120).getRGB(), f), 8.0f * f2);
        this.drawEditIcon(guiGraphics, n8, n9, numberSetting, f, f2);
        int n10 = Math.round(12.0f * f2);
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n4, n5, n6, n7, 5.0f * f2, this.applyAlpha(0x50F5F5F5, f));
        int n11 = n4 + n10;
        int n12 = n6 - n10 * 2;
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n11, n5, n12, n7, 0.0f, this.applyAlpha(1086900424, f));
        GlyphMetrics glyphMetrics2 = fontRenderer3.getMetrics();
        float f6 = (float)n5 + (float)n7 / 2.0f;
        float f7 = f6 - (glyphMetrics2.ascent() + glyphMetrics2.descent()) / 2.0f + glyphMetrics2.ascent() + 5.0f * f2 - 2.5f;
        int n13 = this.minusButtonHover.getOrDefault(numberSetting, false) != false ? new Color(255, 255, 255).getRGB() : -1;
        GlHelper.drawText("-", (float)n4 + (float)n10 / 2.0f - GlHelper.getStringWidth("-", fontRenderer3) / 2.0f, f7, fontRenderer3, this.applyAlpha(n13, f));
        int n14 = this.plusButtonHover.getOrDefault(numberSetting, false) != false ? new Color(255, 255, 255).getRGB() : -1;
        GlHelper.drawText("+", (float)(n4 + n6 - n10) + ((float)n10 / 2.0f - GlHelper.getStringWidth("+", fontRenderer3) / 2.0f), f7, fontRenderer3, this.applyAlpha(n14, f));
        String string2 = string = bl ? editingText : this.formatValue(numberSetting.getValue().doubleValue());
        if (bl && string.isEmpty()) {
            string = "0";
        }
        float f8 = GlHelper.getStringWidth(string, fontRenderer2);
        float f9 = (float)n4 + (float)n6 / 2.0f - f8 / 2.0f;
        GlyphMetrics glyphMetrics3 = fontRenderer2.getMetrics();
        float f10 = f7 + f2 - 1.5f;
        if (bl) {
            long l = System.currentTimeMillis();
            float f11 = (float)(l % 1000L) / 1000.0f;
            float f12 = (float)(Math.sin((double)f11 * Math.PI * 2.0) * 0.5 + 0.5);
            int n15 = (int)(255.0f * (0.6f + f12 * 0.4f) * f);
            int n16 = n15 << 24 | 0xFFFFFF;
            GlHelper.drawText(string, f9, f10, fontRenderer2, n16);
            float f13 = f9 + f8 + 2.0f * f2;
            int n17 = (int)(255.0f * f12 * f);
            int n18 = n17 << 24 | 0xFFFFFF;
            float f14 = glyphMetrics3.capHeight();
            float f15 = f10 - glyphMetrics3.ascent() + (glyphMetrics3.ascent() - f14) / 2.0f;
            RenderUtil.drawFilledRect(guiGraphics.pose(), f13, f15, Math.round(f2), Math.round(f14), n18);
        } else {
            int n19 = new Color(255, 255, 255, 120).getRGB();
            TextGlow.drawGlowText(string, f9, f10, fontRenderer2, this.applyAlpha(-1, f), this.applyAlpha(n19, f), 6.0f * f2);
        }
    }

    private void drawEditIcon(GuiGraphics guiGraphics, int n, int n2, NumberSetting numberSetting, float f, float f2) {
        boolean bl = this.editIconHover.getOrDefault(numberSetting, false);
        long l = this.editIconTimers.getOrDefault(numberSetting, 0L);
        long l2 = System.currentTimeMillis() - l;
        int n3 = Math.round(16.0f * f2);
        float f3 = Math.min(1.0f, (float)l2 / 200.0f);
        if (!bl) {
            f3 = 1.0f - f3;
        }
        int n4 = -5197648;
        int n5 = -1;
        int n6 = RenderUtil.lerpColorHSB(n4, n5, f3);
        FontRenderer fontRenderer = FontPresets.materialIcons(n3);
        String string = "";
        float f4 = GlHelper.getStringWidth(string, fontRenderer);
        GlyphMetrics glyphMetrics = fontRenderer.getMetrics();
        float f5 = (float)n + ((float)n3 - f4) / 2.0f;
        float f6 = (float)n2 + (float)n3 / 2.0f - (glyphMetrics.ascent() + glyphMetrics.descent()) / 2.0f + glyphMetrics.ascent() + 1.0f;
        GlHelper.drawText(string, f5, f6, fontRenderer, this.applyAlpha(n6, f));
    }

    private void incrementValue(NumberSetting numberSetting) {
        double d = numberSetting.getValue().doubleValue();
        double d2 = numberSetting.getStep().doubleValue();
        double d3 = numberSetting.getMax().doubleValue();
        double d4 = Math.min(d3, d + d2);
        this.applyValue(numberSetting, d4);
        PanelClickGui.panelClickGui.addToast(numberSetting.getName() + " set to " + this.formatValue(d4));
    }

    private void decrementValue(NumberSetting numberSetting) {
        double d = numberSetting.getValue().doubleValue();
        double d2 = numberSetting.getStep().doubleValue();
        double d3 = numberSetting.getMin().doubleValue();
        double d4 = Math.max(d3, d - d2);
        this.applyValue(numberSetting, d4);
        PanelClickGui.panelClickGui.addToast(numberSetting.getName() + " set to " + this.formatValue(d4));
    }

    private void applyValue(NumberSetting numberSetting, double d) {
        if (numberSetting.getValue() instanceof Integer) {
            numberSetting.setValue((int)Math.round(d));
        } else if (numberSetting.getValue() instanceof Long) {
            numberSetting.setValue(Math.round(d));
        } else if (numberSetting.getValue() instanceof Float) {
            numberSetting.setValue(Float.valueOf((float)d));
        } else {
            numberSetting.setValue(d);
        }
    }

    private static void applyValueStatic(NumberSetting numberSetting, double d) {
        if (numberSetting.getValue() instanceof Integer) {
            numberSetting.setValue((int)Math.round(d));
        } else if (numberSetting.getValue() instanceof Long) {
            numberSetting.setValue(Math.round(d));
        } else if (numberSetting.getValue() instanceof Float) {
            numberSetting.setValue(Float.valueOf((float)d));
        } else {
            numberSetting.setValue(d);
        }
    }

    private void updateHoverStates(NumberSetting numberSetting, int n, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10) {
        boolean bl;
        boolean bl2 = n >= n3 && n <= n3 + n5 && n2 >= n4 && n2 <= n4 + n7;
        this.minusButtonHover.put(numberSetting, bl2 && n < n3 + n6);
        this.plusButtonHover.put(numberSetting, bl2 && n > n3 + n5 - n6);
        boolean bl3 = bl = n >= n8 && n <= n8 + n10 && n2 >= n9 && n2 <= n9 + n10;
        if (bl != this.editIconHover.getOrDefault(numberSetting, false)) {
            this.editIconHover.put(numberSetting, bl);
            this.editIconTimers.put(numberSetting, System.currentTimeMillis());
        }
    }

    private String formatValue(double d) {
        return String.format(Locale.US, "%.1f", new Object[]{d});
    }

    @Override
    public boolean supports(Setting<?> setting) {
        return setting instanceof NumberSetting;
    }

    @Override
    public int getHeight(Setting<?> setting, float f) {
        return Math.round(24.0f * f);
    }

    static {
        editingText = "";
        lastInputTime = 0L;
    }
}