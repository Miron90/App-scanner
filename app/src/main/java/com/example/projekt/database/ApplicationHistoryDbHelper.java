package com.example.projekt.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class ApplicationHistoryDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME="applicationHistory.db";
    public static final int DATABASE_VERSION=3;

    public ApplicationHistoryDbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_APPLICATION_TABLE = "CREATE TABLE " +
                ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME + " (" +
                ApplicationCotract.ApllicationHistroyEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_PACKAGE + " TEXT, " +
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE + " TEXT, " +
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME + " TEXT, " +
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_LAST_USED_DATE + " TEXT, " +
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_IN_FOREGROUND + " INTEGER, " +
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED + " INTEGER, " +
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND + " INTEGER"+
                ");";
        sqLiteDatabase.execSQL(SQL_CREATE_APPLICATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+ ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
