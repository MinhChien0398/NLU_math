package com.nguyenchiphong.nlumath_1.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.nguyenchiphong.nlumath_1.R;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import maes.tech.intentanim.CustomIntent;

public class MyCropActivity extends AppCompatActivity {
    CropImageView cropImageView;
    Button btnQuayLai, btnGiai;
    boolean isFromCamera, isFromGallery;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_crop);
        //getSupportActionBar().hide();

        anhXa();
        suKien();
        Intent intent = getIntent();
        Uri imageURI = Uri.parse(intent.getStringExtra("imageURI"));
        isFromCamera = intent.getBooleanExtra("isFromCamera", false);
        isFromGallery = intent.getBooleanExtra("isFromGallery", false);
//        cropImageView.setShowProgressBar(false);
        cropImageView.setAutoZoomEnabled(false);
        cropImageView.setImageUriAsync(imageURI);
        // Nhớ đặt dòng này sau cùng!!
        cropImageView.setCropRect(new Rect(300, 1500, 2800, 2500));
    }

    private void suKien() {
        btnQuayLai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        
        btnGiai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cropImageView.setOnCropImageCompleteListener(new CropImageView.OnCropImageCompleteListener() {
                    @Override
                    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
//                        Intent intent = new Intent(MyCropActivity.this, SolveActivityLong.class);
                        Intent intent = new Intent(MyCropActivity.this, SolveActivityShort.class);
                        Uri imageCroppedURI = result.getUri();  //Đường dẫn của cropped image đã lưu
                        intent.putExtra("imageCroppedURI", imageCroppedURI.toString());
                        intent.putExtra("isQuant", false);
                        startActivity(intent);
                        CustomIntent.customType(MyCropActivity.this, "left-to-right");
                    }
                });

                File imageCroppedFile = null;
                try {
                    imageCroppedFile = createImageCroppedFile();
                } catch (IOException ex) {
                    Toast.makeText(MyCropActivity.this, "Không thể tạo Cropped Image File!", Toast.LENGTH_LONG);
                }
                if (imageCroppedFile != null) {
                    Uri imageCroppedURI = FileProvider.getUriForFile(MyCropActivity.this,
                            "com.example.android.fileprovider",
                            imageCroppedFile);
                    cropImageView.saveCroppedImageAsync(imageCroppedURI);   // Phải đặt hàm này sau listener
                }
            }
        });
    }

    private File createImageCroppedFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "CroppedNLUmath_" + timeStamp;
        File storageDir = getExternalFilesDir("Cropped");
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory to save*/
        );
        return imageFile;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(MyCropActivity.this, MainActivity.class);
        intent.putExtra("openCamera", isFromCamera);
        intent.putExtra("openGallery", isFromGallery);
        startActivity(intent);
    }

    private void anhXa() {
        cropImageView = findViewById(R.id.cropImageView);
        btnQuayLai = findViewById(R.id.btnQuayLai);
        btnGiai = findViewById(R.id.btnGiai);
    }
}
