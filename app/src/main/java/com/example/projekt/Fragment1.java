package com.example.projekt;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationDBHelper;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;

public class Fragment1 extends Fragment {

    Context context = null;
    private RecyclerView recyclerView;
    private ConstraintLayout rootView;
    private TextView emptyView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private SQLiteDatabase database;
    ArrayList<Item> items = null;
    Handler handler = new Handler();

    public Fragment1() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment1_layout, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        adapter = new Adapter(context, getAllItems(), getActivity(),true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        emptyView = (TextView) view.findViewById(R.id.emptyView);
        boolean granted = false;
        AppOpsManager appOps = (AppOpsManager) context
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        if(!granted) {
            Intent intent = new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
        Cursor cursor=getAllItems();
        cursor.moveToNext();
        if (cursor.getCount()<1) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    scanAppsAndSaveToDatabase();
                }
            }).start();
        }
        else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        context = getActivity();


        //startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        ApplicationDBHelper dbHelper = new ApplicationDBHelper(context);
        database = dbHelper.getWritableDatabase();

        //database.execSQL("DELETE FROM "+ ApplicationCotract.ApllicationEntry.TABLE_NAME);
        //scanAppsAndSaveToDatabase();////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public void scanAppsAndSaveToDatabase() {
        SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.GERMANY);
        df.applyPattern("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        PackageManager packageManager = context.getPackageManager();
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStats networkStatsMobile;
        NetworkStats networkStatsWIFI;
        long startTime = new GregorianCalendar(2000, 0, 1).getTimeInMillis();
        long endTime = new GregorianCalendar().getTimeInMillis();
        final List<PackageInfo> packageList = context.getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packageList.size(); i++) {
            PackageInfo packageInfo = packageList.get(i);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                final int finalI = i;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        emptyView.setText("Scanning app "+ finalI +" / "+packageList.size());
                    }
                });
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
                    String properDate = (String) df.format(date);
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
                    if (database.update(ApplicationCotract.ApllicationEntry.TABLE_NAME, cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + "=?", new String[]{(String) packageManager.getApplicationLabel(packageInfo.applicationInfo)}) == 0) {
                        database.insert(ApplicationCotract.ApllicationEntry.TABLE_NAME, null, cv);
                    }
                } else {
                    Date date = new Date(longInsattationDate);
                    String properDate = (String) df.format(date);
                    networkStatsWIFI.getNextBucket(bucketWifi);
                    long appReceived = bucketWifi.getRxBytes();
                    long appSend = bucketWifi.getTxBytes();
                    networkStatsMobile.getNextBucket(bucketMobile);
                    appReceived = appReceived+bucketMobile.getRxBytes();
                    appSend = appSend+bucketMobile.getTxBytes();
                    Date date1=new Date(System.currentTimeMillis());
                    String nowDate= (String) df.format(date1);
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
                    if (database.update(ApplicationCotract.ApllicationEntry.TABLE_NAME, cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME + "=?", new String[]{(String) packageManager.getApplicationLabel(packageInfo.applicationInfo)}) == 0) {
                        database.insert(ApplicationCotract.ApllicationEntry.TABLE_NAME, null, cv);
                    }
                }
            }
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                Cursor cursor=getAllItems();
                cursor.moveToNext();
                if (cursor.getCount()<1) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);

                }
                else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private String getSubscriberId(Context context, int networkType) {
        if (ConnectivityManager.TYPE_MOBILE == networkType) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if ( ActivityCompat.checkSelfPermission(context, READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{ READ_PHONE_NUMBERS, READ_PHONE_STATE}, 1);
            }
            try{
                return tm.getSubscriberId();
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(context,"Error occured while collecting internet data",Toast.LENGTH_LONG);
            }
        }
        return "";
    }

    public byte[] insertImg(Bitmap img) {


        byte[] data = getBitmapAsByteArray(img); // this is a function

        return data;


    }

    public static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
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
