package net.sample.pandroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {

    /* Name of Database */
    private final static String DB_NAME = "History";
    /* Version of Database */
    private final static int DB_VER = 1;
    /* Name of Table */
    private final static String DB_TABLE = "HistoryTable";

    /*
     * Constructor
      */
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    /*
     * onCreate
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " +DB_TABLE+ "(id text primary key,info text)");
    }

    /*
     * onUpgrade
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}