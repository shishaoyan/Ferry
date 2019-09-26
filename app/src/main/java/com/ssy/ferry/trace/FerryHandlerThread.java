/*
 * Tencent is pleased to support the open source community by making wechat-Ferry available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ssy.ferry.trace;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by zhangshaowen on 17/7/5.
 */

public class FerryHandlerThread {
    private static final String TAG = "Ferry.HandlerThread";

    public static final String Ferry_THREAD_NAME = "default_Ferry_thread";

    /**
     * unite defaultHandlerThread for lightweight work,
     * if you have heavy work checking, you can create a new thread
     */
    private static volatile HandlerThread defaultHandlerThread;
    private static volatile Handler defaultHandler;
    private static volatile Handler defaultMainHandler = new Handler(Looper.getMainLooper());
    private static HashSet<HandlerThread> handlerThreads = new HashSet<>();

    public static Handler getDefaultMainHandler() {
        return defaultMainHandler;
    }

    public static HandlerThread getDefaultHandlerThread() {

        synchronized (FerryHandlerThread.class) {
            if (null == defaultHandlerThread) {
                defaultHandlerThread = new HandlerThread(Ferry_THREAD_NAME);
                defaultHandlerThread.start();
                defaultHandler = new Handler(defaultHandlerThread.getLooper());
            }
            return defaultHandlerThread;
        }
    }

    public static Handler getDefaultHandler() {
        return defaultHandler;
    }

    public static HandlerThread getNewHandlerThread(String name) {
        for (Iterator<HandlerThread> i = handlerThreads.iterator(); i.hasNext();) {
            HandlerThread element = i.next();
            if (!element.isAlive()) {
                i.remove();
            }
        }
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        handlerThreads.add(handlerThread);
        return handlerThread;
    }
}