package com.nguyenchiphong.nlumath_1.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.nguyenchiphong.nlumath_1.R;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import katex.hourglass.in.mathlib.MathView;
import maes.tech.intentanim.CustomIntent;

public class SolveActivityLong extends AppCompatActivity {
    ImageView imgViewAnhDaCrop;
    Uri imageCroppedURI;
    private String resSolve = "";   //dùng để giải
    private String resView = "";    //dùng để hiện KaTeX
    private boolean isHePT = false;
    EditText edtBieuThuc;
    public static final String TAG = "AAA";

    private static final String MODEL_PATH = "model.tflite";
    private static final int NUM_CLASSES = 18;
    private static final String[] CLASSES = {"-", "+", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "x", "y", "z", "(", ")"};

//    private static final String MODEL_PATH = "model_20.tflite";
//    private static final int NUM_CLASSES = 20;
//    private static final String[] CLASSES = {"-", "+", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "x", "y", "z", "(", ")", "sqrt()", "{"};

    // selected classifier information received from extras
    private boolean isQuant = false;
    // presets for rgb conversion
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    // input image dimensions for the Inception Model
    //CÁI NÀY GIỐNG VỚI input_shape CỦA MODEL:
    private int DIM_IMG_SIZE_X = 100;
    private int DIM_IMG_SIZE_Y = 100;
    private int DIM_PIXEL_SIZE = 1;

    // int array to hold image data
    private int[] intValues;

    // options for model interpreter
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private Interpreter tflite;

