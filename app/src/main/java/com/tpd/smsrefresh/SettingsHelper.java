package com.tpd.smsrefresh;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {

    public static final String INBOX_THREAD_NAME_PREF_KEY = "inbox_thread_name";
    public static final String INBOX_LAST_MESSAGE_PREF_KEY = "inbox_last_message";
    public static final String OUTBOX_THREAD_NAME_PREF_KEY = "outbox_thread_name";
    public static final String OUTBOX_LAST_MESSAGE_PREF_KEY = "outbox_last_message";
    public static final String TIME_OFFSET_PREF_KEY = "time_offset";
    public static final String SHARED_PREF_NAME = "porn";

    public static String getThreadNameFromSettings(Context context, boolean isInbox) {
        SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getString(isInbox ? INBOX_THREAD_NAME_PREF_KEY : OUTBOX_THREAD_NAME_PREF_KEY, "");
    }

    public static void setThreadNameToSettings(Context context, String threadName, boolean isInbox) {
        SharedPreferences.Editor edit = getSharedPreferences(context).edit();

        edit.putString(isInbox ? INBOX_THREAD_NAME_PREF_KEY : OUTBOX_THREAD_NAME_PREF_KEY, threadName);
        edit.apply();
    }

    public static String getMessageBodyFromSettings(Context context, boolean isInbox) {
        SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getString(isInbox ? INBOX_LAST_MESSAGE_PREF_KEY : OUTBOX_LAST_MESSAGE_PREF_KEY, "");
    }

    public static void setMessageBodyToSettings(Context context, String body, boolean isInbox) {
        SharedPreferences.Editor edit = getSharedPreferences(context).edit();

        edit.putString(isInbox ? INBOX_LAST_MESSAGE_PREF_KEY : OUTBOX_LAST_MESSAGE_PREF_KEY, body);
        edit.apply();
    }

    public static int getTimeOffsetFromSettings(Context context) {
        SharedPreferences preferences = getSharedPreferences(context);
        return preferences.getInt(TIME_OFFSET_PREF_KEY, 30);
    }

    public static void setTimeOffsetToSettings(Context context, int offset) {
        SharedPreferences.Editor edit = getSharedPreferences(context).edit();

        edit.putInt(TIME_OFFSET_PREF_KEY, offset);
        edit.apply();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }
}
