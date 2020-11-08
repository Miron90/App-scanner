package com.example.projekt;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentValues;
import android.content.Context;
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
import android.text.format.DateFormat;
import android.util.Log;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationDBHelper;
import com.example.projekt.database.ApplicationHistoryDbHelper;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SchedulerJobService extends JobService {

    private static final String TAG="JobService";
    private boolean jobCancelled = false;
    private Context context;
    private SQLiteDatabase entireDatabase,scanDatabase;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        context=getApplicationContext();
        ApplicationDBHelper dbHelper = new ApplicationDBHelper(context);
        entireDatabase = dbHelper.getWritableDatabase();
        ApplicationHistoryDbHelper dbHelper2 = new ApplicationHistoryDbHelper(context);
        scanDatabase = dbHelper2.getWritableDatabase();
        Log.e(TAG,"JOB started");
        doBackground(jobParameters,context, entireDatabase,scanDatabase);
        return true;
    }


    private  void doBackground(final JobParameters params, final Context context,final SQLiteDatabase entireDatabase,final SQLiteDatabase scanDatabase){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //scanDatabase.delete(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,null,null);
                scanApps(context,entireDatabase,scanDatabase);
                //TODO compareScans
                scanForUselessAndDangerApps(context,scanDatabase,entireDatabase);
                Log.e(TAG,"JOB finished");
                jobFinished(params,false);
            }


        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.e(TAG,"job cancelled");
        jobCancelled=true;
        return true;
    }

    private void scanForUselessAndDangerApps(Context context, SQLiteDatabase scanDatabase,SQLiteDatabase entireDatabase) {

        Log.e("skan","scanning for useless apps");

        Cursor scanCursor=scanDatabase.query(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE);
        Map<String,Long> appRX=new HashMap<>();
        Map<String,Long> appTX=new HashMap<>();
        Map<String,String> lastUsed=new HashMap<>();
        Map<String,Long> appRXFirst=new HashMap<>(),appTXFirst=new HashMap<>();
        scanCursor.moveToNext();
        for(int i=0;i<scanCursor.getCount();i++){
            Cursor entireDatabaseCursor=entireDatabase.query(ApplicationCotract.ApllicationEntry.TABLE_NAME,
                    null,
                    ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",
                    new String[]{scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))},
                    null,
                    null,
                    null);
            entireDatabaseCursor.moveToNext();
            if(entireDatabaseCursor.getInt(entireDatabaseCursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE))==1) {
                if (appRX.containsKey(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))) {
                    appRX.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            appRX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))) +
                                    scanCursor.getLong(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED))-
                                    appRXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))));
                    if(appRX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                            appRXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))>0)
                    {
                        Log.e("Dziala", "Bits: " + appRX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))) +
                                "FirstBits: " + appRXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))+"    "+scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)));
                    }
                } else {
                    appRX.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            scanCursor.getLong(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED)));
                    appRXFirst.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            scanCursor.getLong(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED)));
                }
                if (appTX.containsKey(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))) {
                    appTX.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            appTX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))) +
                                    scanCursor.getLong(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND))-
                                    appTXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))));
                    if(appTX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                            appTXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))>0)
                    {
                        Log.e("Dziala", "Bits: " + appRX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))) +
                                "FirstBits: " + appRXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))+"    "+scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)));
                    }
                } else {
                    appTX.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            scanCursor.getLong(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND)));
                    appTXFirst.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            scanCursor.getLong(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND))) ;
                }
                if (lastUsed.containsKey(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))) {
                    lastUsed.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_LAST_USED_DATE)));
                } else {
                    lastUsed.put(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)),
                            scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_LAST_USED_DATE)));
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date = scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE));
                Date databaseDate = null;
                try {
                    databaseDate = sdf.parse(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long scanDate = databaseDate.getTime();
                long now = System.currentTimeMillis();
                long diff=(now-scanDate)/1000/60/60/24;
                if (now - scanDate > ConstClass.FIVE_DAYS) {
                    inspect(appRX,appTX,appRXFirst,appTXFirst,scanCursor,sdf,now,8*1024*1024);
                } else if (now - scanDate > ConstClass.THREE_DAYS) {
                    inspect(appRX,appTX,appRXFirst,appTXFirst,scanCursor,sdf,now,8*1024*512);
                } else if (now - scanDate > ConstClass.ONE_DAY) {
                    inspect(appRX,appTX,appRXFirst,appTXFirst,scanCursor,sdf,now,8*1024*128);
                }
            }
            if(!scanCursor.moveToNext()){
                break;
            }
        }
        long trochdbeka=0;

    }

    public void inspect(Map<String,Long> appRX,Map<String,Long> appTX,Map<String,Long> appRXFirst,Map<String,Long> appTXFirst,Cursor scanCursor,SimpleDateFormat sdf,long now,long data){
        Log.e("skan","inscepting for useless apps");
        Log.e("ile rx", String.valueOf(appRX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                appRXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))))+scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME));
        if (appRX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                appRXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))>data*10){
            ContentValues cv =new ContentValues();
            cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE,true);
            entireDatabase.update(ApplicationCotract.ApllicationEntry.TABLE_NAME,cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",
                    new String[]{scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))});
        }
        Log.e("ile tx", String.valueOf(appTX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                appTXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))))+scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME));
        if (appTX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                appTXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))>data){
            ContentValues cv =new ContentValues();
            cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE,true);
            entireDatabase.update(ApplicationCotract.ApllicationEntry.TABLE_NAME,cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",
                    new String[]{scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))});
        }
        String date2 = scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_LAST_USED_DATE));
        if(!date2.equals("")) {
            Date databaseDate2 = null;
            try {
                databaseDate2 = sdf.parse(date2);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long lastUsedData = databaseDate2.getTime();
            long diff = now - lastUsedData;
            if (diff > ConstClass.THREE_DAYS) {
                ContentValues cv = new ContentValues();
                cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE, true);
                entireDatabase.update(ApplicationCotract.ApllicationEntry.TABLE_NAME, cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + "=?",
                        new String[]{scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))});
            }
        }
        if(date2.equals("")){
            if (appRX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                    appRXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))>data*10/16){
                ContentValues cv =new ContentValues();
                cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE,true);
                entireDatabase.update(ApplicationCotract.ApllicationEntry.TABLE_NAME,cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",
                        new String[]{scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))});
            }
            if (appTX.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))-
                    appTXFirst.get(scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME)))>data/16){
                ContentValues cv =new ContentValues();
                cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE,true);
                entireDatabase.update(ApplicationCotract.ApllicationEntry.TABLE_NAME,cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",
                        new String[]{scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))});
            }
        }
    }

    public void scanApps(Context context, SQLiteDatabase entireDatabase, SQLiteDatabase scanDatabase) {
        Log.e("skan","scanning for apps");
        Cursor entireCursor=entireDatabase.query(ApplicationCotract.ApllicationEntry.TABLE_NAME,
                new String[]{ApplicationCotract.ApllicationEntry.COLUMN_ENTIRE_SCAN},
                null,
                null,
                null,
                null,
                ApplicationCotract.ApllicationEntry.COLUMN_ENTIRE_SCAN+" DESC");
        if(entireCursor!=null&&entireCursor.moveToNext()){
            for(int i=0;i<entireCursor.getCount();i++){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date=entireCursor.getString(entireCursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_ENTIRE_SCAN));
                Date databaseDate= null;
                try {
                    databaseDate = sdf.parse(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long databaseDateMilis=databaseDate.getTime();
                long nowMilis=System.currentTimeMillis();
                if(nowMilis-databaseDateMilis>ConstClass.TWO_HOURS){ //TODO Change
                    //TODO calyskan
                    doEntireScan();
                    break;
                }else{
                    break;
                }
            }
        }
        Cursor scanCursor=scanDatabase.query(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,
                new String[]{ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE},
                null,
                null,
                null,
                null,
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE+" DESC");
        if(scanCursor!=null&&scanCursor.moveToNext()){
            for(int i=0;i<scanCursor.getCount();i++){
                Log.e("skan",scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE)));
                SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.GERMANY);
                sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
                String date=scanCursor.getString(scanCursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE));
                Date databaseDate= null;
                try {
                    databaseDate = sdf.parse(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long databaseDateMilis=databaseDate.getTime();
                long nowMilis=System.currentTimeMillis();
                long diff=nowMilis-databaseDateMilis;
                if(diff>ConstClass.FIFTEEN){ //TODO Change
                    //TODO malyskan
                    doSmallScan();
                    break;
                }else{
                    break;
                }
            }
        }else{
            doSmallScan();
        }
        //TODO lekkiskan
        //doSmallScan();


        //TODO usun usuniete aplikacje
        deleteDeletedApplications();

        //TODO usun stare wspisy
        deleteOldScans();


    }

    private void deleteOldScans() {
        Log.e("skan","deleteing old scnas");
        List<PackageInfo> packageList = context.getPackageManager().getInstalledPackages(0);
        Cursor cursor = getAllItems(scanDatabase,ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME);
        cursor.moveToNext();
        for(int i=0;i<cursor.getCount();i++){
            String date=cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date1= null;
            try {
                date1 = sdf.parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long date1milis=date1.getTime();
            long now=System.currentTimeMillis();
            long diff=now-date1milis;
            if(diff>ConstClass.THREE_DAYS){//TODO Change
                scanDatabase.delete(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,
                        ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME+"=?",
                        new String[]{cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))});
            }
            if(!cursor.moveToNext()){
                break;
            }
        }
        /*
        ArrayList<Item> appsPackage = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            appsPackage.add(new Item(cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_PACKAGE)), -1));
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
            }
        }
        for (int i = 0; i < appsPackage.size(); i++) {
            if (appsPackage.get(i).getPosition() == -1) {
                entireDatabase.delete(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,
                        ApplicationCotract.ApllicationHistroyEntry.COLUMN_PACKAGE + "=?",
                        new String[]{appsPackage.get(i).getAppPackage()});
            }
        }
*/
    }

    private void doSmallScan() {
        Log.e("skan","doing small scan apps");
        SimpleDateFormat df = new  SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PackageManager packageManager = context.getPackageManager();
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStatsMobile;
        NetworkStats networkStatsWIFI;
        long startTime = new GregorianCalendar(2000, 0, 1).getTimeInMillis();
        long endTime = new GregorianCalendar().getTimeInMillis();
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        List<PackageInfo> packageList = context.getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appPackage = packageInfo.packageName;
                Map<String, UsageStats> aggregatedStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
                UsageStats usageStats = aggregatedStatsMap.get(packageInfo.packageName);
                networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, null,
                        0, System.currentTimeMillis(), packageInfo.applicationInfo.uid);
                networkStatsWIFI = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis(), packageInfo.applicationInfo.uid);
                NetworkStats.Bucket bucketMobile = new NetworkStats.Bucket();
                NetworkStats.Bucket bucketWifi = new NetworkStats.Bucket();
                if (usageStats != null) {
                    Date date2 = new Date(usageStats.getLastTimeUsed());
                    String properDate2 = (String) df.format(date2);
                    networkStatsWIFI.getNextBucket(bucketWifi);
                    long appReceived = bucketWifi.getRxBytes();
                    long appSend = bucketWifi.getTxBytes();
                    if(appPackage.contains("facebook")){
                        Log.e("Job facebookre", String.valueOf(appReceived)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                        Log.e("Job facebookse", String.valueOf(appSend)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    }
                    networkStatsMobile.getNextBucket(bucketMobile);
                    appReceived = appReceived+bucketMobile.getRxBytes();
                    appSend = appSend+bucketMobile.getTxBytes();
                    if(appPackage.contains("facebook")){
                        Log.e("Job facebookre", String.valueOf(appReceived)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                        Log.e("Job facebookse", String.valueOf(appSend)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    }
                    Date date1=new Date(System.currentTimeMillis());
                    String nowDate= (String) df.format(date1);
                    ContentValues cv = new ContentValues();
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_PACKAGE, packageInfo.packageName);
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME, (String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_LAST_USED_DATE, properDate2);
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_IN_FOREGROUND, usageStats.getTotalTimeInForeground());
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED, appReceived);
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND, appSend);
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE, nowDate);

                    scanDatabase.insert(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME, null, cv);

                } else {
                    networkStatsWIFI.getNextBucket(bucketWifi);
                    long appReceived = bucketWifi.getRxBytes();
                    long appSend = bucketWifi.getTxBytes();
                    networkStatsMobile.getNextBucket(bucketMobile);
                    appReceived = appReceived+bucketMobile.getRxBytes();
                    appSend = appSend+bucketMobile.getTxBytes();
                    Date date1= new Date(System.currentTimeMillis());;
                    String nowDate= (String) df.format(date1);
                    Log.e("sprawdzenie",nowDate);
                    ContentValues cv = new ContentValues();
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_PACKAGE, packageInfo.packageName);
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME, (String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_LAST_USED_DATE, "");
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_IN_FOREGROUND, "");
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED, appReceived);
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND, appSend);
                    cv.put(ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE, nowDate);

                     scanDatabase.insert(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME, null, cv);

                }

            }
        }


    }

    private void doEntireScan() {
        Log.e("skan","doing entire scan apps");
        DateFormat df = new DateFormat();
        PackageManager packageManager = context.getPackageManager();
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStatsMobile;
        NetworkStats networkStatsWIFI;
        long startTime = new GregorianCalendar(2000, 0, 1).getTimeInMillis();
        long endTime = new GregorianCalendar().getTimeInMillis();
        List<PackageInfo> packageList = context.getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appPackage = packageInfo.packageName;
                long longInsattationDate = 0;
                longInsattationDate = packageInfo.firstInstallTime;
                Map<String, UsageStats> aggregatedStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
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
                        Log.e("Job facebookre", String.valueOf(appReceived)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                        Log.e("Job facebookse", String.valueOf(appSend)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));

                    networkStatsMobile.getNextBucket(bucketMobile);
                    appReceived = appReceived+bucketMobile.getRxBytes();
                    appSend = appSend+bucketMobile.getTxBytes();
                    if(appPackage.contains("facebook")){
                        Log.e("Job facebookre", String.valueOf(appReceived)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                        Log.e("Job facebookse", String.valueOf(appSend)+" app name and other" + appPackage+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    }
                    Date date1=new Date(System.currentTimeMillis());
                    String nowDate= (String) df.format("yyyy-MM-dd HH:mm:ss",date1);
                    Log.e("Job usagelasttime", properDate2+"     "+(String) packageManager.getApplicationLabel(packageInfo.applicationInfo));

                    ContentValues cv = new ContentValues();
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE, packageInfo.packageName);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME, (String) packageManager.getApplicationLabel(packageInfo.applicationInfo));
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE, properDate);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_LAST_USED_DATE, properDate2);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_IN_FOREGROUND, usageStats.getTotalTimeInForeground());
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_RX_RECEIVED, appReceived);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_APP_TX_SEND, appSend);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE, true);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE, false);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_ENTIRE_SCAN, nowDate);
                    Drawable drawable = packageManager.getApplicationIcon(packageInfo.applicationInfo);
                    Bitmap bitmap = drawableToBitmap(drawable);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_ICON, insertImg(bitmap));
                    if (entireDatabase.update(ApplicationCotract.ApllicationEntry.TABLE_NAME, cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + "=?", new String[]{(String) packageManager.getApplicationLabel(packageInfo.applicationInfo)}) == 0) {
                        entireDatabase.insert(ApplicationCotract.ApllicationEntry.TABLE_NAME, null, cv);
                    }
                } else {
                    Date date = new Date(longInsattationDate);
                    String properDate = (String) df.format("yyyy-MM-dd HH:mm:ss", date);
                    networkStatsWIFI.getNextBucket(bucketWifi);
                    long appReceived = bucketWifi.getRxBytes();
                    long appSend = bucketWifi.getTxBytes();
                    networkStatsMobile.getNextBucket(bucketMobile);
                    appReceived = appReceived+bucketMobile.getRxBytes();
                    appSend = appSend+bucketMobile.getTxBytes();
                    Date date1=new Date(System.currentTimeMillis());
                    String nowDate= (String) df.format("yyyy-MM-dd HH:mm:ss",date1);
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
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_ENTIRE_SCAN, nowDate);
                    Drawable drawable = packageManager.getApplicationIcon(packageInfo.applicationInfo);
                    Bitmap bitmap = drawableToBitmap(drawable);
                    cv.put(ApplicationCotract.ApllicationEntry.COLUMN_ICON, insertImg(bitmap));
                    if (entireDatabase.update(ApplicationCotract.ApllicationEntry.TABLE_NAME, cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + "=?", new String[]{(String) packageManager.getApplicationLabel(packageInfo.applicationInfo)}) == 0) {
                        entireDatabase.insert(ApplicationCotract.ApllicationEntry.TABLE_NAME, null, cv);
                    }
                }
            }
        }
    }

    private void deleteDeletedApplications() {
        Log.e("skan","deleting deleted apps");
        List<PackageInfo> packageList = context.getPackageManager().getInstalledPackages(0);
        Cursor cursor = getAllItems(entireDatabase,ApplicationCotract.ApllicationEntry.TABLE_NAME);
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
            }
        }
        for (int i = 0; i < appsPackage.size(); i++) {
            if (appsPackage.get(i).getPosition() == -1) {
                entireDatabase.delete(ApplicationCotract.ApllicationEntry.TABLE_NAME,
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

    private Cursor getAllItems(SQLiteDatabase database,String tableName) {
        return database.query(tableName,
                null,
                null,
                null,
                null,
                null,
                ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + " DESC");
    }
}
