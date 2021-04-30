package com.haha.sms.exceptions;

public class ThreadNotFoundException extends Exception {
    private final String threadName;

    public ThreadNotFoundException(String threadName) {
        this.threadName = threadName;
    }

    public String getThreadName() {
        return threadName;
    }
}
