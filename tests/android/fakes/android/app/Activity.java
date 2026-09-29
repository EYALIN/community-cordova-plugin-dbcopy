package android.app;
import android.content.Context;
import java.io.File;

public class Activity extends Context {
    public File cacheDir;
    public File getCacheDir() { return cacheDir; }
    public Context getApplicationContext() { return this; }
}
