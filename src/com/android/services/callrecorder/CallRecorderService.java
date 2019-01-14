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
 * limitations under the License.
 */

package com.android.services.callrecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.provider.Settings;
import android.util.Log;

import com.android.services.callrecorder.common.CallRecording;
import com.android.services.callrecorder.common.ICallRecorderService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.android.dialer.R;

public class CallRecorderService extends Service {
    private static final String TAG = "CallRecorderService";
    private static final boolean DBG = true;
        
    private static final String PREF_NAME = "CRS_Preferences";
    private static final String KEY_CRS_STATE = "call_recorder_service_created";
    private static final String KEY_CRS_READY = "CRS_ready_for_record";

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    
    private static enum RecorderState {
        IDLE,
        RECORDING
    };

    private MediaRecorder mMediaRecorder = null;
    private RecorderState mState = RecorderState.IDLE;
    private CallRecording mCurrentRecording = null;

    private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMdd_HHmmssSSS");

    private final ICallRecorderService.Stub mBinder = new ICallRecorderService.Stub() {
        @Override
        public CallRecording stopRecording() {
            if (getState() == RecorderState.RECORDING) {
                stopRecordingInternal();
                return mCurrentRecording;
            }
            return null;
        }

        @Override
        public boolean startRecording(String phoneNumber, long creationTime)
                throws RemoteException {
            String fileName = generateFilename(phoneNumber);
            mCurrentRecording = new CallRecording(phoneNumber, creationTime,
                    fileName, System.currentTimeMillis());
            return startRecordingInternal(mCurrentRecording.getFile());

        }

        @Override
        public boolean isRecording() throws RemoteException {
            return getState() == RecorderState.RECORDING;
        }

        @Override
        public CallRecording getActiveRecording() throws RemoteException {
            return mCurrentRecording;
        }
    };

    @Override
    public void onCreate() {
        if (DBG) Log.d(TAG, "Creating CallRecorderService");
        /*** Set value to inform other process that
        * CallRecorderService is already created and ready for recording.
        ***/
        // Get SharedPreferences
        sp = getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
        // Save SharedPreferences
        editor = sp.edit();
        editor.putBoolean(KEY_CRS_STATE, true);
        editor.putBoolean(KEY_CRS_READY, false);
        editor.commit();
       
        boolean mCRScreated = sp.getBoolean(KEY_CRS_STATE, true); 
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private static int[] mSampleRates = new int[] { 16000, 8000 };
    
    private synchronized boolean startRecordingInternal(File file) {
        if (mMediaRecorder != null) {
            if (DBG) {
                Log.d(TAG, "Start called with recording in progress, stopping  current recording");
            }
            stopRecordingInternal();
        }

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Record audio permission not granted, can't record call");
            return false;
        }
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "External storage permission not granted, can't save recorded call");
            return false;
        }

        if (DBG) Log.d(TAG, "Starting recording");
        
        // Get SharedPreferences
        sp = getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
       
        boolean mCRScreated = sp.getBoolean(KEY_CRS_STATE, true);

        mMediaRecorder = new MediaRecorder();
        for (short audioSource : new short[] { MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.AudioSource.MIC }){
            for (short audioFormat : new short[] { MediaRecorder.OutputFormat.AMR_WB, MediaRecorder.OutputFormat.AMR_NB }) {
                for (short audioEncoder : new short[] {MediaRecorder.AudioEncoder.AMR_WB, MediaRecorder.AudioEncoder.AMR_NB}) {
                   for (int rate : mSampleRates) {
                        for (int channelConfig : new int[] { 2 , 1 }){
                            try {
                                     if (DBG) Log.d(TAG, "Creating media recorder with audio source " + audioSource + " format " + audioFormat + " encoding " + audioEncoder + " sampling rate " + rate);
                                    mMediaRecorder.setAudioSource(audioSource);
                                    mMediaRecorder.setOutputFormat(audioFormat);
                                    mMediaRecorder.setAudioEncoder(audioEncoder);
                                    mMediaRecorder.setAudioSamplingRate(rate);
                                    mMediaRecorder.setAudioChannels(channelConfig);

                            file.getParentFile().mkdirs();
                            String outputPath = file.getAbsolutePath();
                            if (DBG) Log.d(TAG, "Writing output to file " + outputPath);
                            
                            mMediaRecorder.setOutputFile(outputPath);
                            mMediaRecorder.prepare();

                            mMediaRecorder.start();
                            mState = RecorderState.RECORDING;
                            return true;


                            } catch (Exception e) {
                                mMediaRecorder.reset();
                                Log.e(TAG, "Exception, keep trying.",e);
                                file.delete();
                            }
                        }
                    }
                }
            }
        }

        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;

        return false;
    }

    private synchronized void stopRecordingInternal() {
        if (DBG) Log.d(TAG, "Stopping current recording");
        if (mMediaRecorder != null) {
            try {
                if (getState() == RecorderState.RECORDING) {
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    mMediaRecorder.release();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Exception closing media recorder", e);
            }
            MediaScannerConnection.scanFile(this, new String[] {
                mCurrentRecording.fileName
            }, null, null);
            mMediaRecorder = null;
            mState = RecorderState.IDLE;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DBG) Log.d(TAG, "Destroying CallRecorderService");
        /*** Set value to inform other process that
        * CallRecorderService is already destroy and not aviable for recording.
        ***/
        // Get SharedPreferences
        sp = getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
        // Save SharedPreferences
        editor = sp.edit();
        editor.putBoolean(KEY_CRS_STATE, false);
        editor.putBoolean(KEY_CRS_READY, false);
        editor.commit();
       
        boolean mCRScreated = sp.getBoolean(KEY_CRS_STATE, false); 
    }

    private synchronized RecorderState getState() {
        return mState;
    }

    private String generateFilename(String number) {
        String timestamp = DATE_FORMAT.format(new Date());

        if (TextUtils.isEmpty(number)) {
            number = "unknown";
        }

        String extension = ".amr"; /*".m4a";*/
        return number + "_" + timestamp + extension;
    }

    public static boolean isEnabled(Context context) {
        return CallRecorderSettings.isCallRecorderEnabled(context);
    }
    
    public static boolean isAutoRecordEnabled(Context context) {
        return CallRecorderSettings.isAutoCallRecorderEnabled(context);
    }
}
