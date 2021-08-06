package cyberdroid.cyberdroid;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;

/**
 * Created by jkmdroid on 8/6/21.
 */
public class Images {
    Uri uri;
    String name, dateAdded, dateModified;
    int size;

    public Images(Uri uri, String name, int size, String dateAdded, String dateModified){
        this.uri = uri;
        this.name = name;
        this.size = size;
        this.dateAdded = dateAdded;
        this.dateModified = dateModified;
    }
}
