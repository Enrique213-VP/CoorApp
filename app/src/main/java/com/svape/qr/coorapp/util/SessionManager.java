package com.svape.qr.coorapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_LAST_USERNAME = "lastUsername";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setLoggedIn(boolean isLoggedIn) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }

    public void setLastUsername(String username) {
        editor.putString(KEY_LAST_USERNAME, username);
        editor.apply();
    }

    public String getLastUsername() {
        return sharedPreferences.getString(KEY_LAST_USERNAME, "");
    }

    public boolean isUserChanged() {
        String currentUser = getUsername();
        String lastUser = getLastUsername();
        return !currentUser.isEmpty() && !lastUser.isEmpty() && !currentUser.equals(lastUser);
    }

    public void clearLoginStatus() {
        String username = getUsername();
        setLastUsername(username);
        setLoggedIn(false);
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}