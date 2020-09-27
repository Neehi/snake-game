import java.nio.IntBuffer;

import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DECORATED;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SnakeGame {

    private static final Logger logger = LoggerFactory.getLogger(SnakeGame.class);

    private long window;
    private String title = "Snake Game";
    private int width = 800;
    private int height = 600;
    private int fbWidth = width;
    private int fbHeight = height;

    private void init() {
        logger.debug("LWJGL " + Version.getVersion());

        // Setup error callback
        glfwSetErrorCallback((error, description) -> {
            final String msg = GLFWErrorCallback.getDescription(description);
            logger.error(msg, new IllegalStateException());
        });

        // Initialize GLFW
        logger.debug("Initializing GLFW");
        if (!glfwInit())
            throw new IllegalStateException("Error initializing GLFW");

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);
        logger.debug(
                "Current video mode: {} x {} {}:{}:{} @ {}Hz",
                vidmode.width(),
                vidmode.height(),
                vidmode.redBits(),
                vidmode.greenBits(),
                vidmode.blueBits(),
                vidmode.refreshRate()
        );

        // Setup window creation hints
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);  // XXX: Check Mac still needs this?
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        // Create the window
        logger.debug("Creating window: {} x {}", this.width, this.height);
        this.window = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if (NULL == this.window)
            logger.error("Error creating GLFW window", new RuntimeException());

        // Setup a key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            // Exit on 'Esc'
            if (GLFW_KEY_ESCAPE == key && GLFW_RELEASE == action) {
                logger.trace("Key: Escape pressed");
                glfwSetWindowShouldClose(window, true);
            }
        });

        // Setup window size callback
        glfwSetWindowSizeCallback(this.window, (window, width, height) -> {
            if (window == SnakeGame.this.window && width > 0 && height > 0 && (width != SnakeGame.this.width || height != SnakeGame.this.height)) {
                SnakeGame.this.width = width;
                SnakeGame.this.height = height;
                logger.trace("Window resized: {} x {}", width, height);
            }
        });

        // Setup framebuffer size callback
        glfwSetFramebufferSizeCallback(this.window, (window, width, height) -> {
            if (window == SnakeGame.this.window && width > 0 && height > 0 && (width != SnakeGame.this.fbWidth || height != SnakeGame.this.fbHeight)) {
                SnakeGame.this.fbWidth = width;
                SnakeGame.this.fbHeight = height;
                logger.trace("Framebuffer resized: {} x {}", width, height);
            }
        });

        // Center the window
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);  // int*
            IntBuffer pHeight = stack.mallocInt(1);  // int*
            glfwGetWindowSize(this.window, pWidth, pHeight);
            glfwSetWindowPos(
                    this.window,
                    (vidmode.width() - pWidth.get(0)) >> 1,
                    (vidmode.height() - pHeight.get(0)) >> 1
            );
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(this.window);

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(this.window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
    }

    private void run() {
        try {
            init();

            while (!glfwWindowShouldClose(this.window)) {
                glfwPollEvents();
                glfwSwapBuffers(this.window);
            }

            logger.debug("Destroying GLFW window");
            Callbacks.glfwFreeCallbacks(this.window);
            glfwDestroyWindow(this.window);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            logger.debug("Terminating GLFW");
            glfwTerminate();
        }
    }

    public static void main(String[] args) {
        logger.info("Hello world!");
        new SnakeGame().run();
    }

}
