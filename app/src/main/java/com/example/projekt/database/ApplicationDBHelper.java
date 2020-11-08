package com.example.projekt.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.projekt.database.ApplicationCotract.*;

import androidx.annotation.Nullable;

public class ApplicationDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME="application.db";
    public static final int DATABASE_VERSION=12;

    public ApplicationDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_APPLICATION_TABLE = "CREATE TABLE " +
                ApllicationEntry.TABLE_NAME + " (" +
                ApllicationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ApllicationEntry.COLUMN_PACKAGE + " TEXT, " +
                ApllicationEntry.COLUMN_ICON + " BLOB, " +
                ApllicationEntry.COLUMN_APP_NAME + " TEXT, " +
                ApllicationEntry.COLUMN_ENTIRE_SCAN + " TEXT, " +
                ApllicationEntry.COLUMN_APP_INSTALL_DATE + " TEXT, " +
                ApllicationEntry.COLUMN_APP_LAST_USED_DATE + " TEXT, " +
                ApllicationEntry.COLUMN_APP_IN_FOREGROUND + " INTEGER, " +
                ApllicationEntry.COLUMN_APP_RX_RECEIVED + " INTEGER, " +
                ApllicationEntry.COLUMN_APP_TX_SEND + " INTEGER," +
                ApllicationEntry.COLUMN_SUGGEST_DELETE + " BOOLEAN," +
                ApllicationEntry.COLUMN_NEED_DELETE + " BOOLEAN" +
                ");";
        sqLiteDatabase.execSQL(SQL_CREATE_APPLICATION_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+ApllicationEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
