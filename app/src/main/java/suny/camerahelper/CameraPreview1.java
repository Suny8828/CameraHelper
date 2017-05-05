package suny.camerahelper;

/**
 * Created by suny on 17-4-28.
 */

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraPreview1 extends Handler implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraPreview" ;
    private boolean         isStart = false;
    private Camera          mCamera = null;
    private SurfaceHolder   surfaceHolder;
    private short           dropFrameCount;
    private int             photoNum;

    public static final int OPENCAMERA = 1;
    public static final int CLOSECAMERA = 2;
    public static final int TAKEPHOTO = 3;

    public CameraPreview1(Context context, Looper looper, SurfaceView surfaceView) {
        super(looper);
        Log.d(TAG, "CameraPreview start");
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        Log.d(TAG, "CameraPreview end");
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated");
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        stopCarmera();
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i(TAG, "handleMessage: msg.what:" + msg.what);
        switch (msg.what) {
            case OPENCAMERA:
                if (!isStart) {

                }
                break;
            case CLOSECAMERA:
                if (isStart) {

                }
                break;
            case TAKEPHOTO:
                if (isStart) {
                    photoNum = 1;
                }
                break;
            default:
                Log.w(TAG, "handleMessage: unknown message id: " + msg.what);
                break;
        }
        Log.i(TAG, "handleMessage end");
    }

    public void release() {
        Log.d(TAG, "release start");
        stopCarmera();
        Log.d(TAG, "release end");
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if(dropFrameCount <= 15) {
            dropFrameCount++;
            camera.addCallbackBuffer(data);
            return;
        }

        if (photoNum > 0) {
            onSavePhoto(data);
            photoNum = 0;
        }

        //reset buffer to camera.
        camera.addCallbackBuffer(data);
    }

    private boolean startCamera() {
        Log.d(TAG, "startCamera start");
        if (mCamera == null) {
            Log.i(TAG, "startCamera: open back camera");
            try {
                mCamera = Camera.open(0);
            } catch (RuntimeException e) {
                Log.e(TAG, "open camera: " + e);
                return false;
            }
            try {
                // if need preview, call interface of setPreviewDisplay
                mCamera.setPreviewDisplay(surfaceHolder);
                // if does not need preview, call interface of setPreviewTexture
//                mCamera.setPreviewTexture(mSurfaceTexture);

                //alloc buffer for camera callback data with once.
                byte[] rawBuf = new byte[PictureParam.PictureSize];
                mCamera.addCallbackBuffer(rawBuf);
                // set the buffer use setPreviewCallbackWithBuffer instead of setPreviewCallback
                mCamera.setPreviewCallbackWithBuffer(this);

                Camera.Parameters parameters = mCamera.getParameters();
                // old library of "camera.msm8909.so" produces video data which format is NV21;
                // new library of "camera.msm8909.so" produces video data which format is NV12,
                // but function of "setPreviewFormat" still need be seted format NV21.
                parameters.setPreviewFormat(ImageFormat.NV21);

                // Msm8909
                // 15000 : 15Fps
                // 24000 : 24Fps
                // 30000 : 30Fps
                parameters.setPreviewFpsRange(15000, 15000);
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                parameters.setPreviewSize(PictureParam.PictureWidth, PictureParam.PictureHeight);
//                parameters.setPictureSize(PictureParam.PictureWidth, PictureParam.PictureHeight);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                isStart = true;
            } catch(Exception e) {
                Log.e(TAG, "startCamera: ", e);
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
                return false;
            }
        }
        Log.d(TAG, "startCamera end");
        return true;
    }

    private void stopCarmera() {
        Log.d(TAG, "stopCarmera start");
        if (mCamera != null) {
            Log.i(TAG, "stopCarmera: stop camera");
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            isStart = false;
        }

        Log.d(TAG, "stopCarmera end");
    }

    private boolean onSavePhoto(byte[] yuvData) {
        byte[] jpegData = encoderJepg(yuvData);
        if (jpegData != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
            String name = format.format(new Date()) + ".jpg";
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    + File.separator + name;
            File file = new File(path);
            FileOutputStream filestream;
            try {
                filestream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "onTakePhoto: " + e);
                return false;
            }
            try {
                filestream.write(jpegData, 0, jpegData.length);
                filestream.flush();
                filestream.close();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "onTakePhoto: " + e);
            }
        }
        return false;
    }

    private byte[] encoderJepg(byte[] data) {
        Log.d(TAG, "encoderJepg: start");

        byte[] picData = null;
        if (data != null) {
            YUVSwap(data, CameraPreview.PictureParam.PictureWidth, CameraPreview.PictureParam.PictureHeight);
            YuvImage image = new YuvImage(data, ImageFormat.NV21, CameraPreview.PictureParam.PictureWidth,
                    CameraPreview.PictureParam.PictureHeight, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, stream);
            byte[] jpegData = stream.toByteArray();
            picData = new byte[jpegData.length];
            System.arraycopy(jpegData, 0, picData, 0, jpegData.length);
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "encoderJepg: close stream error " + e);
            }
        } else {
            Log.i(TAG, "encoderJepg: input data is null");
        }

        Log.d(TAG, "encoderJepg: end");
        return picData;
    }

    /**
     * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU
     * I420YUVSemiPlanar is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UVUV
     * Apply NV21 to I420YUVSemiPlanar(NV12) Refer to https://wiki.videolan.org/YUV/
     */
    private void YUVSwap(byte[] data, int width, int height) {
        if (data == null) {
            return;
        }
        byte tmp;
        for (int i = width * height; i < width * height * 3 / 2; i += 2) {
            tmp = data[i];
            data[i] = data[i + 1];
            data[i + 1] = tmp;
        }
    }

    public interface SavePhoto {
        boolean onSavePhoto(byte[] yuvData, int user, int index);
    }

    public class PictureParam {
        public static final int PictureWidth = 1280;
        public static final int PictureHeight = 720;
        public static final int PictureSize = PictureWidth * PictureHeight * 3 / 2;
    }
}


