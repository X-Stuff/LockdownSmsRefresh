package com.haha.fakesms;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

class FakeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

    }
}

class FakeSmsReceiver extends FakeReceiver {
}

class FakeMmsReceiver extends FakeReceiver {
}

class FakeSmsActivity extends Activity {
}

class FakeSmsSendService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
