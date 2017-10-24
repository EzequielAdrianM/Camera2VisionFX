package com.ezequiel.camera2visionfx.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;

import com.ezequiel.camera2visionfx.utils.Utils;
import com.google.android.gms.vision.face.FaceDetector;

import org.wysaid.common.Common;
import org.wysaid.nativePort.CGEFrameRenderer;
import org.wysaid.texUtils.TextureRenderer;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Ezequiel on 02/10/2017.
 */

public class CustomCameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private Context context;
    private CameraSource mCameraSource = null;
    private Camera2Source mCamera2Source = null;
    private boolean useCamera2 = false;
    private boolean isReadyToStart = false;
    public int mSurfaceWidth;
    public int mSurfaceHeight;
    protected int mRecordWidth = 768;
    protected int mRecordHeight = 1280;
    protected SurfaceTexture mSurfaceTexture;
    protected int mTextureID;
    protected CGEFrameRenderer mFrameRecorder;
    protected TextureRenderer.Viewport mDrawViewport = new TextureRenderer.Viewport();
    protected boolean mIsTransformMatrixSet = false;
    private boolean usingFrontCamera = true;
    private FaceDetector mDetector;
    private GraphicOverlay mGrOverlay;

    public CustomCameraGLSurfaceView(Context context) {super(context);}

    public CustomCameraGLSurfaceView(Context context, GraphicOverlay gro, FaceDetector detector) {
        super(context);
        this.context = context;
        mDetector = detector;
        mGrOverlay = gro;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 8, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        useCamera2 = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        createCameraSourceFront();
    }

    public boolean useCamera2() {
        return useCamera2;
    }

    public synchronized void switchCamera() {
        if(usingFrontCamera) {
            isReadyToStart = false;
            if(useCamera2) {
                mCamera2Source.stop();
            } else {
                mCameraSource.stop();
            }
            createCameraSourceBack();
            usingFrontCamera = false;
            resumePreview();
        } else {
            isReadyToStart = false;
            if(useCamera2) {
                mCamera2Source.stop();
            } else {
                mCameraSource.stop();
            }
            createCameraSourceFront();
            usingFrontCamera = true;
            resumePreview();
        }
    }

    public synchronized void setFilterWithConfig(final String config) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRecorder != null) {
                    mFrameRecorder.setFilterWidthConfig(config);
                }
            }
        });
    }

    public void setFilterIntensity(final float intensity) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFrameRecorder != null) {
                    mFrameRecorder.setFilterIntensity(intensity);
                }
            }
        });
    }

    private void createCameraSourceFront() {
        if(useCamera2) {
            mCamera2Source = new Camera2Source.Builder(context, mDetector)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                    .build();

            //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
            //WE WILL USE CAMERA1.
            if(mCamera2Source.isCamera2Native()) {
                isReadyToStart = true;
            } else {
                useCamera2 = false;
                if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
            }
        } else {
            mCameraSource = new CameraSource.Builder(context, mDetector)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedFps(30.0f)
                    .build();

            isReadyToStart = true;
        }
    }

    private void createCameraSourceBack() {
        if(useCamera2) {
            mCamera2Source = new Camera2Source.Builder(context, mDetector)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_BACK)
                    .build();

            //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
            //WE WILL USE CAMERA1.
            if(mCamera2Source.isCamera2Native()) {
                isReadyToStart = true;
            } else {
                useCamera2 = false;
                if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
            }
        } else {
            mCameraSource = new CameraSource.Builder(context, mDetector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(30.0f)
                    .build();

            isReadyToStart = true;
        }
    }

    public void takePicture(CameraSource.ShutterCallback sc, CameraSource.PictureCallback pc) {
        if(mCameraSource != null) mCameraSource.takePicture(sc, pc);
    }

    public void takePicture(Camera2Source.ShutterCallback sc, Camera2Source.PictureCallback pc) {
        if(mCamera2Source != null) mCamera2Source.takePicture(sc, pc);
    }

    public void autoFocus(CameraSource.AutoFocusCallback ac) {
        if(mCameraSource != null) {
            mCameraSource.autoFocus(new CameraSource.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success) {

                }
            });
        }
    }

    public void autoFocus(Camera2Source.AutoFocusCallback ac, MotionEvent pEvent, View v) {
        if(mCamera2Source != null) {
            mCamera2Source.autoFocus(ac, pEvent, v.getWidth(), v.getHeight());
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        int texSize[] = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, texSize, 0);
        mTextureID = Common.genSurfaceTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mFrameRecorder = new CGEFrameRenderer();
        mIsTransformMatrixSet = false;
        if (!mFrameRecorder.init(mRecordWidth, mRecordHeight, mRecordWidth, mRecordHeight)) {
            Log.e("ASD", "Frame Recorder init failed!");
        }
        //It is safe to use zero deg in both cameras.
        mFrameRecorder.setSrcRotation((float) Math.toRadians(0));
        mFrameRecorder.setSrcFlipScale(1.0f, -1.0f);
        mFrameRecorder.setRenderFlipScale(1.0f, -1.0f);
    }

    protected void calcFiltersViewport() {
        float scaling = mRecordWidth / (float) mRecordHeight;
        float viewRatio = mSurfaceWidth / (float) mSurfaceHeight;
        float s = scaling / viewRatio;
        int w, h;
        if (s > 1.0) {
            w = (int) (mSurfaceHeight * scaling);
            h = mSurfaceHeight;
        } else {
            w = mSurfaceWidth;
            h = (int) (mSurfaceWidth / scaling);
        }
        mDrawViewport.width = w;
        mDrawViewport.height = h;
        mDrawViewport.x = (mSurfaceWidth - mDrawViewport.width) / 2;
        mDrawViewport.y = (mSurfaceHeight - mDrawViewport.height) / 2;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glClearColor(0, 0, 0, 0);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        calcFiltersViewport();

        if(isReadyToStart) {
            if(useCamera2) {
                if (!mCamera2Source.isCameraOpened()) {
                    try {
                        mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context), mSurfaceWidth, mSurfaceHeight, mGrOverlay);
                    } catch (IOException e) {
                        Log.e("ASD", "Unable to start camera 2 source.", e);
                        mCamera2Source.release();
                        mCamera2Source = null;
                    } catch (SecurityException e) {
                        Log.e("ASD", "Unable to start camera 2 source.", e);
                        mCamera2Source.release();
                        mCamera2Source = null;
                    }
                } else {
                    if(!mCamera2Source.isPreviewing()) {
                        try {
                            mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context), mSurfaceWidth, mSurfaceHeight, mGrOverlay);
                        } catch (IOException e) {
                            Log.e("ASD", "Unable to start camera 2 source.", e);
                            mCamera2Source.release();
                            mCamera2Source = null;
                        } catch (SecurityException e) {
                            Log.e("ASD", "Unable to start camera 2 source.", e);
                            mCamera2Source.release();
                            mCamera2Source = null;
                        }
                    }
                }
            } else {
                if (!mCameraSource.isCameraOpened()) {
                    try {
                        mCameraSource.start(mSurfaceTexture, mGrOverlay);
                    } catch (IOException e) {
                        Log.e("ASD", "Unable to start camera source.", e);
                        mCameraSource.release();
                        mCameraSource = null;
                    } catch (SecurityException e) {
                        Log.e("ASD", "Unable to start camera source.", e);
                        mCameraSource.release();
                        mCameraSource = null;
                    }
                } else {
                    if(!mCameraSource.isPreviewing()) {
                        try {
                            mCameraSource.start(mSurfaceTexture, mGrOverlay);
                        } catch (IOException e) {
                            Log.e("ASD", "Unable to start camera source.", e);
                            mCameraSource.release();
                            mCameraSource = null;
                        } catch (SecurityException e) {
                            Log.e("ASD", "Unable to start camera source.", e);
                            mCameraSource.release();
                            mCameraSource = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        if(useCamera2) {
            mCamera2Source.stop();
        } else {
            mCameraSource.stop();
        }
    }

    public synchronized void resumePreview() {
        if (mFrameRecorder == null) {
            Log.e("ASD", "resumePreview after release!!");
            return;
        }
        if(useCamera2) {
            if (!mCamera2Source.isCameraOpened()) {
                try {
                    mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context), mSurfaceWidth, mSurfaceHeight, mGrOverlay);
                } catch(SecurityException ex) {} catch(IOException e) {}
            }
            if (!mCamera2Source.isPreviewing()) {
                try {
                    mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context), mSurfaceWidth, mSurfaceHeight, mGrOverlay);
                } catch (SecurityException ex) {} catch (IOException e) {}
                mFrameRecorder.srcResize(mCamera2Source.getPreviewSize().getHeight(), mCamera2Source.getPreviewSize().getWidth());
            }
            requestRender();
        } else {
            if (!mCameraSource.isCameraOpened()) {
                try {
                    mCameraSource.start(mSurfaceTexture, mGrOverlay);
                } catch(SecurityException ex) {} catch(IOException e) {}
            }
            if (!mCameraSource.isPreviewing()) {
                try {
                    mCameraSource.start(mSurfaceTexture, mGrOverlay);
                } catch (SecurityException ex) {} catch (IOException e) {}
                mFrameRecorder.srcResize(mCameraSource.getPreviewSize().getHeight(), mCameraSource.getPreviewSize().getWidth());
            }
            requestRender();
        }
    }

    private float[] _transformMatrix = new float[16];

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture == null) return;
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(_transformMatrix);
        mFrameRecorder.update(mTextureID, _transformMatrix);
        mFrameRecorder.runProc();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);
        mFrameRecorder.render(mDrawViewport.x, mDrawViewport.y, mDrawViewport.width, mDrawViewport.height);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        if(useCamera2) {
            mCamera2Source.stop();
        } else {
            mCameraSource.stop();
        }
        super.onPause();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {requestRender();}
}
