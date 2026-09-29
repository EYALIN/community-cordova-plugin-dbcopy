package android.util;
public class Log {
    public static int d(String tag, String msg) { System.out.println("D/" + tag + ": " + msg); return 0; }
    public static int e(String tag, String msg) { System.out.println("E/" + tag + ": " + msg); return 0; }
}
