package ndk.renard.com.perftest;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    static {
        System.loadLibrary("pngo");
        System.loadLibrary("jpeg");
        System.loadLibrary("lept");
        System.loadLibrary("perf-test");
    }

    private void copyInputStreamToFile(InputStream in, File file) {
        try {
            file.createNewFile();
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button b = (Button) findViewById(R.id.button);
        final InputStream inputStream = getResources().openRawResource(R.raw.test_image);
        final File myFile = new File(Environment.getExternalStorageDirectory(), "test_image.png");
        if (!myFile.exists()) {
            copyInputStreamToFile(inputStream, myFile);
        }

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                b.setEnabled(false);

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        b.setEnabled(true);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        Log.i(LOG_TAG, stringFromJNI(myFile.getAbsolutePath()));
                        return null;
                    }
                }.execute();
            }
        });
    }

    public native String stringFromJNI(String imagePath);

}


