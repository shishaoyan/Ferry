package com.ssy.ferry.core;

/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public class Constants {
    public static final int DEFAULT_EVIL_METHOD_THRESHOLD_MS = 700;
    public static final float FILTER_STACK_KEY_ALL_PERCENT = .3F;
    public static int BUFFER_SIZE = 100 * 10000;// 7.6M
    public static long TIME_UPDATE_CYCLE_MS = 5;
    public static int FILTER_STACK_MAX_COUNT = 60;
    public static long TIME_MILLIS_TO_NANO = 1000000;
    public static long DEFAULT_ANR = 5 * 1000;

    public static int DEFAULT_DROPPED_NORMAL = 3;
    public static int DEFAULT_DROPPED_MIDDLE = 9;
    public static int DEFAULT_DROPPED_HIGH = 24;
    public static int DEFAULT_DROPPED_FROZEN = 42;

    public static long DEFAULT_STARTUP_THRESHOLD_MS_WARM = 4 * 1000;
    public static long DEFAULT_STARTUP_THRESHOLD_MS_COLD = 10 * 1000;

    public static long DEFAULT_RELEASE_BUFFER_DELAY = 15 * 1000;
    public static int TARGET_EVIL_METHOD_STACK = 30;
    public static long MAX_LIMIT_ANALYSE_STACK_KEY_NUM = 10;

    public static long LIMIT_WARM_THRESHOLD_MS = 5 * 1000;

    enum Type {
        NORMAL, ANR, STARTUP
    }

}