    // holds the selected image data as bytes
    private ByteBuffer imgData = null;
    // holds the probabilities of each label for non-quantized graphs
    private float[][] labelProbArray = null;
    // holds the probabilities of each label for quantized graphs
    private byte[][] labelProbArrayB = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solve_long); //dùng để xem quá trình xử lý ảnh
        //getSupportActionBar().hide();

        anhXa();
        layDuLieuIntent();
        khoiTaoCacThuocTinh();  //các thuộc tính phục vụ cho việc nhận dạng
        imgViewAnhDaCrop.setImageURI(imageCroppedURI);

        Process_2();
    }

    private String nhanDang(Bitmap bmSegmentedCharacter) {
//        Bitmap bitmap_orig = getBitmap(this.getContentResolver(), imageCroppedURI);
        // resize the bitmap to the required input size to the CNN
//        Bitmap bitmap = getResizedBitmap(bmSegmentedCharacter, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y); // không cần vì mình đã resize bằng Improc rồi
        // convert bitmap to byte array
        convertBitmapToByteBuffer(bmSegmentedCharacter);
        // pass byte data to the graph
        if(isQuant){
            tflite.run(imgData, labelProbArrayB);
        } else {
            tflite.run(imgData, labelProbArray);
            Log.d(TAG, "Mảng kết quả dự đoán:" + Arrays.toString(labelProbArray[0]));
        }
        //Lấy ra vị trí trong mảng chứa giá trị lớn nhất:
        float maxProbability = labelProbArray[0][0];
        int index = 0;
        for (int i = 1; i < labelProbArray[0].length; i++) {
            if (labelProbArray[0][i] > maxProbability) {
                maxProbability = labelProbArray[0][i];
                index = i;
            }
        }
        return findLabel(index);
    }

    private String findLabel(int index) {
        for (int i = 0; i < CLASSES.length; i++) {
            if (index == i) {
                return CLASSES[i];
            }
        }
        return "(Not found label)";
    }


    public Bitmap getBitmap(ContentResolver cr, Uri url)
            throws FileNotFoundException, IOException {
        InputStream input = cr.openInputStream(url);
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        input.close();
        return bitmap;
    }

    // resizes bitmap to given dimensions
    public Bitmap getResizedBitmap(Bitmap bmSegmentedCharacter, int newWidth, int newHeight) {
        int width = bmSegmentedCharacter.getWidth();
        int height = bmSegmentedCharacter.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bmSegmentedCharacter, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    // converts bitmap to byte array which is passed in the tflite graph
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind(); // reset the image data
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        Log.d(TAG, "Mảng intValues: " + Arrays.toString(intValues));
/*        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float
                if(isQuant){
                    imgData.put((byte) ((val >> 16) & 0xFF));   //lấy màu Red
                    imgData.put((byte) ((val >> 8) & 0xFF));    //lấy màu Green
                    imgData.put((byte) (val & 0xFF));           //lấy màu Blue
                } else {
                    //Sử dụng 1 trong 3 dòng thì được (kết quả đều SAI như nhau)
//                    imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
//                    imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);

                }
            }
        } */

        for (int i = 0; i < intValues.length; ++i) {
            // Set 0 for white and 255 for black pixels
            int pixelValue = intValues[i];
            //Sử dụng 1 trong 3 dòng thì được (kết quả đều như nhau)
//            imgData.putFloat((pixelValue >> 16) & 0xFF);
//            imgData.putFloat((pixelValue >> 8) & 0xFF);
            imgData.putFloat(pixelValue & 0xFF);

            /*BỎ phần dưới này vì nó cho kết quả sai*/
            // The color of the input is black so the blue channel will be 0xFF.
//            int channel = pixelValue & 0xff;
//            imgData.putFloat(0xff - channel);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(this, "right-to-left");
    }

    private void anhXa() {
        imgViewAnhDaCrop = findViewById(R.id.imgViewAnhDaCrop);
        edtBieuThuc = findViewById(R.id.edtBieuThuc);
    }

    private void layDuLieuIntent() {
        Intent intent = getIntent();
        imageCroppedURI = Uri.parse(intent.getStringExtra("imageCroppedURI"));
    }

    private void khoiTaoCacThuocTinh() {
        // initialize array that holds image data
        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        //initilize graph and labels
        try {
            tflite = new Interpreter(loadModelFile(), tfliteOptions);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // initialize byte array. The size depends if the input data needs to be quantized or not
        if (isQuant) {
            imgData =
                    ByteBuffer.allocateDirect(
                            DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        } else {
            imgData =
                    ByteBuffer.allocateDirect(
                            4 * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        }
        imgData.order(ByteOrder.nativeOrder());

        // initialize probabilities array. The datatype that array holds depends if the input data needs to be quantized or not
        if(isQuant){
            labelProbArrayB= new byte[1][NUM_CLASSES];
        } else {
            labelProbArray = new float[1][NUM_CLASSES];
        }
    }

    // loads tflite graph from file
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // CÁI NÀY LÀ LÀM THEO GIỐNG BÊN PYTHON:
    private void Process_2() {
//        Mat mat = new Mat(300, 300, CvType.CV_8UC3, new Scalar(150,89,98)); // Scalar là màu
//        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
//        Utils.matToBitmap(mat, bitmap);
//        imgViewAnhDaCrop.setImageBitmap(bitmap);

        ImageView imgGray = findViewById(R.id.imgGray);
        ImageView imgBlur = findViewById(R.id.imgBlur);
        ImageView imgThreshold = findViewById(R.id.imgThreshold);
        ImageView imgContour = findViewById(R.id.imgContour);
        ImageView imgBounding = findViewById(R.id.imgBounding);
        ImageView imgDilate = findViewById(R.id.imgDilate);
        ImageView imgThreshold_ChuDenNenTrang = findViewById(R.id.imgThreshold_ChuDenNenTrang);
        ImageView imgErode = findViewById(R.id.imgErode);

        // "CHUYỂN BITMAP SANG MAT, THAO TÁC, RỒI CHUYỂN LẠI"
        Bitmap bitmap_origin = null;
        Mat mat_origin = new Mat();
        try {
            bitmap_origin = getBitmap(getContentResolver(), imageCroppedURI);
            Utils.bitmapToMat(bitmap_origin, mat_origin);  //chuyển dữ liệu tự bitmap đến mat
        } catch (IOException e) {
            e.printStackTrace();
        }
/*
        // Blur:
        Mat matBlur = new Mat();    // có thể nhận vào tham số (rows, cols, CvType.CV_8UC4) tương đương với (bitmap.getHeigh(), bitmap.getWidth(), CvType.CV_8UC4)
        // Origin -> Blur
        Imgproc.blur(mat_origin, matBlur, new Size(10,10)); //thao tác
        Bitmap bmBlur = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matBlur, bmBlur);
        imgBlur.setImageBitmap(bmBlur);
*/

        // Ảnh xám:
        Mat matGray = new Mat();
        // Blur -> Gray
        Imgproc.cvtColor(mat_origin, matGray, Imgproc.COLOR_BGR2GRAY); //thao tác
        Bitmap bmGray = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matGray, bmGray);
        imgGray.setImageBitmap(bmGray);

        // Threshold:
        Mat matThreshold = new Mat();
        // Gray -> Threshold
        Imgproc.threshold(matGray, matThreshold, 90, 255, Imgproc.THRESH_BINARY_INV); // pixel nào lớn hơn ngưỡng (127) thì gán giá trị "sáng nhất" của màu đó (255)
//        Imgproc.adaptiveThreshold(matGray, matThreshold, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 12);
        Bitmap bmThreshold = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matThreshold, bmThreshold);
        imgThreshold.setImageBitmap(bmThreshold);

        // Dilate:
        Mat matDilate = new Mat();
        // Threshold -> Dilate
        Imgproc.dilate(matThreshold, matDilate, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 1)));
        Bitmap bmDilate = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matDilate, bmDilate);
        imgDilate.setImageBitmap(bmDilate);

        // Find Contours:
        List<MatOfPoint> lstMatOfPoint = new ArrayList<>(); //danh sách các contour
        Mat matHierarchy = new Mat();
        // RETR_EXTERNAL nghĩa là lấy contour bên ngoài, không tính contour bên trong, ví dụ: 6, 8, 9
        Imgproc.findContours(matDilate, lstMatOfPoint, matHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE); // SIMPLE chỉ lưu những điểm cần thiết, NONE lưu hết

        // Sắp xếp các contours theo thứ tự trái sang phải:
        sortContours(lstMatOfPoint);
        // chuyển về kênh BGR để hiển thị được màu của Scalar
        // tham số -1 là vẽ tất cả contour
        // thickness quan trọng, nhờ nó lớn mà mình mới thấy được contour đã vẽ :), nếu thickness = -1 thì nó sẽ fill luôn lỗ của số 6, 8, 9,...
        Mat matContour = matDilate.clone();  // clone để bảo toàn matThreshold
        Imgproc.cvtColor(matContour, matContour, Imgproc.COLOR_GRAY2BGR);
        Imgproc.drawContours(matContour, lstMatOfPoint, -1, new Scalar(0, 255, 0), 10);   //vẽ các contour lên matContour
        Bitmap bmContour = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matContour, bmContour);
        imgContour.setImageBitmap(bmContour);

        //Bounding Box Rectangle:
        List<Rect> lstRect = new ArrayList<>(); // chứa pixel của hình chữ nhật
        for (int i = 0; i < lstMatOfPoint.size(); i++) {
            Rect rect = Imgproc.boundingRect(lstMatOfPoint.get(i));
            lstRect.add(rect);
        }
        Mat matBounding = matContour.clone();
        // LỌC CONTOURS không phải ký tự rồi vẽ các CONTOURS là ký tự:
        chonLocContours(lstMatOfPoint, lstRect);
        for (int i = 0; i < lstMatOfPoint.size(); i++) {
            Imgproc.rectangle(matBounding, lstRect.get(i).tl(), lstRect.get(i).br(), new Scalar(255,0,0), 10); //vẽ
        }
        Bitmap bmBounding = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matBounding, bmBounding);
        imgBounding.setImageBitmap(bmBounding);

        // THRESHOLD CHỮ ĐEN NỀN TRẮNG:
        Imgproc.threshold(matGray, matThreshold, 90, 255, Imgproc.THRESH_BINARY);
        Bitmap bmThreshold_ChuDenNenTrang = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matThreshold, bmThreshold_ChuDenNenTrang);
        imgThreshold_ChuDenNenTrang.setImageBitmap(bmThreshold_ChuDenNenTrang);

        // Erode:
        Mat matErode = new Mat();
        // Threshold -> Erode
        Imgproc.erode(matThreshold, matErode, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 1)));
        Bitmap bmErode = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matErode, bmErode);
        imgErode.setImageBitmap(bmErode);

        String[] arrRes = nhanDangToanBoBieuThuc(matDilate, lstRect, lstMatOfPoint);
        resView += "$$" + arrRes[1] + "$$";
        resSolve = arrRes[0];

        // Hiện MathView:
        MathView mathView = findViewById(R.id.mathView);

        String tmp = "$x=\\frac{1+y}{1+2z^2}$";
        String pt1 = "a_1x+b_1y+c_1z &=d_1+e_1+p_2+p_3";
        String pt2 = "a_2x+b_2y &=d_2";
        String tmp0 = "$\\left\\{\\begin{array}{ll}" + pt1 + " \\\\" + pt2 + "\\end{array}\\right.$";
        String tmp1 = "$\n" +
                "\\left\\{\n" +
                "\\begin{array}{ll}\n" +
                "a_1x+b_1y+c_1z &=d_1+e_1 \\\\ \n" +
                "a_2x+b_2y &=d_2 \\\\ \n" +
                "a_3x+b_3y+c_3z &=d_3 \n" +
                "\\end{array} \n" +
                "\\right.\n" +
                "$";
        String tmp2 = "$$\\begin{cases}\n" +
                "a_1x+b_1y+c_1z=d_1 \\\\ \n" +
                "a_2x+b_2y+c_2z=d_2 \\\\ \n" +
                "a_3x+b_3y+c_3z=d_3\n" +
                "\\end{cases}\n" +
                "$$";
        String tmp3 = "$$\\frac{" + "1+2" + "}" + "{" + "3+4" + "})$$";
        String tmp4 = "$$x^2+(1\\over 2)=0$$"; // => thay ngoặc () bằng ngoặc {} để hiển thị đc phân số với đk phân số phải được bao bởi ()
        String tmp5 = "Solving \\(2(x+8)(x-4) = x^{2}\\):$$ 2(x+8)(x-4) = x^{2} $$\n" +
                "$$ 2x^{2}-8x+16x-64 = x^{2} $$\n" +
                "$$ 2x^{2}+8x-64 = x^{2} $$\n" +
                "$$ x^{2}+8x-64 = 0 $$\n" +
                "$$ x = \\frac{-(8 \\cdot 1)\\pm\\sqrt{(8 \\cdot 1)^{2}-4 \\cdot 1 \\cdot (-64)}}{2 \\cdot 1} $$\n" +
                "$$ x = \\frac{-8\\pm\\sqrt{64+256}}{2} $$\n" +
                "$$ x = \\frac{-8\\pm\\sqrt{320}}{2} $$\n" +
                "$$ x = \\frac{-8\\pm17,8885438}{2} $$\n" +
                "Split the solution to these equations: $$x = \\frac{-8+17,123456789}{2}\\,,x = \\frac{-8-17,123456789}{2}$$\n" +
                "Solve the first equation: $$x = \\frac{-8+17,8885438999999999999}{2}$$\n" +
                "$$ x = 4,9442719 $$\n" +
                "Solve the next equation: $$x = \\frac{-8-17,8885438}{2}$$\n" +
                "$$ x = -12,9442719 $$\n" +
                "$$ $$ Solution:$$ $$ $$x = 4,9442719\\,\\,\\,\\,Or\\,\\,\\,\\,x = -12,9442719$$$$ $$ $$ $$ $$ $$";

        edtBieuThuc.setText(resSolve);
        String latexBuocGiai = giai(resSolve);
        mathView.setTextSize(18);   //mặc định của nó là 18
        if (isHePT) {
//            mathView.setTextSize(20);
        }
