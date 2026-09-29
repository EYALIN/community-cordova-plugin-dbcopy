package org.apache.cordova;
import org.json.JSONArray;
import org.json.JSONException;
public class CordovaPlugin {
    public CordovaInterface cordova;
    public final void privateInitialize(CordovaInterface c) { cordova = c; }
    public boolean execute(String action, JSONArray args, CallbackContext cb) throws JSONException { return false; }
}
