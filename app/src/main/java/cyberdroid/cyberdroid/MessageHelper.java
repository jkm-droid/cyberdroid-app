package cyberdroid.cyberdroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jkm-droid on 05/04/2021.
 */

public class MessageHelper extends SQLiteOpenHelper {
    SQLiteDatabase sqLiteDatabase;
    private static final String DATABASE_NAME = "messages.db";
    private static final String TABLE_NAME = "read_messages";

    private static final String COLUMN_PK = "ID";
    private static final String COLUMN_SMS_ID = "SMS_ID";
    private static final String COLUMN_ADDRESS = "ADDRESS";
    private static final String COLUMN_BODY = "BODY";
    private static final String COLUMN_DATE = "DATE";
    private static final String COLUMN_SMS_TYPE = "SMS_TYPE";

    public MessageHelper(Context context){
        super(context,DATABASE_NAME, null, 6);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME +
                "(ID INTEGER PRIMARY KEY AUTOINCREMENT, SMS_ID INTEGER, ADDRESS TEXT, BODY TEXT, DATE TEXT, SMS_TYPE TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i1, int i3) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
    //inserting all the messages into the database
    public void insert_all_messages(int sms_id, String address, String body, String date, String sms_type){
        long result = 0;
        sqLiteDatabase = this.getWritableDatabase();
        //check if record exists
        String query = "SELECT * FROM "+TABLE_NAME+" WHERE "+COLUMN_SMS_ID+"="+sms_id;
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

//        if (cursor == null && cursor.getCount() < 0){
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_SMS_ID, sms_id);
            contentValues.put(COLUMN_ADDRESS, address);
            contentValues.put(COLUMN_BODY, body);
            contentValues.put(COLUMN_DATE, date);
            contentValues.put(COLUMN_SMS_TYPE, sms_type);

            sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
//        }

        sqLiteDatabase.close();

    }

    //retrieving all the messages from the database
    public Cursor get_all_messages() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        return sqLiteDatabase.rawQuery("Select * from " + TABLE_NAME, null);
    }
    //deleting all the messages from the database
    public void delete_all_messages(){
        sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.execSQL("delete from "+ TABLE_NAME);
        sqLiteDatabase.close();
    }

    //delete a single row
    public void delete_message(String sms_id){
        sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.delete(TABLE_NAME,COLUMN_SMS_ID+"="+sms_id, null);
    }

    //get the total records
    public int count_records(){
        sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT * FROM "+TABLE_NAME;
        int messages = sqLiteDatabase.rawQuery(query, null).getCount();
        sqLiteDatabase.close();

        return messages;
    }
}

