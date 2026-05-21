package shit.zen.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.*;

import java.awt.Color;
import java.awt.Font;
import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Setter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import shit.zen.ClientBase;
import shit.zen.utils.math.MathUtil;

public class CustomFont
implements Closeable {

    public record GlyphEntry(float atX, float atY, float r, float g, float b, Glyph toDraw) {
    }

    static class MinecraftColorMap extends Char2IntArrayMap {
        MinecraftColorMap() {
            this.put('0', 0);
            this.put('1', 170);
            this.put('2', 43520);
            this.put('3', 43690);
            this.put('4', 0xAA0000);
            this.put('5', 0xAA00AA);
            this.put('6', 0xFFAA00);
            this.put('7', 0xAAAAAA);
            this.put('8', 0x555555);
            this.put('9', 0x5555FF);
            this.put('A', 0x55FF55);
            this.put('B', 0x55FFFF);
            this.put('C', 0xFF5555);
            this.put('D', 0xFF55FF);
            this.put('E', 0xFFFF55);
            this.put('F', 0xFFFFFF);
        }
    }

    private static final Char2IntArrayMap MC_COLOR_CODES = new CustomFont.MinecraftColorMap();
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private final Object2ObjectMap<ResourceLocation, ObjectList<CustomFont.GlyphEntry>> glyphPageMap = new Object2ObjectOpenHashMap();
    private final float fontSize;
    private final ObjectList<GlyphPage> glyphPages = new ObjectArrayList();
    private final Char2ObjectArrayMap<Glyph> glyphCache = new Char2ObjectArrayMap();
    private final int pageSize;
    private final int charsPerPage;
    private final String preloadChars;
    private int scale = 0;
    private Font scaledFont;
    private int guiScaleCache = -1;
    private Future<Void> preloadFuture;
    private boolean initialized;
    private static final Color SHADOW_COLOR = new Color(26, 26, 26, 160);
    @Setter
    private float letterSpacing = 0.0f;
    private FontMetricsImpl fontMetrics;

    public CustomFont(Font font, float f, int n, int n2, @Nullable String string) {
        this.fontSize = f;
        this.pageSize = n;
        this.charsPerPage = n2;
        this.preloadChars = string;
        this.fontMetrics = new FontMetricsImpl(font);
        this.initFont(font, f);
    }

    public CustomFont(Font font, float f) {
        this(font, f, 256, 5, null);
    }

    private static int alignToPageBoundary(int n, int n2) {
        return n2 * (int)Math.floor((double)n / (double)n2);
    }

    public static String stripFormatting(String string) {
        char[] cArray = string.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < cArray.length; ++i) {
            char c = cArray[i];
            if (c == '§') {
                ++i;
                continue;
            }
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    private void checkGuiScaleChanged() {
        int n = (int)ClientBase.mc.getWindow().getGuiScale();
        if (n != this.guiScaleCache) {
            this.close();
            this.initFont(this.scaledFont, this.fontSize);
        }
    }

    private void initFont(Font font, float f) {
        if (this.initialized) {
            throw new IllegalStateException("Double call to init()");
        }
        this.initialized = true;
        this.guiScaleCache = (int)ClientBase.mc.getWindow().getGuiScale();
        this.scale = Math.max(2, this.guiScaleCache * 2);
        this.scaledFont = font.deriveFont(f * (float)this.scale);
        this.fontMetrics = new FontMetricsImpl(this.scaledFont);
        if (this.preloadChars != null && !this.preloadChars.isEmpty()) {
            this.preloadFuture = this.startPreload();
        }
    }

    private Future<Void> startPreload() {
        return EXECUTOR.submit(() -> {
            for (char c : this.preloadChars.toCharArray()) {
                if (Thread.interrupted()) break;
                this.getOrLoadGlyph(c);
            }
            return null;
        });
    }

    private GlyphPage createGlyphPage(char c, char c2) {
        GlyphPage glyphPage = new GlyphPage(c, c2, this.scaledFont, CustomFont.getTempResourceLocation(), this.charsPerPage);
        this.glyphPages.add(glyphPage);
        return glyphPage;
    }

    private Glyph loadGlyph(char c) {
        for (GlyphPage existing : this.glyphPages) {
            if (!existing.contains(c)) continue;
            return existing.getGlyph(c);
        }
        int n = CustomFont.alignToPageBoundary(c, this.pageSize);
        GlyphPage page = this.createGlyphPage((char)n, (char)(n + this.pageSize));
        return page.getGlyph(c);
    }

    @Nullable
    private Glyph getOrLoadGlyph(char c) {
        return this.glyphCache.computeIfAbsent(c, this::loadGlyph);
    }

    public void drawString(PoseStack poseStack, String string, double d, double d2, int n) {
        float f = (float)(n >> 16 & 0xFF) / 255.0f;
        float f2 = (float)(n >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(n & 0xFF) / 255.0f;
        float f4 = (float)(n >> 24 & 0xFF) / 255.0f;
        this.drawStringRGB(poseStack, string, (float)d, (float)d2, f, f2, f3, f4);
    }

    public void drawStringShadow(PoseStack poseStack, String string, double d, double d2, int n) {
        float f = (float)(n >> 16 & 0xFF) / 255.0f;
        float f2 = (float)(n >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(n & 0xFF) / 255.0f;
        float f4 = (float)(n >> 24 & 0xFF) / 255.0f;
        this.drawStringRGB(poseStack, string, (float)d, (float)d2, f, f2, f3, f4);
    }

    public void drawStringWithShadow(PoseStack poseStack, String string, double d, double d2, int n) {
        this.drawStringColor(poseStack, string, (double)((float)d) + 0.5, (double)((float)d2) + 0.5, SHADOW_COLOR);
        this.drawString(poseStack, string, (float)d, (float)d2, n);
    }

    public void drawStringColor(PoseStack poseStack, String string, double d, double d2, Color color) {
        this.drawStringRGB(poseStack, string, (float)d, (float)d2, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, color.getAlpha());
    }

    public void drawStringRGB(PoseStack poseStack, String string, float f, float f2, float f3, float f4, float f5, float f6) {
        this.drawStringRGBFull(poseStack, string, f, f2, f3, f4, f5, f6, false, 0);
    }
    public void drawStringRGBFull(PoseStack var1, String var2, float var3, float var4, float var5, float var6, float var7, float var8, boolean var9, int var10) {
        if (this.preloadFuture != null && !this.preloadFuture.isDone()) {
            try {
                this.preloadFuture.get();
            } catch (ExecutionException | InterruptedException var44) {
            }
        }

        this.checkGuiScaleChanged();
        float var13 = var5;
        float var14 = var6;
        float var15 = var7;
        var1.pushPose();
        var1.translate(MathUtil.round(var3, 1), MathUtil.round(--var4, 1), 0.0);
        var1.scale(1.0F / this.scale, 1.0F / this.scale, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        Matrix4f var16 = var1.last().pose();
        char[] var17 = var2.toCharArray();
        float var18 = 0.0F;
        float var19 = 0.0F;
        boolean var20 = false;
        int var21 = 0;
        synchronized (this.glyphPageMap) {
            for (int var23 = 0; var23 < var17.length; var23++) {
                char var24 = var17[var23];
                if (var20) {
                    var20 = false;
                    char var25 = Character.toUpperCase(var24);
                    if (MC_COLOR_CODES.containsKey(var25)) {
                        int var26 = MC_COLOR_CODES.get(var25);
                        int[] var27 = colorToRGB(var26);
                        var13 = var27[0] / 255.0F;
                        var14 = var27[1] / 255.0F;
                        var15 = var27[2] / 255.0F;
                    } else if (var25 == 'R') {
                        var13 = var5;
                        var14 = var6;
                        var15 = var7;
                    }
                } else if (var24 == 167) {
                    var20 = true;
                } else if (var24 == '\n') {
                    var19 += this.getStringHeight(var2.substring(var21, var23)) * this.scale;
                    var18 = 0.0F;
                    var21 = var23 + 1;
                } else {
                    Glyph var49 = this.getOrLoadGlyph(var24);
                    if (var49 != null) {
                        if (var49.value() != ' ') {
                            ResourceLocation var51 = var49.owner().textureLocation;
                            GlyphEntry var53 = new GlyphEntry(var18, var19, var13, var14, var15, var49);
                            this.glyphPageMap.computeIfAbsent(var51, var0 -> new ObjectArrayList<>()).add(var53);
                        }

                        var18 += var49.width() + this.letterSpacing;
                    }
                }
            }

            for (ResourceLocation var48 : this.glyphPageMap.keySet()) {
                RenderSystem.setShaderTexture(0, var48);
                List<GlyphEntry> var50 = this.glyphPageMap.get(var48);
                Tesselator var52 = Tesselator.getInstance();
                BufferBuilder var28 = var52.getBuilder();
                var28.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                for (GlyphEntry var29 : var50) {
                    float var30 = var29.atX;
                    float var31 = var29.atY;
                    float var32 = var29.r;
                    float var33 = var29.g;
                    float var34 = var29.b;
                    Glyph var35 = var29.toDraw;
                    GlyphPage var36 = var35.owner();
                    float var37 = var35.width();
                    float var38 = var35.height();
                    float var39 = (float) var35.u() / var36.imageWidth;
                    float var40 = (float) var35.v() / var36.imageHeight;
                    float var41 = (float) (var35.u() + var35.width()) / var36.imageWidth;
                    float var42 = (float) (var35.v() + var35.height()) / var36.imageHeight;
                    var28.vertex(var16, var30 + 0.0F, var31 + var38, 0.0F).uv(var39, var42).color(var32, var33, var34, var8).endVertex();
                    var28.vertex(var16, var30 + var37, var31 + var38, 0.0F).uv(var41, var42).color(var32, var33, var34, var8).endVertex();
                    var28.vertex(var16, var30 + var37, var31 + 0.0F, 0.0F).uv(var41, var40).color(var32, var33, var34, var8).endVertex();
                    var28.vertex(var16, var30 + 0.0F, var31 + 0.0F, 0.0F).uv(var39, var40).color(var32, var33, var34, var8).endVertex();
                }

                var52.end();
            }

            this.glyphPageMap.clear();
        }

        var1.popPose();
        RenderSystem.disableBlend();
    }

    public void drawStringCentered(PoseStack poseStack, String string, double d, double d2, int n) {
        float f = (float)(n >> 16 & 0xFF) / 255.0f;
        float f2 = (float)(n >> 8 & 0xFF) / 255.0f;
        float f3 = (float)(n & 0xFF) / 255.0f;
        float f4 = (float)(n >> 24 & 0xFF) / 255.0f;
        this.drawStringRGB(poseStack, string, (float)(d - (double)(this.getStringWidth(string) / 2.0f)), (float)d2, f, f2, f3, f4);
    }

    public void drawStringCenteredColor(PoseStack poseStack, String string, double d, double d2, Color color) {
        this.drawStringRGB(poseStack, string, (float)(d - (double)(this.getStringWidth(string) / 2.0f)), (float)d2, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
    }

    public void drawStringCenteredRGB(PoseStack poseStack, String string, float f, float f2, float f3, float f4, float f5, float f6) {
        this.drawStringRGB(poseStack, string, f - this.getStringWidth(string) / 2.0f, f2, f3, f4, f5, f6);
    }

    public float getStringWidth(String string) {
        char[] cArray = CustomFont.stripFormatting(string).toCharArray();
        float f = 0.0f;
        float f2 = 0.0f;
        for (char c : cArray) {
            if (c == '\n') {
                f2 = Math.max(f, f2);
                f = 0.0f;
                continue;
            }
            Glyph glyph = this.getOrLoadGlyph(c);
            f += (glyph == null ? 0.0f : (float)glyph.width() / (float)this.scale) + this.letterSpacing;
        }
        return Math.max(f, f2);
    }

    public float getStringHeight(String string) {
        char[] cArray = CustomFont.stripFormatting(string).toCharArray();
        if (cArray.length == 0) {
            cArray = new char[]{' '};
        }
        float f = 0.0f;
        float f2 = 0.0f;
        for (char c : cArray) {
            if (c == '\n') {
                if (f == 0.0f) {
                    f = this.getOrLoadGlyph(' ') == null ? 0.0f : (float)((Glyph)(Objects.requireNonNull((Object)(this.getOrLoadGlyph(' '))))).height() / (float)this.scale;
                }
                f2 += f;
                f = 0.0f;
                continue;
            }
            Glyph glyph = this.getOrLoadGlyph(c);
            f = Math.max(glyph == null ? 0.0f : (float)glyph.height() / (float)this.scale, f);
        }
        return f + f2;
    }

    public float getFontHeight() {
        return (float)(this.fontMetrics.getLeading() + this.fontMetrics.getAscent() + this.fontMetrics.getDescent()) / (float)this.scale;
    }

    public FontMetricsImpl getFontMetrics() {
        return this.fontMetrics;
    }

    public int getScale() {
        return this.scale;
    }

    public void close() {
        try {
            if (this.preloadFuture != null && !this.preloadFuture.isDone() && !this.preloadFuture.isCancelled()) {
                this.preloadFuture.cancel(true);
                this.preloadFuture.get();
                this.preloadFuture = null;
            }
            for (GlyphPage glyphPage : this.glyphPages) {
                glyphPage.reset();
            }
            this.glyphPages.clear();
            this.glyphCache.clear();
            this.initialized = false;
        } catch (Exception exception) {
            // empty catch block
        }
    }

    @Contract(value="-> new", pure=true)
    @NotNull
    public static ResourceLocation getTempResourceLocation() {
        return ResourceLocation.tryParse("zen:temp/" + CustomFont.generateRandomName());
    }

    private static String generateRandomName() {
        return IntStream.range(0, 32).mapToObj(n -> String.valueOf((char)new Random().nextInt(97, 123))).collect(Collectors.joining());
    }

    public static int @NotNull [] colorToRGB(int n) {
        int n2 = n >> 16 & 0xFF;
        int n3 = n >> 8 & 0xFF;
        int n4 = n & 0xFF;
        return new int[]{n2, n3, n4};
    }

    public float getStringHeightAlias(String string) {
        return this.getStringHeight(string);
    }

    public void drawStringRainbow(PoseStack poseStack, String string, float f, float f2, int n) {
        this.drawStringRGBFull(poseStack, string, f, f2, 255.0f, 255.0f, 255.0f, 255.0f, true, n);
    }

    public void drawStringCenteredRainbow(PoseStack poseStack, String string, float f, float f2, int n) {
        this.drawStringRainbow(poseStack, string, f - this.getStringWidth(string) / 2.0f, f2, n);
    }

    public void resetLetterSpacing() {
        this.letterSpacing = 0.0f;
    }

    }