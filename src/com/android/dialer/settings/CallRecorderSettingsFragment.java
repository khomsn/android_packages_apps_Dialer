/*
 * Copyright (C) 2014 The CyanogenMod Project
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
 * limitations under the License
 */

package com.android.dialer.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.android.dialer.R;

import cyanogenmod.providers.CMSettings;
import com.android.services.callrecorder.CallRecorderService;

import java.util.Arrays;

public class CallRecorderSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ENABLE_CALL_RECORDER = "enable_call_recorder";

    private static final String KEY_AUTO_CALL_RECORDER = "auto_call_recorder";

    private SwitchPreference mEnableCallRecorder;
    
    private SwitchPreference mAutoCallRecorder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.callrecorder_settings);

        mEnableCallRecorder = (SwitchPreference) findPreference(KEY_ENABLE_CALL_RECORDER);
        mEnableCallRecorder.setOnPreferenceChangeListener(this);

        mAutoCallRecorder = (SwitchPreference) findPreference(KEY_AUTO_CALL_RECORDER);
        mAutoCallRecorder.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        restoreCallrecorderProviderSwitches();
        
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver cr = getActivity().getContentResolver();

        if (preference == mEnableCallRecorder) {
            CMSettings.System.putInt(cr, CMSettings.System.ENABLE_CALL_RECORDER,
                    ((Boolean) newValue) ? 1 : 0);
        } else if (preference == mAutoCallRecorder) {
            CMSettings.System.putInt(cr, CMSettings.System.AUTO_CALL_RECORDER,
                    ((Boolean) newValue) ? 1 : 0);
        }
        
        return true;
    }

    private void restoreCallrecorderProviderSwitches() {
        final ContentResolver cr = getActivity().getContentResolver();
        mEnableCallRecorder.setChecked(CMSettings.System.getInt(cr,
                CMSettings.System.ENABLE_CALL_RECORDER, 1) != 0);
        mAutoCallRecorder.setChecked(CMSettings.System.getInt(cr,
                CMSettings.System.AUTO_CALL_RECORDER, 1) != 0);
    }
}
