package com.nguyenchiphong.nlumath_1.activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.nguyenchiphong.nlumath_1.R;
import com.nguyenchiphong.nlumath_1.dao.DatabaseHelper;
import com.softmoore.android.graphlib.Function;
import com.softmoore.android.graphlib.Graph;
import com.softmoore.android.graphlib.GraphView;
import com.udojava.evalex.Expression;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import katex.hourglass.in.mathlib.MathView;
import maes.tech.intentanim.CustomIntent;
import nlumath.LatexSolver;
import nlumath.PrintSolving;

public class SolveActivityShort extends AppCompatActivity {
    ImageView imgViewAnhDaCrop;
    Uri imageCroppedURI;
    String bieuThucGiongNoi, bieuThucLichSu;
    private String resSolve = "";   // dùng để giải
    private String resView = "";    // dùng để hiện KaTeX
    private boolean isHePT = false;
    LinearLayout linearLayoutChracters;
    EditText edtBieuThuc;
    Button btnGiaiSauKhiDaSua;
    TextView txtGiai, txtDoThiHamSo;
    MathView mathView;
    public static final String TAG = "AAA";
    DatabaseHelper databaseHelper;
    public static double A_Bac1 = 0;
    public static double B_Bac1 = 0;
    public static int bacDoThi = 0;

    private static final String MODEL_PATH = "model.tflite";
    private static final int NUM_CLASSES = 18;
    private static final String[] CLASSES = {"-", "+", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "x", "y", "z", "(", ")"};

//    private static final String MODEL_PATH = "model_20.tflite";
//    private static final int NUM_CLASSES = 20;
//    private static final String[] CLASSES = {"-", "+", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "x", "y", "z", "(", ")", "sqrt()", "{"};
    private boolean isGiongNoi = false;
    public static int REQUEST_VOICE_CODE = 4;
    // selected classifier information received from extras
    private boolean isQuant = false;
    // presets for rgb conversion
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    // input image dimensions for the Inception Model
    // CÁI NÀY GIỐNG VỚI input_shape CỦA MODEL:
    private final int DIM_IMG_SIZE_X = 100;
    private final int DIM_IMG_SIZE_Y = 100;
    private final int DIM_PIXEL_SIZE = 1;

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
        setContentView(R.layout.activity_solve_short);
        //getSupportActionBar().hide();

