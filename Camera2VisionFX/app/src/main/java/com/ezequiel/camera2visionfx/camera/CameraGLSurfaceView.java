package com.ezequiel.camera2visionfx.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import com.ezequiel.camera2visionfx.utils.Utils;

import org.wysaid.common.Common;
import org.wysaid.nativePort.CGEFrameRenderer;
import org.wysaid.texUtils.TextureRenderer;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Ezequiel on 02/10/2017.
 */

public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private Context context;
    private Camera2Source mCamera2Source = null;
    private boolean isReadyToStart = false;
    public int mMaxTextureSize = 0;
    public int mViewWidth;
    public int mViewHeight;
    protected int mRecordWidth = 720;
    protected int mRecordHeight = 1280;
    protected SurfaceTexture mSurfaceTexture;
    protected int mTextureID;
    protected CGEFrameRenderer mFrameRecorder;
    protected TextureRenderer.Viewport mDrawViewport = new TextureRenderer.Viewport();
    protected boolean mIsTransformMatrixSet = false;
    private boolean usingFrontCamera = true;

    public CameraGLSurfaceView(Context context) {
        super(context);
        this.context = context;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 8, 0);
        getHolder().setFormat(PixelFormat.RGBA_8888);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        createCameraSourceFront();
    }

    public synchronized void switchCamera() {
        if(usingFrontCamera) {
            isReadyToStart = false;
            mCamera2Source.stop();
            createCameraSourceBack();
            usingFrontCamera = false;
            resumePreview();
        } else {
            isReadyToStart = false;
            mCamera2Source.stop();
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
        mCamera2Source = new Camera2Source.Builder(context)
                .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                .build();

        //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
        //WE WILL USE CAMERA1.
        if(mCamera2Source.isCamera2Native()) {
            isReadyToStart = true;
        } else {
            if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
        }
    }

    private void createCameraSourceBack() {
        mCamera2Source = new Camera2Source.Builder(context)
                .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                .setFacing(Camera2Source.CAMERA_FACING_BACK)
                .build();

        //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
        if(mCamera2Source.isCamera2Native()) {
            isReadyToStart = true;
        } else {
            if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
        }
    }

    public void takePicture(Camera2Source.ShutterCallback sc, Camera2Source.PictureCallback pc) {
        if(mCamera2Source != null) mCamera2Source.takePicture(sc, pc);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        int texSize[] = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, texSize, 0);
        mMaxTextureSize = texSize[0];
        mTextureID = Common.genSurfaceTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mFrameRecorder = new CGEFrameRenderer();
        mIsTransformMatrixSet = false;
        if (!mFrameRecorder.init(mRecordWidth, mRecordHeight, mRecordWidth, mRecordHeight)) {
            Log.e("ASD", "Frame Recorder init failed!");
        }

        mFrameRecorder.setSrcRotation((float) (Math.PI / 2.0));
        mFrameRecorder.setSrcFlipScale(1.0f, -1.0f);
        mFrameRecorder.setRenderFlipScale(1.0f, -1.0f);

        requestRender();

        if(isReadyToStart) {
            if (!mCamera2Source.isCameraOpened()) {
                try {
                    mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context));
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
    }

    protected void calcViewport() {
        float scaling = mRecordWidth / (float) mRecordHeight;
        float viewRatio = mViewWidth / (float) mViewHeight;
        float s = scaling / viewRatio;
        int w, h;
        //撑满全部view(内容大于view)
        if (s > 1.0) {
            w = (int) (mViewHeight * scaling);
            h = mViewHeight;
        } else {
            w = mViewWidth;
            h = (int) (mViewWidth / scaling);
        }
        mDrawViewport.width = w;
        mDrawViewport.height = h;
        mDrawViewport.x = (mViewWidth - mDrawViewport.width) / 2;
        mDrawViewport.y = (mViewHeight - mDrawViewport.height) / 2;
        Log.i("ASD", String.format("View port: %d, %d, %d, %d", mDrawViewport.x, mDrawViewport.y, mDrawViewport.width, mDrawViewport.height));
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i("ASD", String.format("onSurfaceChanged: %d x %d", width, height));

        GLES20.glClearColor(0, 0, 0, 0);

        mViewWidth = width;
        mViewHeight = height;

        calcViewport();

        if(isReadyToStart) {
            if (!mCamera2Source.isCameraOpened()) {
                try {
                    mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context));
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
                        mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context));
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
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        mCamera2Source.stop();
    }

    public synchronized void resumePreview() {
        if (mFrameRecorder == null) {
            Log.e("ASD", "resumePreview after release!!");
            return;
        }
        if (!mCamera2Source.isCameraOpened()) {
            try {
                mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context));
            } catch(SecurityException ex) {} catch(IOException e) {}
        }
        if (!mCamera2Source.isPreviewing()) {
            try {
                mCamera2Source.start(mSurfaceTexture, Utils.getScreenRotation(context));
            } catch (SecurityException ex) {} catch (IOException e) {}
            mFrameRecorder.srcResize(mCamera2Source.getPreviewSize().getHeight(), mCamera2Source.getPreviewSize().getWidth());
        }
        requestRender();
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
        mCamera2Source.stop();
        super.onPause();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {requestRender();}
}
