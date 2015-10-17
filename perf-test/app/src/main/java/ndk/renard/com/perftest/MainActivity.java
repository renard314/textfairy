package ndk.renard.com.perftest;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    static {
        System.loadLibrary("pngo");
        System.loadLibrary("jpeg");
        System.loadLibrary("lept");
        System.loadLibrary("perf-test");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button b = (Button) findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                b.setEnabled(false);

                new AsyncTask<Void,Void,Void>(){

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        b.setEnabled(true);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        Log.i(LOG_TAG, stringFromJNI());
                        return null;
                    }
                }.execute();
            }
        });
    }

    public native String stringFromJNI();

}


