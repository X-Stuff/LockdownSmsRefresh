package com.haha.horseporn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Date;

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;
import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    public static final String INBOX_THREAD_NAME_PREF_KEY = "inbox_thread_name";
    public static final String OUTBOX_THREAD_NAME_PREF_KEY = "outbox_thread_name";
    public static final String TIME_OFFSET_PREF_KEY = "time_offset";
    public static final String SHARED_PREF_NAME = "porn";
    public static final String LOG_TAG = "HorsePorn";

    public static final int REQ_DEFAULT_SMS_APP = 101;

    private void checkWritePermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, "android.permission.WRITE_SMS") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{"android.permission.WRITE_SMS", Manifest.permission.SEND_SMS},
                    1001);
        }
    }

    private void checkReadPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},
                    999);
        }
    }

    private String getThreadNameFromSettings(boolean isInbox) {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        return preferences.getString(isInbox ? INBOX_THREAD_NAME_PREF_KEY : OUTBOX_THREAD_NAME_PREF_KEY, "");
    }

    private void setThreadNameToSettings(String threadName, boolean isInbox) {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();

        edit.putString(isInbox ? INBOX_THREAD_NAME_PREF_KEY : OUTBOX_THREAD_NAME_PREF_KEY, threadName);
        edit.apply();
    }

    private int getTimeOffsetFromSettings() {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        return preferences.getInt(TIME_OFFSET_PREF_KEY, 30);
    }

    private void setTimeOffsetToSettings(int offset) {
        SharedPreferences preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();

        edit.putInt(TIME_OFFSET_PREF_KEY, offset);
        edit.apply();
    }

    private void dumpAllSms() {
        try (Cursor cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, null)) {
            if (cursor.moveToFirst()) {
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

    private String getLastSmsId(String smsAddress, boolean isInbox) throws Exception {
        int smsType = isInbox ? Telephony.Sms.MESSAGE_TYPE_INBOX : Telephony.Sms.MESSAGE_TYPE_SENT;

        @SuppressLint("DefaultLocale")
        String selection = String.format("UPPER(%s)=UPPER('%s') and %s=%d", Telephony.Sms.ADDRESS, smsAddress, Telephony.Sms.TYPE, smsType);
        String order = String.format("%s", Telephony.Sms.DATE);

        try (Cursor cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, selection, null, order)) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);
                return cursor.getString(idIndex);
            } else {
                throw new Exception(
                        String.format("Cannot retrieve last %s message for '%s'",
                                isInbox ? getResources().getString(R.string.inbox_text) : getResources().getString(R.string.outbox_text),
                                smsAddress));
            }
        }
    }

    private boolean checkDefaultSmsAppSettings() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_SMS);
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            String smsPackageName = Telephony.Sms.getDefaultSmsPackage(MainActivity.this);
            return getPackageName().equals(smsPackageName);
        }

        return false;
    }

    private void openDefaultSmsAppSettings() {
        Intent intent = null;
        String packageName = getPackageName();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);

            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
                } else {
                    intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                }
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            String smsPackageName = Telephony.Sms.getDefaultSmsPackage(MainActivity.this);
            intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
        } else {
            // not supported
            return;
        }

        startActivityForResult(intent, REQ_DEFAULT_SMS_APP);
    }

    private void doSmsModification(View view, String threadName, boolean isInbox, int minutesOffset) {
        try {

            String lastSmsId = getLastSmsId(threadName, isInbox);
            if (lastSmsId == null) {
                throw new Exception("Sms Thread " + threadName + " not found");
            }

            long dateOffset = minutesOffset * 60 * 1000; // minutes in millis

            ContentValues values = new ContentValues();
            values.put(Telephony.Sms.DATE_SENT, new Date().getTime() - dateOffset - 3325);
            values.put(Telephony.Sms.DATE, new Date().getTime() - dateOffset);


            String query = String.format("%s=%s", Telephony.Sms._ID, lastSmsId);
            int result = getContentResolver().update(Telephony.Sms.CONTENT_URI, values, query, null);

            String resultText = "Done!";

            if (result == 0) {
                throw new Exception("Sms update query failed. Setup default SMS app.");
            } else if (result != 1) {
                resultText = String.format("Warning: %s sms was modified", result);
            }

            Snackbar.make(view, resultText, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            setThreadNameToSettings(threadName, isInbox);
            setTimeOffsetToSettings(minutesOffset);

        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());

            dumpAllSms();

            Snackbar.make(view, "Exception: " + e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void showSettingsFab(boolean init) {
        FloatingActionButton openSmsSettingsFab = findViewById(R.id.fabOpenSmsSettings);

        if (!checkDefaultSmsAppSettings()) {
            openSmsSettingsFab.setVisibility(View.GONE);
            openSmsSettingsFab.setEnabled(false);
        }
        else {
            openSmsSettingsFab.setVisibility(View.VISIBLE);
            openSmsSettingsFab.setEnabled(true);
        }

        if (init) {
            openSmsSettingsFab.setOnClickListener(v -> {
                openDefaultSmsAppSettings();
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        checkReadPermissions();
        checkWritePermissions();

        TextView timeOffsetValue = findViewById(R.id.timeOffsetValueView);
        TextView threadNameLabel = findViewById(R.id.threadNameLabel);
        TextView threadNameText = findViewById(R.id.editTextSmsThreadName);
        Switch inboxSwitch = findViewById(R.id.inboxSwitch);
        SeekBar timeOffset = findViewById(R.id.timeOffset);
        FloatingActionButton fab = findViewById(R.id.fab);

        timeOffset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeOffsetValue.setText(String.valueOf(progress));
            }
        });
        timeOffset.setProgress(getTimeOffsetFromSettings());

        threadNameText.setText(getThreadNameFromSettings(inboxSwitch.isChecked()));

        fab.setOnClickListener(v -> {
            boolean isInbox = inboxSwitch.isChecked();
            String threadName = threadNameText.getText().toString();

            doSmsModification(v, threadName, isInbox, parseInt(timeOffsetValue.getText().toString()));
        });

        inboxSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            threadNameLabel.setText(isChecked ? R.string.sms_thread_name_hint : R.string.outbox_sms_thread_name_hint);
            threadNameText.setText(getThreadNameFromSettings(isChecked));

            inboxSwitch.setText(isChecked ? R.string.inbox_text : R.string.outbox_text);
        });

        showSettingsFab(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            openDefaultSmsAppSettings();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_DEFAULT_SMS_APP) {
            showSettingsFab(false);
        }
    }
}