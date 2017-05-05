package suny.camerahelper;

import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
//    private CameraPreview       cameraPreview;
    private CameraTexture       cameraPreview;
    private HandlerThread       cameraThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraThread = new HandlerThread("CameraPreview");
        cameraThread.start();
//        cameraPreview = new CameraPreview(this, cameraThread.getLooper());
        cameraPreview = new CameraTexture(cameraThread.getLooper());
        Log.i(TAG, "onCreate end");
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart start");
        super.onStart();
        Message msg = cameraPreview.obtainMessage(CameraPreview.OPENCAMERA);
        msg.sendToTarget();

        Button takePhotoBtn = (Button) findViewById(R.id.takePhotoBtn);
        takePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message msg = cameraPreview.obtainMessage(CameraPreview.TAKEPHOTO);
                msg.sendToTarget();
            }
        });
        Log.i(TAG, "onStart end");
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop start");
        Message msg = cameraPreview.obtainMessage(CameraPreview.CLOSECAMERA);
        msg.sendToTarget();
        super.onStop();
        Log.i(TAG, "onStop end");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy start");
        if (cameraPreview != null) {
            cameraPreview = null;
        }
        if (cameraThread != null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cameraThread.getLooper().quit();
            cameraThread = null;
        }
        super.onDestroy();
        Log.i(TAG, "onDestroy end");
    }
}
