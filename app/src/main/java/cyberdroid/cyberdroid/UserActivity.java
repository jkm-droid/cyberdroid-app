package cyberdroid.cyberdroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UserActivity extends AppCompatActivity {

    ProgressDialog progressDialog;
    TextView errorText, getKeyText;
    EditText  spyKeyText;
    Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        errorText = findViewById(R.id.error);
        getKeyText = findViewById(R.id.get_key);
        login = findViewById(R.id.login_button);
        spyKeyText = findViewById(R.id.spy_key);

        initialize();
    }

    private void initialize() {
        SharedPreferences preferences = getSharedPreferences(UserPreferences.Login.NAME, MODE_PRIVATE);

        if (preferences.getString(UserPreferences.Login.USERNAME,"").length() > 3){
            if (preferences.getBoolean(UserPreferences.Login.LOGGED, false)){
                startActivity(new Intent(UserActivity.this, MainActivity.class));
                finish();
            }
        }else{
            login();
        }
    }

    private void login() {
        getKeyText.setOnClickListener(v -> get_spy_key());
        login.setOnClickListener(v -> {
            String spy_key = spyKeyText.getText().toString();

            spy_key = spy_key.trim();

            String error = "";
            if (spy_key.length() != 6)
                error += "\nInvalid key";

            if (error.trim().length() > 5)
                ((TextView)findViewById(R.id.error)).setText(error);
            else if (!MyHelper.isNetworkAvailable(UserActivity.this)){
                Snackbar.make(findViewById(R.id.layout), "No internet connection", Snackbar.LENGTH_LONG)
                        .setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        })
                        .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ))
                        .show();
            }
            else {
                String data = "";
                try {
                    data += URLEncoder.encode("login_user", "UTF-8") + "=" + URLEncoder.encode("login", "UTF-8") + "&";
                    data += URLEncoder.encode("spy_key", "UTF-8") + "=" + URLEncoder.encode(spy_key, "UTF-8");

                    progressDialog = new ProgressDialog(UserActivity.this);
                    progressDialog.setMessage("Authenticating...Please wait");
                    progressDialog.setIndeterminate(false);
                    progressDialog.setCancelable(true);
                    if (progressDialog != null)
                        progressDialog.show();
                    sendOnline("https://cyberdroid.mblog.co.ke/cyberdroid/login.php", data);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void get_spy_key() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://cyberdroid.mblog.co.ke/users/register"));
        startActivity(intent);
    }

    public void sendOnline(final String link, final String data){
        @SuppressLint("HandlerLeak") Handler handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (progressDialog != null)
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();
                //check if the response is in json format
                if(((String)msg.obj).startsWith("{") && (((String) msg.obj).endsWith("}"))){
                    JSONObject object;
                    try {
                        object = new JSONObject((String)msg.obj);
                        //if the status code is 200==>login is successful
                        if(object.getInt("status_code") == 200){
                            SharedPreferences preferences = getSharedPreferences(UserPreferences.Login.NAME, MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(UserPreferences.Login.EMAIL, object.getString("email"));
                            editor.putString(UserPreferences.Login.USERNAME, object.getString("username"));
                            editor.putString(UserPreferences.Login.SPY_KEY, object.getString("spy_key"));
                            editor.putBoolean(UserPreferences.Login.LOGGED, true);
                            editor.apply();

                            Toast.makeText(UserActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();;

                            startActivity(new Intent(UserActivity.this, MainActivity.class));
                            finish();
                            //login attempt is unsuccessful
                        }else if(object.getInt("status_code") == 201){
                            AlertDialog.Builder builder = new AlertDialog.Builder(UserActivity.this);
                            builder.setMessage(object.getString("message"))
                                    .setTitle("An Error Occurred")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", null)
                                    .show();

                            spyKeyText.setText("");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(UserActivity.this);
                    builder.setMessage(((String)msg.obj))
                            .setTitle("Error Occurred")
                            .setCancelable(false)
                            .setPositiveButton("OK", null)
                            .show();

                    spyKeyText.setText("");
                }

            }
        };
        Thread thread = new Thread(){
            @Override
            public void run() {
                try {
                    System.out.println("data to send: "+data);
                    String response = MyHelper.connect_to_server(link, data);
                    Message message = new Message();
                    message.arg1 = 1;
                    message.obj = response;
                    System.out.println("response: "+response);
                    handler.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
}