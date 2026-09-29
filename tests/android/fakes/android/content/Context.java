package android.content;
import java.io.File;

/** Minimal fake covering only the surface DbCopyPlugin actually calls. */
public class Context {
    public File filesDir;
    public File externalFilesDir;
    public File databaseDir; // parent dir used by getDatabasePath()

    public File getFilesDir() { return filesDir; }
    public File getExternalFilesDir(String type) { return externalFilesDir; }
    public File getDatabasePath(String name) { return new File(databaseDir, name); }
}
