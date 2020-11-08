package com.example.projekt;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationHistoryDbHelper;

public class ApplicationDetails extends AppCompatActivity {

    private byte[] icon;
    private String appName,appPackage,appInstall,appLastUse;
    private long appInForegroung,appRxReceived,appTxSend;

    private SQLiteDatabase database;
    private ApplicationHistoryDbHelper dbHelper;

    private TextView appNameTextView,appPackageTextView,appInstallDateTextView,appLastUseTextView,appRXTextView,appTXTextView,appInForegroungTextView;
    private ImageView iconImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_details);
        dbHelper=new ApplicationHistoryDbHelper(getApplicationContext());
        database=dbHelper.getReadableDatabase();
        Intent intent=getIntent();
        icon=intent.getByteArrayExtra(ApplicationCotract.ApllicationEntry.COLUMN_ICON);
        appName=intent.getStringExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME);
        appPackage=intent.getStringExtra(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE);
        appInForegroung=intent.getLongExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_IN_FOREGROUND,0);
        appInstall=intent.getStringExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE);
        appLastUse=intent.getStringExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_LAST_USED_DATE);
        if(appLastUse.equals("")){
            appLastUse="No information";
        }
        appRxReceived=intent.getLongExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_RX_RECEIVED,0);
        appTxSend=intent.getLongExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_TX_SEND,0);
        boolean xd=false;
        Cursor cursor=database.query(ApplicationCotract.ApllicationHistroyEntry.TABLE_NAME,
                null,
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_NAME+"=?",
                new String[]{appName},
                null,
                null ,
                ApplicationCotract.ApllicationHistroyEntry.COLUMN_HISTORY_DATE);
        if(cursor.moveToNext()){
            xd=true;
        }
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        android.app.ActionBar actionBar2 = getActionBar();
        if(actionBar2!=null) {
            actionBar2.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(appName);
        iconImageView=findViewById(R.id.appIcon);
        appNameTextView=findViewById(R.id.appName);
        appPackageTextView=findViewById(R.id.appPackage);
        appInstallDateTextView=findViewById(R.id.appInstallDate);
        appLastUseTextView=findViewById(R.id.appLastUsedDate);
        appRXTextView=findViewById(R.id.appRXData);
        appTXTextView=findViewById(R.id.appTXData);
        appInForegroungTextView=findViewById(R.id.appInForeground);
        iconImageView.setImageDrawable(byteToDrawable(icon));
        appNameTextView.setText(appName);
        appPackageTextView.setText(appPackage);
        appInstallDateTextView.setText(appInstall);
        appLastUseTextView.setText(appLastUse);
        float valuechanger;
        if(appInForegroung>1000*60){
            valuechanger=appInForegroung/1000/60;
            appInForegroungTextView.setText(valuechanger+" min");
        }else{
            valuechanger=appInForegroung/1000;
            appInForegroungTextView.setText(valuechanger+" sec");
        }
        long oldrx=0;
        if(xd) {
            oldrx = cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_RX_RECEIVED));
        }
        if(appRxReceived>8*1024*1024){
            valuechanger=appRxReceived/8/1024/1024;
            appRXTextView.setText(valuechanger+" MB vs before "+oldrx/8/1024/1024);
        }else if(appRxReceived>8*1024){
            valuechanger=appRxReceived/8/1024;
            appRXTextView.setText(valuechanger+" KB vs before "+oldrx/8/1024);
        }
        else{
            valuechanger=appRxReceived/8;
            appRXTextView.setText(valuechanger+" B vs before "+oldrx/8);
        }
        long oldtx=0;
        if(xd) {
            oldtx = cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationHistroyEntry.COLUMN_APP_TX_SEND));
        }
        if(appTxSend>8*1024*1024){
            valuechanger=appTxSend/8/1024/1024;
            appTXTextView.setText(valuechanger+" MB vs before "+oldtx/8/1024/1024);
        }else if(appTxSend>8*1024){
            valuechanger=appTxSend/8/1024;
            appTXTextView.setText(valuechanger+" KB vs before "+oldtx/8/1024);
        }
        else{
            valuechanger=appTxSend/8;
            appTXTextView.setText(valuechanger+" B vs before "+oldtx/8);
        }


    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        return true;
    }

    public Drawable byteToDrawable(byte[] img){
        Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
        Drawable d = new BitmapDrawable(getApplicationContext().getResources(), bitmap);
        return d;
    }
}