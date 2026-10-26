package com.envyworld.client.gui;

import com.envyworld.graphics.misc.ClasspathResource;
import com.envyworld.graphics.misc.ResourceResolver;
import com.envyworld.graphics.render.GlShaderProgram;
import com.envyworld.graphics.render.RenderFrameClock;
import com.envyworld.graphics.util.GuiDrawApi;
import com.envyworld.graphics.util.GuiRenderBackend;
import com.envyworld.graphics.util.GlRenderStateSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33;

/** Minecraft lifecycle bridge. UI geometry and text share the GL backend. */
public final class UiRenderer {
    private static GuiDrawApi renderer;
    private static GuiRenderBackend backend;
    private static GlShaderProgram shader;
    private static final List<BiConsumer<GuiDrawApi, Matrix4f>> commands = new ArrayList<>();
    private static final List<BiConsumer<GuiDrawApi, Matrix4f>> overlays = new ArrayList<>();
    private static boolean prepared;
    private static boolean warmed;
    private static boolean overlayRouting;
    private static boolean backdropReady;
    private static float menuAlpha=1;
    private static boolean copiedWorld;
    private static FrameCopy worldCopy;
    private record FrameCopy(int getTextureId,int getWidth,int getHeight) implements com.envyworld.graphics.misc.TextureHandle {}
    private static Object transitionOwner;
    private static boolean transitionClosing,transitionSnapshot;
    public static void menuTransition(float alpha,boolean closing){
        Object owner=Minecraft.getInstance().screen;
        if(owner!=transitionOwner||closing!=transitionClosing){transitionOwner=owner;transitionClosing=closing;transitionSnapshot=false;}
        menuAlpha=Math.clamp(alpha,0,1);
    }


    private UiRenderer() {}

    public static void beginFrame() {
        commands.clear();
        overlays.clear();
        menuAlpha=1;copiedWorld=false;backdropReady=false;
        prepared = true;
        RenderFrameClock.advanceFrame();
    }

    public static void submit(BiConsumer<GuiDrawApi, Matrix4f> draw) {
        (overlayRouting ? overlays : commands).add(draw);
    }

    public static void overlayCommands(Runnable commands) {
        boolean previous = overlayRouting; overlayRouting = true;
        try { commands.run(); } finally { overlayRouting = previous; }
    }

    public static void overlay(BiConsumer<GuiDrawApi, Matrix4f> draw) {
        overlays.add(draw);
    }

    public static void backdrop(int width, int height) {
        submit((draw, matrix) -> draw.drawBlurredRectangle(matrix, 0, 0, width, height));
    }

    public static void beforeVanillaGui() {
        if(prepared&&menuAlpha<.999f){
            transitionSnapshot=captureWorld();
            copiedWorld=transitionSnapshot;
        }else transitionSnapshot=false;
        if (prepared) backdropReady=render(commands, false);
    }

    public static void afterVanillaGui() {
        if(prepared&&copiedWorld&&worldCopy!=null){
            int tint=Math.round((1-menuAlpha)*255)<<24|0xFFFFFF;
            var window=Minecraft.getInstance().getWindow();
            overlays.add((draw,matrix)->draw.drawLayerTextureFlipped(matrix,worldCopy,0,0,window.getGuiScaledWidth(),window.getGuiScaledHeight(),tint));
        }
        // Reuse the backdrop from the world pass. Recapturing the completed GUI here
        // ran another fullscreen blur and fed menu pixels into the next backdrop.
        if (prepared && !overlays.isEmpty()) render(overlays, backdropReady);
        prepared = false;
    }

