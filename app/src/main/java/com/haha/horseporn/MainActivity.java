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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.haha.sms.SmsHelper;

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
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.Date;

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;
import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

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

    private void openDefaultSmsAppSettings() {

        Intent intent = SmsHelper.createOpenDefaultSmsManagerIntent(MainActivity.this);
        if (intent != null) {
            startActivityForResult(intent, REQ_DEFAULT_SMS_APP);
        }
    }

    private void doSmsModification(View view, String threadName, boolean isInbox, int minutesOffset) {
        try {

            SettingsHelper.setThreadNameToSettings(MainActivity.this, threadName, isInbox);
            SettingsHelper.setTimeOffsetToSettings(MainActivity.this, minutesOffset);

            String resultText = "Done!";
            int result = SmsHelper.updateSms(MainActivity.this, threadName, isInbox, minutesOffset);

            if (result == 0) {
                throw new Exception("Sms update query failed. Setup default SMS app.");
            } else if (result != 1) {
                resultText = String.format("Warning: %s sms was modified", result);
            }

            Snackbar.make(view, resultText, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());

            SmsHelper.dumpAllSms(MainActivity.this);

            Snackbar.make(view, "Exception: " + e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void doSmsInsertion(View view, String threadName, String body, boolean isInbox, int minutesOffset) {
        try {
            SettingsHelper.setThreadNameToSettings(MainActivity.this, threadName, isInbox);
            SettingsHelper.setTimeOffsetToSettings(MainActivity.this, minutesOffset);
            SettingsHelper.setMessageBodyToSettings(MainActivity.this, body, isInbox);

            SmsHelper.insertSms(MainActivity.this, threadName, body, isInbox, minutesOffset);

            Snackbar.make(view, "Done!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());

            Snackbar.make(view, "Exception: " + e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }


    private void showSettingsFab(boolean init) {
        FloatingActionButton openSmsSettingsFab = findViewById(R.id.fabOpenSmsSettings);

        if (!SmsHelper.checkIsDefaultSmsManager(MainActivity.this)) {
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

        // pre-check
        checkReadPermissions();
        checkWritePermissions();

        TextView timeOffsetValue = findViewById(R.id.timeOffsetValueView);
        TextView threadNameLabel = findViewById(R.id.threadNameLabel);
        TextView threadNameText = findViewById(R.id.editTextSmsThreadName);
        EditText messageBodyText = findViewById(R.id.textViewSmsBody);
        TextView messageBodyHint = findViewById(R.id.textViewHintSmsBody);
        Switch inboxSwitch = findViewById(R.id.inboxSwitch);
        Switch modeSwitch = findViewById(R.id.switchMode);
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

        // read from settings
        timeOffset.setProgress(SettingsHelper.getTimeOffsetFromSettings(MainActivity.this));
        threadNameText.setText(SettingsHelper.getThreadNameFromSettings(MainActivity.this, inboxSwitch.isChecked()));
        messageBodyText.setText(SettingsHelper.getMessageBodyFromSettings(MainActivity.this, inboxSwitch.isChecked()));

        // setup listeners
        fab.setOnClickListener(v -> {
            boolean isInbox = inboxSwitch.isChecked();
            String threadName = threadNameText.getText().toString();
            int timeOffsetString = parseInt(timeOffsetValue.getText().toString());

            if (modeSwitch.isChecked()) {
                String messageBody = messageBodyText.getText().toString();
                doSmsInsertion(v, threadName, messageBody, isInbox, timeOffsetString);
            }
            else {
                doSmsModification(v, threadName, isInbox, timeOffsetString);
            }
        });

        inboxSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            threadNameLabel.setText(isChecked ? R.string.sms_thread_name_hint : R.string.outbox_sms_thread_name_hint);
            threadNameText.setText(SettingsHelper.getThreadNameFromSettings(MainActivity.this, isChecked));

            inboxSwitch.setText(isChecked ? R.string.inbox_text : R.string.outbox_text);
            messageBodyText.setText(SettingsHelper.getMessageBodyFromSettings(MainActivity.this, isChecked));
        });

        modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                messageBodyHint.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                messageBodyText.setVisibility(isChecked ? View.VISIBLE : View.GONE);

                modeSwitch.setText(isChecked ? R.string.mode_name_send : R.string.mode_name_edit);
        });

        // apply view
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