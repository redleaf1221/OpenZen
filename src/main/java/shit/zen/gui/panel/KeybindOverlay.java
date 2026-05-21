package shit.zen.gui.panel;

import java.awt.Color;

import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.ZenClient;
import shit.zen.gui.PanelClickGui;
import shit.zen.modules.KeyBind;
import shit.zen.modules.Module;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Renderer;
import shit.zen.render.TextGlow;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class KeybindOverlay
extends ClientBase {
    private static final Color OVERLAY_BG_COLOR = new Color(124, 124, 124, 13);
    private boolean isActive = false;
    private Module targetModule = null;
    private float alpha = 0.0f;
    private long startTime = 0L;

    public void startBinding(Module module) {
        this.targetModule = module;
        this.isActive = true;
        this.startTime = System.currentTimeMillis();
    }

    public void cancel() {
        this.isActive = false;
        this.targetModule = null;
    }

    public boolean isVisible() {
        return this.isActive && this.alpha > 0.01f;
    }

    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        if (!this.isActive && this.alpha <= 0.005f) {
            return;
        }
        this.updateAlpha();
        if (this.alpha <= 0.005f) {
            return;
        }
        try {
            this.drawBackground(guiGraphics, n, n2);
            float f2 = 400.0f * f;
            float f3 = 180.0f * f;
            int n3 = (int)(((float)n - f2) / 2.0f);
            int n4 = (int)(((float)n2 - f3) / 2.0f);
            this.drawGlow(guiGraphics, n3, n4, f2, f3, f);
            this.drawContent(guiGraphics, n3, n4, f2, f);
        } catch (Exception exception) {
            // empty catch block
        }
    }

    public boolean onKeyPress(int n, int n2, int n3) {
        if (!this.isVisible()) {
            return false;
        }
        if (n == 256) {
            if (this.targetModule != null) {
                this.targetModule.setKey(-1);
                if (ZenClient.isReady()) {
                    ZenClient.instance.getConfigManager().saveAll();
                }
                PanelClickGui.panelClickGui.addToast(this.targetModule.getName() + " keybind cleared");
            }
            this.cancel();
            return true;
        }
        if (this.targetModule != null && n != -1) {
            this.targetModule.setKey(n);
            if (ZenClient.isReady()) {
                ZenClient.instance.getConfigManager().saveAll();
            }
            KeyBind keyBind = new KeyBind(n);
            String string = keyBind.getName();
            PanelClickGui.panelClickGui.addToast(this.targetModule.getName() + " bound to " + string.toUpperCase());
            this.cancel();
            return true;
        }
        return false;
    }

    private void updateAlpha() {
        this.alpha = this.isActive ? LerpUtil.lerp(this.alpha, 1.0f, 0.08f) : LerpUtil.lerp(this.alpha, 0.0f, 0.08f);
    }

    private void onRenderExtra() {
    }

    private void drawBackground(GuiGraphics guiGraphics, int n, int n2) {
        Color color = new Color(OVERLAY_BG_COLOR.getRed(), OVERLAY_BG_COLOR.getGreen(), OVERLAY_BG_COLOR.getBlue(), (int)((float)OVERLAY_BG_COLOR.getAlpha() * this.alpha));
        RenderUtil.drawRoundedRect(guiGraphics.pose(), 0.0f, 0.0f, n, n2, 0.0f, color.getRGB());
    }

    private void drawGlow(GuiGraphics guiGraphics, int n, int n2, float f, float f2, float f3) {
        TextGlow.drawBackground(guiGraphics.pose(), n, n2, f, f2, 12.0f * f3, this.alpha);
    }

    private void drawContent(GuiGraphics guiGraphics, int n, int n2, float f, float f2) {
        Renderer.renderConsumer((drawContext -> {
            int n3;
            float f3;
            float f4;
            float f5;
            String string;
            FontRenderer fontRenderer;
            int n4 = (int)(255.0f * this.alpha);
            FontRenderer fontRenderer2 = FontPresets.axiformaBold(24.0f * f2);
            String string2 = "KEYBIND";
            float f6 = GlHelper.getStringWidth(string2, fontRenderer2);
            float f7 = (float)n + (f - f6) / 2.0f;
            float f8 = (float)n2 + 45.0f * f2;
            int n5 = n4 << 24 | 0xFFFFFF;
            int n6 = n4 << 24 | 0xFFFFFF;
            TextGlow.drawGlowText(string2, f7, f8, fontRenderer2, n5, n6, 10.0f * f2);
            if (this.targetModule != null) {
                fontRenderer = FontPresets.axiformaRegular(18.0f * f2);
                string = "Module: " + this.targetModule.getName();
                f5 = GlHelper.getStringWidth(string, fontRenderer);
                f4 = (float)n + (f - f5) / 2.0f;
                f3 = (float)n2 + 75.0f * f2;
                n3 = n4 << 24 | 0xFFFFFF;
                GlHelper.drawText(string, f4, f3, fontRenderer, n3);
            }
            fontRenderer = FontPresets.axiformaRegular(16.0f * f2);
            string = "Press any key to bind";
            f5 = GlHelper.getStringWidth(string, fontRenderer);
            f4 = (float)n + (f - f5) / 2.0f;
            f3 = (float)n2 + 105.0f * f2;
            n3 = n4 << 24 | 0xCCCCCC;
            GlHelper.drawText(string, f4, f3, fontRenderer, n3);
            this.drawAnimatedDots(n, (int)((float)n2 + 125.0f * f2), (int)f, n4, f2);
            FontRenderer fontRenderer3 = FontPresets.axiformaRegular(14.0f * f2);
            String string3 = "Press ESC to cancel";
            float f9 = GlHelper.getStringWidth(string3, fontRenderer3);
            float f10 = (float)n + (f - f9) / 2.0f;
            float f11 = (float)n2 + 155.0f * f2;
            int n7 = n4 << 24 | 0xCCCCCC;
            GlHelper.drawText(string3, f10, f11, fontRenderer3, n7);
        }));
    }

    private void drawAnimatedDots(int n, int n2, int n3, int n4, float f) {
        FontRenderer fontRenderer = FontPresets.axiformaBold(20.0f * f);
        String string = "•";
        float f2 = GlHelper.getStringWidth(string, fontRenderer);
        float f3 = f2 * 3.0f + 20.0f * f;
        float f4 = (float)n + ((float)n3 - f3) / 2.0f;
        int n5 = n4 << 24 | 0xFFFFFF;
        long l = System.currentTimeMillis();
        long l2 = l - this.startTime;
        long l3 = l2 % 1400L;
        for (int i = 0; i < 3; ++i) {
            float f5;
            float f6 = f4 + (float)i * (f2 + 10.0f * f);
            long l4 = (long)i * 150L;
            long l5 = l4 + 300L;
            float f7 = 0.0f;
            if (l3 >= l4 && l3 <= l5) {
                f5 = (float)(l3 - l4) / 300.0f;
                float f8 = f5 * (float)Math.PI;
                f7 = (float)(Math.sin(f8) * 6.0 * (double)f);
            }
            f5 = (float)n2 - f7;
            GlHelper.drawText(string, f6, f5, fontRenderer, n5);
        }
    }

    public Module getTargetModule() {
        return this.targetModule;
    }

    static {
        new Color(255, 255, 255, 40);
    }
}