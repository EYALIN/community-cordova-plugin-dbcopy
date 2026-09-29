package org.apache.cordova;
import android.app.Activity;
import android.content.Intent;
import java.util.concurrent.ExecutorService;
public interface CordovaInterface {
    Activity getActivity();
    ExecutorService getThreadPool();
    void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode);
}
