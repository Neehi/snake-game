import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Random;
import org.joml.Vector2f;
import org.joml.Vector3f;
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
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
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
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
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
            "layout (location = 0) in vec3 a_Position;\n",
            "uniform mat4 u_Projection;\n",
            "uniform mat4 u_Model;\n",
            "void main() {\n",
            "  gl_Position = u_Projection * u_Model * vec4(a_Position, 1.0f);\n",
            "}"
    };

    public static CharSequence[] fragmentShaderSource = {
            "#version 330 core\n",
            "uniform vec3 u_Color;\n",
            "out vec4 fragColor;\n",
            "void main() {\n",
            "  fragColor = vec4(u_Color, 1.0f);\n",
            "}"
    };

    private static final float[] blockVertices = {
            0.0f, 1.0f, 0.0f,  // bottom left
            1.0f, 1.0f, 0.0f,  // bottom right
            0.0f, 0.0f, 0.0f,  // top left
            1.0f, 0.0f, 0.0f,  // top right
    };

    private static final int[] blockIndices = {
            0, 1, 2,  // first triangle
            2, 1, 3,  // second triangle
    };

    private static final Vector3f snakeHeadColor = new Vector3f(0.0f, 1.0f, 1.0f);  // Cyan
    private static final Vector3f snakeTailColor = new Vector3f(1.0f, 1.0f, 1.0f);  // White
    private static final Vector3f deadSnakeColor = new Vector3f(1.0f, 0.0f, 0.0f);  // Red
    private static final Vector3f foodColor = new Vector3f (1.0f, 0.9f, 0.0f);  // Yellow

    private static class Snake {
        public static enum Direction { UP, DOWN, LEFT, RIGHT }
        public Direction direction = Direction.UP;
        public Vector2f head = new Vector2f();
        public List<Vector2f> tail = new ArrayList<Vector2f>();
        public boolean growing = false;
        public boolean alive = true;
        public float velocity = 0.1f;
    }

    private final Random random = new Random();

    private long window;
    private final String title = "Snake Game";
    private int width = 800;
    private int height = 600;
    private int fbWidth = width;
    private int fbHeight = height;
    private final boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];

    private final int gridCols = width / 20;
    private final int gridRows = height / 20;

    private final Snake snake = new Snake();
    {
        this.snake.head.x = (float)(this.gridCols / 2);
        this.snake.head.y = (float)(this.gridRows / 2);
    }
    private final Vector2f food = new Vector2f();
    {
        placeFood();
    }
    private int score = 0;

    private int shaderProgram;
    private int shaderProjUniform;
    private int shaderModelUniform;
    private int shaderColorUniform;

    private int blockVao;
    private int blockVbo;
    private int blockEbo;

    private Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

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
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
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
            if (GLFW_KEY_UNKNOWN == key || key > GLFW_KEY_LAST)
                return;
            if (GLFW_KEY_ESCAPE == key && GLFW_RELEASE == action) {
                logger.trace("Key: Escape pressed");
                glfwSetWindowShouldClose(window, true);  // Exit on 'Esc'
            }
            final boolean pressed = GLFW_PRESS == action || GLFW_REPEAT == action;
            if (GLFW_PRESS == action)
                logger.trace("Key: #{} pressed", key);
            this.keyPressed[key] = pressed;
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
        createShaderProgram();
        createBlockMesh();
    }

    private int createShader(int type, CharSequence... source) {
        final int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            logger.error("Error compiling shader - " + GL20.glGetShaderInfoLog(shader));
            throw new RuntimeException("Error compiling shader - " + GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private void createShaderProgram() {
        final int vertexShader = createShader(GL20.GL_VERTEX_SHADER, vertexShaderSource);
        final int fragmentShader = createShader(GL20.GL_FRAGMENT_SHADER, fragmentShaderSource);
        this.shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(this.shaderProgram, vertexShader);
        GL20.glAttachShader(this.shaderProgram, fragmentShader);
        GL20.glLinkProgram(this.shaderProgram);
        if (GL20.glGetProgrami(this.shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            logger.error("Error linking shader program - " + GL20.glGetProgramInfoLog(this.shaderProgram));
            throw new RuntimeException("Error linking shader  program - " + GL20.glGetShaderInfoLog(this.shaderProgram));
        }
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        GL20.glUseProgram(this.shaderProgram);
        this.shaderProjUniform = GL20.glGetUniformLocation(this.shaderProgram, "u_Projection");
        this.shaderModelUniform = GL20.glGetUniformLocation(this.shaderProgram, "u_Model");
        this.shaderColorUniform = GL20.glGetUniformLocation(this.shaderProgram, "u_Color");
        GL20.glUseProgram(0);
    }

    private void createBlockMesh() {
        try (MemoryStack stack = stackPush()) {
            this.blockVao =  GL30.glGenVertexArrays();
            GL30.glBindVertexArray(this.blockVao);

            // Vertices
            FloatBuffer verticesBuffer = stackMallocFloat(blockVertices.length * 3);
            verticesBuffer.put(blockVertices);
            verticesBuffer.flip();
            this.blockVbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.blockVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
            GL30.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
            GL30.glEnableVertexAttribArray(0);

            // Indices
            IntBuffer indicesBuffer = stackMallocInt(blockIndices.length);
            indicesBuffer.put(blockIndices);
            indicesBuffer.flip();
            this.blockEbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.blockEbo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

            // ...
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);  // XXX: Needed?
        }
    }

    private void processInput() {
        // Up
        if (this.keyPressed[GLFW_KEY_W]) {
            if (this.snake.direction != Snake.Direction.UP && this.snake.direction != Snake.Direction.DOWN) {
                logger.trace("Snake: Direction changed - UP");
                this.snake.direction = Snake.Direction.UP;
            }
        }
        // Down
        if (this.keyPressed[GLFW_KEY_S]) {
            if (this.snake.direction != Snake.Direction.DOWN && this.snake.direction != Snake.Direction.UP) {
                logger.trace("Snake: Direction changed - DOWN");
                this.snake.direction = Snake.Direction.DOWN;
            }
        }
        // Left
        if (this.keyPressed[GLFW_KEY_A]) {
            if (this.snake.direction != Snake.Direction.LEFT && this.snake.direction != Snake.Direction.RIGHT) {
                logger.trace("Snake: Direction changed - LEFT");
                this.snake.direction = Snake.Direction.LEFT;
            }
        }
        // Right
        if (this.keyPressed[GLFW_KEY_D]) {
            if (this.snake.direction != Snake.Direction.RIGHT && this.snake.direction != Snake.Direction.LEFT) {
                logger.trace("Snake: Direction changed - RIGHT");
                this.snake.direction = Snake.Direction.RIGHT;
            }
        }
    }

    private void update() {
        if (!this.snake.alive)
            return;

        updateSnake();

        // Collision detection
        if ((int)this.snake.head.x == (int)this.food.x && (int)this.snake.head.y == (int)this.food.y) {
            logger.trace("Snake: Collision with food! Snake{}, Food{}", this.snake.head, this.food);
            placeFood();  // Move food
            this.snake.growing = true;  // Set snake to growing
            this.snake.velocity += 0.02f;
        }
    }

    private void updateSnake() {
        // Grid position pre-update
        final Vector2f previous = new Vector2f((float)Math.floor(this.snake.head.x), (float)Math.floor(this.snake.head.y));

        // Update head
        switch (this.snake.direction) {
            case UP:
                this.snake.head.y -= this.snake.velocity;
                if (this.snake.head.y < 0)
                    this.snake.head.y = this.gridRows + this.snake.head.y;
                break;
            case DOWN:
                this.snake.head.y += this.snake.velocity;
                if (this.snake.head.y >= this.gridRows)
                    this.snake.head.y -= this.gridRows;
                break;
            case LEFT:
                this.snake.head.x -= this.snake.velocity;
                if (this.snake.head.x < 0)
                    this.snake.head.x = this.gridCols + this.snake.head.x;
                break;
            case RIGHT:
                this.snake.head.x += this.snake.velocity;
                if (this.snake.head.x >= this.gridCols)
                    this.snake.head.x -= this.gridCols;
                break;
        }

        // Grid position post-update
        final Vector2f current = new Vector2f((float)Math.floor(this.snake.head.x), (float)Math.floor(this.snake.head.y));

        // Update body
        if (!previous.equals(current)) {
            // 1. Add current position to end of tail
            // 2. If not growing remove stat of tail
            this.snake.tail.add(new Vector2f(previous.x, previous.y));
            if (!this.snake.growing) {
                this.snake.tail.remove(0);
            } else {
                this.snake.growing = false;
                this.score++;
            }
        }

        // Check for snake colliding with itself
        for (final Vector2f tail : this.snake.tail) {
            if (tail.equals(current)) {
                logger.trace("Snake: Dead!");
                this.snake.alive = false;
            }
        }
    }

    private void placeFood() {
        this.food.x = this.random.nextInt(this.gridCols - 1);
        this.food.y = this.random.nextInt(this.gridRows - 1);
    }

    private void drawBlock(final Vector2f position, final Vector3f color) {
        // Block size and position
        final int blockWidth = this.fbWidth / this.gridCols;
        final int blockHeight = this.fbHeight / this.gridRows;
        // Block position - top left is (0,0)
        final float blockX = ((float)Math.floor(position.x) * blockWidth);
        final float blockY = ((float)Math.floor(position.y) * blockHeight);
        // Render block
        this.modelMatrix.translation(blockX, blockY, 0.0f);
        this.modelMatrix.scale(blockWidth, blockHeight, 1.0f);
        GL20.glUniformMatrix4fv(this.shaderModelUniform, false, this.modelMatrix.get(matrixBuffer));
        GL20.glUniformMatrix4fv(this.shaderProjUniform, false, this.projectionMatrix.get(matrixBuffer));
        GL20.glUniform3f(this.shaderColorUniform, color.x, color.y, color.z);
        GL15.glDrawElements(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0);
    }

    private void drawSnake() {
        for (Vector2f tail : this.snake.tail) {
            drawBlock(tail, snakeTailColor);
        }
        drawBlock(this.snake.head, this.snake.alive ? snakeHeadColor : deadSnakeColor);
    }

    private void drawFood() {
        drawBlock(this.food, foodColor);
    }

    private void render() {
        GL11.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL20.glUseProgram(this.shaderProgram);
        GL30.glBindVertexArray(this.blockVao);
        drawSnake();
        drawFood();
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);
    }

    private void run() {
        try {
            init();

            final int targetFrames = 60;
            final double secondsPerFrame = 1.0d / targetFrames;

            double lastTime = glfwGetTime();
            double currentTime;
            double delta;
            double accumulatedDelta = 0;
            double fpsTime = 0;
            int fps = targetFrames;
            int frameCount = 0;

            while (!glfwWindowShouldClose(this.window)) {
                // Update timers
                currentTime = glfwGetTime();
                delta = currentTime - lastTime;
                lastTime = currentTime;
                accumulatedDelta += delta;
                fpsTime += delta;

                // Handle input
                processInput();

                // Update game
                while (accumulatedDelta >= secondsPerFrame) {
                    update();
                    accumulatedDelta -= secondsPerFrame;
                }

                // Render game
                this.projectionMatrix = new Matrix4f().ortho2D(0, this.fbWidth, this.fbHeight,0);
                render();

                // Update fps
                frameCount++;
                if (fpsTime >= 1.0d) {
                    fps = frameCount;
                    frameCount = 0;
                    fpsTime -= 1.0d;
                }

                // Update window title
                final String title = this.title + " - Score: " + this.score + " FPS: " + fps;
                glfwSetWindowTitle(this.window, title);

                // ...
                glfwSwapBuffers(this.window);
                glfwPollEvents();
            }

            logger.debug("Releasing GL resources");
            GL15.glDeleteBuffers(this.blockEbo);
            GL15.glDeleteBuffers(this.blockVbo);
            GL30.glDeleteVertexArrays(this.blockVao);
            GL20.glDeleteProgram(this.shaderProgram);

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
