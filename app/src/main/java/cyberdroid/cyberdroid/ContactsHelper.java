package cyberdroid.cyberdroid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jkm-droid on 22/04/2021.
 */

public class ContactsHelper extends SQLiteOpenHelper {
    SQLiteDatabase sqLiteDatabase;
    private static final String DATABASE_NAME = "contacts.db";
    private static final String TABLE_NAME = "read_contacts";

    private static final String COLUMN_PK = "ID";
    private static final String COLUMN_CONTACT_ID = "CONTACT_ID";
    private static final String COLUMN_CONTACT_NAME = "CONTACT_NAME";
    private static final String COLUMN_PHONENUMBER = "PHONENUMBER";

    public ContactsHelper(Context context) {
        super(context, DATABASE_NAME, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_NAME +
                "(ID INTEGER PRIMARY KEY AUTOINCREMENT, CONTACT_ID INTEGER, CONTACT_NAME TEXT, PHONENUMBER TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i1, int i3) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    //inserting all the contacts into the database
    public void insert_all_contacts(int contact_id, String contact_name, String phone_number) {
        long result = 0;
        sqLiteDatabase = this.getWritableDatabase();
        //check if record exists
        String query = "SELECT * FROM " + TABLE_NAME + " WHERE (" + COLUMN_CONTACT_ID + "=" + contact_id + " OR " + COLUMN_PHONENUMBER + "=" + phone_number +")";
        Cursor cursor = sqLiteDatabase.rawQuery(query, null);

//        if (cursor.getCount() < 0) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_CONTACT_ID, contact_id);
            contentValues.put(COLUMN_CONTACT_NAME, contact_name);
            contentValues.put(COLUMN_PHONENUMBER, phone_number);
            sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
//        }else{
//            System.out.println("---------------contacts exists-----------------");
//        }

        sqLiteDatabase.close();

    }

    //retrieving all the contacts from the database
    public Cursor get_all_contacts() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        return sqLiteDatabase.rawQuery("Select * from " + TABLE_NAME, null);
    }

    //deleting all the contacts from the database
    public void delete_all_contacts() {
        sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.execSQL("delete from " + TABLE_NAME);
        sqLiteDatabase.close();
    }

    //delete a single row
    public void delete_contact(String phonenumber) {
        sqLiteDatabase = this.getWritableDatabase();
        sqLiteDatabase.delete(TABLE_NAME, COLUMN_PHONENUMBER + "=" + phonenumber, null);
    }

    //get the total records
    public int count_records() {
        sqLiteDatabase = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        int contacts = sqLiteDatabase.rawQuery(query, null).getCount();
        sqLiteDatabase.close();

        return contacts;
    }
}