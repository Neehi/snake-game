import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
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
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
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
import static org.lwjgl.system.MemoryStack.stackMallocFloat;
import static org.lwjgl.system.MemoryStack.stackMallocInt;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SnakeGame {

    private static final Logger logger = LoggerFactory.getLogger(SnakeGame.class);

    public static CharSequence[] vertexShaderSource = {
            "#version 330 core\n",
            "layout (location = 0) in vec3 position;\n",
            "uniform mat4 projection;\n",
            "uniform mat4 model;\n",
            "void main() {",
            "  gl_Position = projection * model * vec4(position, 1.0f);\n",
            "}"
    };

    public static CharSequence[] fragmentShaderSource = {
            "#version 330 core\n",
            "out vec4 fragColor;\n",
            "void main() {",
            "  fragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);",
            "}"
    };

    private static final float[] blockVertices = {
            -0.5f, -0.5f, 0.0f,  // bottom left
             0.5f, -0.5f, 0.0f,  // bottom right
            -0.5f,  0.5f, 0.0f,  // top left
             0.5f,  0.5f, 0.0f,  // top right
    };

    private static final int[] blockIndices = {
            0, 1, 2, // first triangle
            1, 2, 3  // second triangle
    };

    private long window;
    private String title = "Snake Game";
    private int width = 800;
    private int height = 600;
    private int fbWidth = width;
    private int fbHeight = height;

    private int vao;

    private int blockVbo;
    private int blockEbo;
    private int blockProgram;
    private int blockProjUniform;
    private int blockModelUniform;

    private Matrix4f projectionMatrix = new Matrix4f();
    private Matrix4f modelMatrix = new Matrix4f();
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

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
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);  // int*
            IntBuffer pHeight = stack.mallocInt(1);  // int*
            glfwGetWindowSize(this.window, pWidth, pHeight);
            glfwSetWindowPos(
                    this.window,
                    (vidmode.width() - pWidth.get(0)) >> 1,
                    (vidmode.height() - pHeight.get(0)) >> 1
            );
            // XXX: Allow for Mac?
            glfwGetFramebufferSize(this.window, pWidth, pHeight);
            if (this.fbWidth != pWidth.get(0) || this.fbHeight != pHeight.get(0)) {
                this.fbWidth = pWidth.get(0);
                this.fbHeight = pHeight.get(0);
                logger.trace("Framebuffer size: {} x {}", this.fbWidth, this.fbHeight);
            }
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

        // Create GL resources
        this.vao =  GL30.glGenVertexArrays();
        GL30.glBindVertexArray(this.vao);

        createBlockProgram();
        createBlockMesh();
    }

    private int createShader(int type, CharSequence... source) {
        final int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        final int compiled = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS); //, pSuccess);
        if (compiled == 0)
            logger.error("Error compiling shader - " + GL20.glGetShaderInfoLog(shader), new RuntimeException());
        return shader;
    }

    private void createBlockProgram() {
        final int vertexShader = createShader(GL20.GL_VERTEX_SHADER, vertexShaderSource);
        final int fragmentShader = createShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSource);
        this.blockProgram = GL20.glCreateProgram();
        GL20.glAttachShader(this.blockProgram, vertexShader);
        GL20.glAttachShader(this.blockProgram, fragmentShader);
        GL20.glLinkProgram(this.blockProgram);
        final int linked = GL20.glGetProgrami(this.blockProgram, GL20.GL_LINK_STATUS);
        if (linked == 0)
            logger.error("Error linking shader program - " + GL20.glGetProgramInfoLog(this.blockProgram), new RuntimeException());
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        GL20.glUseProgram(this.blockProgram);
        this.blockProjUniform = GL20.glGetUniformLocation(this.blockProgram, "projection");
        this.blockModelUniform = GL20.glGetUniformLocation(this.blockProgram, "model");
        GL20.glUseProgram(0);
    }

    private void createBlockMesh() {
        try (MemoryStack stack = stackPush()) {
            // Vertices
            FloatBuffer verticesBuffer = stackMallocFloat(4 * 3);
            verticesBuffer.put(blockVertices);
            verticesBuffer.flip();
            this.blockVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.blockVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);

            // Indices
            IntBuffer indicesBuffer = stackMallocInt(6);
            indicesBuffer.put(blockIndices);
            indicesBuffer.flip();
            this.blockEbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.blockEbo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

            // ...
            GL30.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0L);
            GL30.glEnableVertexAttribArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//            GL30.glBindVertexArray(0);  // XXX: Needed?
        }
    }

    private void render() {
        // Clear screen
        GL11.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);  // | GL11.GL_DEPTH_BUFFER_BIT);
        // Draw square
        GL20.glUseProgram(this.blockProgram);
        GL30.glBindVertexArray(this.vao);
        this.modelMatrix.translation(0.0f, 0.0f, 0.0f);
        this.modelMatrix.scale(100);
        GL20.glUniformMatrix4fv(this.blockModelUniform, false, this.modelMatrix.get(matrixBuffer));
        GL20.glUniformMatrix4fv(this.blockProjUniform, false, this.projectionMatrix.get(matrixBuffer));
        GL15.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
    }

    private void run() {
        try {
            init();

            while (!glfwWindowShouldClose(this.window)) {
                glfwPollEvents();
                GL11.glViewport(0, 0, this.fbWidth, this.fbHeight);  // XXX: Needed?
                this.projectionMatrix = new Matrix4f().ortho2D(-this.fbWidth/2, this.fbWidth/2, -this.fbHeight/2,this.fbHeight/2);
                render();
                glfwSwapBuffers(this.window);
            }

            logger.debug("Releasing GL resources");
            GL20.glDeleteProgram(this.blockProgram);
            GL15.glDeleteBuffers(this.blockEbo);
            GL15.glDeleteBuffers(this.blockVbo);
            GL30.glDeleteVertexArrays(this.vao);

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
