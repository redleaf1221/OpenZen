package shit.zen.gui.panel;

import java.awt.Color;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import shit.zen.ClientBase;
import shit.zen.ZenClient;
import shit.zen.gui.PanelClickGui;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.render.DrawContext;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Rectangle;
import shit.zen.render.Renderer;
import shit.zen.render.TextGlow;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class ModuleListPanel
extends ClientBase {
    public enum AnimationState { NONE, FADE_IN, FADE_OUT, SWITCHING }

    private static final int HOVER_BG_COLOR = new Color(255, 255, 255, 20).getRGB();
    private final Map<Module, Float> hoverAnimations = new HashMap<>();
    private Module hoveredModule;
    private ModuleListPanel.AnimationState animationState = ModuleListPanel.AnimationState.NONE;
    private float animProgress = 0.0f;
    private Category currentCategory;
    private Category prevCategory;
    private List<Module> currentModules;
    private List<Module> prevModules;
    private float lastScale = 1.0f;
    private float scrollOffset = 0.0f;
    private float scrollTarget = 0.0f;
    private float totalContentHeight = 0.0f;
    private boolean isDraggingScrollbar = false;
    private float scrollbarDragStartY = 0.0f;
    private float scrollOffsetAtDragStart = 0.0f;
    private float scrollbarAlpha = 0.0f;
    private long lastScrollTime = 0L;
    private String searchQuery = "";
    private List<Module> searchResults;

    public void setSearchQuery(String string) {
        if (string == null) {
            this.searchQuery = "";
            this.searchResults = null;
            return;
        }
        this.searchQuery = string;
        this.searchResults = !string.isEmpty() ? ZenClient.instance.getModuleManager().getModules().stream().filter(module -> module.getName().toLowerCase().contains(string.toLowerCase())).sorted(Comparator.comparing(Module::getName)).collect(Collectors.toList()) : ZenClient.instance.getModuleManager().getModules().stream().sorted(Comparator.comparing(Module::getName)).collect(Collectors.toList());
        this.currentCategory = null;
        this.scrollOffset = 0.0f;
        this.scrollTarget = 0.0f;
        this.animationState = ModuleListPanel.AnimationState.NONE;
    }

    public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, Category category, float f, float f2) {
        if (Math.abs(f - this.lastScale) > 0.001f) {
            this.rescaleScroll(f);
            this.lastScale = f;
        }
        this.scrollOffset = Math.abs(this.scrollOffset - this.scrollTarget) > 0.01f ? LerpUtil.smoothLerp(this.scrollOffset, this.scrollTarget, 0.35f) : this.scrollTarget;
        if (this.searchResults != null) {
            this.renderSearchResults(guiGraphics, n, n2, n3, n4, f, f2);
            return;
        }
        if (this.currentCategory != category) {
            this.scrollOffset = 0.0f;
            this.scrollTarget = 0.0f;
            if (this.currentCategory != null) {
                this.prevCategory = this.currentCategory;
                this.prevModules = this.currentModules;
                this.animationState = ModuleListPanel.AnimationState.FADE_OUT;
                this.animProgress = 0.0f;
            } else if (category != null) {
                this.animationState = ModuleListPanel.AnimationState.FADE_IN;
                this.animProgress = 0.0f;
            }
            this.currentCategory = category;
            List<Module> list = this.currentModules = category == null ? null : ZenClient.instance.getModuleManager().getModules().stream().filter(module -> module.getCategory() == category).sorted(Comparator.comparing(Module::getName)).collect(Collectors.toList());
        }
        if (this.animationState != ModuleListPanel.AnimationState.NONE) {
            this.animProgress = LerpUtil.smoothLerp(this.animProgress, 1.0f, 0.18f);
            if (this.animProgress > 0.99f) {
                this.animProgress = 1.0f;
                if (this.animationState == ModuleListPanel.AnimationState.FADE_OUT) {
                    this.prevCategory = null;
                    this.prevModules = null;
                    if (this.currentCategory != null) {
                        this.animationState = ModuleListPanel.AnimationState.FADE_IN;
                        this.animProgress = 0.0f;
                    } else {
                        this.animationState = ModuleListPanel.AnimationState.NONE;
                    }
                } else {
                    this.animationState = ModuleListPanel.AnimationState.NONE;
                }
            }
        }
        try {
            int n5 = (int)(160.0f * f);
            int n6 = (int)(20.0f * f);
            int n7 = (int)(20.0f * f);
            int n8 = (int)(400.0f * f);
            int n9 = n + n6 - (int)(8.0f * f);
            int n10 = n2 + n7 + (int)(23.0f * f);
            int n11 = n8 - 2 * n6 - (int)(20.0f * f);
            RenderUtil.drawRoundedRect(guiGraphics.pose(), n9, n10, n5, n11, 4.0f * f, this.applyAlpha(HOVER_BG_COLOR, f2));
            Renderer.renderConsumer(drawContext -> {
                Category renderCategory = this.currentCategory;
                List<Module> renderList = this.currentModules;
                float slideOffset = 0.0f;
                float renderAlpha = f2;
                if (this.animationState == ModuleListPanel.AnimationState.FADE_OUT) {
                    renderCategory = this.prevCategory;
                    renderList = this.prevModules;
                    slideOffset = this.animProgress * 20.0f * f;
                    renderAlpha = (1.0f - this.animProgress) * f2;
                } else if (this.animationState == ModuleListPanel.AnimationState.FADE_IN) {
                    renderCategory = this.currentCategory;
                    renderList = this.currentModules;
                    slideOffset = (1.0f - this.animProgress) * 20.0f * f;
                    renderAlpha = this.animProgress * f2;
                }
                drawContext.save();
                drawContext.clip(Rectangle.ofXYWH(n9, n10, n5, n11));
                if (renderCategory != null) {
                    drawContext.save();
                    drawContext.translate(0.0f, slideOffset);
                    this.renderModuleList(renderCategory, renderList, n9, n10, n11, n3, n4, renderAlpha, this.animationState != ModuleListPanel.AnimationState.NONE, f);
                    drawContext.restore();
                }
                drawContext.restore();
            });
            if (this.animationState == ModuleListPanel.AnimationState.NONE) {
                this.renderScrollbar(guiGraphics, n9, n10, n11, f, f2);
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }

    private void renderSearchResults(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, float f, float f2) {
        try {
            int n5 = (int)(160.0f * f);
            int n6 = (int)(20.0f * f);
            int n7 = (int)(20.0f * f);
            int n8 = (int)(400.0f * f);
            int n9 = n + n6 - (int)(8.0f * f);
            int n10 = n2 + n7 + (int)(23.0f * f);
            int n11 = n8 - 2 * n6 - (int)(20.0f * f);
            RenderUtil.drawRoundedRect(guiGraphics.pose(), n9, n10, n5, n11, 4.0f * f, this.applyAlpha(HOVER_BG_COLOR, f2));
            Renderer.renderConsumer((drawContext -> this.renderModuleList(null, this.searchResults, n9, n10, n11, n3, n4, f2, false, f)));
            this.renderScrollbar(guiGraphics, n9, n10, n11, f, f2);
        } catch (Exception exception) {
            // empty catch block
        }
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int n, int n2, int n3, float f, float f2) {
        float f3;
        float f4 = 30.0f * f;
        float f5 = (float)n3 - f4;
        if (this.totalContentHeight <= f5) {
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
        float f6 = this.totalContentHeight - f5;
        if (f6 <= 0.0f) {
            return;
        }
        float f7 = Math.max(20.0f * f, f5 / this.totalContentHeight * f5);
        float f8 = (float)n2 + f4 + this.scrollOffset / f6 * (f5 - f7);
        int n4 = n + (int)(160.0f * f) - (int)(4.0f * f) - 2;
        float f9 = 4.0f * f;
        int n5 = new Color(1.0f, 1.0f, 1.0f, this.scrollbarAlpha * f2).getRGB();
        RenderUtil.drawRoundedRect(guiGraphics.pose(), n4, f8, f9, f7, f9 / 2.0f, n5);
    }

    private void updateModuleHover(Module module, int n, int n2, int n3, int n4, float f) {
        if (module.isEnabled()) {
            this.hoverAnimations.put(module, Float.valueOf(0.0f));
            return;
        }
        float f2 = this.hoverAnimations.getOrDefault(module, Float.valueOf(0.0f)).floatValue();
        boolean bl = this.isMouseOverModule(module, n, n2, n3, n4, f);
        this.hoverAnimations.put(module, Float.valueOf(LerpUtil.lerp(f2, bl ? 1.0f : 0.0f, 0.12f)));
    }

    private int applyAlpha(int n, float f) {
        int n2 = n >> 24 & 0xFF;
        int n3 = (int)((float)n2 * f);
        return n3 << 24 | n & 0xFFFFFF;
    }

    private void renderModuleList(Category category, List<Module> list, int n, int n2, int n3, int n4, int n5, float f, boolean bl, float f2) {
        float f3 = 30.0f * f2;
        int n6 = (int)(160.0f * f2);
        FontRenderer fontRenderer = FontPresets.axiformaBold(20.0f * f2);
        String string = category == null ? "Search" : category.name().substring(0, 1).toUpperCase() + category.name().substring(1).toLowerCase();
        GlHelper.drawText(string, (float)n + 10.0f * f2, (float)n2 + 12.0f * f2, fontRenderer, this.applyAlpha(-1, f));
        FontRenderer fontRenderer2 = FontPresets.axiformaRegular(12.0f * f2);
        String string2 = category == null ? list.size() + " Results" : "Sorting: A-Z";
        float f4 = GlHelper.getStringWidth(string2, fontRenderer2);
        float f5 = (float)(n + n6) - f4 - 10.0f * f2;
        GlHelper.drawText(string2, f5, (float)n2 + 14.0f * f2, fontRenderer2, this.applyAlpha(-5592406, f));
        DrawContext drawContext = GlHelper.getCanvas();
        drawContext.save();
        drawContext.clip(Rectangle.ofXYWH(n, (float)n2 + f3, n6, (float)n3 - f3));
        int n7 = n2 + (int)f3 + (int)(2.0f * f2);
        if (list != null) {
            this.totalContentHeight = list.size() * Math.round(18.0f * f2);
            for (Module module : list) {
                float f6 = (float)n7 - this.scrollOffset;
                if (!bl) {
                    this.updateModuleHover(module, n, (int)f6, n4, n5, f2);
                }
                FontRenderer fontRenderer3 = module.isEnabled() ? FontPresets.axiformaBold(16.0f * f2) : FontPresets.axiformaRegular(16.0f * f2);
                String string3 = module.getName();
                if (this.searchResults != null) {
                    String string4 = module.getCategory().name().substring(1).toLowerCase();
                    char c = module.getCategory().name().charAt(0);
                    string3 = string3 + " (" + c + string4 + ")";
                }
                if (module.isEnabled()) {
                    int n8 = this.applyAlpha(-1, f);
                    int glowColor = this.applyAlpha(new Color(255, 255, 255, 150).getRGB(), f);
                    TextGlow.drawGlowText(string3, (float)n + 10.0f * f2, f6, fontRenderer3, n8, glowColor, 8.0f * f2);
                    String string5 = module.getBind().getName();
                    if (!string5.equalsIgnoreCase("None")) {
                        FontRenderer fontRenderer4 = FontPresets.materialIcons(16.0f * f2);
                        String string6 = "";
                        FontRenderer fontRenderer5 = FontPresets.axiformaRegular(16.0f * f2);
                        float f7 = GlHelper.getStringWidth(string5, fontRenderer5);
                        float f8 = GlHelper.getStringWidth(string6, fontRenderer4);
                        float f9 = 2.0f * f2;
                        float f10 = f7 + f8 + f9;
                        float f11 = (float)(n + n6) - f10 - 10.0f * f2;
                        TextGlow.drawGlowText(string6, f11, f6 - (float)Math.round(f2), fontRenderer4, n8, glowColor, 8.0f * f2);
                        TextGlow.drawGlowText(string5, f11 + f8 + f9, f6 - 2.0f, fontRenderer3, n8, glowColor, 8.0f * f2);
                    }
                } else {
                    float f12 = this.hoverAnimations.getOrDefault(module, Float.valueOf(0.0f)).floatValue();
                    int glowColor = 170;
                    int n9 = 170;
                    int n10 = 170;
                    int n11 = 204;
                    int n12 = 204;
                    int n13 = 204;
                    int n14 = (int)((float)glowColor + (float)(n11 - glowColor) * f12);
                    int n15 = (int)((float)n9 + (float)(n12 - n9) * f12);
                    int n16 = (int)((float)n10 + (float)(n13 - n10) * f12);
                    int n17 = 0xFF000000 | n14 << 16 | n15 << 8 | n16;
                    int n18 = this.applyAlpha(n17, f);
                    GlHelper.drawText(string3, (float)n + 10.0f * f2, f6, fontRenderer3, n18);
                    String string7 = module.getBind().getName();
                    if (!string7.equalsIgnoreCase("None")) {
                        int n19 = (int)(f * 0.39215687f * f12);
                        int n20 = this.applyAlpha(-3355444, (float)n19 / 255.0f);
                        FontRenderer fontRenderer6 = FontPresets.materialIcons(16.0f * f2);
                        String string8 = "";
                        FontRenderer fontRenderer7 = FontPresets.axiformaRegular(16.0f * f2);
                        float f13 = GlHelper.getStringWidth(string7, fontRenderer7);
                        float f14 = GlHelper.getStringWidth(string8, fontRenderer6);
                        float f15 = 2.0f * f2;
                        float f16 = f13 + f14 + f15;
                        float f17 = (float)(n + n6) - f16 - 10.0f * f2;
                        TextGlow.drawGlowText(string8, f17, f6 - (float)Math.round(f2), fontRenderer6, n18, n20, 5.0f * f2);
                        TextGlow.drawGlowText(string7, f17 + f14 + f15, f6 - 2.0f, fontRenderer3, n18, n20, 5.0f * f2);
                    }
                }
                n7 += Math.round(18.0f * f2);
            }
        } else {
            this.totalContentHeight = 0.0f;
        }
        drawContext.restore();
    }

    public boolean onMouseClick(int n, int n2, int n3, int n4, Category category, int n5, float f) {
        List<Module> list;
        List<Module> list2 = list = !this.searchQuery.isEmpty() ? this.searchResults : this.currentModules;
        if (list == null || this.searchQuery.isEmpty() && this.animationState != ModuleListPanel.AnimationState.NONE) {
            return false;
        }
        int n6 = (int)(160.0f * f);
        int n7 = (int)(20.0f * f);
        int n8 = (int)(20.0f * f);
        int n9 = (int)(400.0f * f);
        int n10 = (int)(30.0f * f);
        int n11 = n + n7 - (int)(8.0f * f);
        int n12 = n2 + n8 + (int)(23.0f * f);
        int n13 = n9 - 2 * n7 - (int)(20.0f * f);
        float f2 = n13 - n10;
        if (this.totalContentHeight > f2) {
            float f3 = this.totalContentHeight - f2;
            float f4 = Math.max(20.0f * f, f2 / this.totalContentHeight * f2);
            float f5 = (float)(n12 + n10) + this.scrollOffset / f3 * (f2 - f4);
            float f6 = 4.0f * f;
            int n14 = n11 + n6 - (int)f6 - 2;
            if (n3 >= n14 && (float)n3 <= (float)n14 + f6 && (float)n4 >= f5 && (float)n4 <= f5 + f4) {
                this.isDraggingScrollbar = true;
                this.scrollbarDragStartY = n4;
                this.scrollOffsetAtDragStart = this.scrollOffset;
                this.lastScrollTime = System.currentTimeMillis();
                return true;
            }
        }
        if (!this.isMouseOverPanel(n, n2, n3, n4, f)) {
            return false;
        }
        int n15 = n12 + n10 + Math.round(2.0f * f);
        for (Module module : list) {
            int n16;
            if (this.isMouseOverModule(module, n11, n16 = (int)((float)n15 - this.scrollOffset), n3, n4, f)) {
                if (n5 == 0) {
                    module.toggle();
                    String string = module.isEnabled() ? "On" : "Off";
                    PanelClickGui.panelClickGui.addToast(module.getName() + " Module " + string);
                } else if (n5 == 1) {
                    this.hoveredModule = module;
                } else if (n5 == 2) {
                    PanelClickGui.panelClickGui.selectModule(module);
                }
                return true;
            }
            n15 += Math.round(18.0f * f);
        }
        return false;
    }

    private boolean isMouseOverModule(Module module, int n, int n2, int n3, int n4, float f) {
        int n5 = Math.round(18.0f * f);
        int n6 = (int)(160.0f * f);
        boolean bl = n3 >= n && n3 <= n + n6;
        boolean bl2 = n4 >= n2 - n5 / 2 && n4 <= n2 + n5 / 2;
        return bl && bl2;
    }

    public void onScroll(double d, float f) {
        int n = (int)(400.0f * f) - (int)(40.0f * f) - (int)(20.0f * f);
        float f2 = 30.0f * f;
        float f3 = (float)n - f2;
        if (this.totalContentHeight > f3) {
            float f4 = this.totalContentHeight - f3;
            this.scrollTarget -= (float)d * 18.0f * f;
            this.scrollTarget = Math.max(0.0f, Math.min(this.scrollTarget, f4));
            this.lastScrollTime = System.currentTimeMillis();
        }
    }

    public void onMouseDrag(double d, double d2, float f) {
        if (this.isDraggingScrollbar) {
            int n = (int)(400.0f * f) - (int)(40.0f * f) - (int)(20.0f * f);
            float f2 = (float)n - 30.0f * f;
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
    }

    public void onMouseRelease() {
        this.isDraggingScrollbar = false;
        this.lastScrollTime = System.currentTimeMillis();
    }

    public boolean isMouseOverPanel(int n, int n2, int n3, int n4, float f) {
        int n5 = (int)(160.0f * f);
        int n6 = (int)(20.0f * f);
        int n7 = (int)(20.0f * f);
        int n8 = (int)(400.0f * f);
        int n9 = (int)(30.0f * f);
        int n10 = n + n6 - (int)(8.0f * f);
        int n11 = n2 + n7 + (int)(23.0f * f);
        int n12 = n8 - 2 * n6 - (int)(20.0f * f);
        return n3 >= n10 && n3 <= n10 + n5 && n4 >= n11 + n9 && n4 <= n11 + n12;
    }

    public Module getHoveredModule() {
        return this.hoveredModule;
    }

    public void setHoveredModule(Module module) {
        this.hoveredModule = module;
    }

    private void rescaleScroll(float f) {
        List<Module> list;
        if (this.lastScale <= 0.0f) {
            return;
        }
        float f2 = f / this.lastScale;
        this.scrollOffset *= f2;
        this.scrollTarget *= f2;
        int n = (int)(400.0f * f) - (int)(40.0f * f) - (int)(20.0f * f);
        float f3 = 30.0f * f;
        float f4 = (float)n - f3;
        List<Module> list2 = list = !this.searchQuery.isEmpty() ? this.searchResults : this.currentModules;
        if (list != null) {
            this.totalContentHeight = list.size() * Math.round(18.0f * f);
        }
        if (this.totalContentHeight > f4) {
            float f5 = this.totalContentHeight - f4;
            this.scrollOffset = Math.max(0.0f, Math.min(this.scrollOffset, f5));
            this.scrollTarget = Math.max(0.0f, Math.min(this.scrollTarget, f5));
        } else {
            this.scrollOffset = 0.0f;
            this.scrollTarget = 0.0f;
        }
    }
}