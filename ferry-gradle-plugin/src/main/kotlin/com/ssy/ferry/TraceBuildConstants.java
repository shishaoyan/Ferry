/*
 * Tencent is pleased to support the open source community by making wechat-FERRY available.
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

package com.ssy.ferry;

/**
 * Created by caichongyang on 2017/6/20.
 */
public class TraceBuildConstants {

    public final static String FERRY_TRACE_CLASS = "com/ssy/ferry/core/MethodMonitor";
    public final static String FERRY_TRACE_ON_WINDOW_FOCUS_METHOD = "onWindowFocusChanged";
    public final static String FERRY_TRACE_ATTACH_BASE_CONTEXT = "attachBaseContext";
    public final static String FERRY_TRACE_ATTACH_BASE_CONTEXT_ARGS = "(Landroid/content/Context;)V";
    public final static String FERRY_TRACE_APPLICATION_ON_CREATE = "onCreate";
    public final static String FERRY_TRACE_APPLICATION_ON_CREATE_ARGS = "()V";
    public final static String FERRY_TRACE_ACTIVITY_CLASS = "android/app/Activity";
    public final static String FERRY_TRACE_V7_ACTIVITY_CLASS = "android/support/v7/app/AppCompatActivity";
    public final static String FERRY_TRACE_APPLICATION_CLASS = "android/app/Application";
    public final static String FERRY_TRACE_ON_WINDOW_FOCUS_METHOD_ARGS = "(Z)V";
    public static final String[] UN_TRACE_CLASS = {"R.class", "R$", "Manifest", "BuildConfig"};
    public final static String DEFAULT_BLACK_TRACE =
                    "[package]\n"
                    + "-keeppackage android/\n"
                    + "-keeppackage com/tencent/FERRY/\n";

    private static final int METHOD_ID_MAX = 0xFFFFF;
    public static final int METHOD_ID_DISPATCH = METHOD_ID_MAX - 1;
}
