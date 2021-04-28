package com.haha.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {

    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    public static final String LOG_TAG = "FAKE_SMS_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SMS_RECEIVED.equals(intent.getAction())) {

            Bundle bundle = intent.getExtras();
            if (bundle != null){
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus");

                    for (Object o : pdus) {
                        SmsMessage message = SmsMessage.createFromPdu((byte[]) o);
                        storeSms(message);
                    }

                } catch(Exception e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }

            Toast.makeText(context, "Warning! SMS received whilst this app was set as manager", 10)
                .show();
        }
    }

    private void storeSms(SmsMessage sms) {

    }
}
