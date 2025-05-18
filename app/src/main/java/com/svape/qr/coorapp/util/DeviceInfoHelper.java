package com.svape.qr.coorapp.util;

import android.content.Context;
import android.provider.Settings;

public class DeviceInfoHelper {
    private final Context context;

    public DeviceInfoHelper(Context context) {
        this.context = context;
    }

    public String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}