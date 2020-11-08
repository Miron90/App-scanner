package com.example.projekt;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationDBHelper;

import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private  Context context;
    private Cursor cursor;
    private Activity activity;
    int position;
    ApplicationDBHelper dbHelper;
    SQLiteDatabase database;
    private boolean savedBundle;
    private Fragment2 fragment2;



    public Adapter(Context context, Cursor cursor, Activity activity,boolean savedBundle){
        this.context=context;
        this.cursor=cursor;
        this.activity=activity;
        dbHelper = new ApplicationDBHelper(context);
        database = dbHelper.getWritableDatabase();
        this.notifyDataSetChanged();
        this.savedBundle=savedBundle;
    }
    public Adapter(Context context, Cursor cursor, Activity activity,boolean savedBundle,Fragment2 fragment2){
        this.context=context;
        this.cursor=cursor;
        this.activity=activity;
        dbHelper = new ApplicationDBHelper(context);
        database = dbHelper.getWritableDatabase();
        this.notifyDataSetChanged();
        this.savedBundle=savedBundle;
        this.fragment2=fragment2;
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        String appNameString,appInstall,appLastUse,
                appPackage;
        long  appRxReceived,
                appTxSend,appInForegroung;
        int suggestDelete,needDelete;
        byte[] appIcon;

        public ImageView icon;
        public TextView appName;
        public TextView installationDate;
        ImageButton imageButton;
        View view;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon=itemView.findViewById(R.id.icon);
            appName=itemView.findViewById(R.id.appName);
            installationDate=itemView.findViewById(R.id.appInstall);
            imageButton=itemView.findViewById(R.id.moreButton);
            imageButton.setOnClickListener(this);
            view=itemView;
        }

        @Override
        public void onClick(View view) {
            showPopupMenu(view);
        }

        private void showPopupMenu(View view) {
            PopupMenu popupMenu=new PopupMenu(view.getContext(),view);
            popupMenu.inflate(R.menu.popup_menu);
            popupMenu.setOnMenuItemClickListener(this);
            Cursor cursor=database.query(ApplicationCotract.ApllicationEntry.TABLE_NAME,
                    new String[]{ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE},
                    ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",
                    new String[]{appNameString},
                    null,
                    null,
                    null);
            cursor.moveToNext();
            if(cursor!=null){
                int suggest=cursor.getInt(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE));
                if(suggest==0){
                    popupMenu.getMenu().getItem(2).setTitle("Suggest");
                }else{
                    popupMenu.getMenu().getItem(2).setTitle("Don't suggest");
                }
                if(!savedBundle)  popupMenu.getMenu().getItem(0).setVisible(false);
            }
            popupMenu.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()){
                case R.id.details:
                    Intent intent=new Intent(context,ApplicationDetails.class);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME,appNameString);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE,appPackage);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_IN_FOREGROUND,appInForegroung);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE,appInstall);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_LAST_USED_DATE,appLastUse);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_RX_RECEIVED,appRxReceived);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_APP_TX_SEND,appTxSend);
                    intent.putExtra(ApplicationCotract.ApllicationEntry.COLUMN_ICON,appIcon);
                    activity.startActivity(intent);
                    return true;
                case R.id.delete:
                    Uri packageUri = Uri.parse("package:"+appPackage);
                    Intent uninstallIntent =
                            new Intent(Intent.ACTION_DELETE, packageUri);
                    //activity.startActivity(uninstallIntent);
                    //activity.startActivityForResult();
                    return true;
                case R.id.dontSuggest:
                    Cursor cursor=database.query(ApplicationCotract.ApllicationEntry.TABLE_NAME,
                            new String[]{ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE},
                            ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",
                            new String[]{appNameString},
                            null,
                            null,
                            null);
                    cursor.moveToNext();
                    ContentValues cv = new ContentValues();
                    if(cursor!=null){
                        int suggest=cursor.getInt(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE));
                        if(suggest==0){
                            cv.put(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE,true);
                            cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE,false);
                        }else{
                            cv.put(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE,false);
                            cv.put(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE,false);
                        }
                    }


                    Log.e("TAGG", String.valueOf(database.update(ApplicationCotract.ApllicationEntry.TABLE_NAME,cv, ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME+"=?",new String[]{appNameString})));
                    if(fragment2!=null) {
                        swapCursor(getAllItems());
                        fragment2.checkIfNoData();
                        notifyDataSetChanged();
                    }
                    return true;
                default:
                    return false;
            }

        }
        public void changeViewHolder(Cursor cursor,byte[] icon,String appName,String appInstall){
            if(savedBundle) {
                appPackage = cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_PACKAGE));
                appLastUse = cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_LAST_USED_DATE));
                appRxReceived = cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_RX_RECEIVED));
                appTxSend = cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_TX_SEND));
                appInForegroung = cursor.getLong(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_IN_FOREGROUND));

                suggestDelete = cursor.getInt(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_SUGGEST_DELETE));
                needDelete = cursor.getInt(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_NEED_DELETE));
            }
            this.appIcon = icon;
            this.appNameString=appName;
            this.appInstall=appInstall;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from((parent.getContext())).inflate(R.layout.item,parent,false);
        ViewHolder evh=new ViewHolder(v);
        return evh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(Adapter.this.position>position){
            for(int i=Adapter.this.position;i>position;i--){
                if(!cursor.moveToPrevious()){//napraw to zeby dzialalo dobrze narazie to next nie moze byc musi byc position
                    return;
                }
            }
        }else if (Adapter.this.position==position){
            if(!cursor.moveToNext()){//napraw to zeby dzialalo dobrze narazie to next nie moze byc musi byc position
                return;
            }
        }
        else{
            for(int i=Adapter.this.position;i<position;i++){
                if(!cursor.moveToNext()){//napraw to zeby dzialalo dobrze narazie to next nie moze byc musi byc position
                    return;
                }
            }
        }

        Adapter.this.position=position;

        byte[] icon = cursor.getBlob(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_ICON));

        String appName=cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME));
        String appInstall=cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE));

        //Item currentItem=items.get(position);
        holder.changeViewHolder(cursor,icon,appName,appInstall);
        holder.icon.setImageDrawable(byteToDrawable(icon));
        holder.appName.setText(appName);
        holder.installationDate.setText(appInstall);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }
    public void swapCursor(Cursor newCursor){
        if (cursor !=null){
            cursor.close();
        }
        cursor=newCursor;

        if(newCursor!=null){
            notifyDataSetChanged();
        }
    }
    public Drawable byteToDrawable(byte[] img){
        Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
        Drawable d = new BitmapDrawable(context.getResources(), bitmap);
        return d;
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
