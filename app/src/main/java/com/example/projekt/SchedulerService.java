package com.example.projekt;

import android.app.Service;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationDBHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
////////////////TODO DELETE
public class SchedulerService extends Service {
    BroadcastReceiver mReceiver;
    private Context context;
    private SQLiteDatabase database;



    @Override
    public void onCreate() {
        // get an instance of the receiver in your service
        this.context=getApplicationContext();
        ApplicationDBHelper dbHelper = new ApplicationDBHelper(context);
        database = dbHelper.getWritableDatabase();
        //this.st
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                scanAppsAndSaveToDatabase();
            }
        }, 1000*10);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void scanAppsAndSaveToDatabase() {
        ApplicationDBHelper dbHelper = new ApplicationDBHelper(context);
        SQLiteDatabase readAbleDatabase = dbHelper.getReadableDatabase();
        DateFormat df = new DateFormat();
        PackageManager packageManager = context.getPackageManager();
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStatsMobile = null;
        NetworkStats networkStatsWIFI = null;
        long startTime = new GregorianCalendar(2000, 0, 1).getTimeInMillis();
        long endTime = new GregorianCalendar(2021, 0, 1).getTimeInMillis();
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        List<PackageInfo> packageList = context.getPackageManager().getInstalledPackages(0);

        Cursor cursor = getAllItems();
        cursor.moveToNext();
        ArrayList<Item> appsPackage = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            appsPackage.add(new Item(cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE)), -1));
            if (!cursor.moveToNext()) {
                break;
            }
        }
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appPackage = packageInfo.packageName;
                for (int j = 0; j < appsPackage.size(); j++) {
                    if (appPackage.equals(appsPackage.get(j).getAppPackage())) {
                        appsPackage.get(j).setPosition(j);
                        break;
                    }
                }

                long longInsattationDate = 0;
                longInsattationDate = packageInfo.firstInstallTime;

                Map<String, UsageStats> aggregatedStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
//Get stats for particular package as follows:
                UsageStats usageStats = aggregatedStatsMap.get(packageInfo.packageName);

                networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, null,
                        0, System.currentTimeMillis(), packageInfo.applicationInfo.uid);
                networkStatsWIFI = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis(), packageInfo.applicationInfo.uid);
                NetworkStats.Bucket bucketMobile = new NetworkStats.Bucket();
                NetworkStats.Bucket bucketWifi = new NetworkStats.Bucket();
                if (usageStats != null) {
                    Date date = new Date(longInsattationDate);
                    String properDate = (String) df.format("yyyy-MM-dd HH:mm:ss", date);


                    Date date2 = new Date(usageStats.getLastTimeUsed());
                    String properDate2 = (String) df.format("yyyy-MM-dd HH:mm:ss", date2);
                    networkStatsWIFI.getNextBucket(bucketWifi);
                    long appReceived = bucketWifi.getRxBytes();
                    long appSend = bucketWifi.getTxBytes();
                    networkStatsMobile.getNextBucket(bucketMobile);
                    appReceived = bucketMobile.getRxBytes();
                    appSend = bucketMobile.getTxBytes();

                    ContentValues cv = new ContentValues();
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE, packageInfo.packageName);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME, (String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE, properDate);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_LAST_USED_DATE, properDate2);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_IN_FOREGROUND, usageStats.getTotalTimeInForeground());
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_RX_RECEIVED, appReceived);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_TX_SEND, appSend);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE, true);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE, true);
                    Drawable drawable = packageManager.getApplicationIcon(packageInfo.applicationInfo);
                    Bitmap bitmap = drawableToBitmap(drawable);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_ICON, insertImg(bitmap));

                    if (database.update(ApplicationCotract.ApllicationEntry.TABLE_NAME, cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + "=?", new String[]{(String) packageManager.getApplicationLabel(packageInfo.applicationInfo)}) == 0) {
                        database.insert(ApplicationCotract.ApllicationEntry.TABLE_NAME, null, cv);
                    }
                } else {
                    Date date = new Date(longInsattationDate);
                    String properDate = (String) df.format("yyyy-MM-dd HH:mm:ss", date);
                    networkStatsWIFI.getNextBucket(bucketWifi);
                    long appReceived = bucketWifi.getRxBytes();
                    long appSend = bucketWifi.getTxBytes();
                    networkStatsMobile.getNextBucket(bucketMobile);
                    appReceived = bucketMobile.getRxBytes();
                    appSend = bucketMobile.getTxBytes();

                    ContentValues cv = new ContentValues();
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE, packageInfo.packageName);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME, (String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE, String.valueOf(properDate));
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_LAST_USED_DATE, "");
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_IN_FOREGROUND, "");
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_RX_RECEIVED, appReceived);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_TX_SEND, appSend);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE, true);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE, false);
                    Drawable drawable = packageManager.getApplicationIcon(packageInfo.applicationInfo);
                    Bitmap bitmap = drawableToBitmap(drawable);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_ICON, insertImg(bitmap));

                    if (database.update(ApplicationCotract.ApllicationEntry.TABLE_NAME, cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + "=?", new String[]{(String) packageManager.getApplicationLabel(packageInfo.applicationInfo)}) == 0) {
                        database.insert(ApplicationCotract.ApllicationEntry.TABLE_NAME, null, cv);
                    }
                }

            }
        }
        for (int i = 0; i < appsPackage.size(); i++) {
            if (appsPackage.get(i).getPosition() == -1) {
                database.delete(ApplicationCotract.ApllicationEntry.TABLE_NAME,
                        ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE + "=?",
                        new String[]{appsPackage.get(i).getAppPackage()});
            }
        }

    }
    public byte[] insertImg(Bitmap img) {


        byte[] data = getBitmapAsByteArray(img); // this is a function

        return data;


    }

    public  byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    public  Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Cursor getAllItems() {
        return database.query(ApplicationCotract.ApllicationEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + " DESC");
    }
}
