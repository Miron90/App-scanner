package com.example.projekt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationDBHelper;

public class Notification extends BroadcastReceiver {
    SQLiteDatabase database;
    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationDBHelper dbHelper = new ApplicationDBHelper(context);
        database = dbHelper.getWritableDatabase();
        Cursor cursor = getAllItems();
        cursor.moveToNext();
        if(cursor.getCount()>0) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notify")
                    .setSmallIcon(R.drawable.ic_baseline_adb_24)
                    .setContentTitle("Danger or useless apps")
                    .setContentText("You have: "+cursor.getCount())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManagerCompat=NotificationManagerCompat.from(context);

            notificationManagerCompat.notify(200,builder.build());
        }
    }
    private Cursor getAllItems() {
        return database.query(ApplicationCotract.ApllicationEntry.TABLE_NAME,
                new String[]{ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME,
                        ApplicationCotract.ApllicationEntry.COLUMN_ICON,
                        ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE},
                ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE+"=?",
                new String[]{"1"},
                null,
                null,
                ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + " DESC");
    }
}
