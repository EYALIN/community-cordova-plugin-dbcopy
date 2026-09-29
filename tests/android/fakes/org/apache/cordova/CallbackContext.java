package org.apache.cordova;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
public class CallbackContext {
    public final List<PluginResult> results = new ArrayList<>();
    public void sendPluginResult(PluginResult r) { results.add(r); }
    public void success(String m) { sendPluginResult(new PluginResult(PluginResult.Status.OK, m)); }
    public void success(JSONArray m) { sendPluginResult(new PluginResult(PluginResult.Status.OK, m)); }
    public void success(JSONObject m) { sendPluginResult(new PluginResult(PluginResult.Status.OK, m)); }
    public void error(String m) { sendPluginResult(new PluginResult(PluginResult.Status.ERROR, m)); }
    public void error(JSONObject m) { sendPluginResult(new PluginResult(PluginResult.Status.ERROR, m)); }
}
