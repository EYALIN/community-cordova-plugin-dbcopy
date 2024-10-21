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
import java.io.FileOutputStream;

public class DbCopyPlugin extends CordovaPlugin {
private static final int REQUEST_CODE = 123; // Any unique number for your request code


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
        try {
            JSONObject options = args.getJSONObject(0);
            String dbName = options.getString("dbName");
            String base64Source = options.getString("sourcePath"); // Now the base64-encoded string
            String location = options.optString("location", "default");
            boolean deleteOldDb = options.optBoolean("deleteOldDb", false);

            // Determine the destination path for the database
            String destPath = getDatabasePath(location, dbName);

            // Check if the old DB exists and if it should be deleted
            File destFile = new File(destPath);
            if (deleteOldDb && destFile.exists()) {
                if (!destFile.delete()) {
                    callbackContext.error("Failed to delete old database.");
                    return false;
                }
            }

            // Decode Base64 string and write it to a temporary file
            byte[] fileBytes = Base64.decode(base64Source, Base64.DEFAULT);
            File tempFile = new File(cordova.getActivity().getCacheDir(), dbName);

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileBytes);
            }

            // Copy the temporary file (decoded from Base64) to the destination
            if (!copyFile(tempFile, destFile)) {
                callbackContext.error("Failed to copy database from the Base64 source.");
                return false;
            }

            callbackContext.success("Database copied successfully.");
            return true;
        } catch (JSONException e) {
            callbackContext.error("JSON Exception: " + e.getMessage());
            return false;
        } catch (IOException e) {
            callbackContext.error("IO Exception: " + e.getMessage());
            return false;
        }
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

            // Destination file in storage
            File destFile = new File(fullPath);

            // Check if the destination file already exists
            if (destFile.exists()) {
                if (!overwrite) {
                    callbackContext.error("File already exists and overwrite is set to false.");
                    return false;
                } else if (!destFile.delete()) {
                    callbackContext.error("Failed to overwrite the existing file.");
                    return false;
                }
            }

            // Copy the database from sourcePath to the provided fullPath
            if (!copyFile(new File(sourcePath), destFile)) {
                callbackContext.error("Failed to copy database to storage.");
                return false;
            }

            callbackContext.success("Database copied to storage successfully.");
            return true;
        } catch (JSONException e) {
            callbackContext.error("JSON Exception: " + e.getMessage());
            return false;
        } catch (IOException e) {
            callbackContext.error("IO Exception: " + e.getMessage());
            return false;
        }
    }
}
