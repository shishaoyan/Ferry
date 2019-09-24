package com.ssy.ferry.monitor.util;

import android.util.Log;

public class L {
    private static final String DEFAULT_TAG = "Ferry";

    public interface LogProxy {
        public void d(String msg);

        public void e(String msg);

        void onRuntimeException(RuntimeException e);
    }

    private static LogProxy sLogProxy;

    public static void setProxy(LogProxy logProxy) {
        sLogProxy = logProxy;
    }


    private static LogProxy getLogProxy() {
        if (sLogProxy == null) {
            return new LogProxy() {

                @Override
                public void d(String msg) {
                    Log.d(DEFAULT_TAG, msg);
                }

                @Override
                public void e(String msg) {
                    Log.e(DEFAULT_TAG, msg);
                }

                @Override
                public void onRuntimeException(RuntimeException e) {
                    throw e;
                }
            };
        }
        return sLogProxy;
    }


    public static void d(String msg) {
        getLogProxy().d(msg);
    }

    public static void e(String msg) {
        getLogProxy().e(msg);
    }

    public static void onRuntimeException(RuntimeException e) {
        getLogProxy().onRuntimeException(e);
    }

}
