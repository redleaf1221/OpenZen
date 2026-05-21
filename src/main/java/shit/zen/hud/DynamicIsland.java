package shit.zen.hud;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import net.minecraft.util.Mth;
import shit.zen.ClientBase;
import shit.zen.event.impl.Render2DEvent;
import shit.zen.render.Paint;
import shit.zen.render.Renderer;
import shit.zen.render.RoundedRectangle;
import shit.zen.utils.animation.SpringAnimation;
import shit.zen.utils.render.RenderUtil;

public class DynamicIsland {
    public static final class ActiveElementSelector {
        private final DynamicIsland owner;

        public ActiveElementSelector(DynamicIsland owner) {
            this.owner = owner;
        }

        public IHudElement visible() {
            for (IHudElement element : this.owner.elements) {
                if (element.isVisible()) return element;
            }
            return null;
        }
    }

    final List<IHudElement> elements = Arrays.asList(new TabListHud(), new ScaffoldHud(), new EventAlertHud(), new AutoPlayHud(), new WatermarkHud());
    private final DynamicIsland.ActiveElementSelector activeElementSelector = new DynamicIsland.ActiveElementSelector(this);
    private final SpringAnimation widthAnim = new SpringAnimation(300.0f, 1.2f, 20.0f, 170.0f);
    private final SpringAnimation heightAnim = new SpringAnimation(300.0f, 1.2f, 20.0f, 18.0f);
    private final SpringAnimation transitionAnim = new SpringAnimation(250.0f, 1.0f, 22.0f, 1.0f);
    private IHudElement activeElement = null;
    private IHudElement outgoingElement = null;
    private final long lastFrameTime = 0L;
    private long lastFrameTimestamp = 0L;

    public void onRender2D(Render2DEvent render2DEvent) {
        float f;
        float f2;
        IHudElement.Size size;
        if (ClientBase.mc.player == null) {
            return;
        }
        long l = System.currentTimeMillis();
        if (this.lastFrameTimestamp == 0L) {
            this.lastFrameTimestamp = l;
        }
        float f3 = (float)(l - this.lastFrameTimestamp) / 1000.0f;
        this.lastFrameTimestamp = l;
        f3 = Math.min(f3, 0.033333335f);
        IHudElement iHudElement = this.activeElementSelector.visible();
        if (this.activeElement != iHudElement) {
            this.outgoingElement = this.activeElement;
            this.activeElement = iHudElement;
            this.transitionAnim.reset(0.0f);
            this.transitionAnim.setTargetValue(1.0f);
            if (this.outgoingElement == null) {
                size = this.activeElement.getHudAlignment();
                this.widthAnim.reset(size.width());
                this.heightAnim.reset(size.height());
                this.transitionAnim.reset(1.0f);
            }
        }
        size = this.activeElement.getHudAlignment();
        float f4 = size.width();
        float f5 = size.height();
        float f6 = this.transitionAnim.getValue();
        if (this.outgoingElement != null && f6 < 1.0f) {
            IHudElement.Size iHudElement$Size2 = this.outgoingElement.getHudAlignment();
            f4 = Mth.lerp(f6, iHudElement$Size2.width(), size.width());
            f5 = Mth.lerp(f6, iHudElement$Size2.height(), size.height());
        }
        this.widthAnim.setTargetValue(f4);
        this.heightAnim.setTargetValue(f5);
        this.widthAnim.update(f3);
        this.heightAnim.update(f3);
        this.transitionAnim.update(f3);
        float f7 = Math.max(0.0f, this.widthAnim.getValue() + 30.0f);
        float f8 = Math.max(0.0f, this.heightAnim.getValue() + 3.0f);
        float f9 = ((float)ClientBase.mc.getWindow().getGuiScaledWidth() - f7) / 2.0f;
        float f10 = 25.0f;
        float f11 = 25.0f;
        float f12 = f11 + f10 / 2.0f;
        float f13 = this.activeElement.getHudSize() == IHudElement.Alignment.CENTER ? f12 - f8 / 2.0f : f11;
        if (this.outgoingElement != null && f6 < 1.0f) {
            f2 = this.outgoingElement.getHudSize() == IHudElement.Alignment.CENTER ? f12 - f8 / 2.0f : f11;
            f = Mth.lerp(f6, f2, f13);
        } else {
            f = f13;
        }
        f2 = 12.0f;
        final float finalF = f;
        final float finalF2 = f2;
        if (this.activeElement.hasBackground()) {
            Renderer.renderConsumer((drawContext -> {
                try (Paint paint = new Paint()){
                    paint.setColor(new Color(0, 0, 0, 40).getRGB());
                    drawContext.drawRoundedRect(RoundedRectangle.ofXYWHR(f9, finalF, f7, f8, finalF2), paint);
                }
                drawContext.save();
                drawContext.clipRoundedRect(RoundedRectangle.ofXYWHR(f9, finalF, f7, f8, finalF2), true);
                this.activeElement.render(drawContext, f9, finalF, f7, f8, f6);
                drawContext.restore();
            }));
        }
        RenderUtil.pushScissor((int)f9, (int)f, (int)f7, (int)f8);
        this.activeElement.renderGui(render2DEvent.guiGraphics(), render2DEvent.poseStack(), f9, f, f7, f8, f6);
        RenderUtil.popScissor();
        if (f6 >= 1.0f) {
            this.outgoingElement = null;
        }
    }
}