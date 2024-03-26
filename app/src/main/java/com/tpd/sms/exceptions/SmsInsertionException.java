package com.tpd.sms.exceptions;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SmsInsertionException extends Exception {
    public SmsInsertionException(String s) {
        super(s);
    }

    @Override
    @NonNull
    public String getMessage() {
        return Objects.requireNonNull(super.getMessage());
    }
}
