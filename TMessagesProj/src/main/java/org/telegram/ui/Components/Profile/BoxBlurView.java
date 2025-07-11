package org.telegram.ui.Components.Profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


public class BoxBlurView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    gl_Position = aPosition;\n" +
            "    vTexCoord = aTexCoord;\n" +
            "}";

    
    private static final String HORIZONTAL_BLUR_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uTexelWidth;\n" +
            "uniform float uBlurRadius;\n" +
            "uniform bool uIsMirrored;\n" +
            "varying vec2 vTexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 texCoord = vTexCoord;\n" +
            "    if (uIsMirrored) {\n" +
            "        texCoord.y = 1.0 - texCoord.y;\n" +
            "    }\n" +
            "    vec4 sum = vec4(0.0);\n" +
            "    int radius = int(uBlurRadius);\n" +
            "    if (radius == 0) {\n" +
            "       gl_FragColor = texture2D(uTexture, texCoord);\n" +
            "       return;\n" +
            "    }\n" +
            "    float weight = 1.0 / float(2 * radius + 1);\n" +
            "    for (int i = -radius; i <= radius; i++) {\n" +
            "        sum += texture2D(uTexture, texCoord + vec2(uTexelWidth * float(i), 0.0));\n" +
            "    }\n" +
            "    gl_FragColor = sum * weight;\n" +
            "}";

    
    private static final String VERTICAL_BLUR_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uTexelHeight;\n" +
            "uniform float uBlurRadius;\n" +
            "uniform bool uIsMirrored;\n" +
            "varying vec2 vTexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 texCoord = vTexCoord;\n" +
            "    if (uIsMirrored) {\n" +
            "        texCoord.y = 1.0 - texCoord.y;\n" +
            "    }\n" +
            "    vec4 sum = vec4(0.0);\n" +
            "    int radius = int(uBlurRadius);\n" +
            "    if (radius == 0) {\n" +
            "       gl_FragColor = texture2D(uTexture, texCoord);\n" +
            "       return;\n" +
            "    }\n" +
            "    float weight = 1.0 / float(2 * radius + 1);\n" +
            "    for (int i = -radius; i <= radius; i++) {\n" +
            "        sum += texture2D(uTexture, texCoord + vec2(0.0, uTexelHeight * float(i)));\n" +
            "    }\n" +
            "    gl_FragColor = sum * weight;\n" +
            "}";

    
    private static final String FINAL_GRADIENT_FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform float uGradientStart;\n" +
            "uniform float uGradientEnd;\n" +
            "uniform bool uIsMirrored;\n" +
            "uniform vec2 uSize;\n" +
            "uniform vec4 uCornerRadius;\n" +
            "varying vec2 vTexCoord;\n" +
            "\n" +
            "float roundedRectMask(vec2 coord, vec2 size, vec4 radius) {\n" +
            "    vec2 pixelCoord = coord * size;\n" +
            "    vec2 cornerCoord = vec2(0.0);\n" +
            "    float cornerRadius = 0.0;\n" +
            "    \n" +
            "    if (pixelCoord.x < radius.x && pixelCoord.y < radius.x) {\n" +
            "        cornerCoord = pixelCoord - vec2(radius.x);\n" +
            "        cornerRadius = radius.x;\n" +
            "    } else if (pixelCoord.x > size.x - radius.y && pixelCoord.y < radius.y) {\n" +
            "        cornerCoord = pixelCoord - vec2(size.x - radius.y, radius.y);\n" +
            "        cornerRadius = radius.y;\n" +
            "    } else if (pixelCoord.x < radius.z && pixelCoord.y > size.y - radius.z) {\n" +
            "        cornerCoord = pixelCoord - vec2(radius.z, size.y - radius.z);\n" +
            "        cornerRadius = radius.z;\n" +
            "    } else if (pixelCoord.x > size.x - radius.w && pixelCoord.y > size.y - radius.w) {\n" +
            "        cornerCoord = pixelCoord - vec2(size.x - radius.w, size.y - radius.w);\n" +
            "        cornerRadius = radius.w;\n" +
            "    } else {\n" +
            "        return 1.0;\n" +
            "    }\n" +
            "    \n" +
            "    if (cornerRadius <= 0.0) {\n" +
            "        return 1.0;\n" +
            "    }\n" +
            "    \n" +
            "    float dist = length(cornerCoord);\n" +
            "    \n" +
            "    if (dist <= cornerRadius) {\n" +
            "        return 1.0;\n" +
            "    }\n" +
            "    \n" +
            "    return 0.0;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 texCoord = vTexCoord;\n" +
            "    if (uIsMirrored) {\n" +
            "        texCoord.y = 1.0 - texCoord.y;\n" +
            "    }\n" +
            "    vec4 color = texture2D(uTexture, texCoord);\n" +
            "    float t = smoothstep(uGradientStart, uGradientEnd, vTexCoord.y);\n" +
            "    float alpha = pow(t, 0.75);\n" +
            "    \n" +
            "    float cornerMask = roundedRectMask(vTexCoord, uSize, uCornerRadius);\n" +
            "    \n" +
            "    gl_FragColor = vec4(color.rgb * alpha * cornerMask, color.a * alpha * cornerMask);\n" +
            "}";

    
    private static final float[] VERTICES = {
        -1.0f, -1.0f, 0.0f, 1.0f,  
         1.0f, -1.0f, 1.0f, 1.0f,  
        -1.0f,  1.0f, 0.0f, 0.0f,  
         1.0f,  1.0f, 1.0f, 0.0f   
    };

    private BoxBlurRenderer renderer;
    private View sourceView;
    private ImageReceiver imageReceiver;
    private float blurRadius = 1.0f;
    private boolean needsUpdate = false;
    private boolean useDownsampling = true; 
    private float scaleCoefficient = 8.0f; 
    private int blurPasses = 1; 
    private android.graphics.RectF cropRect = new android.graphics.RectF(0, 0, 1, 1);
    private float gradientStart = 0.0f; 
    private float gradientEnd = 0.3f; 
    private boolean useGradientAlpha = true; 
    private boolean isMirrored = true; 
    public boolean renderSourceView = false;

    
    private float cornerRadiusTopLeft = 0.0f;
    private float cornerRadiusTopRight = 0.0f;
    private float cornerRadiusBottomLeft = 0.0f;
    private float cornerRadiusBottomRight = 0.0f;

    public BoxBlurView(Context context) {
        this(context, null);
    }

    public BoxBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSurfaceTextureListener(this);
        setOpaque(false);
        renderer = new BoxBlurRenderer();
    }

    public void setSourceView(View sourceView) {
        this.sourceView = sourceView;
        requestUpdate();
    }

    public void setImageReceiver(ImageReceiver imageReceiver) {
        this.imageReceiver = imageReceiver;
        requestUpdate();
    }

    public ImageReceiver getImageReceiver() {
        return imageReceiver;
    }

    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(0, Math.min(25, radius));
        requestUpdate();
    }

    public void setUseDownsampling(boolean useDownsampling) {
        this.useDownsampling = useDownsampling;
    }


    public void setScaleCoefficient(float scale) {
        this.scaleCoefficient = Math.max(1.0f, scale);
        requestUpdate();
    }


    public void setCropRect(android.graphics.RectF rect) {
        if (rect == null) {
            this.cropRect.set(0, 0, 1, 1);
        } else {
            this.cropRect.set(
                Math.max(0f, rect.left),
                Math.max(0f, rect.top),
                Math.min(1f, rect.right),
                Math.min(1f, rect.bottom)
            );
        }
        requestUpdate();
    }

    public void setBlurPasses(int passes) {
        this.blurPasses = Math.max(1, Math.min(6, passes));
        requestUpdate();
    }


    public void setGradientAlpha(float start, float end) {
        this.gradientStart = Math.max(0.0f, Math.min(1.0f, start));
        this.gradientEnd = Math.max(0.0f, Math.min(1.0f, end));
        requestUpdate();
    }


    public void setUseGradientAlpha(boolean use) {
        this.useGradientAlpha = use;
        requestUpdate();
    }


    public void setMirrored(boolean mirrored) {
        this.isMirrored = mirrored;
        requestUpdate();
    }


    public boolean isMirrored() {
        return isMirrored;
    }


    public float getScaleCoefficient() {
        return scaleCoefficient;
    }


    public void setCornerRadiusTopLeft(float radius) {
        this.cornerRadiusTopLeft = Math.max(0.0f, radius);
        requestUpdate();
    }


    public void setCornerRadiusTopRight(float radius) {
        this.cornerRadiusTopRight = Math.max(0.0f, radius);
        requestUpdate();
    }


    public void setCornerRadiusBottomLeft(float radius) {
        this.cornerRadiusBottomLeft = Math.max(0.0f, radius);
        requestUpdate();
    }


    public void setCornerRadiusBottomRight(float radius) {
        this.cornerRadiusBottomRight = Math.max(0.0f, radius);
        requestUpdate();
    }


    public void setCornerRadius(float radius) {
        this.cornerRadiusTopLeft = Math.max(0.0f, radius);
        this.cornerRadiusTopRight = Math.max(0.0f, radius);
        this.cornerRadiusBottomLeft = Math.max(0.0f, radius);
        this.cornerRadiusBottomRight = Math.max(0.0f, radius);
        requestUpdate();
    }


    public float getCornerRadiusTopLeft() {
        return cornerRadiusTopLeft;
    }


    public float getCornerRadiusTopRight() {
        return cornerRadiusTopRight;
    }


    public float getCornerRadiusBottomLeft() {
        return cornerRadiusBottomLeft;
    }


    public float getCornerRadiusBottomRight() {
        return cornerRadiusBottomRight;
    }

    public void requestUpdate() {
        needsUpdate = true;
        if (renderer != null) {
            renderer.requestRender();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        renderer.onSurfaceCreated(surface, width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        renderer.onSurfaceChanged(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        renderer.onSurfaceDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        
    }

    private class BoxBlurRenderer {
        private Surface surface;
        private android.opengl.EGLDisplay eglDisplay;
        private android.opengl.EGLContext eglContext;
        private android.opengl.EGLSurface eglSurface;

        private int horizontalBlurProgram;
        private int verticalBlurProgram;
        private int finalGradientProgram;

        
        private int hAPositionHandle, hATexCoordHandle;
        private int hUTextureHandle, hUTexelWidthHandle, hUBlurRadiusHandle, hUIsMirroredHandle;

        
        private int vAPositionHandle, vATexCoordHandle;
        private int vUTextureHandle, vUTexelHeightHandle, vUBlurRadiusHandle, vUIsMirroredHandle;

        
        private int fAPositionHandle, fATexCoordHandle;
        private int fUTextureHandle, fUGradientStartHandle, fUGradientEndHandle, fUIsMirroredHandle;
        private int fUSizeHandle, fUCornerRadiusHandle;

        private int[] sourceTexture = new int[1];
        private int[] pingTexture = new int[1];
        private int[] pongTexture = new int[1];
        private int[] framebuffers = new int[2];

        private FloatBuffer vertexBuffer;
        private android.graphics.Bitmap sourceBitmap;

        private int surfaceWidth, surfaceHeight;
        private int textureWidth, textureHeight;
        private boolean initialized = false;

        public BoxBlurRenderer() {
            ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(VERTICES);
            vertexBuffer.position(0);
        }

        public void onSurfaceCreated(SurfaceTexture surfaceTexture, int width, int height) {
            surface = new Surface(surfaceTexture);
            surfaceWidth = width;
            surfaceHeight = height;
            initializeGL();
        }

        public void onSurfaceChanged(int width, int height) {
            surfaceWidth = width;
            surfaceHeight = height;
            if (initialized) {
                GLES20.glViewport(0, 0, width, height);
            }
        }

        public void onSurfaceDestroyed() {
            cleanupGL();
            if (surface != null) {
                surface.release();
                surface = null;
            }
        }

        public void requestRender() {
            if (!initialized) return;

            AndroidUtilities.runOnUIThread(() -> {
                try {
                    if(!needsUpdate || !initialized) return;
                    if (eglDisplay != null && eglContext != null && eglSurface != null) {
                        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
                    }

                    updateSourceTexture();
                    renderMultiPassBlur();
                } catch (Exception e) {
                    // ignore
                }
            });
        }

        private void initializeGL() {
            try {
                eglDisplay = android.opengl.EGL14.eglGetDisplay(android.opengl.EGL14.EGL_DEFAULT_DISPLAY);
                android.opengl.EGL14.eglInitialize(eglDisplay, new int[1], 0, new int[1], 0);

                int[] configs = new int[1];
                int[] configAttribs = {
                    android.opengl.EGL14.EGL_RENDERABLE_TYPE, android.opengl.EGL14.EGL_OPENGL_ES2_BIT,
                    android.opengl.EGL14.EGL_RED_SIZE, 8,
                    android.opengl.EGL14.EGL_GREEN_SIZE, 8,
                    android.opengl.EGL14.EGL_BLUE_SIZE, 8,
                    android.opengl.EGL14.EGL_ALPHA_SIZE, 8,
                    android.opengl.EGL14.EGL_NONE
                };

                android.opengl.EGLConfig[] eglConfigs = new android.opengl.EGLConfig[1];
                android.opengl.EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, eglConfigs, 0, 1, configs, 0);

                eglContext = android.opengl.EGL14.eglCreateContext(eglDisplay, eglConfigs[0],
                    android.opengl.EGL14.EGL_NO_CONTEXT, new int[]{
                        android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                        android.opengl.EGL14.EGL_NONE
                    }, 0);

                eglSurface = android.opengl.EGL14.eglCreateWindowSurface(eglDisplay, eglConfigs[0], surface, new int[]{
                    android.opengl.EGL14.EGL_NONE
                }, 0);

                android.opengl.EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

                setupShaders();
                setupTextures();

                GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

                initialized = true;
                requestRender();
            } catch (Exception e) {
                FileLog.e("BoxBlurView: initialization error", e);
            }
        }

        private void setupShaders() {
            
            int hVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int hFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, HORIZONTAL_BLUR_FRAGMENT_SHADER);

            horizontalBlurProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(horizontalBlurProgram, hVertexShader);
            GLES20.glAttachShader(horizontalBlurProgram, hFragmentShader);
            GLES20.glLinkProgram(horizontalBlurProgram);

            hAPositionHandle = GLES20.glGetAttribLocation(horizontalBlurProgram, "aPosition");
            hATexCoordHandle = GLES20.glGetAttribLocation(horizontalBlurProgram, "aTexCoord");
            hUTextureHandle = GLES20.glGetUniformLocation(horizontalBlurProgram, "uTexture");
            hUTexelWidthHandle = GLES20.glGetUniformLocation(horizontalBlurProgram, "uTexelWidth");
            hUBlurRadiusHandle = GLES20.glGetUniformLocation(horizontalBlurProgram, "uBlurRadius");
            hUIsMirroredHandle = GLES20.glGetUniformLocation(horizontalBlurProgram, "uIsMirrored");

            
            int vVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int vFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, VERTICAL_BLUR_FRAGMENT_SHADER);

            verticalBlurProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(verticalBlurProgram, vVertexShader);
            GLES20.glAttachShader(verticalBlurProgram, vFragmentShader);
            GLES20.glLinkProgram(verticalBlurProgram);

            vAPositionHandle = GLES20.glGetAttribLocation(verticalBlurProgram, "aPosition");
            vATexCoordHandle = GLES20.glGetAttribLocation(verticalBlurProgram, "aTexCoord");
            vUTextureHandle = GLES20.glGetUniformLocation(verticalBlurProgram, "uTexture");
            vUTexelHeightHandle = GLES20.glGetUniformLocation(verticalBlurProgram, "uTexelHeight");
            vUBlurRadiusHandle = GLES20.glGetUniformLocation(verticalBlurProgram, "uBlurRadius");
            vUIsMirroredHandle = GLES20.glGetUniformLocation(verticalBlurProgram, "uIsMirrored");

            
            int fVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FINAL_GRADIENT_FRAGMENT_SHADER);

            finalGradientProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(finalGradientProgram, fVertexShader);
            GLES20.glAttachShader(finalGradientProgram, fFragmentShader);
            GLES20.glLinkProgram(finalGradientProgram);

            fAPositionHandle = GLES20.glGetAttribLocation(finalGradientProgram, "aPosition");
            fATexCoordHandle = GLES20.glGetAttribLocation(finalGradientProgram, "aTexCoord");
            fUTextureHandle = GLES20.glGetUniformLocation(finalGradientProgram, "uTexture");
            fUGradientStartHandle = GLES20.glGetUniformLocation(finalGradientProgram, "uGradientStart");
            fUGradientEndHandle = GLES20.glGetUniformLocation(finalGradientProgram, "uGradientEnd");
            fUIsMirroredHandle = GLES20.glGetUniformLocation(finalGradientProgram, "uIsMirrored");
            fUSizeHandle = GLES20.glGetUniformLocation(finalGradientProgram, "uSize");
            fUCornerRadiusHandle = GLES20.glGetUniformLocation(finalGradientProgram, "uCornerRadius");
        }

        private void setupTextures() {
            GLES20.glGenTextures(1, sourceTexture, 0);
            GLES20.glGenTextures(1, pingTexture, 0);
            GLES20.glGenTextures(1, pongTexture, 0);
            GLES20.glGenFramebuffers(2, framebuffers, 0);

            
            int[] textures = {sourceTexture[0], pingTexture[0], pongTexture[0]};
            for (int texture : textures) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            }
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private void updateSourceTexture() {
            if (!needsUpdate) return;

            if (sourceView == null && imageReceiver == null) {
                return;
            }

            try {
                int viewWidth;
                int viewHeight;
                android.graphics.Bitmap imageBitmap = null;

                if ((sourceView != null && renderSourceView) || imageReceiver == null || !imageReceiver.hasImageLoaded()) {

                    viewWidth = sourceView.getWidth();
                    viewHeight = sourceView.getHeight();
                    if (viewWidth <= 0 || viewHeight <= 0) return;
                } else {
                    imageBitmap = imageReceiver.getBitmap();
                    if (imageBitmap == null) return;
                    viewWidth = imageBitmap.getWidth();
                    viewHeight = imageBitmap.getHeight();
                }

                
                if (useDownsampling) {
                    textureWidth = Math.max(32, (int) (viewWidth / scaleCoefficient));
                    textureHeight = Math.max(32, (int) (viewHeight / scaleCoefficient));
                } else {
                    textureWidth = viewWidth;
                    textureHeight = viewHeight;
                }

                if (sourceBitmap == null || sourceBitmap.getWidth() != textureWidth || sourceBitmap.getHeight() != textureHeight) {
                    if (sourceBitmap != null) {
                        sourceBitmap.recycle();
                    }
                    sourceBitmap = android.graphics.Bitmap.createBitmap(textureWidth, textureHeight, android.graphics.Bitmap.Config.ARGB_8888);
                }

                Canvas canvas = new Canvas(sourceBitmap);
                float cropWidth = viewWidth * (cropRect.right - cropRect.left);
                float cropHeight = viewHeight * (cropRect.bottom - cropRect.top);

                if (cropWidth <= 0 || cropHeight <= 0) return;

                canvas.save();
                canvas.scale((float) textureWidth / cropWidth, (float) textureHeight / cropHeight);
                canvas.translate(-viewWidth * cropRect.left, -viewHeight * cropRect.top);

                if ((sourceView != null && renderSourceView) || imageReceiver == null) {
                    sourceView.draw(canvas);
                } else if (imageBitmap != null) {
                    canvas.drawBitmap(imageBitmap, 0, 0, null);
                }
                canvas.restore();

                
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTexture[0]);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, sourceBitmap, 0);

                
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pingTexture[0]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, textureWidth, textureHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pongTexture[0]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, textureWidth, textureHeight,
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

                needsUpdate = false;

            } catch (Exception e) {
                // ignore
            }
        }

        private void renderMultiPassBlur() {
            if (!initialized) return;

            try {
                int currentSource = sourceTexture[0];

                GLES20.glEnableVertexAttribArray(hAPositionHandle);
                GLES20.glEnableVertexAttribArray(hATexCoordHandle);
                GLES20.glEnableVertexAttribArray(vAPositionHandle);
                GLES20.glEnableVertexAttribArray(vATexCoordHandle);

                for (int pass = 0; pass < blurPasses; pass++) {
                    boolean lastPass = pass == blurPasses - 1;

                    
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffers[0]);
                    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, pingTexture[0], 0);
                    GLES20.glViewport(0, 0, textureWidth, textureHeight);

                    GLES20.glUseProgram(horizontalBlurProgram);
                    setupVertexAttributes(hAPositionHandle, hATexCoordHandle);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentSource);
                    GLES20.glUniform1i(hUTextureHandle, 0);
                    GLES20.glUniform1f(hUTexelWidthHandle, 1.0f / textureWidth);
                    GLES20.glUniform1f(hUBlurRadiusHandle, blurRadius);
                    GLES20.glUniform1i(hUIsMirroredHandle, isMirrored ? 1 : 0);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                    
                    if (lastPass) {
                        
                        if (useGradientAlpha) {
                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffers[1]);
                            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, pongTexture[0], 0);
                            GLES20.glViewport(0, 0, textureWidth, textureHeight);
                        } else {
                            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
                        }
                    } else {
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffers[1]);
                        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, pongTexture[0], 0);
                        GLES20.glViewport(0, 0, textureWidth, textureHeight);
                    }

                    GLES20.glUseProgram(verticalBlurProgram);
                    setupVertexAttributes(vAPositionHandle, vATexCoordHandle);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pingTexture[0]);
                    GLES20.glUniform1i(vUTextureHandle, 0);
                    GLES20.glUniform1f(vUTexelHeightHandle, 1.0f / textureHeight);
                    GLES20.glUniform1f(vUBlurRadiusHandle, blurRadius);
                    GLES20.glUniform1i(vUIsMirroredHandle, isMirrored ? 1 : 0);

                    if (lastPass && !useGradientAlpha) {
                        GLES20.glEnable(GLES20.GL_BLEND);
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                    }

                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                    if (lastPass && !useGradientAlpha) {
                        GLES20.glDisable(GLES20.GL_BLEND);
                    }
                    
                    currentSource = pongTexture[0];
                }

                
                if (useGradientAlpha) {
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    GLES20.glUseProgram(finalGradientProgram);
                    setupVertexAttributes(fAPositionHandle, fATexCoordHandle);
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentSource);
                    GLES20.glUniform1i(fUTextureHandle, 0);
                    GLES20.glUniform1f(fUGradientStartHandle, gradientStart);
                    GLES20.glUniform1f(fUGradientEndHandle, gradientEnd);
                    GLES20.glUniform1i(fUIsMirroredHandle, isMirrored ? 1 : 0);
                    GLES20.glUniform2f(fUSizeHandle, surfaceWidth, surfaceHeight);
                    GLES20.glUniform4f(fUCornerRadiusHandle, cornerRadiusTopLeft / getScaleX(), cornerRadiusTopRight / getScaleX(), cornerRadiusBottomLeft / getScaleX(), cornerRadiusBottomRight / getScaleX());

                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                    GLES20.glDisable(GLES20.GL_BLEND);

                    cleanupVertexAttributes(fAPositionHandle, fATexCoordHandle);
                }

                cleanupVertexAttributes(hAPositionHandle, hATexCoordHandle);
                cleanupVertexAttributes(vAPositionHandle, vATexCoordHandle);

                android.opengl.EGL14.eglSwapBuffers(eglDisplay, eglSurface);

            } catch (Exception e) {
                // ignore
            }
        }

        private void setupVertexAttributes(int positionHandle, int texCoordHandle) {
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glEnableVertexAttribArray(texCoordHandle);

            vertexBuffer.position(0);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer);
            vertexBuffer.position(2);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer);
        }

        private void cleanupVertexAttributes(int positionHandle, int texCoordHandle) {
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }

        private void cleanupGL() {
            if (eglDisplay != null) {
                android.opengl.EGL14.eglMakeCurrent(eglDisplay,
                    android.opengl.EGL14.EGL_NO_SURFACE,
                    android.opengl.EGL14.EGL_NO_SURFACE,
                    android.opengl.EGL14.EGL_NO_CONTEXT);

                if (eglSurface != null) {
                    android.opengl.EGL14.eglDestroySurface(eglDisplay, eglSurface);
                }
                if (eglContext != null) {
                    android.opengl.EGL14.eglDestroyContext(eglDisplay, eglContext);
                }
                android.opengl.EGL14.eglTerminate(eglDisplay);
            }

            if (sourceBitmap != null) {
                sourceBitmap.recycle();
                sourceBitmap = null;
            }

            initialized = false;
        }
    }
} 