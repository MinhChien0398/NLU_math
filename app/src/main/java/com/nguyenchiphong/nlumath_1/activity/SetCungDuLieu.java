package com.nguyenchiphong.nlumath_1.activity;

public class SetCungDuLieu {
    // Hiện MathView:
    public static String tmp = "$x=\\frac{1+y}{1+2z^2}$";
    public static String pt1 = "a_1x+b_1y+c_1z &=d_1+e_1+p_2+p_3";
    public static String pt2 = "a_2x+b_2y &=d_2";
    public static String tmp0 = "$\\left\\{\\begin{array}{ll}" + pt1 + " \\\\" + pt2 + "\\end{array}\\right.$";
    public static String tmp1 = "$\n" +
            "\\left\\{\n" +
            "\\begin{array}{ll}\n" +
            "a_1x+b_1y+c_1z &=d_1+e_1 \\\\ \n" +
            "a_2x+b_2y &=d_2 \\\\ \n" +
            "a_3x+b_3y+c_3z &=d_3 \n" +
            "\\end{array} \n" +
            "\\right.\n" +
            "$";
    public static String tmp2 = "$$\\begin{cases}\n" +
            "a_1x+b_1y+c_1z=d_1 \\\\ \n" +
            "a_2x+b_2y+c_2z=d_2 \\\\ \n" +
            "a_3x+b_3y+c_3z=d_3\n" +
            "\\end{cases}\n" +
            "$$";
    public static String tmp3 = "$$\\frac{" + "1+2" + "}" + "{" + "3+4" + "})$$";
    public static String tmp4 = "$$x^2+(1\\over 2)=0$$"; //=> thay ngoặc () bằng ngoặc {} để hiển thị đc phân số với đk phân số phải được bao bởi ()
    public static String tmp5 = "Solving \\(2(x+8)(x-4) = x^{2}\\):$$ 2(x+8)(x-4) = x^{2} $$\n" +
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

    public static String ppp = "Giải hệ phương trình:\n" +
            "$$\\begin{cases}-3x+5y+6x-7=2y-3+4x-6\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\\\5y-x+10-9y=-12+4x-2x+2y-2y\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\end{cases}$$\n" +
            "Sắp xếp lại hệ phương trình, ta được như sau:\n" +
            "$$\\begin{cases}-3x+6x+5y-7=4x+2y-3-6\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\\\-x+5y-9y+10=4x-2x+2y-2y-12\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\end{cases}$$\n" +
            "Thu gọn hệ phương trình, ta được:\n" +
            "$$\\begin{cases}3x+5y-7=4x+2y-9\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\\\-x-4y+10=2x-12\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\end{cases}$$\n" +
            "Chuyển biến sang trái, chuyển số tự do sang phải, kết hợp đổi dấu ta được:\n" +
            "$$\\begin{cases}3x-4x+5y-2y=-9+7\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\\\-x-2x-4y=-12-10\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\end{cases}$$\n" +
            "Thu gọn lại, ta được:\n" +
            "$$\\begin{cases}-x+3y=-2\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\\\-3x-4y=-22\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,\\end{cases}$$\n" +
            "Cách giải hệ phương trình:\n" +
            "$$\\begin{cases}-x+3y=-2\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,(1)\\\\-3x-4y=-22\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,(2)\\end{cases}$$\n" +
            "*Cách 1: Giải hệ bằng phương pháp THẾ$\\newline$Từ phương trình $(1)$, ta suy ra $x$ theo $y$ như sau:$$x = \\frac{-2 - 3y}{-1}\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,(3)$$Thay $(3)$ vào phương trình $(2)$, ta giải phương trình bậc nhất theo biến $y$ như sau:$\\newline$Giải phương trình: $$-(\\frac{3}{-1})(-2-3y)-4y = -22$$$$ -4y+3(-2-3y) = -22 $$\n" +
            "Nhân phân phối với biểu thức trong ngoặc: \n" +
            "$$ -4y-9y-6 = -22 $$\n" +
            "$$ -13y-6 = -22 $$\n" +
            "Chuyển số tự do sang bên phải rồi thu gọn, ta được:\n" +
            "$$ -13y = -16 $$\n" +
            "Chia hai vế cho \\(-13\\), suy ra:\n" +
            "$$ y = 1,230769 $$\n" +
            "Thay $y = 1,230769$ vào phương trình $(3)$, suy ra:$$x = \\frac{-2 - 3\\cdot1,230769}{-1}$$$$x = \\frac{-2-3,692307}{-1}$$$$x = \\frac{-5,692307}{-1}$$$$x = 5,692307$$Vậy hệ phương trình có nghiệm là: $(5,692307\\,;\\,1,230769)$\n" +
            "$\\newline\\newline$*Cách 2: Giải hệ bằng phương pháp CỘNG ĐẠI SỐ$\\newline$Quan sát hệ hai phương trình $(1)$ và $(2)$, ta nhận thấy hệ số $a_2$ là bội của hệ số $a_1$ vì:$$\\frac{a_2}{a_1} = \\frac{-3}{-1} = 3$$Bằng cách nhân 3 vào hai vế của $(1)$, ta được hệ phương trình mới như sau:$$\\begin{cases}-3x+9y=-6\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,(4)\\\\-3x-4y=-22\\,\\,\\,\\,\\,\\,\\,\\,\\,\\,(2)\\end{cases}$$Lấy $(4)$ trừ cho $(2)$, ta có:$$-3x - (-3)x+9y - (-4)y = -6 - (-22)$$$$13y = 16$$$$y = \\frac{16}{13}$$$$y = 1,230769$$Thay $y = 1,230769$ vào phương trình $(2)$, ta có:$$-3x-4\\cdot1,230769=-22$$$$-3x-4,923077 = -22$$$$-3x = -22-(-4,923077)$$$$-3x = -17,076923$$$$x = \\frac{-17,076923}{-3}$$$$x = 5,692308$$Vậy hệ phương trình có nghiệm là: $(5,692308\\,;\\,1,230769)$\n" +
            "\n";

