package com.example.projekt;

import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationDBHelper;
import com.example.projekt.database.ApplicationHistoryDbHelper;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.projekt.ui.main.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity {

    private Button notification,scheduler;
    AlarmManager alarmManager,schedulerManager;
    protected SchedulerScan schedulerScan;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        createSchedulerChannel();
        schedulerScan = new SchedulerScan();
        registerReceiver(schedulerScan,new IntentFilter("ACTION"));
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        schedulerManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        notification = findViewById(R.id.notificationButton);
        scheduler=findViewById(R.id.scheduler);
        SQLiteOpenHelper dbHelper=new ApplicationHistoryDbHelper(getApplicationContext());
        SQLiteDatabase database=dbHelper.getReadableDatabase();
        Cursor cursor=database.query(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,null,null,null,null,null,null);
        cursor.moveToNext();
        int xx=cursor.getCount();
        boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 0,
                new Intent(MainActivity.this, Notification.class),
                PendingIntent.FLAG_NO_CREATE) != null);
        if (alarmUp) {
            notification.setText("Cancel the notification: "+xx);
        } else {
            notification.setText("Set up the notification: "+xx);
        }
        JobScheduler jobScheduler=(JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        JobInfo info=jobScheduler.getPendingJob(ConstClass.JOB_ID);
        if(info==null){
            scheduler.setText("Scan the Device");
            Log.e("JobNull","job is null");
        }
        else{
            scheduler.setText("Don't scan the Device");
            Log.e("JobNot Null","job is not null");
        }
        notification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 0,
                        new Intent(MainActivity.this, Notification.class),
                        PendingIntent.FLAG_NO_CREATE) != null);
                SQLiteOpenHelper dbHelper=new ApplicationHistoryDbHelper(getApplicationContext());
                SQLiteDatabase database=dbHelper.getReadableDatabase();
                Cursor cursor=database.query(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,null,null,null,null,null,null);
                cursor.moveToNext();
                int xx=cursor.getCount();
                Intent intent = new Intent(MainActivity.this, Notification.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,
                        0, intent, 0);
                seeSql();
                if (alarmUp) {
                    notification.setText("Setup the notification: "+xx);
                    pendingIntent.cancel();
                    alarmManager.cancel(pendingIntent);
                } else {
                    notification.setText("Cancel the notification: "+xx);

                    long time = System.currentTimeMillis();
                    long ten = ConstClass.TWO_HOURS; //TODO Change
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time, ten, pendingIntent);
                }
            }
        });
        scheduler.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean granted = false;
                AppOpsManager appOps = (AppOpsManager) getApplicationContext()
                        .getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), getApplicationContext().getPackageName());

                if (mode == AppOpsManager.MODE_DEFAULT) {
                    granted = (getApplicationContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
                } else {
                    granted = (mode == AppOpsManager.MODE_ALLOWED);
                }
                if(!granted) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivity(intent);
                }

                JobScheduler jobScheduler=(JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
                JobInfo info=jobScheduler.getPendingJob(ConstClass.JOB_ID);
                if(info==null){
                    scheduler.setText("Don't scan the Device");
                    Log.e("JobNull","job is null");
                    scheduleJob();
                }
                else{
                    scheduler.setText("Scan the Device");
                    Log.e("JobNot Null","job is not null");
                    jobScheduler.cancel(ConstClass.JOB_ID);
                }



            }
        });
    }

    private void seeSql() {
        SQLiteDatabase entireDatabase,scanDatabase;
        ApplicationDBHelper dbHelper = new ApplicationDBHelper(getApplicationContext());
        entireDatabase = dbHelper.getWritableDatabase();
        ApplicationHistoryDbHelper dbHelper2 = new ApplicationHistoryDbHelper(getApplicationContext());
        scanDatabase = dbHelper2.getWritableDatabase();
        Cursor cursor=getAllItems(entireDatabase, ApplicationCotract.ApllicationEntry.TABLE_NAME);
        while(cursor.moveToNext()){
            Log.e("Sprawdzenie", String.valueOf(cursor.getCount()));
            Log.e("Sprawdzenie", cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_IN_FOREGROUND))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_ENTIRE_SCAN))+"   "+
                    cursor.getInt(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME))+"   "+
                    cursor.getInt(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE))+"   "+
                    cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_TX_SEND))+"   "+
                    cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_RX_RECEIVED))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_LAST_USED_DATE))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE)));
        }
        cursor=getAllItems(scanDatabase, ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME);
        while(cursor.moveToNext()){
            Log.e("Sprawdzenie", String.valueOf(cursor.getCount()));
            Log.e("Sprawdzenie", cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_IN_FOREGROUND))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME))+"   "+
                    cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND))+"   "+
                    cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_LAST_USED_DATE))+"   "+
                    cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_PACKAGE)));
        }




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

    public void scheduleJob(){
        ComponentName componentName=new ComponentName(this,SchedulerJobService.class);
        JobInfo info=new JobInfo.Builder(ConstClass.JOB_ID,componentName)
                .setPeriodic(ConstClass.THIRTY_MINUTES) //TODO Change
                .build();

        JobScheduler scheduler=(JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int result=scheduler.schedule(info);
        if(result==JobScheduler.RESULT_SUCCESS){
            Log.e("Job ", String.valueOf(result)+"JOB SCHEDULED");
        }else{
            Log.e("Job ", String.valueOf(result)+"JOB SCHEDUL failed");
        }
        Toast.makeText(this,"job scheduled",Toast.LENGTH_LONG).show();
    }




    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "projektReminderChannel";
            String description = "Channel for projekt";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("notify", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    public void createSchedulerChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "projektSchedulerChannel";
            String description = "Channel for scheduler projekt";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("scheduler", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}