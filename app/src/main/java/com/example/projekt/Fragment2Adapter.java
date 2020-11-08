package com.example.projekt;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekt.database.ApplicationCotract;

public class Fragment2Adapter extends RecyclerView.Adapter<Fragment2Adapter.ViewHolder> {

    private  Context context;
    private Cursor cursor;
    private Activity activity;
    int position;

    public Fragment2Adapter(Context context, Cursor cursor, Activity activity){
        this.context=context;
        this.cursor=cursor;
        this.activity=activity;
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
        if(Fragment2Adapter.this.position>position){
            for(int i=Fragment2Adapter.this.position;i>position;i--){
                if(!cursor.moveToPrevious()){//napraw to zeby dzialalo dobrze narazie to next nie moze byc musi byc position
                    return;
                }
            }
        }else if (Fragment2Adapter.this.position==position){
            if(!cursor.moveToNext()){//napraw to zeby dzialalo dobrze narazie to next nie moze byc musi byc position
                return;
            }
        }
        else{
            for(int i=Fragment2Adapter.this.position;i<position;i++){
                if(!cursor.moveToNext()){//napraw to zeby dzialalo dobrze narazie to next nie moze byc musi byc position
                    return;
                }
            }
        }
        Fragment2Adapter.this.position=position;

        byte[] icon = cursor.getBlob(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_ICON));

        String appName=cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_NAME));
        String appInstall=cursor.getString(cursor.getColumnIndex(ApplicationCotract.ApllicationEntry.COLUMN_APP_INSTALL_DATE));

        holder.icon.setImageDrawable(byteToDrawable(icon));
        holder.appName.setText(appName);
        holder.installationDate.setText(appInstall);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private ImageView icon;
        private TextView appName;
        private TextView installationDate;
        private ImageButton button;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon=itemView.findViewById(R.id.icon);
            appName=itemView.findViewById(R.id.appName);
            installationDate=itemView.findViewById(R.id.appInstall);
            button=itemView.findViewById(R.id.moreButton);
            button.setVisibility(View.GONE);
        }
    }

    public Drawable byteToDrawable(byte[] img){
        Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
        Drawable d = new BitmapDrawable(context.getResources(), bitmap);
        return d;
    }

}