//        mathView.setDisplayText(resView);
        mathView.setDisplayText(latexBuocGiai);
    }

    private String giai(String resSolve) {
        // KIỂM TRA THUỘC DẠNG NÀO THÌ GỌI HÀM GIẢI CỦA DẠNG ĐÓ:
        String bieuThuc = resSolve;
        String latexBuocGiai = "";
        if (isHePT) {
            latexBuocGiai = giaiHPTBacNhatHaiAn(bieuThuc);
            return latexBuocGiai;
        }
        // Phòng trường hợp nó nhận ra nhiều hơn 1 biến dẫn đến giải không được làm crash app:
        String[] arrBien = {"x", "y", "z"};
        int count = 0;
        for (String s : arrBien) {
            if (bieuThuc.contains(s)) count++;
        }
        if (count > 1) {
            return "Không phải HPT nhưng lại chứa nhiều hơn 1 biến!";
        }

        if (bieuThuc.contains("x^3") || bieuThuc.contains("y^3") || bieuThuc.contains("z^3")) {
            latexBuocGiai = giaiPTBac3(bieuThuc);
            return latexBuocGiai;
        }
        if (bieuThuc.contains("x^2") || bieuThuc.contains("y^2") || bieuThuc.contains("z^2")) {
            latexBuocGiai = giaiPTBac2(bieuThuc);
            return latexBuocGiai;
        }
        if (bieuThuc.contains("x") || bieuThuc.contains("y") || bieuThuc.contains("z")) {
            latexBuocGiai = giaiPTBac1(bieuThuc);
            return latexBuocGiai;
        }
        else {
            latexBuocGiai = giaiBieuThuc(bieuThuc);
            return latexBuocGiai;
        }
    }

    private String giaiHPTBacNhatHaiAn(String bieuThuc) {
        return "";
    }

    private String giaiPTBac3(String bieuThuc) {
        //giai
        //hien tung buoc ra layout
        return "";
    }

    private String giaiPTBac2(String bieuThuc) {
        return "";
    }

    private String giaiPTBac1(String bieuThuc) {
        return "";
    }

    private String giaiBieuThuc(String bieuThuc) {
        return "";
    }













    private String[] nhanDangToanBoBieuThuc(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint) {
        String[] arrRes = new String[2];    // phần tử 0 là res Solve, 1 là res View
        String resSolve = "";
        String resView = "";
        // Segment Characters:
        List<Mat> lstMatCharacter = new ArrayList<>();  // chứa pixel của hình chữ nhật và bên trong
        List<Bitmap> lstBitMapCharacter = new ArrayList<>();    // danh sách ảnh các ký tự được tách
        LinearLayout linearLayoutChracters = findViewById(R.id.linearLayoutCharacters);

        for (int i = 0; i < lstMatOfPoint.size(); ) {
            // Kiểm tra dấu mốc đơn (hệ pt) dựa vào ratio và 2 dấu =
            if (i < lstMatOfPoint.size()-1) {
                if (isHePhuongTrinh(lstRect, lstMatOfPoint, i)) {
                    // loại phân số ra khỏi lstMatOfPoint
                    // nhận dạng phân số rồi cộng riêng vào res
                    String[] arrValue = nhanDangHePhuongTrinh(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra căn:
            if (i < lstMatOfPoint.size()-1) {
                if (isCan(lstRect, lstMatOfPoint, i)) {
                    // loại phân số ra khỏi lstMatOfPoint
                    // nhận dạng phân số rồi cộng riêng vào res
                    String[] arrValue = nhanDangCan(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra phân số (bao gồm dấu : có gạch ngang)
            if (i < lstMatOfPoint.size()-1) {
                if (isPhanSo(lstRect, lstMatOfPoint, i)) {
                    // loại phân số ra khỏi lstMatOfPoint
                    // nhận dạng phân số rồi cộng riêng vào res
                    String[] arrValue = nhanDangPhanSo(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra dấu "=". LUÔN  ĐẶT CÁI NÀY SAU KIỂM TRA PHÂN SỐ
            if (i < lstMatOfPoint.size()-1) {
                if (isDauBang(lstRect, lstMatOfPoint, i)) {
                    // loại dấu bằng ra khỏi lstMatOfPoint
                    // nhận dạng dấu bằng rồi cộng riêng vào res
                    if (resSolve.endsWith("^")) {
                        resSolve = resSolve.substring(0, resSolve.length()-1);
                    }
                    if (resView.endsWith("^")) {
                        resView = resView.substring(0, resView.length()-1);
                    }
                    String[] arrValue = nhanDangDauBang(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra dấu chia ":"
            if (i < lstMatOfPoint.size()-1) {
                if (isDauChia(lstRect, lstMatOfPoint, i)) {
                    // loại dấu chia ra khỏi lstMatOfPoint
                    // nhận dạng dấu chia rồi cộng riêng vào res
                    if (resSolve.endsWith("^")) {
                        resSolve = resSolve.substring(0, resSolve.length()-1);
                    }
                    if (resView.endsWith("^")) {
                        resView = resView.substring(0, resView.length()-1);
                    }
                    String[] arrValue = nhanDangDauChia(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra mũ:
            if (i < lstMatOfPoint.size()-1) {
                if (isMu(matSrc, lstRect, lstMatOfPoint, i)) {
                    //loại các ký tự là mũ ra khỏi lstMatOfPoint (vì mình sẽ nhận dạng nó ở hàm nhanDangMu())
                    //nhận dạng mũ rồi cộng riêng vào res
                    String[] arrValue = nhanDangMu(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            Mat matCharacter = segment(matSrc, lstRect.get(i), lstMatOfPoint, i);
            Imgproc.resize(matCharacter, matCharacter, new Size(100,100), 0, 0, Imgproc.INTER_AREA);    // resize trước rồi add vào list; INTER_AREA thích hợp cho thu nhỏ ảnh
            lstMatCharacter.add(matCharacter);
            Bitmap bmCharacter = Bitmap.createBitmap(matCharacter.cols(), matCharacter.rows(), Bitmap.Config.ARGB_8888);
            lstBitMapCharacter.add(bmCharacter);
            Utils.matToBitmap(lstMatCharacter.get(i), lstBitMapCharacter.get(i));

            // NHẬN DẠNG VÀ XỬ LÝ NHẬN DẠNG CHO ĐÚNG:
            String kyTu = nhanDang(bmCharacter);
//            kyTu = xuLyChoDep(res, kyTu);
            resSolve += kyTu;
            resView += kyTu;
            //Xử lý mũ:
           /* boolean isDau = false;
            String[] lstDau = {"+", "-", "*", "/", "(", "=", "{", "!"};
            for(int j = 0; j <lstDau.length; j++) {
                if (kyTu.equalsIgnoreCase(lstDau[j])) {
                    isDau = true;
                }
            }
            if (isDau) {    // nếu nó là dấu thì kiểm tra ký tự phía trước nó có phải là ^ không, nếu có thì xóa ^ vì không có chuyện ^dấu
                if (res.endsWith("^")) {
                    res = res.substring(0, res.length()-1);
                }
                res += kyTu;
            } else {    //nếu không phải dấu thì kiểm tra để xem có mũ không
                res += kyTu;
                if (i < lstRect.size()-1 && isExponential(lstRect.get(i), lstRect.get(i+1))) {
                    res += "^";
                }
            }*/

            //Vẽ các ký tự ra layout:
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(lstBitMapCharacter.get(i));
            linearLayoutChracters.addView(imageView);

            i++;
        }
        arrRes[0] = resSolve;
        arrRes[1] = resView;
        return arrRes;
    }

    private String xuLy(String res, String kyTu) {
        String kq = kyTu;
        String[] key = {"x^", "y^", "z^"};
        for (int i = 0; i < key.length; i++) {
            if (res.endsWith(key[i]) && kyTu.equalsIgnoreCase("x")) {
                kq = "2";
            }
        }
        return kq;
    }

    private String nhanDangMotKyTu(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Mat matCharacter = segment(matSrc, lstRect.get(i), lstMatOfPoint, i);
        Imgproc.resize(matCharacter, matCharacter, new Size(100,100), 0, 0, Imgproc.INTER_AREA);    // resize trước rồi add vào list; INTER_AREA thích hợp cho thu nhỏ ảnh
        Bitmap bmCharacter = Bitmap.createBitmap(matCharacter.cols(), matCharacter.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matCharacter, bmCharacter);

        String kyTu = nhanDang(bmCharacter);
        return kyTu;
    }

    boolean isNgoacDonMo = false;
    private boolean isDau(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        String kyTu = nhanDangMotKyTu(matSrc, lstRect, lstMatOfPoint, i);
        // Nếu contour tiếp theo là dấu thì sẽ không có mũ cho contour hiện tại. Ngoài trừ 3 dấu + - ( thì vẫn cho phép
        // nhưng phải xét tiếp 1 contour tiếp theo (đối với + -, nếu contour tiếp theo không nằm trên x thì có nghĩa là + - chỉ đơn thuần là phép tính
        // chứ không phải mũ). Tương tự đối với ( nhưng sẽ xét 2 contour tiếp theo

        /*if (kyTu.equalsIgnoreCase("+") || kyTu.equalsIgnoreCase("-")) {
            // rectTiepTheo này là rect theo sau dấu + hoặc - chứ không phải rect theo sau ký tự đang xét có mũ hay không.
            Rect rectTiepTheo = lstRect.get(i+1);
            if (nhanDangMotKyTu(matSrc, lstRect, lstMatOfPoint, i+1).equalsIgnoreCase("-")) {
                return true;    //vì đây là trường hợp của dấu =
            }
            Rect rectKyTu = lstRect.get(i-1);
            // Kiểm tra độ cao:
            if (r2_laMuCua_r1(rectKyTu, rectTiepTheo)) {
                return false;    // chấp nhận + hoặc - là thuộc mũ chứ không phải dấu phép tính
            } else return true;    // chỉ coi nó là dấu của phép tính
        }*/
        // THAY ĐOẠN TRÊN BẰNG ĐOẠN NÀY:
        if (kyTu.equalsIgnoreCase("+") || kyTu.equalsIgnoreCase("-")) {
            return true;    // nếu dấu + hay - thì mình k tính là mũ, vì mình thấy trường hợp + hoặc - làm mũ ít gặp hơn!!!
        }

        if (kyTu.equalsIgnoreCase("(")) {
            Rect rectKyTu = lstRect.get(i-1);
            Rect rectTiepTheo1 = lstRect.get(i+1);
            Rect rectTiepTheo2 = lstRect.get(i+2);
            // Kiểm tra độ cao:
            if (r2_laMuCua_r1(rectKyTu, rectTiepTheo1) && r2_laMuCua_r1(rectKyTu, rectTiepTheo2)) {
                isNgoacDonMo = true;
                return false;    // chấp nhận ( là thuộc mũ chứ không phải dấu phép tính
            } else return true;    // chỉ coi nó là dấu của phép tính
        }
        boolean isDau = false;
        String[] lstDau = {"*", "/", "=", "{", "!", ")"};
        for(int j = 0; j <lstDau.length; j++) {
            if (kyTu.equalsIgnoreCase(lstDau[j])) {
                isDau = true;
            }
        }
        return isDau;
    }

    float t = 0.5f;    // 2/3
    private boolean r2_laMuCua_r1(Rect r1, Rect r2) {
        if ((r1.y - r1.height*2) < (r2.y + r2.height) && (r2.y + r2.height) < (r1.y + r1.height * t)) {  // nghĩa là, điểm trái dưới của r2 phải nằm trên 1/3 của r1 và dưới r1.y - r1.height
//            if (r1.x < r2.x && r2.x < r1.x + r1.width*2) {
                return true;
//            }
        }
        return false;
    }

    private boolean isMu(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        // Nếu ký tự là các dấu sau đây thì k cần xét mũ:
        String kyTu = nhanDangMotKyTu(matSrc, lstRect, lstMatOfPoint, i);
        String[] lstDau = {"+", "-", "*", "/", "=", "{", "!", "("};
        for(int j = 0; j <lstDau.length; j++) {
            if (kyTu.equalsIgnoreCase(lstDau[j])) {
                return false;
            }
        }

        // Nếu contour tiếp theo là dấu thì sẽ không có mũ cho contour hiện tại
        boolean isDau_ContourTiepTheo = isDau(matSrc, lstRect, lstMatOfPoint, i+1); // LƯU Ý: i+1
        if (isDau_ContourTiepTheo) return false;
        // Còn không thì xét độ cao của contour tiếp theo để biết có mũ hay không
        if (r2_laMuCua_r1(lstRect.get(i), lstRect.get(i+1))) {
            return true;    // có mũ
        }
        return false;
    }

    private String[] nhanDangMu(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        String res = "";
        List<MatOfPoint> lstMu = new ArrayList<>();
        List<Rect> lstRectMu = new ArrayList<>();
        res += nhanDangMotKyTu(matSrc, lstRect, lstMatOfPoint, i);  // nhận diện cơ số
        Rect rectKyTu = lstRect.get(i); // lấy nó ra rồi mới remove
        lstMatOfPoint.remove(i);
        lstRect.remove(i);
        Rect rectTruocDo = new Rect();
        int l = 0;
        // Xét đến khi phát hiện đã hết mũ thì dừng:
        for (int k = i; k < lstMatOfPoint.size();) {
            Rect rectTiepTheo = lstRect.get(k);
            if (r2_laMuCua_r1(rectKyTu, rectTiepTheo)) {
                if (l != 0) {   // tức là đang xét từ contour tiếp theo thứ 2 trở đi.
                    // Sẽ lấy contour này so sánh với contour trước đó. Tránh trường hợp sai giống như trong hình 8y^(3-)
                    float ratioRectTruocDo = (float) rectTruocDo.width / rectTruocDo.height;
                    if (ratioRectTruocDo > 2) { // nghi là dấu -
                       int diemGiua = rectTruocDo.y + rectTruocDo.height;  // điểm giữa của dấu -
                        if (!(rectTiepTheo.y < diemGiua && diemGiua < rectTiepTheo.y + rectTiepTheo.height)) {
                            break;
                        }
                    }
                    int diemGiua = rectTiepTheo.y + rectTiepTheo.height/2;
                    if (!(rectTruocDo.y < diemGiua && diemGiua < rectTruocDo.y + rectTruocDo.height)) {
                        break;
                    }
                }
                rectTruocDo = rectTiepTheo; // lưu lại giá trị vào rectTruocDo
                lstMu.add(lstMatOfPoint.get(k));
                lstRectMu.add(lstRect.get(k));
                lstMatOfPoint.remove(k);
                lstRect.remove(k);
                l++;
                // k sẽ tự động tăng
            } else {
                break;  // gặp ký tự không còn là mũ thì dừng
            }
        }
        String[] arrValue = new String[2];
        // 1 phần tử mũ thì k cần ()
        res += "^";
        String resView = res;

        String[] arrValueTmp = nhanDangToanBoBieuThuc(matSrc, lstRectMu, lstMu);
        String mu = arrValueTmp[0];    // resSolve
        if (mu.length() == 1) {
            res += mu;
        }
        else if(!isNgoacDonMo) {
            res += "(" + mu + ")";  // mũ k có ( thì mới cộng
        }
        else {
            res += mu; // có rồi thì k cần cộng thêm () (vì cộng nhiều sẽ gây rối mắt)
            isNgoacDonMo = false; // set lại
        }
        arrValue[0] = res;

        String muView = arrValueTmp[1];
        if (muView.length() == 1) {
            resView += muView;
        } else resView += "{" + muView + "}";
        arrValue[1] = resView;
        return arrValue;
    }

    private boolean isPhanSo(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        float ratio = (float) rect.width / rect.height;
        if (ratio > 3 && i < lstMatOfPoint.size()-2) {
            Rect rectTiepTheo1 = lstRect.get(i+1);
            Rect rectTiepTheo2 = lstRect.get(i+2);
            if (rect.x < rectTiepTheo1.x && rectTiepTheo1.x < (rect.x + rect.width)) {
                if (rect.x < rectTiepTheo2.x && rectTiepTheo2.x < (rect.x + rect.width)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String[] nhanDangPhanSo(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        String res = "";
        Rect rect = lstRect.get(i);
        List<MatOfPoint> lstTuSo = new ArrayList<>();
        List<Rect> lstRectTuSo = new ArrayList<>();
        List<MatOfPoint> lstMauSo = new ArrayList<>();
        List<Rect> lstRectMauSo = new ArrayList<>();
        // remove dấu phân số
        lstMatOfPoint.remove(i);
        lstRect.remove(i);
        for (int j = i; j < lstMatOfPoint.size();) {
            Rect rectTiepTheo = lstRect.get(j);
            if ((rectTiepTheo.y + rectTiepTheo.height) < (rect.y + rect.height) && rect.x < rectTiepTheo.x && rectTiepTheo.x < rect.x + rect.width) {
                lstTuSo.add(lstMatOfPoint.get(j));
                lstRectTuSo.add(lstRect.get(j));
                lstMatOfPoint.remove(j);
                lstRect.remove(j);
                continue;   // bởi vì mình đã remove nên j tự động tăng nên mình không cần cộng j lên!
            }
            if ((rectTiepTheo.y) > (rect.y + rect.height) && rect.x < rectTiepTheo.x && rectTiepTheo.x < rect.x + rect.width) {
                lstMauSo.add(lstMatOfPoint.get(j));
                lstRectMauSo.add(lstRect.get(j));
                lstMatOfPoint.remove(j);
                lstRect.remove(j);
                continue;
            }
            j++;
        }

        String[] arrValue = new String[2];
        // Xử lý dấu : có gạch ngang
        if (lstTuSo.size() == 1 &&lstMauSo.size() == 1) {
            Rect rectTuSo = lstRectTuSo.get(0);
            Rect rectMauSo = lstRectMauSo.get(0);
            int areaTuSo = rectTuSo.height * rectTuSo.width;
            int areaMauSo = rectMauSo.height * rectMauSo.width;
            if (areaTuSo < 5000 && areaMauSo < 5000) {
                int dolech_X = Math.abs(rectTuSo.x - rectMauSo.x);
                if (dolech_X < 500) {
                    arrValue[0] = ":";
                    arrValue[1] = ":";
                    return arrValue;
                }
            }
        }

        res += "(";

        // Nhận dạng tử số
        String[] arrValueTuSo = nhanDangToanBoBieuThuc(matSrc, lstRectTuSo, lstTuSo);
        if (lstRectTuSo.size() == 1) { // chỉ có 1 phần tử thì không cần + dấu ()
            res +=  arrValueTuSo[0];
        } else res += "(" + arrValueTuSo[0] + ")";

        res += "/";

        // Nhận dạng mẫu số
        String[] arrValueMauSo = nhanDangToanBoBieuThuc(matSrc, lstRectMauSo, lstMauSo);
        if (lstRectMauSo.size() == 1) {
            res +=  arrValueMauSo[0];
        } else res += "(" + arrValueMauSo[0] + ")";

        res += ")";
        // cho resSolve
        arrValue[0] = res;
        // cho res View
        arrValue[1] = "\\frac{" + arrValueTuSo[1] + "}" + "{" + arrValueMauSo[1] + "}";
        return arrValue;
    }

    private boolean isDauBang(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        float ratio1 = (float) rect.width / rect.height;
        Rect rectTiepTheo = lstRect.get(i+1);
        float ratio2 = (float) rectTiepTheo.width / rectTiepTheo.height;
        if (ratio1 > 2 && ratio2 > 2) { // kiểm tra nó có phải là 2 dấu trừ
            if ((rectTiepTheo.x <= rect.x && rect.x < rectTiepTheo.x + rectTiepTheo.width)
                    || (rectTiepTheo.x - rectTiepTheo.width < rect.x && rect.x <= rectTiepTheo.x)) {  // dùng dấu <= là bởi vì có thể có trường hợp 2 x của chúng bằng nhau (biểu thức in trong sách)
                return true;
            }
        }
        return false;
    }

    private String[] nhanDangDauBang(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        // remove 2 ký tự dấu = (vì mình nhận dạng thủ công nên k cần đưa vào model để nhận dạng nữa)
        lstMatOfPoint.remove(i);
        lstMatOfPoint.remove(i);
        lstRect.remove(i);
        lstRect.remove(i);
        String[] arrValue = new String[2];
        arrValue[0] = "=";
        arrValue[1] = "=";  // thư viện MathView tự động cách ra " = " cho mình
        return arrValue;
    }

    private boolean isDauChia(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        int areaContour = rect.width * rect.height;
        if (areaContour < 5000) {   // giới hạn 5000 là bởi vì dấu . thường không lớn quá 5000, nếu lớn hơn rất có thể nó là ký tự chứ k phải dấu .
            if (0 < i && i < lstMatOfPoint.size() - 1) {
                Rect rectDauTien = lstRect.get(0);
                Rect rectTiepTheo = lstRect.get(i + 1);
                int areaContourTiepTheo = rectTiepTheo.width * rectTiepTheo.height;
                if (areaContourTiepTheo < 5000) {   //tới đây xác định đc là 2 dấu .
                    int dolech_X = Math.abs(rect.x - rectTiepTheo.x);
                    // độ lệch x giữa 2 dấu chấm không lớn và 2 dấu này nằm trong khoảng height của contour đầu tiên
//                    if (dolech_X < 500 && rectDauTien.y < rect.y && rect.y < rectDauTien.y + rectDauTien.height
//                            && rectDauTien.y < rectTiepTheo.y && rectTiepTheo.y < rectDauTien.y + rectDauTien.height) {
                    if (dolech_X < 500) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String[] nhanDangDauChia(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        // remove 2 ký tự dấu = (vì mình nhận dạng thủ công nên k cần đưa vào model để nhận dạng nữa)
        lstMatOfPoint.remove(i);
        lstMatOfPoint.remove(i);
        lstRect.remove(i);
        lstRect.remove(i);
        String[] arrValue = new String[2];
        arrValue[0] = ":";
        arrValue[1] = ":";
        return arrValue;
    }

    private boolean isCan(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        Rect rectTiepTheo = lstRect.get(i+1);
        int gocPhaiTren = rectTiepTheo.x + rectTiepTheo.width;
        if (rect.x < gocPhaiTren && gocPhaiTren <= rect.x + rect.width) {
            if (rect.y < rectTiepTheo.y && rectTiepTheo.y < rect.y + rect.height) {
                return true;
            }
        }
        return false;
    }

    private String[] nhanDangCan(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        String res = "";
        Rect rect = lstRect.get(i);
        // remove dấu căn (do mình nhận dạng thủ công)
        lstMatOfPoint.remove(i);
        lstRect.remove(i);

        List<MatOfPoint> lstContourTrongCan = new ArrayList<>();
        List<Rect> lstRectTrongCan = new ArrayList<>();
        for (int k = i; k < lstMatOfPoint.size(); ) {
            Rect rectTiepTheo = lstRect.get(k);
            int gocPhaiTren = rectTiepTheo.x + rectTiepTheo.width;
            if (rect.x < gocPhaiTren && gocPhaiTren <= rect.x + rect.width) {
                if (rect.y < rectTiepTheo.y && rectTiepTheo.y < rect.y + rect.height) {
                    lstContourTrongCan.add(lstMatOfPoint.get(k));
                    lstRectTrongCan.add(lstRect.get(k));
                    lstMatOfPoint.remove(k);
                    lstRect.remove(k);
                }
            } else {
                k++;
            }
        }

        String[] arrValueTmp = nhanDangToanBoBieuThuc(matSrc, lstRectTrongCan, lstContourTrongCan);

        String[] arrValue = new String[2];
        arrValue[0] = "sqrt(" + arrValueTmp[0] + ")";
        arrValue[1] = "\\sqrt{" + arrValueTmp[1] + "}";
        return arrValue;
    }

    List<MatOfPoint> lstContourPhuongTrinh1 = new ArrayList<>();
    List<Rect> lstRectPhuongTrinh1 = new ArrayList<>();
    List<MatOfPoint> lstContourPhuongTrinh2 = new ArrayList<>();
    List<Rect> lstRectPhuongTrinh2 = new ArrayList<>();

    private boolean isHePhuongTrinh(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        float ratio = (float) rect.height / rect.width;
        if (ratio >= 3) {
            // Tính đường line ở giữa
            int line = rect.y + rect.height/2;
            // Chia list contours thành 2 phần trên và dưới line
            for (int k = i + 1; k < lstMatOfPoint.size(); k++) {
                Rect rectTiepTheo = lstRect.get(k);
                int diemGiuaKyTu = rectTiepTheo.y + rectTiepTheo.height/2;
                if (diemGiuaKyTu < line) {  //nằm trên line
                    lstContourPhuongTrinh1.add(lstMatOfPoint.get(k));
                    lstRectPhuongTrinh1.add(lstRect.get(k));
                } else if (diemGiuaKyTu > line) {//nằm dưới line
                    lstContourPhuongTrinh2.add(lstMatOfPoint.get(k));
                    lstRectPhuongTrinh2.add(lstRect.get(k));
                }
            }
            boolean isDauBang1 = false;
            boolean isDauBang2 = false;
            // Kiểm tra xem có dấu = ở trên đường line không
            for (int m = 0; m < lstContourPhuongTrinh1.size() - 1; m++) {
                if (isDauBang(lstRectPhuongTrinh1, lstContourPhuongTrinh1, m)) {
                    isDauBang1 = true;
                    break;
                }
            }
            // Kiểm tra xem có dấu = ở dưới đường line không
            for (int m = 0; m < lstContourPhuongTrinh2.size() - 1; m++) {
                if (isDauBang(lstRectPhuongTrinh2, lstContourPhuongTrinh2, m)) {
                    isDauBang2 = true;
                    break;
                }
            }
            if (isDauBang1 && isDauBang2) {
                // Remove hệ pt (vì mình sẽ nhận dạng nó ở hàm nhanDangHePhuongTrinh)
                lstMatOfPoint.remove(i);
                lstRect.remove(i);
                for (int k = i; k < lstMatOfPoint.size();) {
                    Rect rectTiepTheo = lstRect.get(k);
                    int diemGiuaKyTu = rectTiepTheo.y + rectTiepTheo.height/2;
                    if (diemGiuaKyTu < line) {  // nằm trên line
                        lstMatOfPoint.remove(k);
                        lstRect.remove(k);
                        continue;
                    } else if (diemGiuaKyTu > line) {// nằm dưới line
                        lstMatOfPoint.remove(k);
                        lstRect.remove(k);
                        continue;
                    }
                    k++;
                }
                return true;    // đúng là hệ phương trình
            }
        }

        return false;
    }

    private String[] nhanDangHePhuongTrinh(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        String[] arrValuePT1 = nhanDangToanBoBieuThuc(matSrc, lstRectPhuongTrinh1, lstContourPhuongTrinh1);
        String[] arrValuePT2 = nhanDangToanBoBieuThuc(matSrc, lstRectPhuongTrinh2, lstContourPhuongTrinh2);
        isHePT = true;
        String[] arrValue = new String[2];
        String hePT = "";
        hePT += "PT1: " + arrValuePT1[0];
        hePT += "\nPT2: " + arrValuePT2[0];
        // cho resSolve
        arrValue[0] = hePT;
        // cho res View
        arrValue[1] = "\\begin{cases}" + arrValuePT1[1] + " \\\\" + arrValuePT2[1] + "\\end{cases}";

        return arrValue;
    }

    private  Mat segment(Mat matSrc, Rect rect, List<MatOfPoint> lstMatOfPoint, int i) {
//        rect.x = rect.x - padding;
//        rect.y = rect.y - padding;
//        rect.width = rect.width + p;
//        rect.height = rect.height + p;

        // Dùng MASK (tránh trường hợp cắt dính phải ký tự khác):
        Mat mask = new Mat(matSrc.rows(), matSrc.cols(), CvType.CV_8UC1, Scalar.all(0));
        Imgproc.drawContours(mask, lstMatOfPoint, i, new Scalar(255,255,255), -1);   // vẽ các contour lên matContour

        Mat temp = new Mat();
        matSrc.copyTo(temp, mask);
        Mat matCharacter = temp.submat(rect);   // chữ trắng nền đen
        Imgproc.threshold(matCharacter, matCharacter, 90, 255, Imgproc.THRESH_BINARY_INV);  // chữ đen nền trắng

//        LinearLayout linearLayoutChracters = findViewById(R.id.linearLayoutCharacters);
//        // Vẽ các ký tự ra layout:
//        ImageView imageView = new ImageView(this);
//        Bitmap bmTemp = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(temp, bmTemp);
//        imageView.setImageBitmap(bmTemp);
//        linearLayoutChracters.addView(imageView);

//        Mat matCharacter = matSrc.submat(rect);

        // Padding
//        int p = 7;
//        Core.copyMakeBorder(matCharacter, matCharacter, 0, p, 0, p, Core.BORDER_ISOLATED, new Scalar(255,255,255));

        // CÁCH NÀY TỐT HƠN NHIỀU:
        if (rect.height > rect.width) {
            Core.copyMakeBorder(matCharacter, matCharacter, 0, 0, (rect.height - rect.width) / 2, (rect.height - rect.width) / 2, Core.BORDER_ISOLATED, new Scalar(255,255,255));
        } else if (rect.height < rect.width) {
            Core.copyMakeBorder(matCharacter, matCharacter, (rect.width - rect.height) / 2, (rect.width - rect.height) / 2, 0, 0, Core.BORDER_ISOLATED, new Scalar(255,255,255));
        }

        /*float ratio = (float) rect.width / rect.height;
       // Xử lý dấu -
        if (ratio > 3) {
            // Dùng BORDER_ISOLATED để nó đối xử với src một cách độc lập, như thể src không phải là ROI (được trích ra từ 1 ảnh lớn)
            Core.copyMakeBorder(matCharacter, matCharacter, rect.width / 2, rect.width / 2, 0, 0, Core.BORDER_ISOLATED, new Scalar(255,255,255));
        }
        // Xử lý (, ), 1
        if (ratio < 0.5) {
            Core.copyMakeBorder(matCharacter, matCharacter, 0, 0, rect.height / 2, rect.height / 2, Core.BORDER_ISOLATED, new Scalar(255,255,255));
        }*/

        return matCharacter;
    }

    private void sortContours(List<MatOfPoint> lstMatOfPoint) {
        Collections.sort(lstMatOfPoint, new Comparator<MatOfPoint>() {
            @Override
            //Sắp xếp các contours "tăng dần" theo x (chiều ngang)
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                Rect rect1 = Imgproc.boundingRect(o1);
                Rect rect2 = Imgproc.boundingRect(o2);
                int result = 0;
//                double total = rect1.tl().y/rect2.tl().y;
//                if (total>=0.9 && total<=1.4 ){
                result = Double.compare(rect1.tl().x, rect2.tl().x);
//                }
                return result;
            }
        });
    }

    private void chonLocContours(List<MatOfPoint> lstMatOfPoint, List<Rect> lstRect) {
        for (int i = 0; i < lstMatOfPoint.size();) {
            // Lọc dựa vào diện tích (area):
            Rect rect = lstRect.get(i);
            int areaContour = rect.width * rect.height;
            Log.d(TAG, "Width_" + i + " = " + rect.width + "\nHeight_" + i + " = " + rect.height);
            if (areaContour < 100) {    // nhỏ quá thì bỏ luôn k cần xét dấu chia, vì nhỏ hơn 100 thường là nhiễu, dấu : của mình thường lớn hơn 100
                lstMatOfPoint.remove(i);
                lstRect.remove(i);
            }
            else if (areaContour < 400) {
                if (0 < i && i < lstMatOfPoint.size() - 1) {
                    Rect rectDauTien = lstRect.get(0);
                    Rect rectTiepTheo = lstRect.get(i+1);
                    int areaContourTiepTheo = rectTiepTheo.width * rectTiepTheo.height;
                    if (areaContourTiepTheo < 400) {   // tới đây xác định đc là 2 dấu .
                        int dolech_X = Math.abs(rect.x - rectTiepTheo.x);
                        // độ lệch x giữa 2 dấu chấm không lớn và 2 dấu này nằm trong khoảng height của contour đầu tiên
                        if (dolech_X < 150 && rectDauTien.y < rect.y && rect.y < rectDauTien.y + rectDauTien.height
                            && rectDauTien.y < rectTiepTheo.y && rectTiepTheo.y < rectDauTien.y + rectDauTien.height) {
                            i += 2;
                            continue;
                        }
                    }
                }
                lstMatOfPoint.remove(i);
                lstRect.remove(i);
            } else {
                i++;
            }
        }
    }












    // CÁI NÀY LÀ LÚC CHƯA XỬ LÝ ẢNH GIỐNG BÊN PYTHON
    private void Process_1() {
//        Mat mat = new Mat(300, 300, CvType.CV_8UC3, new Scalar(150,89,98)); //Scalar là màu
//        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
//        Utils.matToBitmap(mat, bitmap);
//        imgViewAnhDaCrop.setImageBitmap(bitmap);

        ImageView imgGray = findViewById(R.id.imgGray);
        ImageView imgBlur = findViewById(R.id.imgBlur);
        ImageView imgThreshold = findViewById(R.id.imgThreshold);
        ImageView imgContour = findViewById(R.id.imgContour);
        ImageView imgBounding = findViewById(R.id.imgBounding);

        // "CHUYỂN BITMAP SANG MAT, THAO TÁC, RỒI CHUYỂN LẠI"
        Bitmap bitmap_origin = null;
        Mat mat_origin = new Mat();
        try {
            bitmap_origin = getBitmap(getContentResolver(), imageCroppedURI);
            Utils.bitmapToMat(bitmap_origin, mat_origin);  //chuyển dữ liệu tự bitmap đến mat
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Blur:
        Mat matBlur = new Mat();    // có thể nhận vào tham số (rows, cols, CvType.CV_8UC4) tương đương với (bitmap.getHeigh(), bitmap.getWidth(), CvType.CV_8UC4)
        // Origin -> Blur
        Imgproc.blur(mat_origin, matBlur, new Size(10,10)); // thao tác
        Bitmap bmBlur = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matBlur, bmBlur);
        imgBlur.setImageBitmap(bmBlur);

        // Ảnh xám:
        Mat matGray = new Mat();
        // Blur -> Gray
        Imgproc.cvtColor(matBlur, matGray, Imgproc.COLOR_BGR2GRAY); //thao tác
        Bitmap bmGray = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matGray, bmGray);
        imgGray.setImageBitmap(bmGray);

        // Threshold:
        Mat matThreshold = new Mat();
        // Gray -> Threshold
        Imgproc.threshold(matGray, matThreshold, 127, 255, Imgproc.THRESH_BINARY_INV); // pixel nào lớn hơn ngưỡng (127) thì gán giá trị "sáng nhất" của màu đó (255)
//        Imgproc.adaptiveThreshold(matGray, matThreshold, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 12);
        Bitmap bmThreshold = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matThreshold, bmThreshold);
        imgThreshold.setImageBitmap(bmThreshold);

        // Find Contours:
        List<MatOfPoint> lstMatOfPoint = new ArrayList<>(); // danh sách các contour
        Mat matHierarchy = new Mat();
        // RETR_EXTERNAL nghĩa là lấy contour bên ngoài, không tính contour bên trong, ví dụ: 6, 8, 9
        Imgproc.findContours(matThreshold, lstMatOfPoint, matHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE); // SIMPLE chỉ lưu những điểm cần thiết, NONE lưu hết
        // Sắp xếp các contours theo thứ tự trái sang phải:
        sortContours(lstMatOfPoint);
        // chuyển về kênh BGR để hiển thị được màu của Scalar
        // tham số -1 là vẽ tất cả contour
        // thickness quan trọng, nhờ nó lớn mà mình mới thấy được contour đã vẽ :), nếu thickness = -1 thì nó sẽ fill luôn lỗ của số 6, 8, 9,...
        Mat matContour = matThreshold.clone();  // clone để bảo toàn matThreshold
        Imgproc.cvtColor(matContour, matContour, Imgproc.COLOR_GRAY2BGR);
        Imgproc.drawContours(matContour, lstMatOfPoint, -1, new Scalar(0, 255, 0), 10);   // vẽ các contour lên matContour
        Bitmap bmContour = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matContour, bmContour);
        imgContour.setImageBitmap(bmContour);

        // THRESHOLD CHỮ ĐEN NỀN TRẮNG:
//        Imgproc.threshold(matGray, matThreshold, 127, 255, Imgproc.THRESH_BINARY);

        // Bounding Box Rectangle:
        List<Rect> lstRect = new ArrayList<>(); //chứa pixel của hình chữ nhật
        Mat matBounding = matContour.clone();
        for (int i = 0; i < lstMatOfPoint.size(); i++) {
            Rect rect = Imgproc.boundingRect(lstMatOfPoint.get(i));
            Imgproc.rectangle(matBounding, rect.tl(), rect.br(), new Scalar(255,0,0), 10); //vẽ
            lstRect.add(rect);
        }
        Bitmap bmBounding = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matBounding, bmBounding);
        imgBounding.setImageBitmap(bmBounding);

        // Segment Characters:
        List<Mat> lstMatCharacter = new ArrayList<>();  // chứa pixel của hình chữ nhật và bên trong
        List<Bitmap> lstBitMapCharacter = new ArrayList<>();    // danh sách ảnh các ký tự được tách
        LinearLayout linearLayoutChracters = findViewById(R.id.linearLayoutCharacters);
        for (int i = 0; i < lstMatOfPoint.size(); i++) {
            Mat matCharacter = segment(matThreshold, lstRect.get(i), lstMatOfPoint, i);
            Imgproc.resize(matCharacter, matCharacter, new Size(100,100), 0, 0, Imgproc.INTER_AREA);    // resize trước rồi add vào list
            lstMatCharacter.add(matCharacter);
            Bitmap bmCharacter = Bitmap.createBitmap(matCharacter.cols(), matCharacter.rows(), Bitmap.Config.ARGB_8888);
            lstBitMapCharacter.add(bmCharacter);
            Utils.matToBitmap(lstMatCharacter.get(i), lstBitMapCharacter.get(i));
            // Vẽ các ký tự ra layout:
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(lstBitMapCharacter.get(i));
            linearLayoutChracters.addView(imageView);
        }

        // NHẬN DẠNG CÁC KÝ TỰ RỒI CỘNG LẠI:
/*        String res = "";
        for (int i = 0; i < lstBitMapCharacter.size(); i++) {
            res += nhanDang(lstBitMapCharacter.get(i));
            if (i < lstBitMapCharacter.size()-1 && isExponential(lstRect.get(i), lstRect.get(i+1))) {
                res += "^";
            }
        }
        edtBieuThuc.setText(res);*/
//        nhanDang(lstBitMapCharacter.get(0));
    }
}
