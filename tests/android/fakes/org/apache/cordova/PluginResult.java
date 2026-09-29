package org.apache.cordova;
import org.json.JSONArray;
import org.json.JSONObject;
public class PluginResult {
    public enum Status { OK, ERROR }
    public final Status status;
    public final Object message;
    public PluginResult(Status status, String message) { this.status = status; this.message = message; }
    public PluginResult(Status status, JSONArray message) { this.status = status; this.message = message; }
    public PluginResult(Status status, JSONObject message) { this.status = status; this.message = message; }
    public String toString() { return status + ": " + message; }
}
