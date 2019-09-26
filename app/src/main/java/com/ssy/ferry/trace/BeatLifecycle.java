package com.ssy.ferry.trace;

public interface BeatLifecycle {

    void onStart();

    void onStop();

    boolean isAlive();
}