    public static String ccc = "Giải phương trình:$$-3x^2+2x^3-x^3-4x-1=0$$\n" +
            "Sắp xếp lại phương trình theo bậc giảm dần, ta có:\n" +
            "$$2x^3-x^3-3x^2-4x-1=0$$\n" +
            "Thu gọn lại, ta được:\n" +
            "$$x^3-3x^2-4x-1=0$$\n" +
            "Với các hệ số:$$a=1\\,;\\,b=-3\\,;\\,c=-4\\,;\\,d=-1$$Sử dụng công thức nghiệm, ta có:$$\\Delta = b^2 - 3ac$$$$\\Delta = (-3)^2 - 3\\cdot1\\cdot(-4)$$$$\\Delta = 9 - (-12)$$$$\\Delta = 21$$\n" +
            "Vì $\\Delta = 21 > 0$ nên ta xét tiếp đại lượng k với k được tính như sau:\n" +
            "$$k = \\frac{9abc-2b^3-27a^2d}{2\\sqrt{|\\Delta|^3}}$$$$k = \\frac{9\\cdot1\\cdot(-3)\\cdot(-4)-2\\cdot(-3)^3-27\\cdot1^2\\cdot(-1)}{2\\sqrt{|21|^3}}$$$$k = \\frac{108-2\\cdot(-27)-27\\cdot1\\cdot(-1)}{2\\sqrt{21^3}}$$$$k = \\frac{108-(-54)-(-27)}{2\\sqrt{9261}}$$$$k = \\frac{189}{2\\cdot96,23409}$$$$k = \\frac{189}{192,468179}$$$$k = 0,981981$$\n" +
            "Do $|k| = |0,981981| = 0,981981 \\le 1$ nên phương trình có ba nghiệm:\n" +
            "Nghiệm đầu tiên:\n" +
            "$$x_1 = \\frac{2\\sqrt{\\Delta}\\cos(\\frac{\\arccos(k)}{3})-b}{3a}$$$$x_1 = \\frac{2\\cdot\\sqrt{21}\\cdot\\cos(\\frac{\\arccos(0,981981)}{3})-(-3)}{3\\cdot1}$$$$x_1 = \\frac{2\\cdot4,582576\\cdot\\cos(0,063375)+3}{3}$$$$x_1 = \\frac{2\\cdot4,582576\\cdot0,997992+3}{3}$$$$x_1 = \\frac{9,146752+3}{3}$$$$x_1 = \\frac{12,146752}{3}$$$$x_1 = 4,048917$$\n" +
            "Nghiệm thứ hai:\n" +
            "$$x_2 = \\frac{2\\sqrt{\\Delta}\\cos(\\frac{\\arccos(k)}{3}-\\frac{2\\pi}{3})-b}{3a}$$$$x_2 = \\frac{2\\cdot\\sqrt{21}\\cdot\\cos(\\frac{\\arccos(0,981981)}{3}-\\frac{2\\pi}{3})-(-3)}{3\\cdot1}$$$$x_2 = \\frac{2\\cdot4,582576\\cdot\\cos(-2,03102)+3}{3}$$$$x_2 = \\frac{2\\cdot4,582576\\cdot(-0,444148)+3}{3}$$$$x_2 = \\frac{(-4,070688)+3}{3}$$$$x_2 = \\frac{-1,070688}{3}$$$$x_2 = -0,356896$$\n" +
            "Nghiệm thứ ba:\n" +
            "$$x_3 = \\frac{2\\sqrt{\\Delta}\\cos(\\frac{\\arccos(k)}{3}+\\frac{2\\pi}{3})-b}{3a}$$$$x_3 = \\frac{2\\cdot\\sqrt{21}\\cdot\\cos(\\frac{\\arccos(0,981981)}{3}+\\frac{2\\pi}{3})-(-3)}{3\\cdot1}$$$$x_3 = \\frac{2\\cdot4,582576\\cdot\\cos(2,15777)+3}{3}$$$$x_3 = \\frac{2\\cdot4,582576\\cdot(-0,553844)+3}{3}$$$$x_3 = \\frac{(-5,076064)+3}{3}$$$$x_3 = \\frac{-2,076064}{3}$$$$x_3 = -0,692021$$\n" +
            "Kết luận, vậy phương trình có ba nghiệm:\n" +
            "$$x_1 = 4,048917$$\n" +
            "$$x_2 = -0,356896$$\n" +
            "$$x_3 = -0,692021$$\n" +
            "\n";
}
