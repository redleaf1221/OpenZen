package shit.zen.gui;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import shit.zen.ZenClient;
import shit.zen.gui.panel.CategoryBar;
import shit.zen.gui.panel.KeybindOverlay;
import shit.zen.gui.panel.ModuleListPanel;
import shit.zen.gui.panel.ProfileWidget;
import shit.zen.gui.panel.ScaleSwitchOverlay;
import shit.zen.gui.panel.SettingsPanel;
import shit.zen.gui.panel.setting.NumberSettingRenderer;
import shit.zen.modules.Category;
import shit.zen.modules.Module;
import shit.zen.render.FontPresets;
import shit.zen.render.FontRenderer;
import shit.zen.render.GlHelper;
import shit.zen.render.Renderer;
import shit.zen.render.TextGlow;
import shit.zen.utils.math.LerpUtil;
import shit.zen.utils.render.RenderUtil;

public class PanelClickGui
extends Screen {

    public enum OpenState {
        CLOSED, OPENING, OPEN, CLOSING
    }

    public enum ScaleSwitchState {
        IDLE, FADING_OUT, WAITING, FADING_IN
    }

    public static final class ToastEntry {
        final String message;
        final long createdAt;
        float currentY;
        float targetY;
        float alpha;

        public ToastEntry(String message) {
            this.message = message;
            this.createdAt = System.currentTimeMillis();
            this.currentY = 20.0f;
            this.targetY = 0.0f;
            this.alpha = 0.0f;
        }
    }

    public static PanelClickGui panelClickGui = new PanelClickGui();

    private boolean searchActive = false;
    private boolean searchFocused = false;
    private String searchQuery = "";
    private long searchCursorTime = 0L;
    private final List<PanelClickGui.ToastEntry> toasts = new CopyOnWriteArrayList<>();
    private PanelClickGui.OpenState currentScaleSwitchState = PanelClickGui.OpenState.CLOSED;
    private float openProgress = 0.0f;
    private float currentScale = 1.0f;
    private final ProfileWidget profileWidget;
    private final CategoryBar categoryBar;
    private final ModuleListPanel moduleListPanel;
    private final SettingsPanel settingsPanel;
    public final KeybindOverlay keybindOverlay = new KeybindOverlay();
    private final ScaleSwitchOverlay scaleSwitchOverlay = new ScaleSwitchOverlay();
    private PanelClickGui.ScaleSwitchState currentOpenState = PanelClickGui.ScaleSwitchState.IDLE;
    private long scaleWaitStart = 0L;
    private float targetScale = 1.0f;
    private float panelAlpha = 1.0f;

    protected PanelClickGui() {
        super(Component.nullToEmpty("New Click GUI"));
        this.profileWidget = new ProfileWidget(this::setScale);
        this.categoryBar = new CategoryBar();
        this.moduleListPanel = new ModuleListPanel();
        this.settingsPanel = new SettingsPanel();
    }

    public void setScale(float f) {
        if (this.currentOpenState == PanelClickGui.ScaleSwitchState.IDLE && f != this.currentScale) {
            this.targetScale = f;
            this.currentOpenState = PanelClickGui.ScaleSwitchState.FADING_OUT;
            this.scaleSwitchOverlay.show(this.currentScale, f);
        }
    }

    public void init() {
        super.init();
        LerpUtil.reset();
        if (this.currentScaleSwitchState == PanelClickGui.OpenState.CLOSED) {
            this.openProgress = 0.0f;
        }
        this.currentScaleSwitchState = PanelClickGui.OpenState.OPENING;
    }

    private void updateOpenState() {
        if (this.currentScaleSwitchState == PanelClickGui.OpenState.OPENING) {
            this.openProgress = LerpUtil.lerp(this.openProgress, 1.0f, 0.08f);
            if (this.openProgress >= 1.0f) {
                this.currentScaleSwitchState = PanelClickGui.OpenState.OPEN;
            }
        } else if (this.currentScaleSwitchState == PanelClickGui.OpenState.CLOSING) {
            this.openProgress = LerpUtil.lerp(this.openProgress, 0.0f, 0.1f);
            if (this.openProgress <= 0.0f) {
                this.currentScaleSwitchState = PanelClickGui.OpenState.CLOSED;
                if (ZenClient.isReady()) {
                    ZenClient.instance.getConfigManager().saveAll();
                }
                this.minecraft.setScreen(null);
            }
        }
    }

    private float easeOutCubic(float f) {
        return (float)(1.0 - Math.pow(1.0f - f, 3.0));
    }

    public void render(@Nonnull GuiGraphics guiGraphics, int n, int n2, float f) {
        float f2;
        LerpUtil.update();
        this.updateOpenState();
        if (this.currentScaleSwitchState == PanelClickGui.OpenState.CLOSED && this.openProgress <= 0.0f) {
            return;
        }
        float f3 = f2 = this.easeOutCubic(this.openProgress);
        float f4 = 0.98f + 0.02f * f2;
        guiGraphics.fill(0, 0, this.width, this.height, new Color(0, 0, 0, (int)(80.0f * f3)).getRGB());
        this.updateScaleSwitchState();
        int n3 = this.width / 2;
        int n4 = this.height / 2;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float)n3, (float)n4, 0.0f);
        guiGraphics.pose().scale(f4, f4, 1.0f);
        guiGraphics.pose().translate((float)(-n3), (float)(-n4), 0.0f);
        float f5 = this.currentOpenState == PanelClickGui.ScaleSwitchState.FADING_OUT || this.currentOpenState == PanelClickGui.ScaleSwitchState.WAITING ? this.targetScale : this.currentScale;
        int n5 = (int)(600.0f * this.currentScale);
        int n6 = (int)(400.0f * this.currentScale);
        int n7 = n3 - n5 / 2;
        int n8 = n4 - n6 / 2;
        float f6 = this.panelAlpha * f3;
        if (f6 > 0.005f) {
            this.drawPanelGlow(guiGraphics, n7, n8, f, f6);
            this.categoryBar.render(guiGraphics, n7, n8, n, n2, this.currentScale, f6);
            this.moduleListPanel.render(guiGraphics, n7, n8, n, n2, this.getSelectedCategory(), this.currentScale, f6);
            this.settingsPanel.render(guiGraphics, n7, n8, n, n2, this.moduleListPanel.getHoveredModule(), this.currentScale, f6);
            this.profileWidget.render(guiGraphics, n7, n8, n, n2, this.currentScale, f6);
            this.drawSearchBar(guiGraphics, n7, n8, n, n2, f6);
            this.drawToasts(guiGraphics, n7, n8, f6);
        }
        super.render(guiGraphics, n, n2, f);
        guiGraphics.pose().popPose();
        this.keybindOverlay.render(guiGraphics, this.width, this.height, f5);
        this.scaleSwitchOverlay.render(guiGraphics, this.width, this.height, f5);
    }

    private void updateScaleSwitchState() {
        switch (this.currentOpenState) {
            case FADING_OUT: {
                this.panelAlpha = LerpUtil.lerp(this.panelAlpha, 0.0f, 0.08f);
                if (!(this.panelAlpha <= 0.0f)) break;
                this.currentOpenState = PanelClickGui.ScaleSwitchState.WAITING;
                this.scaleWaitStart = System.currentTimeMillis();
                break;
            }
            case WAITING: {
                if (System.currentTimeMillis() - this.scaleWaitStart <= 3000L) break;
                this.currentScale = this.targetScale;
                this.currentOpenState = PanelClickGui.ScaleSwitchState.FADING_IN;
                this.scaleSwitchOverlay.hide();
                break;
            }
            case FADING_IN: {
                this.panelAlpha = LerpUtil.lerp(this.panelAlpha, 1.0f, 0.08f);
                if (!(this.panelAlpha >= 1.0f) || !this.scaleSwitchOverlay.isFullyHidden()) break;
                this.currentOpenState = PanelClickGui.ScaleSwitchState.IDLE;
            }
        }
    }

    private void drawPanelGlow(GuiGraphics guiGraphics, int n, int n2, float f, float f2) {
        int n3 = (int)(600.0f * this.currentScale);
        int n4 = (int)(400.0f * this.currentScale);
        TextGlow.drawBackground(guiGraphics.pose(), n, n2, n3, n4, 12.0f * this.currentScale, f2);
    }

    public boolean mouseClicked(double d, double d2, int n) {
        boolean bl;
        if (this.openProgress < 1.0f) {
            return true;
        }
        if (this.keybindOverlay.isVisible()) {
            if (n == 0) {
                this.keybindOverlay.cancel();
            }
            return true;
        }
        int n2 = this.width / 2;
        int n3 = this.height / 2;
        int n4 = (int)(600.0f * this.currentScale);
        int n5 = (int)(400.0f * this.currentScale);
        int n6 = n2 - n4 / 2;
        int n7 = n3 - n5 / 2;
        if (this.profileWidget.isPopupOpen() && this.profileWidget.onMouseClick(n6, n7, (int)d, (int)d2, this.currentScale)) {
            return true;
        }
        int n8 = (int)(200.0f * this.currentScale);
        int n9 = (int)(20.0f * this.currentScale);
        int n10 = n6 + (n4 - n8) / 2;
        int n11 = n7 + n5 + (int)(15.0f * this.currentScale);
        boolean bl2 = bl = d >= (double)n10 && d <= (double)(n10 + n8) && d2 >= (double)n11 && d2 <= (double)(n11 + n9);
        if (bl) {
            if (!this.searchActive) {
                this.searchActive = true;
                this.searchQuery = "";
                this.moduleListPanel.setSearchQuery("");
            }
            this.searchFocused = true;
            this.searchCursorTime = System.currentTimeMillis();
            return true;
        }
        this.searchFocused = false;
        boolean bl3 = false;
        if (n == 0 || n == 1 || n == 2) {
            if (!bl3 && n == 0 && this.categoryBar.onMouseClick(n6, n7, (int)d, (int)d2, this.currentScale)) {
                if (this.searchActive) {
                    this.searchActive = false;
                    this.searchFocused = false;
                    this.searchQuery = "";
                    this.moduleListPanel.setSearchQuery(null);
                }
                bl3 = true;
            }
            if (!bl3 && this.moduleListPanel.onMouseClick(n6, n7, (int)d, (int)d2, this.getSelectedCategory(), n, this.currentScale)) {
                bl3 = true;
            }
            if (!bl3 && this.profileWidget.onMouseClick(n6, n7, (int)d, (int)d2, this.currentScale)) {
                bl3 = true;
            }
            if (!bl3 && (n == 0 || n == 1) && this.settingsPanel.onMouseClick(n6, n7, (int)d, (int)d2, n, this.currentScale)) {
                bl3 = true;
            }
        }
        if (!bl3) {
            NumberSettingRenderer.clearEditing();
        }
        return bl3 || super.mouseClicked(d, d2, n);
    }

    public boolean mouseReleased(double d, double d2, int n) {
        if (this.openProgress < 1.0f) {
            return true;
        }
        this.moduleListPanel.onMouseRelease();
        this.settingsPanel.onMouseRelease(d, d2, n);
        this.profileWidget.onMouseRelease();
        return super.mouseReleased(d, d2, n);
    }

    public boolean mouseScrolled(double d, double d2, double d3) {
        if (this.openProgress < 1.0f) {
            return true;
        }
        int n = this.width / 2;
        int n2 = n - (int)(600.0f * this.currentScale) / 2;
        int n3 = this.height / 2;
        int n4 = n3 - (int)(400.0f * this.currentScale) / 2;
        if (this.moduleListPanel.isMouseOverPanel(n2, n4, (int)d, (int)d2, this.currentScale)) {
            this.moduleListPanel.onScroll(d3, this.currentScale);
            return true;
        }
        if (this.settingsPanel.isMouseOverPanel(n2, n4, (int)d, (int)d2, this.currentScale)) {
            this.settingsPanel.onScroll(d3, this.currentScale);
            return true;
        }
        return super.mouseScrolled(d, d2, d3);
    }

    public boolean mouseDragged(double d, double d2, int n, double d3, double d4) {
        if (this.openProgress < 1.0f) {
            return true;
        }
        this.moduleListPanel.onMouseDrag(d, d2, this.currentScale);
        this.settingsPanel.onMouseDrag(d, d2, this.currentScale);
        this.profileWidget.onMouseDrag((int)d, (int)d2);
        return super.mouseDragged(d, d2, n, d3, d4);
    }

    public boolean keyPressed(int n, int n2, int n3) {
        if (this.keybindOverlay.onKeyPress(n, n2, n3)) {
            return true;
        }
        if (this.searchActive) {
            if (n == 256) {
                this.searchActive = false;
                this.searchFocused = false;
                this.searchQuery = "";
                this.moduleListPanel.setSearchQuery(null);
                return true;
            }
            if (this.searchFocused) {
                this.searchCursorTime = System.currentTimeMillis();
                if (n == 259) {
                    if (!this.searchQuery.isEmpty()) {
                        this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                        this.moduleListPanel.setSearchQuery(this.searchQuery);
                    }
                    return true;
                }
            }
        }
        if (NumberSettingRenderer.onKeyPress(n, n2, n3)) {
            return true;
        }
        if (n == 256 && !this.keybindOverlay.isVisible() && !this.searchActive) {
            this.onClose();
            return true;
        }
        return super.keyPressed(n, n2, n3);
    }

    public boolean charTyped(char c, int n) {
        if (this.searchActive && this.searchFocused) {
            this.searchCursorTime = System.currentTimeMillis();
            this.searchQuery = this.searchQuery + c;
            this.moduleListPanel.setSearchQuery(this.searchQuery);
            return true;
        }
        if (NumberSettingRenderer.onCharTyped(c)) {
            return true;
        }
        return super.charTyped(c, n);
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void onClose() {
        if (this.currentScaleSwitchState != PanelClickGui.OpenState.CLOSING) {
            this.currentScaleSwitchState = PanelClickGui.OpenState.CLOSING;
        }
    }

    public Category getSelectedCategory() {
        return this.categoryBar.getSelectedCategory();
    }

    public CategoryBar getCategoryBar() {
        return this.categoryBar;
    }

    public ModuleListPanel getModuleListPanel() {
        return this.moduleListPanel;
    }

    public SettingsPanel getSettingsPanel() {
        return this.settingsPanel;
    }

    private void drawSearchBar(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, float f) {
        try {
            int n5 = (int)(600.0f * this.currentScale);
            int n6 = (int)(400.0f * this.currentScale);
            int n7 = (int)(200.0f * this.currentScale);
            int n8 = (int)(20.0f * this.currentScale);
            int n9 = n + (n5 - n7) / 2;
            int n10 = n2 + n6 + (int)(15.0f * this.currentScale);
            TextGlow.drawBackground(guiGraphics.pose(), n9, n10, n7, n8, 8.0f * this.currentScale, f);
            Renderer.renderConsumer((drawContext -> {
                FontRenderer fontRenderer = FontPresets.materialIcons(20.0f * this.currentScale);
                String string = "";
                float f2 = (float)n9 + 12.0f * this.currentScale;
                float f3 = (float)n10 + (float)n8 / 2.0f + fontRenderer.getMetrics().capHeight() / 2.0f - 12.0f * this.currentScale;
                int iconColor = new Color(255, 255, 255, (int)(180.0f * f)).getRGB();
                TextGlow.drawGlowText(string, f2, f3, fontRenderer, -2236963, iconColor, 12.0f * this.currentScale);
                FontRenderer fontRenderer2 = FontPresets.axiformaRegular(16.0f * this.currentScale);
                if (this.searchActive || !this.searchQuery.isEmpty()) {
                    float f4 = (float)n9 + 35.0f * this.currentScale;
                    float f5 = (float)n10 + (float)n8 / 2.0f + fontRenderer2.getMetrics().capHeight() / 2.0f - 9.0f * this.currentScale;
                    int queryColor = new Color(255, 255, 255, (int)(120.0f * f)).getRGB();
                    TextGlow.drawGlowText(this.searchQuery, f4, f5, fontRenderer2, -1, queryColor, 8.0f * this.currentScale);
                    if (this.searchFocused) {
                        long l = System.currentTimeMillis() - this.searchCursorTime;
                        float f6 = (float)(Math.sin((double)l / 200.0) * 0.5 + 0.5);
                        int cursorColor = (int)(f6 * f * 255.0f) << 24 | 0xFFFFFF;
                        float f7 = f4 + GlHelper.getStringWidth(this.searchQuery, fontRenderer2) + 2.0f * this.currentScale;
                        float f8 = fontRenderer2.getMetrics().capHeight();
                        RenderUtil.drawFilledRect(guiGraphics.pose(), f7, f5 - f8 + 11.0f * this.currentScale, this.currentScale, f8 - 3.0f * this.currentScale, cursorColor);
                    }
                } else {
                    FontRenderer fontRenderer3 = FontPresets.axiformaBold(14.0f * this.currentScale);
                    String string2 = "search";
                    float f9 = GlHelper.getStringWidth(string2, fontRenderer3);
                    float f10 = (float)n9 + ((float)n7 - f9) / 2.0f;
                    float f11 = (float)n10 + (float)n8 / 2.0f + fontRenderer3.getMetrics().capHeight() / 2.0f - 8.0f * this.currentScale;
                    TextGlow.drawGlowText(string2, f10, f11, fontRenderer3, -3355444, new Color(255, 255, 255, (int)(130.0f * f)).getRGB(), 10.0f * this.currentScale);
                }
            }));
        } catch (Exception exception) {
            // empty catch block
        }
    }

    public void addToast(String string) {
        for (PanelClickGui.ToastEntry panelClickGui$ToastEntry : this.toasts) {
            panelClickGui$ToastEntry.targetY -= 20.0f * this.currentScale;
        }
        this.toasts.add(new PanelClickGui.ToastEntry(string));
    }

    public void selectModule(Module module) {
        this.keybindOverlay.startBinding(module);
    }

    private void drawToasts(GuiGraphics guiGraphics, int n, int n2, float f) {
        if (this.toasts.isEmpty()) {
            return;
        }
        try {
            Renderer.renderConsumer((drawContext -> {
                FontRenderer fontRenderer = FontPresets.axiformaBold(18.0f * this.currentScale);
                for (PanelClickGui.ToastEntry panelClickGui$ToastEntry : this.toasts) {
                    long l = System.currentTimeMillis() - panelClickGui$ToastEntry.createdAt;
                    panelClickGui$ToastEntry.currentY = LerpUtil.smoothLerp(panelClickGui$ToastEntry.currentY, panelClickGui$ToastEntry.targetY, 0.2f);
                    float f2 = 0.0f;
                    if (l < 2000L) {
                        f2 = 1.0f;
                    } else if (l < 2500L) {
                        long l2 = l - 2000L;
                        f2 = 1.0f - (float)l2 / 500.0f;
                    }
                    panelClickGui$ToastEntry.alpha = LerpUtil.smoothLerp(panelClickGui$ToastEntry.alpha, f2, 0.25f);
                    if (panelClickGui$ToastEntry.alpha < 0.01f && l > 2000L) {
                        this.toasts.remove(panelClickGui$ToastEntry);
                        continue;
                    }
                    float f3 = GlHelper.getStringWidth(panelClickGui$ToastEntry.message, fontRenderer);
                    int n3 = (int)(600.0f * this.currentScale);
                    float f4 = (float)n + ((float)n3 - f3) / 2.0f;
                    float f5 = (float)(n2 - 25) + panelClickGui$ToastEntry.currentY;
                    int n4 = (int)(255.0f * panelClickGui$ToastEntry.alpha * f);
                    int n5 = n4 << 24 | 0xFFFFFF;
                    int n6 = (int)(120.0f * panelClickGui$ToastEntry.alpha * f);
                    int n7 = n6 << 24 | 0xFFFFFF;
                    TextGlow.drawGlowText(panelClickGui$ToastEntry.message, f4, f5 + 6.0f * this.currentScale, fontRenderer, n5, n7, 8.0f * this.currentScale);
                }
            }));
        } catch (Exception exception) {
            // empty catch block
        }
    }

    static {
        PanelClickGui panelClickGui = new PanelClickGui();
    }
}