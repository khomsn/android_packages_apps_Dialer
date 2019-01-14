/*
 * Copyright (C) 2014 Xiao-Long Chen <chillermillerlong@hotmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.services.callrecorder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import cyanogenmod.providers.CMSettings;

import java.util.List;

public final class CallRecorderSettings {
    private static final String TAG = CallRecorderSettings.class.getSimpleName();
    private CallRecorderSettings() {
    }

    public static boolean isCallRecorderEnabled(Context context) {
        return CMSettings.System.getInt(context.getContentResolver(),
                CMSettings.System.ENABLE_CALL_RECORDER, 0) != 0;
    }
    
    public static boolean isAutoCallRecorderEnabled(Context context) {
        return CMSettings.System.getInt(context.getContentResolver(),
                CMSettings.System.AUTO_CALL_RECORDER, 0) != 0;
    }
}
