package com.tpd.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {

    public static final String LOG_TAG = "FAKE_SMS_RECEIVER";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SmsHelper.isDefaultSmsManager(context)) {
            return;
        }

        if (SMS_RECEIVED.equals(intent.getAction())) {
            for (SmsMessage message : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                try {
                    SmsHelper.receiveSms(context, message.getOriginatingAddress(), message.getMessageBody());
                    Toast.makeText(context, "New SMS received. Check it out after switching default app back.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                    Toast.makeText(context, "Warning! SMS received whilst this app was set as manager. But wasn't stored!" +
                            "It lost forever now :-(", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }
}
