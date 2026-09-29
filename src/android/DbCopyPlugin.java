package CommunityPlugins.DbCopy.android;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Base64;

public class DbCopyPlugin extends CordovaPlugin {
    private static final int REQUEST_CODE = 123; // Any unique number for your request code
    private static final byte[] SQLITE_HEADER = new byte[] {
        'S','Q','L','i','t','e',' ','f','o','r','m','a','t',' ','3',0
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("copyDbFromStorage")) {
            return copyDbFromStorage(args, callbackContext);
        } else if (action.equals("copyDbToStorage")) {
            return copyDbToStorage(args, callbackContext);
        }
        return false;
    }

    private boolean copyDbFromStorage(JSONArray args, CallbackContext callbackContext) {
        // decoded bytes are written to a temp file IN THE SAME DIRECTORY as the destination DB
        // (never a separate cache dir) so the final rename is a same-filesystem atomic rename.
        File tempFile = null;
        File bakFile = null;
        File destFile = null;
        boolean renamedOldToBak = false;
        try {
            JSONObject options = args.getJSONObject(0);
            String dbName = options.getString("dbName");
            String base64Source = options.getString("base64Source"); // Now the base64-encoded string
            String location = options.optString("location", "default");
            boolean deleteOldDb = options.optBoolean("deleteOldDb", false);

            // Determine the destination path for the database
            String destPath = getDatabasePath(location, dbName);
            destFile = new File(destPath);
            File destDir = destFile.getParentFile();
            if (destDir != null && !destDir.exists()) {
                destDir.mkdirs();
            }

            // Decode Base64 string and write it to a temp file in the destination directory.
            byte[] fileBytes;
            try {
                fileBytes = Base64.decode(base64Source, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                callbackContext.error(errorResult("Invalid base64 data: " + e.getMessage()));
                return true;
            }

            // Validate the SQLite header BEFORE touching the existing database at all.
            if (!hasSqliteHeader(fileBytes)) {
                callbackContext.error(errorResult("Source data is not a valid SQLite database (bad header)."));
                return true;
            }

            tempFile = new File(destDir, dbName + ".tmp-" + System.currentTimeMillis());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileBytes);
            }

            // Only now, with a validated payload safely on disk, do we touch the live DB.
            if (deleteOldDb || destFile.exists()) {
                if (destFile.exists()) {
                    bakFile = new File(destDir, dbName + ".bak");
                    if (bakFile.exists()) {
                        bakFile.delete();
                    }
                    if (!destFile.renameTo(bakFile)) {
                        callbackContext.error(errorResult("Failed to back up existing database before replace."));
                        return true;
                    }
                    renamedOldToBak = true;
                }
            }

            if (!tempFile.renameTo(destFile)) {
                // restore original DB, nothing was actually replaced
                if (renamedOldToBak && bakFile != null) {
                    bakFile.renameTo(destFile);
                }
                callbackContext.error(errorResult("Failed to install the new database."));
                return true;
            }

            // Success: sidecars from the OLD database are now stale/inconsistent with the new
            // file and must not be left around for SQLite to try to replay against it.
            deleteSidecars(destDir, dbName);
            if (renamedOldToBak && bakFile != null) {
                bakFile.delete();
            }

            callbackContext.success(successResult("Database copied successfully."));
            return true;
        } catch (JSONException e) {
            callbackContext.error(errorResult("JSON Exception: " + e.getMessage()));
            return true;
        } catch (IOException e) {
            // best-effort restore of the original DB on unexpected failure
            if (renamedOldToBak && bakFile != null) {
                bakFile.renameTo(destFile);
            }
            callbackContext.error(errorResult("IO Exception: " + e.getMessage()));
            return true;
        } finally {
            // ALWAYS clean up the temp file — it must never be left behind, success or failure.
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private static boolean hasSqliteHeader(byte[] data) {
        if (data == null || data.length < SQLITE_HEADER.length) {
            return false;
        }
        for (int i = 0; i < SQLITE_HEADER.length; i++) {
            if (data[i] != SQLITE_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    private static void deleteSidecars(File dir, String dbName) {
        if (dir == null) {
            return;
        }
        String[] suffixes = { "-journal", "-wal", "-shm" };
        for (String suffix : suffixes) {
            File f = new File(dir, dbName + suffix);
            if (f.exists()) {
                f.delete();
            }
        }
    }

    private static JSONObject successResult(String message) {
        JSONObject o = new JSONObject();
        try {
            o.put("success", true);
            o.put("message", message);
        } catch (JSONException ignored) { }
        return o;
    }

    private static JSONObject errorResult(String message) {
        JSONObject o = new JSONObject();
        try {
            o.put("success", false);
            o.put("message", message);
        } catch (JSONException ignored) { }
        return o;
    }

    // Helper method to get the database path based on location
    private String getDatabasePath(String location, String dbName) {
        // Assuming location can be 'default', 'documents', or 'external'
        Context context = this.cordova.getActivity().getApplicationContext();
        String basePath;

        switch (location) {
            case "documents":
                basePath = context.getFilesDir().getAbsolutePath();
                break;
            case "external":
                basePath = context.getExternalFilesDir(null).getAbsolutePath();
                break;
            default:
                basePath = context.getDatabasePath(dbName).getParent();
                break;
        }

        return basePath + File.separator + dbName;
    }

    // Helper method to copy a file from source to destination
    private boolean copyFile(File sourceFile, File destFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            return true;
        } catch (FileNotFoundException e) {
            throw new IOException("File not found: " + e.getMessage(), e);
        }
    }

    private boolean copyDbToStorage(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject options = args.getJSONObject(0);
            String fileName = options.getString("fileName");
            String fullPath = options.getString("fullPath");
            boolean overwrite = options.optBoolean("overwrite", false);

            // Get the app's database path for the specified file
            String sourcePath = getDatabasePath("default", fileName);
            File destFolder;
            File destFile;

            if (fullPath.indexOf("file://") != -1) {
                destFile = new File(fullPath.replace("file://", "") + fileName);
                destFolder = new File(fullPath.replace("file://", ""));
            } else {
                destFile = new File(fullPath + fileName);
                destFolder = new File(fullPath);
            }

            if (!destFolder.exists()) {
                destFolder.mkdirs();
            }
            if (!destFolder.exists()) {
                callbackContext.error(errorResult("Invalid output DB Location"));
                return true;
            }

            // Check if the destination file exists and handle overwrite option
            if (destFile.exists() && !overwrite) {
                callbackContext.error(errorResult("File already exists and overwrite is set to false."));
                return true;
            }

            // Copy the database from sourcePath to the provided fullPath
            if (!copyFile(new File(sourcePath), destFile)) {
                callbackContext.error(errorResult("Failed to copy database to storage."));
                return true;
            }

            callbackContext.success(successResult("Database copied to storage successfully."));
            return true;
        } catch (JSONException e) {
            callbackContext.error(errorResult("JSON Exception: " + e.getMessage()));
            return true;
        } catch (IOException e) {
            callbackContext.error(errorResult("IO Exception: " + e.getMessage()));
            return true;
        }
    }
}