    /** Composite the original coloured world over the complete GUI, including vanilla item/skin images. */
    private static boolean captureWorld(){
        var target=Minecraft.getInstance().getMainRenderTarget();
        if(!(RenderSystem.getDevice() instanceof GlDevice device)||target==null||!(target.getColorTextureView() instanceof GlTextureView color))return false;
        int readFramebuffer=GL33.glGetInteger(GL33.GL_READ_FRAMEBUFFER_BINDING);
        int textureBinding=GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        try{
            GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,color.getFbo(device.directStateAccess(),target.getDepthTexture()));
            if(worldCopy==null||worldCopy.getWidth()!=target.width||worldCopy.getHeight()!=target.height){
                if(worldCopy!=null)GL33.glDeleteTextures(worldCopy.getTextureId());
                worldCopy=new FrameCopy(GL33.glGenTextures(),target.width,target.height);
                GL33.glBindTexture(GL33.GL_TEXTURE_2D,worldCopy.getTextureId());
                GL33.glTexImage2D(GL33.GL_TEXTURE_2D,0,GL33.GL_RGB8,target.width,target.height,0,GL33.GL_RGB,GL33.GL_UNSIGNED_BYTE,0L);
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MIN_FILTER,GL33.GL_LINEAR);
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_MAG_FILTER,GL33.GL_LINEAR);
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_S,GL33.GL_CLAMP_TO_EDGE);
                GL33.glTexParameteri(GL33.GL_TEXTURE_2D,GL33.GL_TEXTURE_WRAP_T,GL33.GL_CLAMP_TO_EDGE);
            }else GL33.glBindTexture(GL33.GL_TEXTURE_2D,worldCopy.getTextureId());
            GL33.glCopyTexSubImage2D(GL33.GL_TEXTURE_2D,0,0,0,0,0,target.width,target.height);
            return true;
        }finally{
            GL33.glBindTexture(GL33.GL_TEXTURE_2D,textureBinding);
            GL33.glBindFramebuffer(GL33.GL_READ_FRAMEBUFFER,readFramebuffer);
        }
    }

    /** Prime lazy font textures, shader programs and blur targets while the title screen is visible. */
    public static void warmUp() {
        if (warmed || Minecraft.getInstance().getOverlay() != null
                || !(Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.TitleScreen)) return;
        long start = System.nanoTime();
        RenderFrameClock.advanceFrame();
        List<BiConsumer<GuiDrawApi, Matrix4f>> warm = new ArrayList<>();
        warm.add((draw, matrix) -> {
            draw.drawBlurredRectangle(matrix, -100, -100, 2, 2);
            draw.drawRoundedRectangle(matrix, -100, -100, 20, 20, 4, 0xFFFFFFFF);
            com.envyworld.graphics.misc.FontRegistry.regularText.process2(matrix, draw, "АБВabc123", -1000, -1000, 7, 0xFFFFFFFF);
            com.envyworld.graphics.misc.FontRegistry.font3.process5(matrix, draw, "CT", -1000, -1000, 7, 0xFFFFFFFF);
        });
        if(!render(warm, false))return;
        warmed = true;
        com.envyworld.EnvyWorld.LOGGER.info("Menu render resources primed in {} ms", (System.nanoTime() - start) / 1_000_000);
    }

    private static boolean render(List<BiConsumer<GuiDrawApi, Matrix4f>> batch, boolean overlay) {
        if (batch.isEmpty()) return false;
        // Startup failure screens and alternative backends may have no OpenGL context/device.
        // Check Java-side capability state before invoking any native GL entry point.
        try {
            if(org.lwjgl.opengl.GL.getCapabilities()==null
                    ||!(RenderSystem.getDevice() instanceof GlDevice)
                    ||Minecraft.getInstance().getMainRenderTarget()==null
                    ||!(Minecraft.getInstance().getMainRenderTarget().getColorTextureView() instanceof GlTextureView)){
                batch.clear();return false;
            }
        }catch(IllegalStateException unavailable){batch.clear();return false;}
        GlRenderStateSnapshot state = GlRenderStateSnapshot.captureRenderState();
        try {
            if (renderer == null) {
                ResourceResolver resources = new ResourceResolver("/assets/envyworld/renderer/shaders/", ClasspathResource::new);
                shader = new GlShaderProgram(resources.resolve("gl/core_gl.frag"), resources.resolve("gl/core_gl.vert"));
                backend = new GuiRenderBackend(shader);
                renderer = new GuiDrawApi(backend);
            }
            var minecraft = Minecraft.getInstance();
            var target = minecraft.getMainRenderTarget();
            // Minecraft unbinds its framebuffer after clearing depth. Raw GL must bind it
            // explicitly before the Envyworld backend captures/restores the current target.
            var color = (GlTextureView) target.getColorTextureView();
            var device = (GlDevice) RenderSystem.getDevice();
            GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, color.getFbo(device.directStateAccess(), target.getDepthTexture()));
            GL33.glViewport(0, 0, target.width, target.height);
            var window = minecraft.getWindow();
            Matrix4f matrix = new Matrix4f().scale((float) window.getGuiScale(), (float) window.getGuiScale(), 1);
            if (overlay) backend.prepareBackdrop();
            renderer.begin();
            try {
                for (var draw : batch) draw.accept(renderer, matrix);
            } finally {
                renderer.end();
            }
        } finally {
            state.restore();
            batch.clear();
        }
        return true;
    }

    public static void close() {
        transitionOwner=null;transitionSnapshot=false;
        if(worldCopy!=null){GL33.glDeleteTextures(worldCopy.getTextureId());worldCopy=null;}
        UiIcons.close();
        TagFont.close();
        if (backend != null) backend.close();
        if (shader != null) shader.close();
        renderer = null;
        backend = null;
        shader = null;
        commands.clear();
        overlays.clear();
        prepared = false;
        warmed = false;
    }
}
