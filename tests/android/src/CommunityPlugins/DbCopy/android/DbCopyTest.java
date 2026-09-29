package CommunityPlugins.DbCopy.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PLU-111 / PLU-112: DbCopyPlugin.java on a plain JVM.
 *   PLU-111 root cause (BEFORE): the destination DB was deleted before the incoming payload was
 *   decoded or validated, the decode target was a separate cache dir that was never cleaned up,
 *   and there was no header check at all -- any bytes, even a non-SQLite file, silently became
 *   the new "database".
 *   PLU-112 root cause (BEFORE): both methods resolved a bare message STRING via
 *   callbackContext.success(String), not the {success,message} object types/index.d.ts and the
 *   shared caller (db-base.service.ts) already expect.
 *
 * Run an older copy of the source against this same test file to see the BEFORE failures:
 *   PLUGIN_SRC=/path/to/pre-fix/src/android tests/android/run.sh
 */
public class DbCopyTest {
    static int passed, failed;
    static final ExecutorService POOL = new AbstractExecutorService() {
        public void execute(Runnable r) { r.run(); }
        public void shutdown() {}
        public List<Runnable> shutdownNow() { return Collections.emptyList(); }
        public boolean isShutdown() { return false; }
        public boolean isTerminated() { return false; }
        public boolean awaitTermination(long l, TimeUnit u) { return true; }
    };

