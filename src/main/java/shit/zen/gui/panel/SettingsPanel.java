package shit.zen.gui.panel;

import java.awt.Color;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.gui.PanelClickGui;
import shit.zen.gui.panel.setting.SettingRenderer;
import shit.zen.gui.panel.setting.SettingRendererRegistry;
import shit.zen.modules.Module;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Rectangle;
import shit.zen.render.Renderer;
import shit.zen.render.StencilHelper;
import shit.zen.render.TextGlow;
import shit.zen.settings.Setting;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class SettingsPanel
extends ClientBase {
    public enum AnimationState { NONE, FADE_IN, FADE_OUT, SWITCHING }

    private static final int PANEL_BG_COLOR = new Color(255, 255, 255, 20).getRGB();
    private static final int TOGGLE_ON_COLOR = new Color(76, 175, 80).getRGB();
    private static final int TOGGLE_OFF_COLOR = new Color(158, 158, 158).getRGB();
    private Module currentModule;
    private boolean isToggleHovered = false;
    private float enabledAlpha = 0.0f;
    private float toggleHoverAlpha = 0.0f;
    private float scrollOffset = 0.0f;
    private float scrollTarget = 0.0f;
    private float totalContentHeight = 0.0f;
    private boolean isDraggingScrollbar = false;
    private float scrollbarDragStartY = 0.0f;
    private float scrollOffsetAtDragStart = 0.0f;
    private float scrollbarAlpha = 0.0f;
    private long lastScrollTime = 0L;
    private Module prevModule;
    private SettingsPanel.AnimationState animationState = SettingsPanel.AnimationState.NONE;
    private float transitionProgress = 0.0f;
    private float lastScale = 1.0f;

    public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, Module module, float f, float f2) {
        if (Math.abs(f - this.lastScale) > 0.001f) {
            this.rescaleScroll(f);
            this.lastScale = f;
        }
        if (this.currentModule != module) {
            if (this.currentModule != null) {
                this.prevModule = this.currentModule;
                this.animationState = SettingsPanel.AnimationState.FADE_OUT;
                this.transitionProgress = 0.0f;
            } else if (module != null) {
                this.animationState = SettingsPanel.AnimationState.FADE_IN;
                this.transitionProgress = 0.0f;
                this.prevModule = null;
            }
            this.currentModule = module;
            this.scrollOffset = 0.0f;
            this.scrollTarget = 0.0f;
        }
        if (this.animationState != SettingsPanel.AnimationState.NONE) {
            this.transitionProgress = LerpUtil.smoothLerp(this.transitionProgress, 1.0f, 0.22f);
            if (this.transitionProgress > 0.99f) {
                this.transitionProgress = 1.0f;
                if (this.animationState == SettingsPanel.AnimationState.FADE_OUT) {
                    this.prevModule = null;
                    if (this.currentModule != null) {
                        this.animationState = SettingsPanel.AnimationState.FADE_IN;
                        this.transitionProgress = 0.0f;
                    } else {
                        this.animationState = SettingsPanel.AnimationState.NONE;
                    }
                } else {
                    this.animationState = SettingsPanel.AnimationState.NONE;
                }
            }
        }
        if (this.currentModule == null && this.animationState == SettingsPanel.AnimationState.NONE) {
            return;
        }
        this.scrollOffset = Math.abs(this.scrollOffset - this.scrollTarget) > 0.01f ? LerpUtil.smoothLerp(this.scrollOffset, this.scrollTarget, 0.35f) : this.scrollTarget;
        float f3 = this.currentModule != null && this.currentModule.isEnabled() ? 1.0f : 0.0f;
        this.enabledAlpha = Math.abs(f3 - this.enabledAlpha) > 0.01f ? LerpUtil.smoothLerp(this.enabledAlpha, f3, 0.28f) : f3;
        try {
            int n5 = (int)(405.0f * f);
            int n6 = (int)(20.0f * f);
            int n7 = (int)(20.0f * f);
            int n8 = (int)(400.0f * f);
            int n9 = (int)(30.0f * f);
            int n10 = (int)(10.0f * f);
            int n11 = n + (int)(600.0f * f) - n5 - n6 + (int)(8.0f * f);
            int n12 = n2 + n7 + (int)(23.0f * f);
            int n13 = n8 - 2 * n6 - (int)(20.0f * f);
            int n14 = (int)(12.0f * f);
            int n15 = (int)(15.0f * f);
            int n16 = n11 + n5 - n14 * 2 - n15;
            int n17 = n12 + (n9 - n14) / 2;
            this.checkToggleHover(n16, n17, n3, n4, n14);
            float f4 = this.isToggleHovered ? 1.0f : 0.0f;
            this.toggleHoverAlpha = Math.abs(f4 - this.toggleHoverAlpha) > 0.01f ? LerpUtil.smoothLerp(this.toggleHoverAlpha, f4, 0.35f) : f4;
            RenderUtil.drawRoundedRect(guiGraphics.pose(), n11, n12, n5, n13, 4.0f * f, this.applyAlpha(PANEL_BG_COLOR, f2));
            this.renderToggleButton(guiGraphics, n16, n17, this.enabledAlpha, this.toggleHoverAlpha, this.currentModule != null && this.currentModule.isEnabled(), f, f2);
            int n18 = n11;
            int n19 = n12 + n9;
            int n20 = n5;
            int n21 = n13 - n9;
            StencilHelper.beginWrite(false);
            RenderUtil.drawRoundedRect(guiGraphics.pose(), n18, n19, n20, n21, 4.0f * f, Color.WHITE.getRGB());
            StencilHelper.beginRead(true);
            Renderer.renderConsumer(drawContext -> {
                this.calculateTotalHeight(this.currentModule, f);
                drawContext.save();
                drawContext.clip(Rectangle.ofXYWH(n11, n12, n5, n13));
                Module renderModule = this.animationState == SettingsPanel.AnimationState.FADE_OUT ? this.prevModule : this.currentModule;
                float titleAlpha = this.animationState == SettingsPanel.AnimationState.FADE_OUT ? (1.0f - this.transitionProgress) * f2 : f2;
                if (renderModule != null) {
                    FontRenderer titleFont = FontPresets.axiformaBold(20.0f * f);
                    String string = renderModule.getName();
                    if (renderModule.isEnabled()) {
                        int glowColor = this.applyAlpha(new Color(255, 255, 255, 150).getRGB(), titleAlpha);
                        TextGlow.drawGlowText(string, (float)n11 + 10.0f * f, (float)n12 + 12.0f * f, titleFont, this.applyAlpha(-1, titleAlpha), glowColor, 12.0f * f);
                    } else {
                        GlHelper.drawText(string, (float)n11 + 10.0f * f, (float)n12 + 12.0f * f, titleFont, this.applyAlpha(-1, titleAlpha));
                    }
                }
                drawContext.restore();
                drawContext.save();
                drawContext.clip(Rectangle.ofXYWH(n11, n12 + n9, n5, n13 - n9));
                float slideY = 0.0f;
                float bodyAlpha = f2;
                Module bodyModule;
                if (this.animationState == SettingsPanel.AnimationState.FADE_OUT && this.prevModule != null) {
                    bodyModule = this.prevModule;
                    slideY = this.transitionProgress * 30.0f * f;
                    bodyAlpha = (1.0f - this.transitionProgress) * f2;
                } else if (this.animationState == SettingsPanel.AnimationState.FADE_IN && this.currentModule != null) {
                    bodyModule = this.currentModule;
                    slideY = (1.0f - this.transitionProgress) * -30.0f * f;
                    bodyAlpha = this.transitionProgress * f2;
                } else {
                    bodyModule = this.currentModule;
                }
                if (bodyModule != null && bodyAlpha > 0.01f) {
                    drawContext.save();
                    drawContext.translate(0.0f, slideY);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0.0f, slideY, 0.0f);
                    List<Setting<?>> list = bodyModule.getSettings();
                    if (list != null && !list.isEmpty()) {
                        int settingY = n12 + n9 - (int)this.scrollOffset;
                        for (Setting<?> setting : list) {
                            if (setting.getVisibility() != null && !setting.getVisibility().displayable()) continue;
                            int dy = SettingRendererRegistry.getInstance().render(guiGraphics, setting, n11 + (int)(10.0f * f), settingY, n5 - (int)(20.0f * f), n3, n4, bodyAlpha, f);
                            settingY += dy;
                        }
                    } else {
                        String string = this.getModuleDescription(bodyModule);
                        if (string != null && !string.isEmpty()) {
                            FontRenderer fontRenderer = FontPresets.axiformaRegular(12.0f * f);
                            this.renderWrappedText(string, n11 + (int)(10.0f * f), n12 + n9 + (int)(10.0f * f), n5 - (int)(20.0f * f), fontRenderer, -5592406, bodyAlpha, f);
                        }
                    }
                    guiGraphics.pose().popPose();
                    drawContext.restore();
                }
                drawContext.restore();
            });
            StencilHelper.end();
            int n22 = n8 - 2 * n6 - (int)(20.0f * f);
            float f5 = n22 - n9 - n10;
            if (this.totalContentHeight > f5) {
                float f6 = this.totalContentHeight - f5;
                if (this.scrollOffset > f6) {
                    this.scrollOffset = f6;
                    this.scrollTarget = f6;
                }
                if (this.scrollTarget > f6) {
                    this.scrollTarget = f6;
                }
            } else {
                this.scrollOffset = 0.0f;
                this.scrollTarget = 0.0f;
            }
            this.renderScrollbar(guiGraphics, n11, n12, n13, f, f2);
        } catch (Exception exception) {
            // empty catch block
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int n, int n2, int n3, float f, float f2) {
        float f3;
        int n4 = (int)(30.0f * f);
        int n5 = (int)(10.0f * f);
        float f4 = n3 - n4 - n5;
        if (this.totalContentHeight <= f4) {
            f3 = 0.0f;
        } else {
            long l = System.currentTimeMillis() - this.lastScrollTime;
            if (this.isDraggingScrollbar || l < 500L) {
                f3 = 1.0f;
            } else if (l < 1000L) {
                long l2 = l - 500L;
                f3 = 1.0f - (float)l2 / 500.0f;
            } else {
                f3 = 0.0f;
            }
        }
        this.scrollbarAlpha = Math.abs(this.scrollbarAlpha - f3) > 0.01f ? LerpUtil.smoothLerp(this.scrollbarAlpha, f3, 0.35f) : f3;
        if (this.scrollbarAlpha <= 0.01f) {
            return;
        }
        float f5 = this.totalContentHeight - f4;
        if (f5 <= 0.0f) {
            return;
        }
        float f6 = Math.max(20.0f * f, f4 / this.totalContentHeight * f4);
        float f7 = (float)(n2 + n4) + this.scrollOffset / f5 * (f4 - f6);
        int n6 = n + (int)(405.0f * f) - (int)(4.0f * f) - 2;
        float f8 = 4.0f * f;
        int n7 = new Color(1.0f, 1.0f, 1.0f, this.scrollbarAlpha * f2).getRGB();
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n6, f7, f8, f6, f8 / 2.0f, n7);
    }

    private void checkToggleHover(int n, int n2, int n3, int n4, int n5) {
        this.isToggleHovered = n3 >= n && n3 <= n + n5 * 2 && n4 >= n2 && n4 <= n2 + n5;
    }

    private void renderToggleButton(GuiGraphics guiGraphics, int n, int n2, float f, float f2, boolean bl, float f3, float f4) {
        int n3;
        int n4 = (int)(12.0f * f3);
        int n5 = n4 * 2;
        int n6 = n4;
        int n7 = this.lerpColor(TOGGLE_OFF_COLOR, TOGGLE_ON_COLOR, f);
        if (f2 > 0.0f) {
            float f5 = 1.0f + 0.3f * f2;
            n7 = this.brightenColor(n7, f5);
        }
        int n8 = this.applyAlpha(n7, f4);
        if (bl) {
            n3 = this.applyAlpha(new Color(76, 175, 80, 70).getRGB(), f4);
            RenderUtil.drawRoundedRect(guiGraphics.pose(), (float)n - f3, (float)n2 - f3, (float)n5 + 2.0f * f3, (float)n6 + 2.0f * f3, (float)n6 / 2.0f, n3);
        }
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n, n2, n5, n6, (float)n6 / 2.0f, n8);
        n3 = n4 - (int)(4.0f * f3);
        int n9 = (n6 - n3) / 2;
        int n10 = n + n9;
        int n11 = n + n5 - n3 - n9;
        int n12 = n10 + (int)((float)(n11 - n10) * f);
        int n13 = n2 + n9;
        if (f2 > 0.0f) {
            int n14 = (int)(50.0f * f2);
            int n15 = n14 << 24 | 0xFFFFFF;
            float f6 = (float)n3 + 2.0f * f3;
            RenderUtil.drawRoundedRect(guiGraphics.pose(), n12 - 1, n13 - 1, f6, f6, f6 / 2.0f, this.applyAlpha(n15, f4));
        }
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n12, n13, n3, n3, (float)n3 / 2.0f, this.applyAlpha(-1, f4));
    }

    private int renderWrappedText(String string, int n, int n2, int n3, FontRenderer fontRenderer, int n4, float f, float f2) {
        String[] stringArray = string.split(" ");
        StringBuilder stringBuilder = new StringBuilder();
        int n5 = n2;
        int n6 = (int)(16.0f * f2);
        for (String string2 : stringArray) {
            String string3 = stringBuilder.length() == 0 ? string2 : stringBuilder + " " + string2;
            float f3 = GlHelper.getStringWidth(string3, fontRenderer);
            if (f3 > (float)n3 && stringBuilder.length() > 0) {
                GlHelper.drawText(stringBuilder.toString(), n, n5, fontRenderer, this.applyAlpha(n4, f));
                stringBuilder = new StringBuilder(string2);
                n5 += n6;
                continue;
            }
            stringBuilder = new StringBuilder(string3);
        }
        if (stringBuilder.length() > 0) {
            GlHelper.drawText(stringBuilder.toString(), n, n5, fontRenderer, this.applyAlpha(n4, f));
        }
        return n5 - n2 + n6;
    }

    private String getModuleDescription(Module module) {
        try {
            return "This module provides " + module.getName().toLowerCase() + " functionality.";
        } catch (Exception exception) {
            return "No description available.";
        }
    }

    private void calculateTotalHeight(Module module, float f) {
        if (module == null) {
            this.totalContentHeight = 0.0f;
            return;
        }
        List<Setting<?>> list = module.getSettings();
        if (list != null && !list.isEmpty()) {
            int n = 0;
            for (Setting setting : list) {
                if (setting.getVisibility() != null && !setting.getVisibility().displayable()) continue;
                n += SettingRendererRegistry.getInstance().getHeightForScroll(setting, f);
            }
            this.totalContentHeight = n;
        } else {
            String string = this.getModuleDescription(module);
            if (string != null && !string.isEmpty()) {
                FontRenderer fontRenderer = FontPresets.axiformaRegular(12.0f * f);
                this.totalContentHeight = this.calcWrappedTextHeight(string, (int)(385.0f * f), fontRenderer, f);
            } else {
                this.totalContentHeight = 0.0f;
            }
        }
    }

    private int calcWrappedTextHeight(String string, int n, FontRenderer fontRenderer, float f) {
        if (string == null || string.isEmpty()) {
            return 0;
        }
        String[] stringArray = string.split(" ");
        StringBuilder stringBuilder = new StringBuilder();
        int n2 = 1;
        int n3 = (int)(16.0f * f);
        for (String string2 : stringArray) {
            String string3;
            String string4 = string3 = stringBuilder.length() == 0 ? string2 : stringBuilder + " " + string2;
            if (GlHelper.getStringWidth(string3, fontRenderer) > (float)n && stringBuilder.length() > 0) {
                ++n2;
                stringBuilder = new StringBuilder(string2);
                continue;
            }
            stringBuilder = new StringBuilder(string3);
        }
        return n2 * n3;
    }

    private int applyAlpha(int n, float f) {
        int n2 = n >> 24 & 0xFF;
        int n3 = (int)((float)n2 * f);
        return n3 << 24 | n & 0xFFFFFF;
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

    public boolean onMouseClick(int n, int n2, int n3, int n4, int n5, float f) {
        if (this.animationState != SettingsPanel.AnimationState.NONE) {
            return true;
        }
        if (this.currentModule == null) {
            return false;
        }
        int n6 = (int)(405.0f * f);
        int n7 = (int)(20.0f * f);
        int n8 = (int)(20.0f * f);
        int n9 = (int)(400.0f * f);
        int n10 = (int)(30.0f * f);
        int n11 = (int)(10.0f * f);
        int n12 = n + (int)(600.0f * f) - n6 - n7 + (int)(8.0f * f);
        int n13 = n2 + n8 + (int)(23.0f * f);
        int n14 = n9 - 2 * n7 - (int)(20.0f * f);
        float f2 = n14 - n10 - n11;
        if (this.totalContentHeight > f2) {
            float f3 = this.totalContentHeight - f2;
            float f4 = Math.max(20.0f * f, f2 / this.totalContentHeight * f2);
            float f5 = (float)(n13 + n10) + this.scrollOffset / f3 * (f2 - f4);
            float f6 = 4.0f * f;
            int n15 = n12 + n6 - (int)f6 - 2;
            if (n3 >= n15 && (float)n3 <= (float)n15 + f6 && (float)n4 >= f5 && (float)n4 <= f5 + f4) {
                this.isDraggingScrollbar = true;
                this.scrollbarDragStartY = n4;
                this.scrollOffsetAtDragStart = this.scrollOffset;
                this.lastScrollTime = System.currentTimeMillis();
                return true;
            }
        }
        int n16 = (int)(12.0f * f);
        int n17 = (int)(15.0f * f);
        int n18 = n12 + n6 - n16 * 2 - n17;
        int n19 = n13 + (n10 - n16) / 2;
        if (n5 == 0 && n3 >= n18 && n3 <= n18 + n16 * 2 && n4 >= n19 && n4 <= n19 + n16) {
            this.currentModule.toggle();
            String string = this.currentModule.isEnabled() ? "On" : "Off";
            PanelClickGui.panelClickGui.addToast(this.currentModule.getName() + " Module " + string);
            return true;
        }
        List<Setting<?>> list = this.currentModule.getSettings();
        if (list != null && !list.isEmpty()) {
            int n20 = n13 + n10;
            int n21 = n4 + (int)this.scrollOffset;
            for (Setting setting : list) {
                if (setting.getVisibility() != null && !setting.getVisibility().displayable()) continue;
                int n22 = SettingRendererRegistry.getInstance().getHeightForScroll(setting, f);
                if (n21 >= n20 && n21 <= n20 + n22 && SettingRendererRegistry.getInstance().onClick(setting, n12 + (int)(10.0f * f), n20 - (int)this.scrollOffset, n6 - (int)(20.0f * f), n3, n4, n5, f)) {
                    return true;
                }
                n20 += n22;
            }
        }
        return false;
    }

    public boolean isMouseOverPanel(int n, int n2, int n3, int n4, float f) {
        if (this.currentModule == null) {
            return false;
        }
        int n5 = (int)(405.0f * f);
        int n6 = (int)(20.0f * f);
        int n7 = (int)(20.0f * f);
        int n8 = (int)(400.0f * f);
        int n9 = n + (int)(600.0f * f) - n5 - n6 + (int)(8.0f * f);
        int n10 = n2 + n7 + (int)(23.0f * f);
        int n11 = n8 - 2 * n6 - (int)(20.0f * f);
        return n3 >= n9 && n3 <= n9 + n5 && n4 >= n10 && n4 <= n10 + n11;
    }

    public void onScroll(double d, float f) {
        int n = (int)(400.0f * f) - (int)(40.0f * f) - (int)(20.0f * f);
        float f2 = (float)n - 30.0f * f - 10.0f * f;
        if (this.totalContentHeight > f2) {
            float f3 = this.totalContentHeight - f2;
            this.scrollTarget -= (float)d * 18.0f * f;
            this.scrollTarget = Math.max(0.0f, Math.min(this.scrollTarget, f3));
            this.lastScrollTime = System.currentTimeMillis();
        }
    }

    public void onMouseDrag(double d, double d2, float f) {
        if (this.isDraggingScrollbar) {
            int n = (int)(400.0f * f) - (int)(40.0f * f) - (int)(20.0f * f);
            float f2 = (float)n - 30.0f * f - 10.0f * f;
            float f3 = this.totalContentHeight - f2;
            float f4 = Math.max(20.0f * f, f2 / this.totalContentHeight * f2);
            float f5 = f2 - f4;
            if (f5 > 0.0f) {
                float f6 = (float)d2 - this.scrollbarDragStartY;
                float f7 = f6 / f5 * f3;
                this.scrollOffset = this.scrollOffsetAtDragStart + f7;
                this.scrollTarget = this.scrollOffset = Math.max(0.0f, Math.min(this.scrollOffset, f3));
            }
            this.lastScrollTime = System.currentTimeMillis();
        }
        if (this.currentModule != null) {
            for (Setting setting : this.currentModule.getSettings()) {
                SettingRenderer settingRenderer;
                if (setting.getVisibility() != null && !setting.getVisibility().displayable() || (settingRenderer = SettingRendererRegistry.getInstance().findRenderer(setting)) == null) continue;
                settingRenderer.onMouseMove(d, d2);
            }
        }
    }

    public void onMouseRelease(double d, double d2, int n) {
        this.isDraggingScrollbar = false;
        this.lastScrollTime = System.currentTimeMillis();
        if (this.currentModule != null) {
            for (Setting setting : this.currentModule.getSettings()) {
                SettingRenderer settingRenderer;
                if (setting.getVisibility() != null && !setting.getVisibility().displayable() || (settingRenderer = SettingRendererRegistry.getInstance().findRenderer(setting)) == null) continue;
                settingRenderer.onMouseRelease(d, d2, n);
            }
        }
    }

    public Module getCurrentModule() {
        return this.currentModule;
    }

    public void setCurrentModule(Module module) {
        this.currentModule = module;
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

    private void rescaleScroll(float f) {
        float f2;
        int n;
        float f3;
        if (this.lastScale <= 0.0f) {
            return;
        }
        float f4 = f / this.lastScale;
        this.scrollOffset *= f4;
        this.scrollTarget *= f4;
        if (this.currentModule != null) {
            this.calculateTotalHeight(this.currentModule, f);
        }
        if (this.totalContentHeight > (f3 = (float)(n = (int)(400.0f * f) - (int)(40.0f * f) - (int)(20.0f * f)) - (f2 = 30.0f * f))) {
            float f5 = this.totalContentHeight - f3;
            this.scrollOffset = Math.max(0.0f, Math.min(this.scrollOffset, f5));
            this.scrollTarget = Math.max(0.0f, Math.min(this.scrollTarget, f5));
        } else {
            this.scrollOffset = 0.0f;
            this.scrollTarget = 0.0f;
        }
    }
}