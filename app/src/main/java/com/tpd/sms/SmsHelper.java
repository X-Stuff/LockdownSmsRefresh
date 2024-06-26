package com.tpd.sms;

import android.annotation.SuppressLint;
import android.app.role.RoleManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;

import com.tpd.sms.exceptions.SmsInsertionException;
import com.tpd.sms.exceptions.ThreadNotFoundException;

import java.util.Arrays;
import java.util.Date;

public class SmsHelper {
    private static final String LOG_TAG = "HP_SMS_MANAGER";
    private static final String SIM_ID = "sim_id";

    public static int getThreadId(Context context, String threadName) throws ThreadNotFoundException {
        String selection = String.format("UPPER(%s)=UPPER('%s')", Telephony.Sms.ADDRESS, threadName);
        String[] projection = new String[]{Telephony.Sms.THREAD_ID};

        try (Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, projection, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int colIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);
                return cursor.getInt(colIndex);
            } else {
                throw new ThreadNotFoundException(threadName);
            }
        }
    }

    public static int getSimId(Context context, String threadName) {
        String selection = String.format("UPPER(%s)=UPPER('%s')", Telephony.Sms.ADDRESS, threadName);

        try (Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, null, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                if (Arrays.asList(cursor.getColumnNames()).contains(SIM_ID)) {
                    int colIndex = cursor.getColumnIndex(SIM_ID);
                    return cursor.getInt(colIndex);
                }
            }
        }
        return -1;
    }

    public static int getLastSmsId(Context context, String smsAddress, boolean isInbox) throws Exception {
        int smsType = isInbox ? Telephony.Sms.MESSAGE_TYPE_INBOX : Telephony.Sms.MESSAGE_TYPE_SENT;

        @SuppressLint("DefaultLocale")
        String selection = String.format("UPPER(%s)=UPPER('%s') and %s=%d", Telephony.Sms.ADDRESS, smsAddress, Telephony.Sms.TYPE, smsType);
        String order = String.format("%s DESC", Telephony.Sms.DATE);

        try (Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, null, selection, null, order)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                return cursor.getInt(idIndex);
            } else {
                throw new Exception(
                        String.format("Cannot retrieve last %s message for '%s'",
                                isInbox ? "received" : "sent",
                                smsAddress));
            }
        }
    }

    public static int updateSms(Context context, String threadName, boolean isInbox, int minutesOffset) throws Exception {

        int lastSmsId = getLastSmsId(context, threadName, isInbox);

        long dateOffset = (long) minutesOffset * 60 * 1000; // minutes in millis

        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.DATE_SENT, new Date().getTime() - dateOffset - 3325);
        values.put(Telephony.Sms.DATE, new Date().getTime() - dateOffset);


        String query = String.format("%s=%s", Telephony.Sms._ID, lastSmsId);
        return context.getContentResolver().update(Telephony.Sms.CONTENT_URI, values, query, null);
    }

    public static int receiveSms(Context context, String address, String body) throws Exception {
        return insertSms(context, address, true, body, true, 0);
    }

    public static void dumpAllSms(Context context) {
        try (Cursor cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    StringBuilder msg = new StringBuilder();

                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        msg.append(cursor.getColumnName(i)).append(":").append(cursor.getString(i)).append(", ");
                    }

                    Log.w(LOG_TAG, msg.toString());
                } while (cursor.moveToNext());
            }
        }
    }

    public static boolean isDefaultSmsManager(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = context.getSystemService(RoleManager.class);
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
        } else {
            String smsPackageName = Telephony.Sms.getDefaultSmsPackage(context);
            return context.getPackageName().equals(smsPackageName);
        }

        return false;
    }

    public static Intent createOpenDefaultSmsManagerIntent(Context context) {
        Intent intent = null;
        String packageName = context.getPackageName();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = context.getSystemService(RoleManager.class);

            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
                } else {
                    intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                }
            }
        } else {
            intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            if (!isDefaultSmsManager(context)) {
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
            }
        }

        return intent;
    }

    public static int insertSms(Context context, String threadName, boolean createThread, String body, boolean isInbox, int minutesOffset) throws Exception {
        long dateOffset = (long) minutesOffset * 60 * 1000; // minutes in millis

        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.DATE_SENT, new Date().getTime() - dateOffset - 3325);
        values.put(Telephony.Sms.DATE, new Date().getTime() - dateOffset);
        values.put(Telephony.Sms.ADDRESS, threadName);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.TYPE, isInbox ? Telephony.Sms.MESSAGE_TYPE_INBOX : Telephony.Sms.MESSAGE_TYPE_SENT);
        values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE);

        int simId = getSimId(context, threadName);
        if (simId >= 0) {
            values.put(SIM_ID, simId);
        }

        try {
            values.put(Telephony.Sms.THREAD_ID, getThreadId(context, threadName));
        } catch (Exception e) {
            if (!createThread) {
                throw e;
            }
            Log.w(LOG_TAG, e.toString());
        }

        Uri result = context.getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);
        if (result == null) {
            throw new SmsInsertionException("Uri is null");
        }

        try (Cursor inserted = context.getContentResolver().query(result, null, null, null, null)) {
            if (inserted == null || !inserted.moveToFirst()) {
                throw new SmsInsertionException("");
            }

            int index = inserted.getColumnIndex(Telephony.Sms._ID);
            if (index < 0) {
                throw new SmsInsertionException("Invalid index");
            }

            return inserted.getInt(index);
        }
    }
}