    static File tmpDbDir() {
        try {
            File dir = Files.createTempDirectory("dbcopytest").toFile();
            dir.deleteOnExit();
            return dir;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    static DbCopyPlugin newPlugin(Activity activity) {
        DbCopyPlugin p = new DbCopyPlugin();
        p.privateInitialize(new CordovaInterface() {
            public Activity getActivity() { return activity; }
            public ExecutorService getThreadPool() { return POOL; }
            public void startActivityForResult(CordovaPlugin c, Intent i, int code) {}
        });
        return p;
    }

    static Activity newActivity(File dbDir) {
        Activity a = new Activity();
        a.databaseDir = dbDir;
        a.cacheDir = new File(dbDir, "__cache_should_never_be_written__");
        a.filesDir = dbDir;
        a.externalFilesDir = dbDir;
        return a;
    }

    static void writeFile(File f, byte[] content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(content); }
    }

    static byte[] realSqliteBytes() {
        byte[] header = "SQLite format 3\0".getBytes();
        byte[] out = new byte[4096];
        System.arraycopy(header, 0, out, 0, header.length);
        return out;
    }

    interface Body { void run() throws Exception; }
    static void test(String name, Body b) {
        String why = null;
        try { b.run(); } catch (AssertionError a) { why = a.getMessage(); } catch (Throwable t) { why = "CRASH: " + t; }
        if (why == null) { passed++; System.out.println("PASS  " + name); }
        else { failed++; System.out.println("FAIL  " + name + "\n        -> " + why); }
    }
    static void check(boolean ok, String msg) { if (!ok) { throw new AssertionError(msg); } }

    public static void main(String[] args) {
        test("[PLU-111] a bad-header payload is rejected and the existing database is untouched", () -> {
            File dir = tmpDbDir();
            File dbFile = new File(dir, "app.db");
            byte[] original = realSqliteBytes();
            writeFile(dbFile, original);

            Activity activity = newActivity(dir);
            DbCopyPlugin p = newPlugin(activity);
            CallbackContext cb = new CallbackContext();
            byte[] junk = "NOT A DATABASE AT ALL, JUST JUNK BYTES".getBytes();
            JSONObject opts = new JSONObject()
                .put("dbName", "app.db")
                .put("base64Source", Base64.getEncoder().encodeToString(junk))
                .put("deleteOldDb", true);
            boolean ret = p.execute("copyDbFromStorage", new JSONArray().put(opts), cb);

            check(ret, "execute() must return true (no spurious INVALID_ACTION callback)");
            check(cb.results.size() == 1, "expected exactly one callback, got " + cb.results);
            PluginResult r = cb.results.get(0);
            check(r.status == PluginResult.Status.ERROR, "expected ERROR status, got " + r);
            check(r.message instanceof JSONObject, "result must be a JSONObject, got " + r.message.getClass());
            check(((JSONObject) r.message).getBoolean("success") == false, "success must be false: " + r.message);

            byte[] onDisk = Files.readAllBytes(dbFile.toPath());
            check(java.util.Arrays.equals(onDisk, original), "existing database must be untouched by a rejected import");
            File[] leftovers = dir.listFiles((d, n) -> n.startsWith("app.db.tmp-"));
            check(leftovers == null || leftovers.length == 0, "no temp file may be left behind: " + java.util.Arrays.toString(leftovers));
            check(!activity.cacheDir.exists(), "the old cache-dir path must never be written to");
        });

        test("[PLU-111] a valid SQLite payload replaces the database, leaves no temp file and no .bak", () -> {
            File dir = tmpDbDir();
            File dbFile = new File(dir, "app.db");
            writeFile(dbFile, "SQLite format 3\0OLD-DATA".getBytes());

            Activity activity = newActivity(dir);
            DbCopyPlugin p = newPlugin(activity);
            CallbackContext cb = new CallbackContext();
            byte[] good = realSqliteBytes();
            JSONObject opts = new JSONObject()
                .put("dbName", "app.db")
                .put("base64Source", Base64.getEncoder().encodeToString(good))
                .put("deleteOldDb", true);
            boolean ret = p.execute("copyDbFromStorage", new JSONArray().put(opts), cb);

            check(ret, "execute() must return true");
            check(cb.results.size() == 1, "expected exactly one callback, got " + cb.results);
            PluginResult r = cb.results.get(0);
            check(r.status == PluginResult.Status.OK, "expected OK status, got " + r);
            check(r.message instanceof JSONObject, "result must be a JSONObject (PLU-112), got " + r.message.getClass());
            JSONObject msg = (JSONObject) r.message;
            check(msg.getBoolean("success") == true, "success must be true: " + msg);
            check(msg.has("message"), "must carry a message field: " + msg);

            byte[] onDisk = Files.readAllBytes(dbFile.toPath());
            check(java.util.Arrays.equals(onDisk, good), "database content must be the new payload");
            File bak = new File(dir, "app.db.bak");
            check(!bak.exists(), ".bak must be removed on success");
            File[] leftovers = dir.listFiles((d, n) -> n.startsWith("app.db.tmp-"));
            check(leftovers == null || leftovers.length == 0, "no temp file may be left behind: " + java.util.Arrays.toString(leftovers));
        });

        test("[PLU-111] stale -journal/-wal/-shm sidecars from the old database are removed on a successful replace", () -> {
            File dir = tmpDbDir();
            File dbFile = new File(dir, "app.db");
            writeFile(dbFile, "SQLite format 3\0OLD-DATA".getBytes());
            writeFile(new File(dir, "app.db-journal"), "stale-journal".getBytes());
            writeFile(new File(dir, "app.db-wal"), "stale-wal".getBytes());
            writeFile(new File(dir, "app.db-shm"), "stale-shm".getBytes());

            Activity activity = newActivity(dir);
            DbCopyPlugin p = newPlugin(activity);
            CallbackContext cb = new CallbackContext();
            JSONObject opts = new JSONObject()
                .put("dbName", "app.db")
                .put("base64Source", Base64.getEncoder().encodeToString(realSqliteBytes()))
                .put("deleteOldDb", true);
            p.execute("copyDbFromStorage", new JSONArray().put(opts), cb);

            check(!new File(dir, "app.db-journal").exists(), "-journal sidecar must be deleted");
            check(!new File(dir, "app.db-wal").exists(), "-wal sidecar must be deleted");
            check(!new File(dir, "app.db-shm").exists(), "-shm sidecar must be deleted");
        });

        test("[PLU-112] copyDbToStorage resolves a {success,message} object, not a bare string", () -> {
            File dir = tmpDbDir();
            File dbFile = new File(dir, "app.db");
            writeFile(dbFile, realSqliteBytes());
            File exportDir = new File(dir, "export");

            Activity activity = newActivity(dir);
            DbCopyPlugin p = newPlugin(activity);
            CallbackContext cb = new CallbackContext();
            JSONObject opts = new JSONObject()
                .put("fileName", "app.db")
                .put("fullPath", exportDir.getAbsolutePath() + File.separator)
                .put("overwrite", true);
            boolean ret = p.execute("copyDbToStorage", new JSONArray().put(opts), cb);

            check(ret, "execute() must return true");
            check(cb.results.size() == 1, "expected exactly one callback, got " + cb.results);
            PluginResult r = cb.results.get(0);
            check(r.status == PluginResult.Status.OK, "expected OK, got " + r);
            check(r.message instanceof JSONObject, "result must be a JSONObject, got " + r.message.getClass());
            check(((JSONObject) r.message).getBoolean("success") == true, "success must be true: " + r.message);
        });

        test("[PLU-112] an error path (missing dest folder creation failure via bad args) never produces a second callback", () -> {
            File dir = tmpDbDir();
            Activity activity = newActivity(dir);
            DbCopyPlugin p = newPlugin(activity);
            CallbackContext cb = new CallbackContext();
            // malformed args: missing required "dbName" key -> JSONException path
            JSONObject opts = new JSONObject().put("base64Source", "====not-valid-base64====");
            boolean ret = p.execute("copyDbFromStorage", new JSONArray().put(opts), cb);

            check(ret, "execute() must return true on the error path (Cordova sends INVALID_ACTION as a SECOND callback when it's false)");
            check(cb.results.size() == 1, "exactly one callback expected on the error path, got " + cb.results.size() + ": " + cb.results);
            check(cb.results.get(0).status == PluginResult.Status.ERROR, "expected ERROR status");
        });

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) { System.exit(1); }
    }
}
