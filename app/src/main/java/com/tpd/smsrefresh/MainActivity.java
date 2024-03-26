package com.tpd.smsrefresh;

import static java.lang.Integer.parseInt;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.tpd.sms.SmsHelper;
import com.tpd.sms.exceptions.SmsInsertionException;
import com.tpd.sms.exceptions.ThreadNotFoundException;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "SmsRefresh";

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    showSettingsFab(false);
                }
            });
    private void checkWritePermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, "android.permission.WRITE_SMS") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{"android.permission.WRITE_SMS", android.Manifest.permission.SEND_SMS},
                    1001);
        }
    }

    private void checkReadPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},
                    999);
        }
    }

    private void openDefaultSmsAppSettings() {

        Intent intent = SmsHelper.createOpenDefaultSmsManagerIntent(MainActivity.this);
        if (intent != null) {
            mStartForResult.launch(intent);
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

            Snackbar.make(view, "Exception: " + e.getMessage(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void showMessage(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    private void doSmsInsertion(View view, String threadName, String body, boolean isInbox, int minutesOffset, boolean createThread) {
        try {
            SettingsHelper.setThreadNameToSettings(MainActivity.this, threadName, isInbox);
            SettingsHelper.setTimeOffsetToSettings(MainActivity.this, minutesOffset);
            SettingsHelper.setMessageBodyToSettings(MainActivity.this, body, isInbox);

            SmsHelper.insertSms(MainActivity.this, threadName, createThread, body, isInbox, minutesOffset);

            Snackbar.make(view, "Done!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        } catch (ThreadNotFoundException threadNotFoundException) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Warning!")
                    .setMessage(String.format("Cannot find thread named: %s. Create it?", threadNotFoundException.getThreadName()))
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // do it again but create thread
                        doSmsInsertion(view, threadName, body, isInbox, minutesOffset, true);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        }
        catch (SmsInsertionException sie) {
            if (SmsHelper.isDefaultSmsManager(MainActivity.this)) {
                showMessage(view, sie.getMessage());
            } else {
                showMessage(view, "Cannot insert SMS, try setup Default SMS app.");
            }
        }
        catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
            showMessage(view,"Exception: " + e.getMessage());
        }
    }


    private void showSettingsFab(boolean init) {
        FloatingActionButton openSmsSettingsFab = findViewById(R.id.fabOpenSmsSettings);

        if (SmsHelper.isDefaultSmsManager(MainActivity.this)) {
            openSmsSettingsFab.setVisibility(View.GONE);
            openSmsSettingsFab.setEnabled(false);
        }
        else {
            openSmsSettingsFab.setVisibility(View.VISIBLE);
            openSmsSettingsFab.setEnabled(true);
        }

        if (init) {
            openSmsSettingsFab.setOnClickListener(v -> openDefaultSmsAppSettings());
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
        SwitchCompat inboxSwitch = findViewById(R.id.inboxSwitch);
        SwitchCompat modeSwitch = findViewById(R.id.switchMode);
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
                doSmsInsertion(v, threadName, messageBody, isInbox, timeOffsetString, false);
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
        } else  if (id == R.id.debug_settings) {
            SmsHelper.dumpAllSms(MainActivity.this);
        }

        return super.onOptionsItemSelected(item);
    }
}