        databaseHelper = new DatabaseHelper(this);
        anhXa();
        drawGraph(); // phải bắt buộc vẽ trước nếu k sẽ lỗi GraphView
        layDuLieuIntent();
        khoiTaoCacThuocTinh();  // các thuộc tính phục vụ cho việc nhận dạng
        imgViewAnhDaCrop.setImageURI(imageCroppedURI);
        // xử lý giọng nói
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (bieuThucGiongNoi != null && !bieuThucGiongNoi.isEmpty()) {
                imgViewAnhDaCrop.setImageResource(R.drawable.abc1);
                resSolve = xuLyBieuThucGiongNoi(bieuThucGiongNoi);  // bỏ ký tự rỗng
                edtBieuThuc.setText(resSolve);
                isGiongNoi = true;
                suKien(isGiongNoi); // có giọng nói thì set sự kiện cho imageView luôn
                try {
                    clearGiaTriBien();
                    String latexBuocGiai = catBoKyHieu(phanGiai(resSolve));
                    mathView.setDisplayText(latexBuocGiai);
                    veDoThi(bacDoThi);
                } catch (Exception e) {
                    edtBieuThuc.setText(resSolve);  // dù sai nhưng cứ set biểu thức để cho người dùng sửa lại rồi giải
                    String mes = "\"Không giải được biểu thức do nhận dạng sai. Mời bạn vui lòng sửa lại biểu thức cho đúng!\"";
                    String s = "<span style=\"color: red;\" class=\"enclosing\">" + mes + "</span>";
                    mathView.setDisplayText(s);
                    e.printStackTrace();
                    drawGraph();
                }
                return; // giải giọng nói riêng rồi nên không để cho code MainProcess bên dưới chạy làm gì nữa
            }
        }
        // xử lý lịch sử
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (bieuThucLichSu != null && !bieuThucLichSu.isEmpty()) {
                imgViewAnhDaCrop.setImageResource(R.drawable.abc1);
                resSolve = xuLyBieuThucGiongNoi(bieuThucLichSu);  // bỏ ký tự rỗng
                edtBieuThuc.setText(resSolve);
                isGiongNoi = true;
                suKien(isGiongNoi); // có giọng nói thì set sự kiện cho imageView luôn
                try {
                    clearGiaTriBien();
                    String latexBuocGiai = catBoKyHieu(phanGiai(resSolve));
                    mathView.setDisplayText(latexBuocGiai);
                    veDoThi(bacDoThi);
                } catch (Exception e) {
                    edtBieuThuc.setText(resSolve);  // dù sai nhưng cứ set biểu thức để cho người dùng sửa lại rồi giải
                    String mes = "\"Không giải được biểu thức do nhận dạng sai. Mời bạn vui lòng sửa lại biểu thức cho đúng!\"";
                    String s = "<span style=\"color: red;\" class=\"enclosing\">" + mes + "</span>";
                    mathView.setDisplayText(s);
                    e.printStackTrace();
                    drawGraph();
                }
                return; // giải lịch sử riêng rồi nên không để cho code MainProcess bên dưới chạy làm gì nữa
            }
        }
        try {
            MainProcess();
        } catch (Exception e) {
            edtBieuThuc.setText(resSolve);  // dù sai nhưng cứ set biểu thức để cho người dùng sửa lại rồi giải
            String mes = "\"Không giải được biểu thức do nhận dạng sai. Mời bạn vui lòng sửa lại biểu thức cho đúng!\"";
            String s = "<span style=\"color: red;\" class=\"enclosing\">" + mes + "</span>";
            mathView.setDisplayText(s);
            e.printStackTrace();
            drawGraph();
        }
        suKien(isGiongNoi);
    }

    private String xuLyBieuThucGiongNoi(String bieuThucGiongNoi) {
        String res = bieuThucGiongNoi + " ";
        if (res.contains("phương trình bậc")) res = res.replace("phương trình một", "x^3");
        if (res.contains("thiết lập phương")) res = res.replace("thiết lập phương", "x^3");
        if (res.contains("căn ")) {
            if (!res.contains("căn bậc")) {
                res = res.replace("căn ", "căn bậc hai của ");
            }
        }
        if (res.contains("căn bậc 2 của ")) res = res.replace("căn bậc 2 của ", "căn bậc hai của "); // đổi như vậy để code bên dưới hoạt động
        if (res.contains("căn bậc hai của ")) {
            int index = res.indexOf("căn bậc hai của ");
            for (int i = index + 17; i < res.length(); i++) {
                if (res.charAt(i) == ' ') {
                    res = res.substring(0, i)
                            + ")"
                            + res.substring(i + 1);
                    break;
                }
            }
            res = res.replace("căn bậc hai của ", "sqrt(");
        }
        res = res.trim();
        char[] arrChar = res.toCharArray();
        res = "";
        for (char c : arrChar) {
            if (c != ' ') res += Character.toString(c);
        }
        // xong bước trên sẽ loại bỏ được khoảng trắng, bây giờ sẽ thay chữ bằng ký hiệu: vd mũ -> ^, cộng -> +
        if (res.contains("mũ")) res = res.replace("mũ", "^");
        if (res.contains("cộng")) res = res.replace("cộng", "+");
        if (res.contains("trừ")) res = res.replace("trừ", "-");
        if (res.contains("nhân")) res = res.replace("nhân", "*");
        if (res.contains("chia")) res = res.replace("chia", "/");
        if (res.contains("bằng")) res = res.replace("bằng", "=");
        if (res.contains("bìnhphương")) res = res.replace("bìnhphương", "^2");
        if (res.contains("bình")) res = res.replace("bình", "^2");
        if (res.contains("lậpphương")) res = res.replace("lậpphương", "^3");
        if (res.contains("phần")) res = res.replace("phần", "/");
        if (res.contains("trên")) res = res.replace("trên", "/");

        if (res.contains("không")) res = res.replace("không", "0");
        if (res.contains("một")) res = res.replace("một", "1");
        if (res.contains("hai")) res = res.replace("hai", "2");
        if (res.contains("ba")) res = res.replace("ba", "3");
        if (res.contains("bốn")) res = res.replace("bốn", "4");
        if (res.contains("năm")) res = res.replace("năm", "5");
        if (res.contains("sáu")) res = res.replace("sáu", "6");
        if (res.contains("bảy")) res = res.replace("bảy", "7");
        if (res.contains("tám")) res = res.replace("tám", "8");
        if (res.contains("chín")) res = res.replace("chín", "9");
        if (res.contains("mươi")) res = res.replace("mươi", "");
        if (res.contains("trăm")) res = res.replace("trăm", "");
        if (res.contains("nghìn")) res = res.replace("nghìn", "");
        if (res.contains("ngàn")) res = res.replace("ngàn", "");
        if (res.contains("ngìn")) res = res.replace("ngìn", "");
        if (res.contains("triệu")) res = res.replace("triệu", "");
        if (res.contains("tỷ")) res = res.replace("tỷ", "");
        if (res.contains("tỉ")) res = res.replace("tỉ", "");
        if (res.contains(".")) res = res.replace(".", "");
        if (!res.contains("bằng") && !res.contains("=")) {
            res = res.replace("x", "*");
            res = res.replace("X", "*");
        }

        // xử lý hệ phương trình:
        if (res.contains("phươngtrìnhmột")) res = res.replace("phươngtrìnhmột", "phươngtrình1");
        if (res.contains("phươngtrìnhhai")) res = res.replace("phươngtrìnhhai", "phươngtrình2");
        if (res.contains("phươngtrình1")) { // tức là có hệ phương trình
            res = res.replace("phươngtrình1", "");
            res = res.replace("phươngtrình2", ";");
        }

        return res;
    }

    // CÁI NÀY LÀ LÀM THEO GIỐNG BÊN PYTHON:
    private void MainProcess() {
        // MAIN PROCESS CHIA THÀNH 2 PHẦN:
        // PHẦN 1: XỬ LÝ ẢNH VÀ NHẬN DẠNG BIỂU THỨC TỪ ẢNH:
        String[] arrRes = phanNhanDang();
        resView += "$$" + arrRes[1] + "$$";
        resSolve = arrRes[0];
        // PHẦN 2: GIẢI BIỂU THỨC ĐÃ NHẬN DẠNG ĐƯỢC VÀ VẼ ĐỒ THỊ
        // Clear:
        clearGiaTriBien();
        resSolve = xuLy(resSolve);

        // SET CỨNG DỮ LIỆU ĐỂ TEST THÔI:
//        resSolve = "-2y+4z=3z-12;5z+y=10y-7"; isHePT = true;
//        resSolve = "-3x^3+6x^2-x-1=2x^2-3x+1";  //3 nghiệm
//        resSolve = "2/4x^2+sqrt(4)x-2=-3x^2+1/x^(-2)-(x-1)(x+2)";
//        resSolve = "2x^2-3x+1=0";
//        resSolve = "-2/4-6x+7(x-3)-sqrt(4)x=5+3sqrt(2^2)";
//        resSolve = "x/2-3x+1=0";
        String latexBuocGiai = catBoKyHieu(phanGiai(resSolve));
        edtBieuThuc.setText(resSolve);

//        mathView.setTextSize(18);   // mặc định của nó là 18
        if (isHePT) {
//            mathView.setTextSize(20);
        }
//        mathView.setDisplayText(resView);
        mathView.setDisplayText(latexBuocGiai);
        veDoThi(bacDoThi);
    }
    // Phương thức để thêm data vào lịch sử (HistoryActivity)
    public void addData(String name, String date) {
        // Nó sẽ gọi phương thức bên databaseHelper để xử lý
        boolean insertData = databaseHelper.insertData(name, date);
        if (insertData) {
            // toastMessage("Data Successfully Inserted!");
        } else {
            Toast.makeText(this, "Lỗi", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm lấy ngày giờ hiện tại
    public String getDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = sdf.format(new Date());
        return date;
    }

    /***LIST CÁC HÀM CỦA PHẦN GIẢI (CÓ VẼ ĐỒ THỊ)***/

    private String catBoKyHieu(String resPhanGiai) {
        if (resPhanGiai.contains(LatexSolver.KiHieuPhuongPhapTheCatBo))
            resPhanGiai = resPhanGiai.replace(LatexSolver.KiHieuPhuongPhapTheCatBo, "");
        if (resPhanGiai.contains(LatexSolver.KiHieuPhuongPhapTheKetQua))
            resPhanGiai = resPhanGiai.replace(LatexSolver.KiHieuPhuongPhapTheKetQua, "");
        return  resPhanGiai;
    }

    private String xuLyRiengChoMu3(String bieuThuc) {
        if (bieuThuc.contains("xx^2")) bieuThuc = bieuThuc.replace("xx^2", "x^3");
        if (bieuThuc.contains("yy^2")) bieuThuc = bieuThuc.replace("yy^2", "y^3");
        if (bieuThuc.contains("zz^2")) bieuThuc = bieuThuc.replace("zz^2", "z^3");
        if (bieuThuc.contains("x^2x")) bieuThuc = bieuThuc.replace("x^2x", "x^3");
        if (bieuThuc.contains("y^2y")) bieuThuc = bieuThuc.replace("y^2y", "y^3");
        if (bieuThuc.contains("z^2z")) bieuThuc = bieuThuc.replace("z^2z", "z^3");

        return bieuThuc;
    }

    private String xuLyRiengChoSQRTMu2(String bieuThuc) {
        if (bieuThuc.contains("sqrt(x^2)")) bieuThuc = bieuThuc.replace("sqrt(x^2)", "x");
        if (bieuThuc.contains("sqrt(y^2)")) bieuThuc = bieuThuc.replace("sqrt(y^2)", "y");
        if (bieuThuc.contains("sqrt(z^2)")) bieuThuc = bieuThuc.replace("sqrt(z^2)", "z");

        return bieuThuc;
    }

    /* Phần Giải: */
    private String phanGiai(String resSolve) {
        // KIỂM TRA THUỘC DẠNG NÀO THÌ GỌI HÀM GIẢI CỦA DẠNG ĐÓ:
        String bieuThuc = resSolve;
        // Xử lý trường hợp xx^2 -> x^3 hoặc x^2x -> x^3, còn trường hợp xx -> x^2 không cần làm vì gọi đến bậc nhất nó cũng tự nhân vào
        bieuThuc = xuLyRiengChoMu3(bieuThuc);
        // Xử lý trường hợp sqrt(x^2) thì không biết gọi tới bậc 2 hay bậc 2 => đổi sqrt(x^2) -> x trước khi giải!
        bieuThuc = xuLyRiengChoSQRTMu2(bieuThuc);

        String tenBien = xacDinhTenBien(bieuThuc);
        String latexBuocGiai = "";
        if (bieuThuc.contains(";")) isHePT = true;
        if (isHePT) {
            bacDoThi = 4;   // tạm giả sử bậc 4 là hệ pt
            latexBuocGiai = giaiHPTBacNhatHaiAn(bieuThuc);
            // khác 0 nghĩa là có giải
            if (PrintSolving.A_PhuongTrinh1 != 0 || PrintSolving.B_PhuongTrinh1 != 0 || PrintSolving.A_PhuongTrinh2 !=0 || PrintSolving.B_PhuongTrinh2 !=0) {
                addData(bieuThuc, getDateTime()); // lưu lịch sử
            }
            return latexBuocGiai;
        }

        // Tránh trường hợp nhận dạng sai ra nhiều hơn 1 biến làm crash app
        String[] arrBien = {"x", "y", "z"};
        int count = 0;
        for (String s : arrBien) {
            if (bieuThuc.contains(s)) count++;
        }
        if (count > 1) {
            String mes = "\"Không phải Hệ phương trình nhưng chứa nhiều hơn 1 biến nên không giải được. Mời bạn vui lòng sửa lại biểu thức cho đúng!\"";
            String s = "<span style=\"color: red;\" class=\"enclosing\">" + mes + "</span>";
            return s;
        }

        // Giải đơn lẻ (mũ 4,5,6,..) bằng cách lấy căn hai vế
        // Không cần thiết phải gọi nữa vì nếu không phải hệ pt, không chứa x^3, không chứa x^2, chứa x^4 nghĩa là chỉ chứa x thì nó sẽ gọi đến bậc nhất để giải
        /*if (bieuThuc.contains("x^4") || bieuThuc.contains("y^4") || bieuThuc.contains("z^4"))
            return new LatexSolver().solveUserLatex(bieuThuc, tenBien);
        if (bieuThuc.contains("x^5") || bieuThuc.contains("y^5") || bieuThuc.contains("z^5"))
            return new LatexSolver().solveUserLatex(bieuThuc, tenBien);
        if (bieuThuc.contains("x^6") || bieuThuc.contains("y^6") || bieuThuc.contains("z^6"))
            return new LatexSolver().solveUserLatex(bieuThuc, tenBien);*/

        if (bieuThuc.contains("x^3") || bieuThuc.contains("y^3") || bieuThuc.contains("z^3")) {
//            if (!(bieuThuc.contains("x^2") && bieuThuc.contains("y^2") && bieuThuc.contains("z^2") &&
//            bieuThuc.contains("x") && bieuThuc.contains("y") && bieuThuc.contains("z"))) {
//                return new LatexSolver().solveUserLatex(bieuThuc, tenBien);
//            }

            // Dù nó không chứa x^2 hay x thì mình vẫn cho nó giải bằng delta vì trong hàm giải mình có set hệ số để vẽ đồ thị
            bacDoThi = 3;
            latexBuocGiai = giaiPTBac3(bieuThuc, tenBien);
            if (PrintSolving.a_bac3 != 0 || PrintSolving.b_bac3 != 0 || PrintSolving.c_bac3 !=0 || PrintSolving.d_bac3 !=0) {
                addData(bieuThuc, getDateTime()); // lưu lịch sử
            }
            return latexBuocGiai;
        }
        if (bieuThuc.contains("x^2") || bieuThuc.contains("y^2") || bieuThuc.contains("z^2")) {
            bacDoThi = 2;
            latexBuocGiai = giaiPTBac2(bieuThuc, tenBien);
            if (PrintSolving.A_Bac2 != 0 || PrintSolving.B_Bac2 != 0 || PrintSolving.C_Bac2 !=0) {
                addData(bieuThuc, getDateTime()); // lưu lịch sử
            }
            return latexBuocGiai;
        }
        if (bieuThuc.contains("x") || bieuThuc.contains("y") || bieuThuc.contains("z")) {
            bacDoThi = 1;
            latexBuocGiai = giaiPTBac1(bieuThuc, tenBien);
            if (PrintSolving.A_Bac2 != 0 || PrintSolving.B_Bac2 != 0 || PrintSolving.C_Bac2 != 0) {
                bacDoThi = 2;
                addData(bieuThuc, getDateTime()); // lưu lịch sử
            } else {
                // Lấy giá trị A, B để vẽ đồ thị
                layGiaTriAB(PrintSolving.cacBuoc, PrintSolving.soChiaBoi2Ve);
                if (A_Bac1 != 0 || B_Bac1 != 0) {
                    addData(bieuThuc, getDateTime()); // lưu lịch sử
                }
            }
            return latexBuocGiai;
        } else {
            bacDoThi = 0;
            latexBuocGiai = giaiBieuThuc(bieuThuc);
            addData(bieuThuc, getDateTime()); // lưu lịch sử
            return latexBuocGiai;
        }
    }

    private String giaiHPTBacNhatHaiAn(String bieuThuc) {
        String[] arrPhuongTrinh = bieuThuc.split(";");
        String bien1 = "x", bien2 = "y";
        if (bieuThuc.contains("x")) {
            bien1 = "x";
            if (bieuThuc.contains("y")) bien2 = "y";
            else if (bieuThuc.contains("z")) bien2 = "z";
        } else if (bieuThuc.contains("y")) {
            bien1 = "y";
            bien2 = "z";
        }
        return new LatexSolver().solveLinearSystem2Var(arrPhuongTrinh[0], arrPhuongTrinh[1], bien1, bien2);
    }

    private String giaiPTBac3(String bieuThuc, String tenBien) {
        return new LatexSolver().solveCubicEquation(bieuThuc, tenBien);
    }

    private String giaiPTBac2(String bieuThuc, String tenBien) {
        return new LatexSolver().solveUserLatex(bieuThuc, tenBien);
    }

    private String giaiPTBac1(String bieuThuc, String tenBien) {
        String res = new LatexSolver().solveUserLatex(bieuThuc, tenBien);
        return res;
    }

    private String giaiBieuThuc(String bieuThuc) {
        String res = "Kết quả bằng: " + new Expression(bieuThuc).eval() + "$\\newline$" + new Expression(bieuThuc).sb + new Expression(bieuThuc).delete();
        if (res.contains(".")) res = res.replace(".", ",");
        return res;
    }

    /***các hàm phụ khác***/

    private void clearGiaTriBien() {
        isHePT = false;
        SolveActivityShort.bacDoThi = 0;
        SolveActivityShort.A_Bac1 = 0;
        SolveActivityShort.B_Bac1 = 0;

        PrintSolving.cacBuoc.clear();
        PrintSolving.soChiaBoi2Ve = 1;

        PrintSolving.soNghiemPTBac2 = -1;
        PrintSolving.A_Bac2 = 0;
        PrintSolving.B_Bac2 = 0;
        PrintSolving.C_Bac2 = 0;

        PrintSolving.a_bac3 = 0;
        PrintSolving.b_bac3 = 0;
        PrintSolving.c_bac3 = 0;
        PrintSolving.d_bac3 = 0;

        PrintSolving.arr6HeSo = new double[6];
        PrintSolving.A_PhuongTrinh1 = 0;
        PrintSolving.B_PhuongTrinh1 = 0;
        PrintSolving.A_PhuongTrinh2 = 0;
        PrintSolving.B_PhuongTrinh2 = 0;
    }

    private String xuLy(String resSolve) {
        String chuoiTraVe = resSolve;
        if (chuoiTraVe.contains("^x")) {
            chuoiTraVe = chuoiTraVe.replace("^x", "^2");
        }
        if (chuoiTraVe.contains("xy")) {
            chuoiTraVe = chuoiTraVe.replace("xy", "2y");
        }
        if (chuoiTraVe.contains("xx")) {
            chuoiTraVe = chuoiTraVe.replace("xx", "2x");
        }
        if (chuoiTraVe.contains("^(")) {
            String mau = "\\^(.+)";
            Pattern pattern = Pattern.compile(mau);
            Matcher matcher = pattern.matcher(chuoiTraVe);
            while (matcher.find()) {
                String old = matcher.group();
                String _new = "";
                if (old.contains("x")) _new = old.replace("x", "2");
                chuoiTraVe = chuoiTraVe.replace(old, _new);
            }
        }
        if (chuoiTraVe.contains("x^8")) {
            chuoiTraVe = chuoiTraVe.replace("x^8", "x^3");
        }
        if (chuoiTraVe.contains("y^8")) {
            chuoiTraVe = chuoiTraVe.replace("y^8", "y^3");
        }
        if (chuoiTraVe.contains("z^8")) {
            chuoiTraVe = chuoiTraVe.replace("z^8", "z^3");
        }
        if (chuoiTraVe.contains("sqrt(x)")) {
            chuoiTraVe = chuoiTraVe.replace("sqrt(x)", "sqrt(2)");
        }
        if (chuoiTraVe.contains("1/x")) {
            chuoiTraVe = chuoiTraVe.replace("1/x", "1/2");
        }
        if (chuoiTraVe.contains("^y")) {
            chuoiTraVe = chuoiTraVe.replace("^y", "^3");
        }
        if (chuoiTraVe.contains("^00000")) {
            chuoiTraVe = chuoiTraVe.replace("^00000", "");
        }
        if (chuoiTraVe.contains("^0000")) {
            chuoiTraVe = chuoiTraVe.replace("^0000", "");
        }
        if (chuoiTraVe.contains("^000")) {
            chuoiTraVe = chuoiTraVe.replace("^000", "");
        }
        if (chuoiTraVe.contains("^00")) {
            chuoiTraVe = chuoiTraVe.replace("^00", "");
        }
        if (chuoiTraVe.contains("^0")) {
            chuoiTraVe = chuoiTraVe.replace("^0", "");
        }

        return chuoiTraVe;
    }

    private String xacDinhTenBien(String bieuThuc) {
        if (bieuThuc.contains("x")) {
            return "x";
        } else if (bieuThuc.contains("y")) {
            return "y";
        } else if (bieuThuc.contains("z")) {
            return "z";
        } else return "";
    }

    private void layGiaTriAB(List<String> cacBuoc, double soChiaBoi2Ve) {
        String buocCuoi = cacBuoc.get(cacBuoc.size() - 1);    // chứa kết quả, vd: x = -26
        if (buocCuoi.contains(",")) buocCuoi = buocCuoi.replaceAll(",", ".");
        String[] arr = buocCuoi.split(" = "); // vd: 6x = 3
        if (arr[1].startsWith("-")) {
            arr[1] = arr[1].replace('-', '+');// vd: -26 => 26
            B_Bac1 = Double.parseDouble(arr[1]);
        } else {
            B_Bac1 = Double.parseDouble("-" + arr[1]); // vd: 26 => -26
        }
        if (soChiaBoi2Ve == 1) {    // vd: x = -26
            A_Bac1 = 1;
        } else {    // soChiaBoi2Ve khác 1 (nghĩa là nó có chia)
            A_Bac1 = soChiaBoi2Ve;
            B_Bac1 = B_Bac1 * soChiaBoi2Ve;
        }
    }

    // chỉ gọi hàm này khi nhận dạng sai hoặc k có đồ thị
    public void drawGraph() {
        Graph graph = new Graph.Builder()
                .addFunction(new Function() {
                    @Override
                    public double apply(double x) {
                        return 0;
                    }
                }, Color.RED)
                .setWorldCoordinates(-11, 11, -11, 11)
                .setXTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                .setYTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                .build();
        GraphView graphView = findViewById(R.id.graph_view);
        txtDoThiHamSo.setText("Không vẽ được đồ thị");
        graphView.setGraph(graph);
    }

    private void veDoThi(int bacDoThi) {
        if (bacDoThi == 0) {
            drawGraph();
        }
        if (bacDoThi == 1) {
            Graph graph = new Graph.Builder()
                    .addFunction(new Function() {
                        @Override
                        public double apply(double x) {
                            return SolveActivityShort.A_Bac1 * x + SolveActivityShort.B_Bac1;
                        }
                    }, Color.RED)
                    .setWorldCoordinates(-11, 11, -11, 11)
                    .setXTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .setYTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .build();
            GraphView graphView = findViewById(R.id.graph_view);
            graphView.setGraph(graph);
            txtDoThiHamSo.setText("Đồ thị hàm số: " + resSolve);
        }
        if (bacDoThi == 2) {
            Graph graph = new Graph.Builder()
                    .addFunction(new Function() {
                        @Override
                        public double apply(double x) {
                            return PrintSolving.A_Bac2 * x * x + PrintSolving.B_Bac2 * x + PrintSolving.C_Bac2;
                        }
                    }, Color.RED)
                    .setWorldCoordinates(-11, 11, -11, 11)
                    .setXTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .setYTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .build();
            GraphView graphView = findViewById(R.id.graph_view);
            graphView.setGraph(graph);
            txtDoThiHamSo.setText("Đồ thị hàm số: " + resSolve);
        }
        if (bacDoThi == 3) {
            Graph graph = new Graph.Builder()
                    .addFunction(new Function() {
                        @Override
                        public double apply(double x) {
                            return PrintSolving.a_bac3 * x * x * x + PrintSolving.b_bac3 * x * x + +PrintSolving.c_bac3 * x + PrintSolving.d_bac3;
                        }
                    }, Color.RED)
                    .setWorldCoordinates(-11, 11, -11, 11)
                    .setXTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .setYTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .build();
            GraphView graphView = findViewById(R.id.graph_view);
            graphView.setGraph(graph);
            txtDoThiHamSo.setText("Đồ thị hàm số: " + resSolve);
        }
        if (bacDoThi == 4) {
            Graph graph = new Graph.Builder()
                    .addFunction(new Function() {
                        @Override
                        public double apply(double x) {
                            return PrintSolving.A_PhuongTrinh1 * x + PrintSolving.B_PhuongTrinh1;
                        }
                    }, Color.RED)
                    .addFunction(new Function() {
                        @Override
                        public double apply(double x) {
                            return PrintSolving.A_PhuongTrinh2 * x + PrintSolving.B_PhuongTrinh2;
                        }
                    }, Color.BLUE)
                    .setWorldCoordinates(-11, 11, -11, 11)
                    .setXTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .setYTicks(new double[]{-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
                    .build();
            GraphView graphView = findViewById(R.id.graph_view);
            graphView.setGraph(graph);
//            txtDoThiHamSo.setText("Đồ thị hàm số: " + resSolve);
            Spannable word = new SpannableString("Đồ thị hàm số: " + resSolve + "\n" + "(đỏ: phương trình 1");

            word.setSpan(new ForegroundColorSpan(Color.RED), 0, word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            txtDoThiHamSo.setText(word);
            Spannable wordTwo = new SpannableString("\nxanh: phương trình 2)");

            wordTwo.setSpan(new ForegroundColorSpan(Color.BLUE), 0, wordTwo.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            txtDoThiHamSo.append(wordTwo);
        }
    }

    /***LIST CÁC HÀM CỦA PHẦN NHẬN DẠNG (CÓ BAO GỒM XỬ LÝ ẢNH)***/

    private String[] phanNhanDang() {
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
        // THẰNG BLUR NÀY LÀM GIẢM ĐỘ CHÍNH XÁC!!!
        // Blur:
        Mat matBlur = new Mat();    // có thể nhận vào tham số (rows, cols, CvType.CV_8UC4) tương đương với (bitmap.getHeigh(), bitmap.getWidth(), CvType.CV_8UC4)
        // Origin -> Blur
        Imgproc.blur(mat_origin, matBlur, new Size(10,10)); // thao tác
//        Bitmap bmBlur = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matBlur, bmBlur);
//        imgBlur.setImageBitmap(bmBlur);
*/

        // Ảnh xám:
        Mat matGray = new Mat();
        // Blur -> Gray
        Imgproc.cvtColor(mat_origin, matGray, Imgproc.COLOR_BGR2GRAY); // thao tác
//        Bitmap bmGray = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matGray, bmGray);
//        imgGray.setImageBitmap(bmGray);

        // Threshold:
        Mat matThreshold = new Mat();
        // Gray -> Threshold
        Imgproc.threshold(matGray, matThreshold, 90, 255, Imgproc.THRESH_BINARY_INV); // pixel nào lớn hơn ngưỡng (127) thì gán giá trị "sáng nhất" của màu đó (255)
//        Imgproc.adaptiveThreshold(matGray, matThreshold, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 12);
//        Bitmap bmThreshold = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matThreshold, bmThreshold);
//        imgThreshold.setImageBitmap(bmThreshold);

        // Dilate:
        Mat matDilate = new Mat();
        // Threshold -> Dilate
        Imgproc.dilate(matThreshold, matDilate, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 1)));
//        Bitmap bmDilate = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matDilate, bmDilate);
//        imgDilate.setImageBitmap(bmDilate);

        // Find Contours:
        List<MatOfPoint> lstMatOfPoint = new ArrayList<>(); // danh sách các contour
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
        Imgproc.drawContours(matContour, lstMatOfPoint, -1, new Scalar(0, 255, 0), 10);   // vẽ các contour lên matContour
//        Bitmap bmContour = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matContour, bmContour);
//        imgContour.setImageBitmap(bmContour);

        // Bounding Box Rectangle:
        List<Rect> lstRect = new ArrayList<>(); // chứa pixel của hình chữ nhật
        for (int i = 0; i < lstMatOfPoint.size(); i++) {
            Rect rect = Imgproc.boundingRect(lstMatOfPoint.get(i));

            lstRect.add(rect);
        }
        Mat matBounding = matContour.clone();
        // LỌC CONTOURS không phải ký tự rồi vẽ các CONTOURS là ký tự:
        chonLocContours(lstMatOfPoint, lstRect);
        for (int i = 0; i < lstMatOfPoint.size(); i++) {
            Imgproc.rectangle(matBounding, lstRect.get(i).tl(), lstRect.get(i).br(), new Scalar(255, 0, 0), 10); // vẽ
        }
//        Bitmap bmBounding = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(matBounding, bmBounding);
//        imgBounding.setImageBitmap(bmBounding);

/*// PHẦN NÀY KHÔNG CẦN NỮA!:
        // THRESHOLD CHỮ ĐEN NỀN TRẮNG:
        Imgproc.threshold(matGray, matThreshold, 90, 255, Imgproc.THRESH_BINARY);
        Bitmap bmThreshold_ChuDenNenTrang = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matThreshold, bmThreshold_ChuDenNenTrang);
//        imgThreshold_ChuDenNenTrang.setImageBitmap(bmThreshold_ChuDenNenTrang);

        // Erode:
        Mat matErode = new Mat();
        // Threshold -> Erode
        Imgproc.erode(matThreshold, matErode, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 1)));
        Bitmap bmErode = Bitmap.createBitmap(mat_origin.cols(), mat_origin.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matErode, bmErode);
//        imgErode.setImageBitmap(bmErode);
*/
        String[] arrRes = nhanDangToanBieuThuc(matDilate, lstRect, lstMatOfPoint);
        return arrRes;
    }

    /* Nhận dạng toàn bộ biểu thức */
    private String[] nhanDangToanBieuThuc(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint) {
        String[] arrRes = new String[2];    // phần tử 0 là res Solve, 1 là res View
        String resSolve = "";
        String resView = "";
        // Segment Characters:
        List<Mat> lstMatCharacter = new ArrayList<>();  // chứa pixel của hình chữ nhật và bên trong
        List<Bitmap> lstBitMapCharacter = new ArrayList<>();    // danh sách ảnh các ký tự được tách

        for (int i = 0; i < lstMatOfPoint.size(); ) {
            // Kiểm tra dấu mốc đơn (hệ pt) dựa vào ratio và 2 dấu =
            if (i == 0) {   // CHẤP NHẬN CHỈ XÉT CONTOUR ĐẦU TIÊN THÔI ĐỂ APP CHẠY TỐT HƠN
//            if (i < lstMatOfPoint.size()-1) {
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
            if (i < lstMatOfPoint.size() - 1) {
                if (isCan(lstRect, lstMatOfPoint, i)) {
                    // loại căn ra khỏi lstMatOfPoint
                    // nhận dạng căn rồi cộng riêng vào res
                    String[] arrValue = nhanDangCan(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra phân số (bao gồm dấu : có gạch ngang)
            if (i < lstMatOfPoint.size() - 1) {
                if (isPhanSo(lstRect, lstMatOfPoint, i)) {
                    // loại phân số ra khỏi lstMatOfPoint
                    // nhận dạng phân số rồi cộng riêng vào res
                    String[] arrValue = nhanDangPhanSo(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra dấu "=". LUÔN ĐẶT CÁI NÀY SAU KIỂM TRA PHÂN SỐ
            if (i < lstMatOfPoint.size() - 1) {
                if (isDauBang(lstRect, lstMatOfPoint, i)) {
                    // loại dấu bằng ra khỏi lstMatOfPoint
                    // nhận dạng dấu bằng rồi cộng riêng vào res
                    if (resSolve.endsWith("^")) {
                        resSolve = resSolve.substring(0, resSolve.length() - 1);
                    }
                    if (resView.endsWith("^")) {
                        resView = resView.substring(0, resView.length() - 1);
                    }
                    String[] arrValue = nhanDangDauBang(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra dấu chia ":"
            if (i < lstMatOfPoint.size() - 1) {
                if (isDauChia(lstRect, lstMatOfPoint, i)) {
                    // loại dấu chia ra khỏi lstMatOfPoint
                    // nhận dạng dấu chia rồi cộng riêng vào res
                    if (resSolve.endsWith("^")) {
                        resSolve = resSolve.substring(0, resSolve.length() - 1);
                    }
                    if (resView.endsWith("^")) {
                        resView = resView.substring(0, resView.length() - 1);
                    }
                    String[] arrValue = nhanDangDauChia(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            // Kiểm tra mũ:
            if (i < lstMatOfPoint.size() - 1) {
                if (isMu(matSrc, lstRect, lstMatOfPoint, i)) {
                    // loại các ký tự là mũ ra khỏi lstMatOfPoint (vì mình sẽ nhận dạng nó ở hàm nhanDangMu())
                    // nhận dạng mũ rồi cộng riêng vào res
                    String[] arrValue = nhanDangMu(matSrc, lstRect, lstMatOfPoint, i);
                    resSolve += arrValue[0];
                    resView += arrValue[1];
                    continue;
                }
            }

            Mat matCharacter = segment(matSrc, lstRect.get(i), lstMatOfPoint, i);
            Imgproc.resize(matCharacter, matCharacter, new Size(100, 100), 0, 0, Imgproc.INTER_AREA);    // resize trước rồi add vào list; INTER_AREA thích hợp cho thu nhỏ ảnh
            lstMatCharacter.add(matCharacter);
            Bitmap bmCharacter = Bitmap.createBitmap(matCharacter.cols(), matCharacter.rows(), Bitmap.Config.ARGB_8888);
            lstBitMapCharacter.add(bmCharacter);
            Utils.matToBitmap(lstMatCharacter.get(i), lstBitMapCharacter.get(i));

            // NHẬN DẠNG:
            String kyTu = nhanDang(bmCharacter);
            resSolve += kyTu;
            resView += kyTu;

            // Vẽ các ký tự ra layout:
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(lstBitMapCharacter.get(i));
//            linearLayoutChracters.addView(imageView);

            i++;
        }
        arrRes[0] = resSolve;
        arrRes[1] = resView;
        return arrRes;
    }

    private String nhanDangMotKyTu(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Mat matCharacter = segment(matSrc, lstRect.get(i), lstMatOfPoint, i);
        Imgproc.resize(matCharacter, matCharacter, new Size(100, 100), 0, 0, Imgproc.INTER_AREA);    // resize trước rồi add vào list; INTER_AREA thích hợp cho thu nhỏ ảnh
        Bitmap bmCharacter = Bitmap.createBitmap(matCharacter.cols(), matCharacter.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matCharacter, bmCharacter);

        String kyTu = nhanDang(bmCharacter);
        return kyTu;
    }

    /***Nhận dạng hệ phương trình***/

    List<MatOfPoint> lstContourPhuongTrinh1 = new ArrayList<>();
    List<Rect> lstRectPhuongTrinh1 = new ArrayList<>();
    List<MatOfPoint> lstContourPhuongTrinh2 = new ArrayList<>();
    List<Rect> lstRectPhuongTrinh2 = new ArrayList<>();

    private boolean isHePhuongTrinh(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        float ratio = (float) rect.height / rect.width;
        if (ratio >= 2) {
            // Tính đường line ở giữa
            int line = rect.y + rect.height / 2;
            // Chia list contours thành 2 phần trên và dưới line
            for (int k = i + 1; k < lstMatOfPoint.size(); k++) {
                Rect rectTiepTheo = lstRect.get(k);
                int diemGiuaKyTu = rectTiepTheo.y + rectTiepTheo.height / 2;
                if (diemGiuaKyTu < line) {  // nằm trên line
                    lstContourPhuongTrinh1.add(lstMatOfPoint.get(k));
                    lstRectPhuongTrinh1.add(lstRect.get(k));
                } else if (diemGiuaKyTu > line) {// nằm dưới line
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
                for (int k = i; k < lstMatOfPoint.size(); ) {
                    Rect rectTiepTheo = lstRect.get(k);
                    int diemGiuaKyTu = rectTiepTheo.y + rectTiepTheo.height / 2;
                    if (diemGiuaKyTu < line) {  // nằm trên line
                        lstMatOfPoint.remove(k);
                        lstRect.remove(k);
                        continue;
                    } else if (diemGiuaKyTu > line) {   // nằm dưới line
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
        String[] arrValuePT1 = nhanDangToanBieuThuc(matSrc, lstRectPhuongTrinh1, lstContourPhuongTrinh1);
        String[] arrValuePT2 = nhanDangToanBieuThuc(matSrc, lstRectPhuongTrinh2, lstContourPhuongTrinh2);
        isHePT = true;
        String[] arrValue = new String[2];
        String hePT = "";
        hePT += arrValuePT1[0];
        hePT += ";"; // phân cách 2 pt bằng dấu ;
        hePT += arrValuePT2[0];
        // cho resSolve
        arrValue[0] = hePT;
        // cho res View
        arrValue[1] = "\\begin{cases}" + arrValuePT1[1] + " \\\\" + arrValuePT2[1] + "\\end{cases}";
        return arrValue;
    }

    /***Nhận dạng căn***/

    private boolean isCan(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        Rect rectTiepTheo = lstRect.get(i + 1);
        // Đã sửa lại (14-09-2020): dựa vào điểm giữa trên chứ không phải góc phải trên hay góc trái trên
        double diemGiuaTren = rectTiepTheo.x + rectTiepTheo.width / 2.0;
        if (rect.x < diemGiuaTren && diemGiuaTren < rect.x + rect.width) {
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

        String[] arrValueTmp = nhanDangToanBieuThuc(matSrc, lstRectTrongCan, lstContourTrongCan);

        String[] arrValue = new String[2];
        arrValue[0] = "sqrt(" + arrValueTmp[0] + ")";
        arrValue[1] = "\\sqrt{" + arrValueTmp[1] + "}";
        return arrValue;
    }

    /***Nhận dạng mũ***/

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
            Rect rectKyTu = lstRect.get(i - 1);
            Rect rectTiepTheo1 = lstRect.get(i + 1);
            Rect rectTiepTheo2 = lstRect.get(i + 2);
            // Kiểm tra độ cao:
            if (r2_laMuCua_r1(rectKyTu, rectTiepTheo1) && r2_laMuCua_r1(rectKyTu, rectTiepTheo2)) {
                isNgoacDonMo = true;
                return false;    // chấp nhận ( là thuộc mũ chứ không phải dấu phép tính
            } else return true;    // chỉ coi nó là dấu của phép tính
        }
        boolean isDau = false;
        String[] lstDau = {"*", "/", "=", "{", "!", ")"};
        for (int j = 0; j < lstDau.length; j++) {
            if (kyTu.equalsIgnoreCase(lstDau[j])) {
                isDau = true;
            }
        }
        return isDau;
    }

    float t = 0.5f;    // 2/3

    private boolean r2_laMuCua_r1(Rect r1, Rect r2) {
        if ((r1.y - r1.height * 2) < (r2.y + r2.height) && (r2.y + r2.height) < (r1.y + r1.height * t)) {  // nghĩa là, điểm trái dưới của r2 phải nằm trên 1/3 của r1 và dưới r1.y - r1.height
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
        for (int j = 0; j < lstDau.length; j++) {
            if (kyTu.equalsIgnoreCase(lstDau[j])) {
                return false;
            }
        }

        // Nếu contour tiếp theo là dấu thì sẽ không có mũ cho contour hiện tại
        boolean isDau_ContourTiepTheo = isDau(matSrc, lstRect, lstMatOfPoint, i + 1); // LƯU Ý: i+1
        if (isDau_ContourTiepTheo) return false;
        // Còn không thì xét độ cao của contour tiếp theo để biết có mũ hay không
        if (r2_laMuCua_r1(lstRect.get(i), lstRect.get(i + 1))) {
            return true;    // có mũ
        }
        return false;
    }

    private String[] nhanDangMu(Mat matSrc, List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        String res = "";
        List<MatOfPoint> lstMu = new ArrayList<>();
        List<Rect> lstRectMu = new ArrayList<>();
        res += nhanDangMotKyTu(matSrc, lstRect, lstMatOfPoint, i);  // nhận dạng cơ số
        Rect rectKyTu = lstRect.get(i); // lấy nó ra rồi mới remove
        lstMatOfPoint.remove(i);
        lstRect.remove(i);
        Rect rectTruocDo = new Rect();
        int l = 0;
        // Xét đến khi phát hiện đã hết mũ thì dừng:
        for (int k = i; k < lstMatOfPoint.size(); ) {
            Rect rectTiepTheo = lstRect.get(k);
            if (r2_laMuCua_r1(rectKyTu, rectTiepTheo)) {
                if (l != 0) {   // tức là đang xét từ contour tiếp theo thứ 2 trở đi.
                    // Sẽ lấy contour này so sánh với contour trước đó. Tránh trường hợp sai giống như trong hình 8y^(3-)
                    float ratioRectTruocDo = (float) rectTruocDo.width / rectTruocDo.height;
                    if (ratioRectTruocDo > 2) { // nghi là dấu -
                        double diemGiua = rectTruocDo.y + rectTruocDo.height / 2.0;  // điểm giữa của dấu -
                        if (!(rectTiepTheo.y < diemGiua && diemGiua < rectTiepTheo.y + rectTiepTheo.height)) {
                            break;
                        }
                    } else {
                        double diemGiua = rectTiepTheo.y + rectTiepTheo.height / 2.0;
                        if (!(rectTruocDo.y < diemGiua && diemGiua < rectTruocDo.y + rectTruocDo.height)) {
                            break;
                        }
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

        String[] arrValueTmp = nhanDangToanBieuThuc(matSrc, lstRectMu, lstMu);
        String mu = arrValueTmp[0];    // resSolve
        if (mu.length() == 1) {
            res += mu;
        } else if (!isNgoacDonMo) {
            res += "(" + mu + ")";  // mũ k có ( thì mới cộng
        } else {
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

    /***Nhận dạng phân số***/

    private boolean isPhanSo(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        float ratio = (float) rect.width / rect.height;
        if (ratio > 3 && i < lstMatOfPoint.size() - 2) {
            Rect rectTiepTheo1 = lstRect.get(i + 1);
            Rect rectTiepTheo2 = lstRect.get(i + 2);
            // 7-9-2020: sửa lại xác định dựa vào điểm giữa (x + width/2) chứ không phải theo góc trái trên (x) nữa
            double diemGiuaTren1 = rectTiepTheo1.x + rectTiepTheo1.width / 2.0;
            double diemGiuaTren2 = rectTiepTheo2.x + rectTiepTheo2.width / 2.0;
            if (rect.x <= diemGiuaTren1 && diemGiuaTren1 <= (rect.x + rect.width)) {
                if (rect.x <= diemGiuaTren2 && diemGiuaTren2 <= (rect.x + rect.width)) {
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
        for (int j = i; j < lstMatOfPoint.size(); ) {
            Rect rectTiepTheo = lstRect.get(j);
            double diemGiuaTren = rectTiepTheo.x + rectTiepTheo.width / 2.0;
            if ((rectTiepTheo.y + rectTiepTheo.height) < (rect.y + rect.height) && rect.x < diemGiuaTren && diemGiuaTren < rect.x + rect.width) {
                lstTuSo.add(lstMatOfPoint.get(j));
                lstRectTuSo.add(lstRect.get(j));
                lstMatOfPoint.remove(j);
                lstRect.remove(j);
                continue;   // bởi vì mình đã remove nên j tự động tăng nên mình không cần cộng j lên!
            }
            if ((rectTiepTheo.y) > (rect.y + rect.height) && rect.x < diemGiuaTren && diemGiuaTren < rect.x + rect.width) {
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
        if (lstTuSo.size() == 1 && lstMauSo.size() == 1) {
            Rect rectTuSo = lstRectTuSo.get(0);
            Rect rectMauSo = lstRectMauSo.get(0);
            int areaTuSo = rectTuSo.height * rectTuSo.width;
            int areaMauSo = rectMauSo.height * rectMauSo.width;
            if (areaTuSo < 5000 && areaMauSo < 5000) {
                int dolech_X = Math.abs(rectTuSo.x - rectMauSo.x);
                if (dolech_X < 500) {
                    arrValue[0] = "/";  // resSolve dùng dấu / để dễ tính
                    arrValue[1] = ":";  // resView dùng : để hiện cho đúng với người dùng nhập
                    return arrValue;
                }
            }
        }

        res += "(";

        // Nhận dạng tử số
        String[] arrValueTuSo = nhanDangToanBieuThuc(matSrc, lstRectTuSo, lstTuSo);
        if (lstRectTuSo.size() == 1) { // chỉ có 1 phần tử thì không cần + dấu ()
            res += arrValueTuSo[0];
        } else res += "(" + arrValueTuSo[0] + ")";

        res += "/";

        // Nhận dạng mẫu số
        String[] arrValueMauSo = nhanDangToanBieuThuc(matSrc, lstRectMauSo, lstMauSo);
        if (lstRectMauSo.size() == 1) {
            res += arrValueMauSo[0];
        } else res += "(" + arrValueMauSo[0] + ")";

        res += ")";
        // cho resSolve
        arrValue[0] = res;
        // cho res View
        arrValue[1] = "\\frac{" + arrValueTuSo[1] + "}" + "{" + arrValueMauSo[1] + "}";
        return arrValue;
    }

    /***Nhận dạng dấu =***/

    private boolean isDauBang(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        float ratio1 = (float) rect.width / rect.height;
        Rect rectTiepTheo = lstRect.get(i + 1);
        float ratio2 = (float) rectTiepTheo.width / rectTiepTheo.height;
        if (ratio1 > 2 && ratio2 > 2) { // kiểm tra nó có phải là 2 dấu trừ
            if (rect.x <= rectTiepTheo.x && rectTiepTheo.x <= rect.x + rect.width / 2.0) {  // dùng dấu <= là bởi vì có thể có trường hợp 2 x của chúng bằng nhau (biểu thức in trong sách)
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

    /***Nhận dạng dấu chia : và -:-***/

    private boolean isDauChia(List<Rect> lstRect, List<MatOfPoint> lstMatOfPoint, int i) {
        Rect rect = lstRect.get(i);
        int areaContour = rect.width * rect.height;
        if (areaContour < 5000) {   // giới hạn 5000 là bởi vì dấu . thường không lớn quá 5000, nếu lớn hơn rất có thể nó là ký tự chứ k phải dấu .
            if (0 < i && i < lstMatOfPoint.size() - 1) {
                Rect rectDauTien = lstRect.get(0);
                Rect rectTiepTheo = lstRect.get(i + 1);
                int areaContourTiepTheo = rectTiepTheo.width * rectTiepTheo.height;
                if (areaContourTiepTheo < 5000) {   // tới đây xác định đc là 2 dấu .
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
        // remove 2 dấu . (dấu : mình nhận dạng thủ công chứ k phải dùng model)
        lstMatOfPoint.remove(i);
        lstMatOfPoint.remove(i);
        lstRect.remove(i);
        lstRect.remove(i);
        String[] arrValue = new String[2];
        arrValue[0] = "/";  // resSolve dùng dấu / để dễ tính
        arrValue[1] = ":";  // resView dùng : để hiện cho đúng với người dùng nhập
        return arrValue;
    }

    /***3 hàm khác***/

    private Mat segment(Mat matSrc, Rect rect, List<MatOfPoint> lstMatOfPoint, int i) {
//        matSrc truyền vào là matDilate
//        rect.x = rect.x - padding;
//        rect.y = rect.y - padding;
//        rect.width = rect.width + p;
//        rect.height = rect.height + p;

        // Dùng MASK (tránh trường hợp cắt dính phải ký tự khác):
        Mat mask = new Mat(matSrc.rows(), matSrc.cols(), CvType.CV_8UC1, Scalar.all(0));
        Imgproc.drawContours(mask, lstMatOfPoint, i, new Scalar(255, 255, 255), -1);   // vẽ các contour lên matContour

        Mat temp = new Mat();
        matSrc.copyTo(temp, mask);
        Mat matCharacter = temp.submat(rect);   // chữ trắng nền đen
        Imgproc.threshold(matCharacter, matCharacter, 90, 255, Imgproc.THRESH_BINARY_INV);  // chữ đen nền trắng

//        // Vẽ các ký tự ra layout:
//        Bitmap bmTemp = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(temp, bmTemp);
//        ImageView imageView = new ImageView(this);
//        imageView.setImageBitmap(bmTemp);
//        linearLayoutChracters.addView(imageView);

//        Mat matCharacter = matSrc.submat(rect);

        // Padding
//        int p = 7;
//        Core.copyMakeBorder(matCharacter, matCharacter, 0, p, 0, p, Core.BORDER_ISOLATED, new Scalar(255,255,255));

        // CÁCH NÀY TỐT HƠN NHIỀU:
        if (rect.height > rect.width) {
            Core.copyMakeBorder(matCharacter, matCharacter, 0, 0, (rect.height - rect.width) / 2, (rect.height - rect.width) / 2, Core.BORDER_ISOLATED, new Scalar(255, 255, 255));
        } else if (rect.height < rect.width) {
            Core.copyMakeBorder(matCharacter, matCharacter, (rect.width - rect.height) / 2, (rect.width - rect.height) / 2, 0, 0, Core.BORDER_ISOLATED, new Scalar(255, 255, 255));
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
            // Sắp xếp các contours "tăng dần" theo x (chiều ngang)
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                Rect rect1 = Imgproc.boundingRect(o1);
                Rect rect2 = Imgproc.boundingRect(o2);
                int result = 0;
//                double total = rect1.tl().y/rect2.tl().y;
//                if (total>=0.9 && total<=1.4 ) {
                result = Double.compare(rect1.tl().x, rect2.tl().x);
//                }
                return result;
            }
        });
    }

    private void chonLocContours(List<MatOfPoint> lstMatOfPoint, List<Rect> lstRect) {
        for (int i = 0; i < lstMatOfPoint.size(); ) {
            // Lọc dựa vào diện tích (area):
            Rect rect = lstRect.get(i);
            int areaContour = rect.width * rect.height;
            Log.d(TAG, "Width_" + i + " = " + rect.width + "\nHeight_" + i + " = " + rect.height);
            if (areaContour < 100) {    // nhỏ quá thì bỏ luôn k cần xét dấu chia, vì nhỏ hơn 100 thường là nhiễu, dấu : của mình thường lớn hơn 100
                lstMatOfPoint.remove(i);
                lstRect.remove(i);
            } else if (areaContour < 400) { // dưới 400 cũng remove luôn, ngoại trừ nó là dấu : thì dưới 400 sẽ không remove
                if (0 < i && i < lstMatOfPoint.size() - 1) {
                    Rect rectDauTien = lstRect.get(0);
                    Rect rectTiepTheo = lstRect.get(i + 1);
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

    /**
     * lIST CÁC HÀM ĐƠN GIẢN:
     **/

    private String nhanDang(Bitmap bmSegmentedCharacter) {
//        Bitmap bitmap_orig = getBitmap(this.getContentResolver(), imageCroppedURI);
        // resize the bitmap to the required input size to the CNN
//        Bitmap bitmap = getResizedBitmap(bmSegmentedCharacter, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y); // không cần vì mình đã resize bằng Improc rồi
        // convert bitmap to byte array
        convertBitmapToByteBuffer(bmSegmentedCharacter);
        // pass byte data to the graph
        if (isQuant) {
            tflite.run(imgData, labelProbArrayB);
        } else {
            tflite.run(imgData, labelProbArray);
            Log.d(TAG, "Mảng kết quả dự đoán:" + Arrays.toString(labelProbArray[0]));
        }
        // Lấy ra vị trí trong mảng chứa giá trị lớn nhất:
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
                if (isQuant) {
                    imgData.put((byte) ((val >> 16) & 0xFF));   //lấy màu Red
                    imgData.put((byte) ((val >> 8) & 0xFF));    //lấy màu Green
                    imgData.put((byte) (val & 0xFF));           //lấy màu Blue
                } else {
                    // Sử dụng 1 trong 3 dòng thì được (kết quả đều SAI như nhau)
//                    imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
//                    imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                    imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                }
            }
        } */

        for (int i = 0; i < intValues.length; ++i) {
            // Set 0 for white and 255 for black pixels
            int pixelValue = intValues[i];
            // Sử dụng 1 trong 3 dòng thì được (kết quả đều như nhau)
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
        linearLayoutChracters = findViewById(R.id.linearLayoutCharacters);
        edtBieuThuc = findViewById(R.id.edtBieuThuc);
        btnGiaiSauKhiDaSua = findViewById(R.id.btnGiaiSauKhiDaSua);
        txtGiai = findViewById(R.id.txtGiai);
        txtGiai.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        txtDoThiHamSo = findViewById(R.id.txtDoThiHamSo);
        mathView = findViewById(R.id.mathView);
    }

    private void suKien(boolean isGiongNoi) {
        btnGiaiSauKhiDaSua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    clearGiaTriBien();
                    resSolve = edtBieuThuc.getText().toString().trim();
                    String latexBuocGiai = catBoKyHieu(phanGiai(resSolve));
                    mathView.setDisplayText(latexBuocGiai);
                    veDoThi(bacDoThi);
                } catch (Exception e) {
                    edtBieuThuc.setText(edtBieuThuc.getText().toString().trim());  // dù sai nhưng cứ set biểu thức để cho người dùng sửa lại rồi giải
                    String mes = "\"Không giải được biểu thức do nhận dạng sai. Mời bạn vui lòng sửa lại biểu thức cho đúng!\"";
                    String s = "<span style=\"color: red;\" class=\"enclosing\">" + mes + "</span>";
                    mathView.setDisplayText(s);
                    e.printStackTrace();
                    drawGraph();
                }
            }
        });
        if (isGiongNoi) {
            imgViewAnhDaCrop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói biểu thức của bạn:");
                    startActivityForResult(speechIntent, SolveActivityShort.REQUEST_VOICE_CODE);

                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VOICE_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String bieuThucGiongNoi = matches.get(0);
            if (bieuThucGiongNoi != null && !bieuThucGiongNoi.isEmpty()) {
                resSolve = xuLyBieuThucGiongNoi(bieuThucGiongNoi);  // bỏ ký tự rỗng
                edtBieuThuc.setText(resSolve);
                try {
                    clearGiaTriBien();
                    String latexBuocGiai = catBoKyHieu(phanGiai(resSolve));
                    mathView.setDisplayText(latexBuocGiai);
                    veDoThi(bacDoThi);
                } catch (Exception e) {
                    edtBieuThuc.setText(resSolve);  // dù sai nhưng cứ set biểu thức để cho người dùng sửa lại rồi giải
                    String mes = "\"Không giải được biểu thức do nhận dạng sai. Mời bạn vui lòng sửa lại biểu thức cho đúng!\"";
                    String s = "<span style=\"color: red;\" class=\"enclosing\">" + mes + "</span>";
                    mathView.setDisplayText(s);
                    e.printStackTrace();
                    drawGraph();
                }
            }
        }
    }

    // tắt focus edit text khi chạm bên ngoài nó
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void layDuLieuIntent() {
        Intent intent = getIntent();
        String uriImage = intent.getStringExtra("imageCroppedURI");
        if (uriImage != null) {
            imageCroppedURI = Uri.parse(uriImage);
        }
        String strGiongNoi = intent.getStringExtra("bieuThucGiongNoi");
        if (strGiongNoi != null) {
            bieuThucGiongNoi = strGiongNoi;
        }
        String dataHistory = intent.getStringExtra("dataHistory");
        if (dataHistory != null) {
            bieuThucLichSu = dataHistory;
        }
    }

    private void khoiTaoCacThuocTinh() {
        // initialize array that holds image data
        intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
        // initilize graph and labels
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
        if (isQuant) {
            labelProbArrayB = new byte[1][NUM_CLASSES];
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
}