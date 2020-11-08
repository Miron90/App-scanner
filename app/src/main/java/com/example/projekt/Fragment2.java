package com.example.projekt;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projekt.database.ApplicationCotract;
import com.example.projekt.database.ApplicationDBHelper;

public class Fragment2 extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private LinearLayoutManager layoutManager;
    private Adapter adapter;
    Context context = null;
    private SQLiteDatabase database;
    private ConstraintLayout rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment2_layout,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        adapter = new Adapter(getContext(),getAllItems(),getActivity(),false,this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        emptyView = (TextView) view.findViewById(R.id.emptyView);
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
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        context = getActivity();

        ApplicationDBHelper dbHelper = new ApplicationDBHelper(context);
        database = dbHelper.getWritableDatabase();

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

    public void checkIfNoData(){
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
    }
}
