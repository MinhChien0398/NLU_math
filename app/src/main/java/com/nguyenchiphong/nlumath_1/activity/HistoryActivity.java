package com.nguyenchiphong.nlumath_1.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nguyenchiphong.nlumath_1.R;
import com.nguyenchiphong.nlumath_1.adapter.HistoryAdapter;
import com.nguyenchiphong.nlumath_1.dao.DatabaseHelper;
import com.nguyenchiphong.nlumath_1.model.History;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";
    DatabaseHelper databaseHelper;
    Toolbar toolbarHistory;
    RecyclerView recyclerView;
    HistoryAdapter myAdapter;
    CheckBox chkSelectAll;
    TextView txtNoData;

    // List data toàn bộ lịch sử
    ArrayList<History> listData;
    // List data được search
    ArrayList<History> histories;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        // Khởi tạo DatabaseHelper
        databaseHelper = new DatabaseHelper(HistoryActivity.this);

        // ẩn thanh ActionBar đi
//        getSupportActionBar().hide();
//        getSupportActionBar().setTitle("Lịch sử tìm kiếm");
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setIcon(R.drawable.ic_back);

        // ánh xạ
        toolbarHistory = (Toolbar) findViewById(R.id.toolBarHistory);
        toolbarHistory.setTitle("Lịch sử tìm kiếm");
        setSupportActionBar(toolbarHistory);
        // Chú ý phải để như thế này nếu không menu sẽ không hiển thị (Quan trọng)
//        toolbarHistory.inflateMenu(R.menu.menu_history);

        txtNoData = (TextView) findViewById(R.id.txtNoData);
        chkSelectAll = (CheckBox) findViewById(R.id.chkSelectAll);
        recyclerView = (RecyclerView) findViewById(R.id.recycleViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(HistoryActivity.this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        myAdapter = new HistoryAdapter(HistoryActivity.this, getMyListData());
        recyclerView.setAdapter(myAdapter);

        // populateListView();
        addEvents();
    }

    // Lấy tất cả dữ liệu từ Database lên để hiển thị
    private ArrayList<History> getMyListData() {
        Cursor data = databaseHelper.getData();
        listData = new ArrayList<History>();
        // Tạm thời set dữ liệu mặc định cho hình ảnh
        while (data.moveToNext()) {
//            model.setImg(R.drawable.hand_icon);
//            model.setTitle(data.getString(1));
//            model.setDescription("This is news feed description");
            listData.add(new History(data.getString(1), data.getString(2), R.drawable.hand_icon));
        }
        return listData;
    }

    // Xử lý sự kiện ở đây
    private void addEvents() {
        // Sự kiện cho nút Back trên thanh Toolbar
        toolbarHistory.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        // Sự kiện chọn tất cả lịch sử
        chkSelectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chkSelectAll.isChecked() == true) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this);
                    // Cài đặt các thuộc tính
                    builder.setTitle("Thông báo!");
                    builder.setMessage("Bạn có chắc chắn muốn xóa " + listData.size() + " lịch sử?");
                    builder.setIcon(R.drawable.ic_notification);

                    // new HistoryAdapter().selectAll();
                    // Cài đặt button Cancel- Hiển thị Toast
                    builder.setPositiveButton("Hủy", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Toast.makeText(HistoryActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                            dialog.cancel();
                            // sau đó set CheckBox == false
                            chkSelectAll.setChecked(false);
                        }
                    });
                    // Cài đặt button Yes Dismiss ẩn Dialog
                    builder.setNegativeButton("Xóa", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Toast.makeText(HistoryActivity.this, "Đang làm...", Toast.LENGTH_SHORT).show();
                            // sau đó set CheckBox == false
                            databaseHelper.deleteAll();
                            chkSelectAll.setChecked(false);
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                } else {
                    // Ngược lại bỏ chọn tất cả
                    Toast.makeText(HistoryActivity.this, "Bỏ chọn tất cả", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    // Gắn Menu trên thanh Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        MenuItem menuItem = menu.findItem(R.id.mnuSearch);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                histories = databaseHelper.searchByName(query);
                if (histories.size() > 0) {
                    HistoryAdapter adapter = new HistoryAdapter(HistoryActivity.this, histories);
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(HistoryActivity.this);
                    recyclerView.setLayoutManager(linearLayoutManager);
                    recyclerView.setAdapter(adapter);
                    txtNoData.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    return true;
                } else {
                    recyclerView.setVisibility(View.GONE);
                    txtNoData.setVisibility(View.VISIBLE);
                    return false;
                }
            }

            @Override
            public boolean onQueryTextChange(String query) {
                histories = databaseHelper.searchByName(query);
                if (histories.size() > 0) {
                    HistoryAdapter adapter = new HistoryAdapter(HistoryActivity.this, histories);
                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(HistoryActivity.this);
                    recyclerView.setLayoutManager(linearLayoutManager);
                    recyclerView.setAdapter(adapter);
                    txtNoData.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    return true;
                } else {
                    recyclerView.setVisibility(View.GONE);
                    txtNoData.setVisibility(View.VISIBLE);
                    return false;
                }
            }
        });
        return true;
    }
}