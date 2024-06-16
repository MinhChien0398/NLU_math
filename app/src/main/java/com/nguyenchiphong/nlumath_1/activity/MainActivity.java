package com.nguyenchiphong.nlumath_1.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nguyenchiphong.nlumath_1.R;
import com.shashank.sony.fancytoastlib.FancyToast;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import maes.tech.intentanim.CustomIntent;

public class MainActivity extends AppCompatActivity {
    CardView cvMayAnh, cvGiongNoi, cvHinhAnh, cvLichSu, cvHuongDan, cvVeNLUmath;
    static final int REQUEST_IMAGE_CAPTURE = 1, REQUEST_IMAGE_GALLERY = 2;
    Uri imageURI;
    boolean openCamera, openGallery;

    public static final String TAG = "AAA";
    public static final int RECOGNIZER_RESULT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getSupportActionBar().hide();
        anhXa();
        suKien();
        Intent intent = getIntent();
        if (intent != null) {
            openCamera = intent.getBooleanExtra("openCamera", false);
            if (openCamera) {
                moCameraCuaMay();
                openCamera = false;
            }
            openGallery = intent.getBooleanExtra("openGallery", false);
            if (openGallery) {
                moGallery();
                openGallery = false;
            }
        }
    }

    private void suKien() {
        cvMayAnh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                CropImage.activity().start(MainActivity.this);
//                Intent intent = CropImage.getPickImageChooserIntent(MainActivity.this);
//                startActivityForResult(intent, 123);
                moCameraCuaMay();
            }
        });
        cvHinhAnh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                CropImage.startPickImageActivity(MainActivity.this);
                moGallery();
            }
        });
        cvGiongNoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói biểu thức của bạn:");
                startActivityForResult(speechIntent, RECOGNIZER_RESULT);
            }
        });
        cvVeNLUmath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setContentView(R.layout.layout_about_dialog);
                TextView txtClose = dialog.findViewById(R.id.txtClose);
                txtClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });
        cvLichSu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FancyToast.makeText(MainActivity.this, "Lịch sử", FancyToast.LENGTH_LONG, FancyToast.SUCCESS, true).show();
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });
        cvHuongDan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FancyToast.makeText(MainActivity.this, "Hướng dẫn", FancyToast.LENGTH_LONG, FancyToast.SUCCESS, true).show();
                Intent intent = new Intent(MainActivity.this, GuideActivity.class);
                startActivity(intent);
            }
        });
    }

    public void moCameraCuaMay() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File imageFile = null;
            try {
                imageFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Không thể tạo file!", Toast.LENGTH_LONG);
            }
            if (imageFile != null) {
                imageURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        imageFile);
                //Truyền imageURI cho Camera để nó lưu ảnh ở chỗ đó
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                if (openCamera) {
                    CustomIntent.customType(this, "right-to-left");
                } else CustomIntent.customType(this, "left-to-right");
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "NLUmath_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory to save*/
        );
        return imageFile;
    }

    private boolean xoaAnhChupCuoiCungTrongDCIM() {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            if (xoaAnhChupCuoiCungTrongDCIM()) {
//                Toast.makeText(this, "Xóa ảnh trong DCIM thành công!", Toast.LENGTH_SHORT).show();
//            }
            Intent intent = new Intent(MainActivity.this, MyCropActivity.class);
            intent.putExtra("imageURI", imageURI.toString());
            intent.putExtra("isFromCamera", true);
            startActivity(intent);
            CustomIntent.customType(this, "left-to-right");
        }

        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == RESULT_OK) {
            imageURI = data.getData();
            Intent intent = new Intent(MainActivity.this, MyCropActivity.class);
            intent.putExtra("imageURI", imageURI.toString());
            intent.putExtra("isFromGallery", true);
            startActivity(intent);
            CustomIntent.customType(this, "left-to-right");
        }

        // cho phần giọng nói:
        if (requestCode == RECOGNIZER_RESULT && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String bieuThucGiongNoi = matches.get(0);
            Intent intent = new Intent(MainActivity.this, SolveActivityShort.class);
            intent.putExtra("bieuThucGiongNoi", bieuThucGiongNoi);
            startActivity(intent);
            CustomIntent.customType(MainActivity.this, "left-to-right");
        }
    }

    private void moGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_IMAGE_GALLERY);
        if (openGallery) {
            CustomIntent.customType(this, "right-to-left");
        }
    }

    private void anhXa() {
        cvMayAnh = findViewById(R.id.cvMayAnh);
        cvGiongNoi = findViewById(R.id.cvGiongNoi);
        cvHinhAnh = findViewById(R.id.cvHinhAnh);
        cvLichSu = findViewById(R.id.cvLichSu);
        cvHuongDan = findViewById(R.id.cvHuongDan);
        cvVeNLUmath = findViewById(R.id.cvVeNLUmath);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    static {
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV 3.4.3 loaded");
        } else {
            Log.d(TAG, "OpenCV 3.4.3 not loaded");
        }
    }
}
