package com.nguyenchiphong.nlumath_1.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nguyenchiphong.nlumath_1.R;

import maes.tech.intentanim.CustomIntent;

public class StartActivity extends AppCompatActivity {
    ImageView imageView;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        //getSupportActionBar().hide();

        imageView = (ImageView) findViewById(R.id.imgView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                startActivity(intent);
                CustomIntent.customType(StartActivity.this, "fadein-to-fadeout");
            }
        }, 1000);

    }
}
