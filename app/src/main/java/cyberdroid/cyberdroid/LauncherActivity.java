package cyberdroid.cyberdroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        int SPLASH_TIME_OUT = 2000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //moving to another activity
                Intent registerActivity = new Intent(LauncherActivity.this, UserActivity.class);
                startActivity(registerActivity);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}