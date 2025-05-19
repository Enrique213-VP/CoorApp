package com.svape.qr.coorapp.util;

import android.content.Context;
import android.provider.Settings;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

public class DeviceInfoHelper {
    private static final String TAG = "DeviceInfoHelper";
    private final Context context;
    private String cachedDeviceId = null;

    public DeviceInfoHelper(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public String getDeviceId() {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }

        String deviceId = null;

        try {
            deviceId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            if (deviceId == null || deviceId.equals("9774d56d682e549c") || deviceId.length() < 8) {
                String deviceInfo = Build.BRAND + Build.DEVICE + Build.MANUFACTURER +
                        Build.MODEL + Build.PRODUCT + Build.SERIAL;

                deviceId = UUID.nameUUIDFromBytes(deviceInfo.getBytes()).toString();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al obtener ID del dispositivo", e);
            deviceId = UUID.randomUUID().toString();
        }

        cachedDeviceId = deviceId;
        Log.d(TAG, "ID del dispositivo: " + deviceId);

        return deviceId;
    }
}