package android.util;

/** Real android.util.Base64 doesn't exist on a plain JVM; shim it onto java.util.Base64. */
public class Base64 {
    public static final int DEFAULT = 0;

    public static byte[] decode(String input, int flags) {
        return java.util.Base64.getMimeDecoder().decode(input.getBytes());
    }

    public static String encodeToString(byte[] input, int flags) {
        return java.util.Base64.getEncoder().encodeToString(input);
    }
}
