import com.envyworld.graphics.misc.ClasspathResource;
import com.envyworld.graphics.misc.FontRegistry;
import com.envyworld.graphics.misc.ResourceResolver;
import com.envyworld.graphics.render.GlShaderProgram;
import com.envyworld.graphics.render.LayerFramebuffer;
import com.envyworld.graphics.render.RenderFrameClock;
import com.envyworld.graphics.util.GuiDrawApi;
import com.envyworld.graphics.util.GuiRenderBackend;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

/** Hidden-context rendering test of the UI shader, vertex and font pipeline. */
public class RendererSmokeTest {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(1200, 700, "Envyworld renderer verification", 0, 0);
        if (window == 0) throw new IllegalStateException("Cannot create hidden OpenGL context");
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        ResourceResolver resources = new ResourceResolver("/assets/envyworld/renderer/shaders/", ClasspathResource::new);
        GlShaderProgram shader = new GlShaderProgram(resources.resolve("gl/core_gl.frag"), resources.resolve("gl/core_gl.vert"));
        GuiRenderBackend backend = new GuiRenderBackend(shader);
        GuiDrawApi draw = new GuiDrawApi(backend);
        LayerFramebuffer target = new LayerFramebuffer(1200, 700);
        RenderFrameClock.advanceFrame();
        Matrix4f matrix = new Matrix4f().scale(2, 2, 1);
        draw.beginLayerFrame(target);
        draw.fillRectangle(matrix, 0, 0, 600, 350, 0xFF10131B);
        draw.drawRoundedRectangle(matrix, 15, 15, 570, 320, 10.5f, 0xFF1C222E);
        draw.drawRoundedOutline(matrix, 15, 15, 570, 320, 10.5f, .75f, 0x40FFFFFF);
        String[] text = {"Наказания   Макросы   Предметы   Права   Правила", "Логи   Онлайн   История   English 0123456789",
                "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ", "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
                "Команда | MUTE | 2.1", "/mute <ник> 1h Оскорбление, насмешка, хамство"};
        for (int i = 0; i < text.length; i++) {
            FontRegistry.regularText.process2(matrix, draw, text[i], 28, 28 + i * 22, 7, 0xFFF4F6FA);
        }
        draw.drawRoundedRectangle(matrix, 28, 180, 260, 18, 5, 0x6012141C);
        draw.drawRoundedOutline(matrix, 28, 180, 260, 18, 5, .75f, 0x38FFFFFF);
        FontRegistry.regularText.process2(matrix, draw, "Поле ввода: Пример", 34, 184, 7, -1);
        draw.drawRoundedRectangle(matrix, 28, 215, 120, 18, 7, 0x901E2028);
        FontRegistry.regularText.process2(matrix, draw, "Сохранить", 62, 219, 6.5f, -1);
        draw.pushScissor(matrix, 30, 270, 90, 20);
        draw.fillRectangle(matrix, 0, 255, 300, 50, 0xFFFF7020);
        draw.popScissor();
        draw.endLayerFrame();
        GL33.glBindFramebuffer(GL33.GL_FRAMEBUFFER, target.framebufferId());
        ByteBuffer pixels = MemoryUtil.memAlloc(1200 * 700 * 4);
        GL33.glReadPixels(0, 0, 1200, 700, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, pixels);
        BufferedImage output = new BufferedImage(1200, 700, BufferedImage.TYPE_INT_ARGB);
        int bright = 0;
        for (int y = 0; y < 700; y++) for (int x = 0; x < 1200; x++) {
            int offset = ((699 - y) * 1200 + x) * 4;
            int r = pixels.get(offset) & 255, g = pixels.get(offset + 1) & 255;
            int b = pixels.get(offset + 2) & 255, a = pixels.get(offset + 3) & 255;
            output.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
            if (y >= 55 && y <= 300 && x >= 50 && r > 150 && g > 150 && b > 150) bright++;
        }
        if (bright < 1000) throw new IllegalStateException("MSDF text did not render: " + bright);
        if ((output.getRGB(100, 550) & 0xFFFFFF) != 0xFF7020) throw new IllegalStateException("Scissor interior missing");
        if ((output.getRGB(40, 550) & 0xFFFFFF) == 0xFF7020) throw new IllegalStateException("Scissor leaked");
        Path path = Path.of("build/verification/renderer.png");
        Files.createDirectories(path.getParent());
        ImageIO.write(output, "png", path.toFile());
        System.out.println("PASS: UI shader compiled, Latin/Cyrillic glyphs rendered, rounded shapes and scissors verified: " + path);
        MemoryUtil.memFree(pixels);
        backend.close();
        shader.close();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
