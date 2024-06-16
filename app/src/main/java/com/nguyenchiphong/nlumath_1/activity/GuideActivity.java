package com.nguyenchiphong.nlumath_1.activity;

import android.animation.ArgbEvaluator;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.nguyenchiphong.nlumath_1.R;
import com.nguyenchiphong.nlumath_1.adapter.GuideApdater;
import com.nguyenchiphong.nlumath_1.model.Model_ViewPager;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.List;

public class GuideActivity extends AppCompatActivity {
    ViewPager viewPager;
    GuideApdater adapter;
    WormDotsIndicator dotsIndicator; // nút chuyển qua lại giữa các View Pager
    List<Model_ViewPager> models; // list của model
    Integer[] colors = null;
    ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

//        getSupportActionBar().hide(); // ẩn thanh ActionBar đi
        toolbar = (Toolbar) findViewById(R.id.toolBarGuide);
        toolbar.setTitle("Hướng dẫn sử dụng");
        dotsIndicator = (WormDotsIndicator) findViewById(R.id.dots_indicator);

        // sự kiện thanh toolbar
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // set data cứng, bởi vì data không thay đổi
        models = new ArrayList<>();
        models.add(new Model_ViewPager(R.drawable.camera_feature, "Chức năng Máy ảnh", "Bật camera điện thoại của bạn và chụp hình ảnh bài toán mà bạn cần giải. Sau đó cắt hình ảnh sao cho vừa đủ với bài toán và nhấn nút Giải"));
        models.add(new Model_ViewPager(R.drawable.load_gallery_feature, "Chức năng Hình ảnh", "Lựa chọn hình ảnh bài toán có sẵn trong thư viện ảnh của bạn. Sau đó cắt hình ảnh sao cho vừa đủ với bài toán và nhấn nút Giải."));
        models.add(new Model_ViewPager(R.drawable.voice_recognize_feature, "Chức năng Giọng nói", "Sau khi bật chức năng giọng nói, hãy đưa điện thoại lại gần bạn, đọc bài toán to, rõ ràng và chậm rãi."));
        models.add(new Model_ViewPager(R.drawable.history_feature, "Chức năng Lịch sử", "Truy cập các bài đã giải gần đây của bạn từ biểu tượng Lịch sử trên màn hình menu của ứng dụng."));

        adapter = new GuideApdater(models, this);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(adapter);
        dotsIndicator.setViewPager(viewPager);
        viewPager.setPadding(130, 0, 130, 0);

        // mảng các màu sẽ thay đổi khi chuyển qua lại giữa các View Pager
        Integer[] colors_temp = {getResources().getColor(R.color.color1),
                getResources().getColor(R.color.color2),
                getResources().getColor(R.color.color3),
                getResources().getColor(R.color.color4)};

        colors = colors_temp;

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position < (adapter.getCount() - 1) && position < (colors.length - 1)) {
                    viewPager.setBackgroundColor(
                            (Integer) argbEvaluator.evaluate(
                                    positionOffset,
                                    colors[position],
                                    colors[position + 1]));
                } else {
                    viewPager.setBackgroundColor(colors[colors.length - 1]);
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
