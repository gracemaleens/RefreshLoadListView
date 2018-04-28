package com.example.com.refreshloadlistview;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private int count;
    private String[] sourceStrs = {"此", "时", "相", "望", "不", "相", "闻", "愿", "逐", "月", "华", "流", "照", "君"};
    private List<String> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RefreshLoadListView listView = (RefreshLoadListView) findViewById(R.id.main_lv);
        initData();
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data.toArray(new String[]{})));
        listView.setOnRefreshDataListener(new RefreshLoadListView.OnRefreshDataListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshData(listView);
                    }
                }, 2000);
            }

            @Override
            public void onLoad() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshData(listView);
                    }
                }, 2000);
            }
        });

//        final LoadListView loadListView = (LoadListView)findViewById(R.id.main_lv);
//        loadListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, data.toArray(new String[]{})));
//        loadListView.setOnLoadDataListener(new LoadListView.OnLoadDataListener() {
//            @Override
//            public void onLoad() {
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        refreshData(loadListView);
//                    }
//                }, 2000);
//            }
//        });
    }

    private void refreshData(RefreshLoadListView listView) {
        addItem();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, data.toArray(new String[]{}));
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        listView.refreshComplete();
    }

    private void refreshData(LoadListView listView){
        addItem();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, data.toArray(new String[]{}));
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        listView.loadComplete();
    }

    private void initData() {
        for(int i = 0; i < 13; i++){
            if(count >= sourceStrs.length){
                count = 0;
            }
            data.add(sourceStrs[count++]);
        }
    }

    private void addItem(){
        if(count >= sourceStrs.length){
            count = 0;
        }
        data.add(sourceStrs[count]);
    }
}
