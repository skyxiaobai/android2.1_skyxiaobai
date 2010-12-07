
/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.camera;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ZoomButtonsController;

import com.android.camera.gallery.IImage;
import com.android.camera.gallery.IImageList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Color;



/**
 * Activity of the Camera which used to see preview and take pictures.
 */
public class Camera extends Activity implements View.OnClickListener,
        ShutterButton.OnShutterButtonListener, SurfaceHolder.Callback,
        Switcher.OnSwitchListener, OnScreenSettings.OnVisibilityChangedListener,
        OnSharedPreferenceChangeListener {

    private static final String TAG = "CAMAPP";


    // force enable capture mode for testing
    public static final boolean ENABLE_CAMERA_PROFILING_LOG = true;


    // Constants of camera setting / setting values
    // TODO: move these constant into camera framework
    public static final String KEY_NUMBER_OF_BURST_SHOT = "burst-no";
    public static final String KEY_CAMERA_MODE = "cam-mode";        // preview mode

    public static final String KEY_LAST_THUMB_URI = "last_thumb_uri";


    // setting values
    public static final int CAMERA_MODE_IMAGE_PREVIEW = 1;
    public static final int CAMERA_MODE_VIDEO_PREVIEW = 2;

    /* constants for handler */
    private static final int CROP_MSG = 1;
    private static final int FIRST_TIME_INIT = 2;
    private static final int RESTART_PREVIEW = 3;
    private static final int CLEAR_SCREEN_DELAY = 4;
    private static final int STORE_DONE = 5;
    private static final int MSG_SELFTIMER_TIMEOUT = 6;
    private static final int MSG_FOCUS_UPDATE = 7;
    private static final int MSG_SAVING_DONE = 8;
    private static final int EV_SELECT = 9;
    private static final int MSG_FIRE_EV_SELECTOR = 10;
    private static final int MSG_EV_SEL_DONE = 11;
    private static final int MSG_SECOND_TIME_INIT = 12;
    private static final int MSG_EV_SAVING_DONE = 13;
    private static final int MSG_SHOW_SAVING_HINT = 14;
    private static final int MSG_UNREGISTER_PREF_CHG_HDLR = 15;

    /* android open, self timer */
    private static final int SELF_TIMER_INTERVAL = 250;
    private static final int SELF_TIMER_SHORT_BOUND = 2000;

    private static final int MAX_EV_SEL_NUM = 3;

    private static final String GPS_MODE_ON = "on";
    private static final String GPS_MODE_OFF = "off";

    private static final int SCREEN_DELAY = 2 * 60 * 1000;
    private static final int FOCUS_BEEP_VOLUME = 100;

/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:23:59 +0800
 * add main/sub sensor tag name is mSrcDev & Tag & count etc;
 */
    private static int mSrcDev = 0;
    boolean mIsSubEnabled = false;
    private ImageView mFunctionPictureButton1;
    int mCount = 1;
// End of Vanzo:zhouwenjie
    private static final String SCENE_MODE_ON = "on";
    private static final String SCENE_MODE_OFF = "off";

    private boolean mZooming = false;
    private boolean mDoubleTapZooming = false;
    
    private boolean mSmoothZoomSupported = false;
    private int mZoomValue;  // The current zoom value.
    private int mZoomMax;

    private Parameters mParameters;
    private Parameters mInitParameters; // keep a permanent copy for Camera parameters

    private OrientationEventListener mOrientationListener;
    private int mLastOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private SharedPreferences mPreferences;

    private static final int IDLE = 1;
    private static final int SNAPSHOT_IN_PROGRESS = 2;

    private static final boolean SWITCH_CAMERA = true;
    private static final boolean SWITCH_VIDEO = false;

    private int mStatus = IDLE;
    private static final String sTempCropFilename = "crop-temp";

    private android.hardware.Camera mCameraDevice;
    private ContentProviderClient mMediaProviderClient;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder = null;
    private ShutterButton mShutterButton;
    private FocusRectangle mFocusRectangle;
    private IconIndicator mGpsIndicator;
    private IconIndicator mFlashIndicator;
    private IconIndicator mFocusIndicator;
    private IconIndicator mWhitebalanceIndicator;
    private IconIndicator mSceneModeIndicator;
    private IconIndicator mSelftimerIndicator;
    private IconIndicator mExposureValueIndicator;
    private ToneGenerator mFocusToneGenerator;
    private ZoomButtonsController mZoomButtons;
    private GestureDetector mGestureDetector;
    private Switcher mSwitcher;
    private boolean mStartPreviewFail = false;
    private Rect mPreviewRect = new Rect();

    // mPostCaptureAlert, mLastPictureButton, mThumbController
    // are non-null only if isImageCaptureIntent() is true.
    private ImageView mLastPictureButton;
    private ThumbnailController mThumbController;

    // mCropValue and mSaveUri are used only if isImageCaptureIntent() is true.
    private String mCropValue;
    private Uri mSaveUri;

    private ImageCapture mImageCapture = null;

    private boolean mPreviewing;
    private boolean mPausing;
    private boolean mFirstTimeInitialized;
    private boolean mIsImageCaptureIntent;
    private boolean mRecordLocation;

    private boolean mFromOnResume = false;
    private boolean mResetSceneMode = false;

    private static final int FOCUS_NOT_STARTED = 0;
    private static final int FOCUSING = 1;
    private static final int FOCUSING_SNAP_ON_FINISH = 2;
    private static final int FOCUS_SUCCESS = 3;
    private static final int FOCUS_FAIL = 4;
    private static final int FOCUSING_SELFTIMER_ON_FINISH = 5;
    private int mFocusState = FOCUS_NOT_STARTED;

    private ContentResolver mContentResolver;
    private boolean mDidRegister = false;

    private final ArrayList<MenuItem> mGalleryItems = new ArrayList<MenuItem>();

    private LocationManager mLocationManager = null;

    // Use OneShotPreviewCallback to measure the time between
    // JpegPictureCallback and preview.
    private final OneShotPreviewCallback mOneShotPreviewCallback =
            new OneShotPreviewCallback();
    private final ShutterCallback mShutterCallback = new ShutterCallback();
    private final PostViewPictureCallback mPostViewPictureCallback =
            new PostViewPictureCallback();
    private final RawPictureCallback mRawPictureCallback =
            new RawPictureCallback();
    private final AutoFocusCallback mAutoFocusCallback =
            new AutoFocusCallback();
    private final ZoomCallback mZoomCallback = new ZoomCallback();
    // Use the ErrorCallback to capture the crash count
    // on the mediaserver
    private final ErrorCallback mErrorCallback = new ErrorCallback();

    private long mFocusStartTime;
    private long mFocusCallbackTime;
    private long mCaptureStartTime;
    private long mShutterCallbackTime;
    private long mPostViewPictureCallbackTime;
    private long mRawPictureCallbackTime;
    private long mJpegPictureCallbackTime;
    private int mPicturesRemaining;

    private TextView mPicturesRemainView;

    // These latency time are for the CameraLatency test.
    public long mAutoFocusTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    public long mPictureDisplayedToJpegCallbackTime;
    public long mJpegCallbackToFirstFrameTime;

    // Add the media server tag
    public static boolean mMediaServerDied = false;
    // Focus mode. Options are pref_camera_focusmode_entryvalues.
    private String mFocusMode;

    private boolean mFocusSupported = false;

    private final Handler mHandler = new MainHandler();
    private OnScreenSettings mSettings;


    private int mDegree;
    private int mBurstNo;
    private int testcnt = 1;
    private int testOpt = 0;
    private long mSetLastPicTime1;
    private long mSetLastPicTime2;
    /* self timer */
    private int mSelfTimerMode;
    private static final int STATE_SELF_TIMER_IDLE = 0;
    private static final int STATE_SELF_TIMER_COUNTING = 1;
    private static final int STATE_SELF_TIMER_SNAP = 2;
    private int mSelfTimerState;

    private static final int CAMAPP_STATE_IDLE = 0;
    private static final int CAMAPP_STATE_PREVIEWING = 1;
    private static final int CAMAPP_STATE_SAVING = 5;
    private int mCameraState = CAMAPP_STATE_PREVIEWING;     // only use here for now.

    private Bitmap mLastPictureThumb;
    private Thread mJpgSaving = null;


    private String mCaptureMode;
    private Location mLastJpegLoc;

    private String [] mEvImageSelected = new String [MAX_EV_SEL_NUM];
    
    private static final String EV_IMG_PREFIX = "/cap0";
    private static final String INTENT_EV_IMG_PREFIX = "/icp0";    

    private static final int LCD_SIZE_UNKNOWN = 0;
    private static final int LCD_SIZE_QVGA = 1;
    private static final int LCD_SIZE_WQVGA = 2;
    private static final int LCD_SIZE_HVGA = 3;
    private static final int LCD_SIZE_WVGA = 4;

    private int mLcdSize = LCD_SIZE_UNKNOWN;
    private OnScreenHint mStorageHint;


    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RESTART_PREVIEW: {
                    restartPreview();
                    break;
                }

                case CLEAR_SCREEN_DELAY: {
                    getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }

                case FIRST_TIME_INIT: {
                    initializeFirstTime();
                    break;
                }

                case STORE_DONE: {
                    if (testOpt == 1) {
                        Sleep(2000);
                        long t = System.currentTimeMillis();
                        boolean bl = (t % 2 == 1) ? true : false;
                        //doZoom(bl);
                        doFocus(true);
                        doSnap();
                    }
                    break;
                }

                case MSG_SELFTIMER_TIMEOUT: {
                    if (!mPausing) selfTimerTimeout();
                    break;
                }

                case MSG_FOCUS_UPDATE: {
                    Log.d(TAG, "[focus] MSG_FOCUS_UPDATE: mState " + mFocusState);
                    updateFocusIndicator();
                    break;
                }

                case MSG_SAVING_DONE: {
                    if (!mPausing) {
                        //updateSavingHint(false);
                        if (!mIsImageCaptureIntent) {
                            setLastPictureThumb(mLastPictureThumb, mDegree, mImageCapture.getLastCaptureUri());
                        } else {
                            showPostCaptureAlert();
                        }
                        mCameraState = CAMAPP_STATE_PREVIEWING;
                        checkStorage();
                        mLastPictureThumb = null;
                    }
                    mJpgSaving = null;
                    break;
                }

                case MSG_FIRE_EV_SELECTOR: {
                    fireEvSelector();
                    break;
                }

                case MSG_EV_SEL_DONE: {
                    try {
                        handleEvSelectCallback();
                    } catch(IOException ex) {
                        // do nothing, since it is simply that bf cannot created.
                        // and since it is not created, don't have to close it.
                    }
                    break;
                }
                
                case MSG_SECOND_TIME_INIT: {
                    initializeSecondTime();
                    break;
                }

                case MSG_EV_SAVING_DONE: {
                    if (!mPausing) {
                        updateSavingHint(false);
                        if (!mIsImageCaptureIntent) {
                            setLastPictureThumb(mLastPictureThumb, mDegree, mImageCapture.getLastCaptureUri());
                        }
                        mCameraState = CAMAPP_STATE_PREVIEWING;
                    }
                    mJpgSaving = null;
                    break;
                }
                
                case MSG_SHOW_SAVING_HINT: {
                    updateSavingHint(true);
                    break;
                }

                case MSG_UNREGISTER_PREF_CHG_HDLR: {
                    unregisterOnSharedPreferenceChangedListener();
                    break;
                }
            }
        }
    }

    private void Sleep(long time) {
        // delay
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void keepMediaProviderInstance() {
        // We want to keep a reference to MediaProvider in camera's lifecycle.
        // TODO: Utilize mMediaProviderClient instance to replace
        // ContentResolver calls.
        if (mMediaProviderClient == null) {
            mMediaProviderClient = getContentResolver()
                    .acquireContentProviderClient(MediaStore.AUTHORITY);
        }
    }

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
        if (mFirstTimeInitialized) return;

        Log.v(TAG, "initializeFirstTime()");

        // Create orientation listenter. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener =
                new OrientationEventListener(Camera.this) {
            @Override
            public void onOrientationChanged(int orientation) {
                // We keep the last known orientation. So if the user
                // first orient the camera then point the camera to
                // floor/sky, we still have the correct orientation.
                if (orientation != ORIENTATION_UNKNOWN) {
                    mLastOrientation = orientation;
                }
            }
        };
        mOrientationListener.enable();

        // Initialize location sevice.
        mLocationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        readPreference();
        if (mRecordLocation) startReceivingLocationUpdates();

        keepMediaProviderInstance();
        checkStorage();
        // Initialize last picture button.
        mContentResolver = getContentResolver();
        if (!mIsImageCaptureIntent)  {
            findViewById(R.id.camera_switch).setOnClickListener(this);
            mLastPictureButton =
                    (ImageView) findViewById(R.id.review_thumbnail);
            mLastPictureButton.setOnClickListener(this);
            mThumbController = new ThumbnailController(
                    getResources(), mLastPictureButton, mContentResolver);
            mThumbController.loadData(ImageManager.getLastImageThumbPath());
            // Update last image thumbnail.
            updateThumbnailButton();
        }

        // Initialize shutter button.
        mShutterButton = (ShutterButton) findViewById(R.id.shutter_button);
        mShutterButton.setOnShutterButtonListener(this);
        mShutterButton.setVisibility(View.VISIBLE);

        mFocusRectangle = (FocusRectangle) findViewById(R.id.focus_rectangle);
        updateFocusIndicator();

        // Initialize GPS indicator.
        mGpsIndicator = (IconIndicator) findViewById(R.id.gps_icon);

        ImageManager.ensureOSXCompatibleFolder();

        installIntentFilter();

        initializeFocus();      //initialize focus, also update indicators
        initSelfTimerTone();

        initializeZoom();
        updateUIduringCapture(View.VISIBLE);
        
        mFirstTimeInitialized = true;
        updateStorageHint(mPicturesRemaining);
    }

    private void updateThumbnailButton() {
        
        if (mPreferences != null) {
            String lastThumbUri = mPreferences.getString(KEY_LAST_THUMB_URI, null);
            if (lastThumbUri != null) {
                if(!mThumbController.isUriValid() 
                    || !mThumbController.getUri().toString().equals(lastThumbUri)) {
                    // set to null to prevent transition
                    mThumbController.setData(null, null);
                    mThumbController.loadData(ImageManager.getLastImageThumbPath());
                }
            }
        }
        
        // Update last image if URI is invalid and the storage is ready.
        if ((!mThumbController.isUriValid() && mPicturesRemaining >= 0)
                || !isUriPathValid(mThumbController.getUri())) {
            updateLastImage();
        }
        mThumbController.updateDisplayIfNeeded();
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:26:53 +0800
 * add main/sub sensor switch function
 */
    public void cameraSwitch() {
        stopPreview();
        try {
            startPreview();
         } catch (CameraHardwareException e) {
             showCameraErrorAndFinish();
         }
    }
// End of Vanzo:zhouwenjie
    private void initializeSecondTime() {
        Log.v(TAG, "initializeSecondTime()");
        // Start orientation listener as soon as possible because it takes
        // some time to get first orientation.
        mOrientationListener.enable();

        // Start location update if needed.
        readPreference();
        if (mRecordLocation) startReceivingLocationUpdates();

        installIntentFilter();

        initializeFocus();
        initSelfTimerTone();
        
        updateUIduringCapture(View.VISIBLE);

        keepMediaProviderInstance();
        checkStorage();

        if (mZoomButtons != null) {
            mCameraDevice.setZoomCallback(mZoomCallback);
        }

        if (!mIsImageCaptureIntent) {
            updateThumbnailButton();
        }
    }

    private void initializeZoom() {
        if (!mParameters.isZoomSupported()) return;

        mZoomMax = mParameters.getMaxZoom();
        Log.v(TAG, "Max zoom=" + mZoomMax);
        mSmoothZoomSupported = mParameters.isSmoothZoomSupported();
        Log.v(TAG, "Smooth zoom supported=" + mSmoothZoomSupported);

        mGestureDetector = new GestureDetector(this, new ZoomGestureListener());
        mCameraDevice.setZoomCallback(mZoomCallback);
        mZoomButtons = new ZoomButtonsController(mSurfaceView);
        mZoomButtons.setAutoDismissed(true);
        mZoomButtons.setZoomSpeed(100);
        mZoomButtons.setOnZoomListener(
                new ZoomButtonsController.OnZoomListener() {
            public void onVisibilityChanged(boolean visible) {
                if (visible) {
                    updateZoomButtonsEnabled();
                }
            }

            public void onZoom(boolean zoomIn) {
                Log.v(TAG, "onZoom()");
                if (!isCameraIdle()) return;

                if (zoomIn) {
                    if (mZoomValue < mZoomMax) {
                        if (mSmoothZoomSupported) {
                            mCameraDevice.startSmoothZoom(mZoomValue + 1);
                            mZooming = true;
                        } else {
                            mParameters.setZoom(++mZoomValue);
                            mCameraDevice.setParameters(mParameters);
                            updateZoomButtonsEnabled();
                            mZooming = true;
                        }
                    }
                } else {
                    if (mZoomValue > 0) {
                        if (mSmoothZoomSupported) {
                            mCameraDevice.startSmoothZoom(mZoomValue - 1);
                            mZooming = true;
                        } else {
                            mParameters.setZoom(--mZoomValue);
                            mCameraDevice.setParameters(mParameters);
                            updateZoomButtonsEnabled();
                            mZooming = true;
                        }
                    }
                }
            }
        });
    }


    private void unregisterOnSharedPreferenceChangedListener() {
        if (mPreferences != null) {
            mPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    public void onVisibilityChanged(boolean visible) {
        // When the on-screen setting is not displayed, we show the gripper.
        // When the on-screen setting is displayed, we hide the gripper.
        int reverseVisibility = visible ? View.INVISIBLE : View.VISIBLE;
        findViewById(R.id.btn_gripper).setVisibility(reverseVisibility);
        findViewById(R.id.indicator_bar).setVisibility(reverseVisibility);

        // an workaround of in an extereme case that visibilty change listener 
        // will be invoked before item click event, so start a timer for it.
        mHandler.removeMessages(MSG_UNREGISTER_PREF_CHG_HDLR);
        if (visible) {
            mPreferences.registerOnSharedPreferenceChangeListener(this);
        } else {
            mHandler.sendEmptyMessageDelayed(MSG_UNREGISTER_PREF_CHG_HDLR, 500);
            // mPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    private boolean isZooming() {
        Log.v(TAG, "mZooming=" + mZooming + "," + mDoubleTapZooming);
        return (mZooming || mDoubleTapZooming);
    }

    private void updateZoomButtonsEnabled() {
        mZoomButtons.setZoomInEnabled(mZoomValue < mZoomMax);
        mZoomButtons.setZoomOutEnabled(mZoomValue > 0);
    }

    private class ZoomGestureListener extends
            GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            // Show zoom buttons only when preview is started and snapshot
            // is not in progress. mZoomButtons may be null if it is not
            // initialized.
            if (!mPausing && isCameraIdle() && mPreviewing
                    && mZoomButtons != null) {
/* Vanzo:zhouwenjie on: Wed, 20 Oct 2010 16:55:08 +0800
 * sub sensor not support zoom button
                mZoomButtons.setVisible(true);
 */
                if (!mIsSubEnabled) {
                    mZoomButtons.setVisible(true);
                }
// End of Vanzo:zhouwenjie
            }
            return true;
        }

        // Original value is 5
        // but preview frame rate is 30 fps, it will update with the current values
        // so if we set zoom value faster than 30fps, the zoom command will be dropped
        // change it to 40 to make the jobe done.
        private static final int DOUBLE_TAP_ZOOM_DELAY = 40;

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Perform zoom only when preview is started and snapshot is not in
            // progress.
            if (mPausing || !isCameraIdle() || !mPreviewing
                    || mZoomButtons == null || isZooming()) {
                return false;
            }

/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:28:23 +0800
 * cancel sub sensor zoom function
            if (mZoomValue < mZoomMax) {
                // Zoom in to the maximum.
                mDoubleTapZooming = true;
                while (mZoomValue < mZoomMax) {
                    mParameters.setZoom(++mZoomValue);
                    mCameraDevice.setParameters(mParameters);
                    // Wait for a while so we are not changing zoom too fast.
                    try {
                        Thread.sleep(DOUBLE_TAP_ZOOM_DELAY);
                    } catch (InterruptedException ex) {
                    }
                }
            } else {
                // Zoom out to the minimum.
                mDoubleTapZooming = true;
                while (mZoomValue > 0) {
                    mParameters.setZoom(--mZoomValue);
                    mCameraDevice.setParameters(mParameters);
                    // Wait for a while so we are not changing zoom too fast.
                    try {
                        Thread.sleep(DOUBLE_TAP_ZOOM_DELAY);
                    } catch (InterruptedException ex) {
                        // ignore
                    }
                }
            }
 */
            if (!mIsSubEnabled) {
                if (mZoomValue < mZoomMax) {
                    // Zoom in to the maximum.
                    mDoubleTapZooming = true;
                    while (mZoomValue < mZoomMax) {
                        mParameters.setZoom(++mZoomValue);
                        mCameraDevice.setParameters(mParameters);
                        // Wait for a while so we are not changing zoom too fast.
                        try {
                            Thread.sleep(DOUBLE_TAP_ZOOM_DELAY);
                        } catch (InterruptedException ex) {
                        }
                    }
                } else {
                    // Zoom out to the minimum.
                    mDoubleTapZooming = true;
                    while (mZoomValue > 0) {
                        mParameters.setZoom(--mZoomValue);
                        mCameraDevice.setParameters(mParameters);
                        // Wait for a while so we are not changing zoom too fast.
                        try {
                            Thread.sleep(DOUBLE_TAP_ZOOM_DELAY);
                        } catch (InterruptedException ex) {
                            // ignore
                        }
                    }
                }
            }
// End of Vanzo:zhouwenjie
            updateZoomButtonsEnabled();
            return true;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        if (!super.dispatchTouchEvent(m) && mGestureDetector != null) {
            return mGestureDetector.onTouchEvent(m);
        }
        return true;
    }

    LocationListener [] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)
                    || action.equals(Intent.ACTION_MEDIA_CHECKING)
                    || action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
                checkStorage();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                checkStorage();
                if (!mIsImageCaptureIntent)  {
                    updateThumbnailButton();
                }
            }
        }
    };

    private class LocationListener
            implements android.location.LocationListener {
        Location mLastLocation;
        boolean mValid = false;
        String mProvider;

        public LocationListener(String provider) {
            mProvider = provider;
            mLastLocation = new Location(mProvider);
        }

        public void onLocationChanged(Location newLocation) {
            if (newLocation.getLatitude() == 0.0
                    && newLocation.getLongitude() == 0.0) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            // If GPS is available before start camera, we won't get status
            // update so update GPS indicator when we receive data.
            if (mRecordLocation
                    && LocationManager.GPS_PROVIDER.equals(mProvider)) {
                mGpsIndicator.setMode(GPS_MODE_ON);
            }
            mLastLocation.set(newLocation);
            mValid = true;
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
            mValid = false;
        }

        public void onStatusChanged(
                String provider, int status, Bundle extras) {
            switch(status) {
                case LocationProvider.OUT_OF_SERVICE:
                case LocationProvider.TEMPORARILY_UNAVAILABLE: {
                    mValid = false;
                    if (mRecordLocation && LocationManager.GPS_PROVIDER.equals(provider)) {
                        mGpsIndicator.setMode(GPS_MODE_OFF);
                    }
                    break;
                }
            }
        }

        public Location current() {
            return mValid ? mLastLocation : null;
        }
    }

    private final class OneShotPreviewCallback
            implements android.hardware.Camera.PreviewCallback {
        public void onPreviewFrame(byte[] data,
                                   android.hardware.Camera camera) {
            if (ENABLE_CAMERA_PROFILING_LOG) {
                long now = System.currentTimeMillis();
                if (mJpegPictureCallbackTime != 0) {
                    mJpegCallbackToFirstFrameTime = now - mJpegPictureCallbackTime;
                    Log.v(TAG, "mJpegCallbackToFirstFrameTime = "
                            + mJpegCallbackToFirstFrameTime + "ms");
                    mJpegPictureCallbackTime = 0;
                } else {
                    Log.v(TAG, "Got first frame");
                }
            } else {
                Log.v(TAG, "OneShotPreviewCallback()");
            }
        }
    }

    private final class ShutterCallback
            implements android.hardware.Camera.ShutterCallback {
        public void onShutter() {
            if (ENABLE_CAMERA_PROFILING_LOG) {
                mShutterCallbackTime = System.currentTimeMillis();
                mShutterLag = mShutterCallbackTime - mCaptureStartTime;
                Log.v(TAG, "mShutterLag = " + mShutterLag + "ms");
            } else {
                Log.v(TAG, "ShutterCallback()");
            }
            clearFocusState();
        }
    }

    private final class PostViewPictureCallback implements PictureCallback {
        public void onPictureTaken(
                byte [] data, android.hardware.Camera camera) {
            if (ENABLE_CAMERA_PROFILING_LOG) {
                mPostViewPictureCallbackTime = System.currentTimeMillis();
                Log.v(TAG, "mShutterToPostViewCallbackTime = "
                        + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                        + "ms");
            } else {
                Log.v(TAG, "PostViewPictureCallback()");
            }
            
        }
        
    }

    private final class RawPictureCallback implements PictureCallback {
        public void onPictureTaken(
                byte [] rawData, android.hardware.Camera camera) {
            if (ENABLE_CAMERA_PROFILING_LOG) {                
                mRawPictureCallbackTime = System.currentTimeMillis();
                Log.v(TAG, "mShutterToRawCallbackTime = "
                        + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
            } else {
                Log.v(TAG, "RawPictureCallback()");
            }
            
        }
    }

    private final class JpegPictureCallback implements PictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
            mLastJpegLoc = loc;     //for EV bracket shot.
        }

        public void onPictureTaken(
                final byte [] jpegData, final android.hardware.Camera camera) {
            if (mPausing) {
                return;
            }

           
            if (ENABLE_CAMERA_PROFILING_LOG){
                logJpegCallbackTime();
            } else {
                Log.v(TAG, "JpegPictureCallback()");
            }

            if (mCaptureMode.equals(Parameters.CAPTURE_MODE_NORMAL)
                || mCaptureMode.equals(Parameters.CAPTURE_MODE_BEST_SHOT)) {

                // Normal Mode

                // decode thumb before restart preview to improve performance.
                //test_dumpJpegDataToFile(jpegData, true);
                mSetLastPicTime1 = System.currentTimeMillis();
                Bitmap lastPictureThumb = decodeLastPictureThumb(jpegData);
                mSetLastPicTime2 = System.currentTimeMillis();
                mLastPictureThumb = lastPictureThumb;

                
                if (!mIsImageCaptureIntent) {

                    // We want to show the taken picture for a while, so we wait
                    // for at least 1.2 second before restarting the preview.
                    /* 
                    long delay = 1200 - mPictureDisplayedToJpegCallbackTime;
                    if (delay < 0) {
                        restartPreview();
                    } else {
                        mHandler.sendEmptyMessageDelayed(RESTART_PREVIEW, delay);
                    }*/
                    mBurstNo--;
                    Log.v(TAG, "mBurstNo = " + mBurstNo);
                    if (mBurstNo == 0) {
                        restartPreview();
                        if (ENABLE_CAMERA_PROFILING_LOG ) {
                            Log.v(TAG, "restartPreview time = " 
                                    + (System.currentTimeMillis() - mSetLastPicTime2));
                        }
                    }

                    mJpgSaving = new Thread(new Runnable() {
                        public void run() {
                            mImageCapture.storeImage(jpegData, camera, mLocation);
                            mHandler.sendEmptyMessage(MSG_SAVING_DONE);
                        }
                    });
//                  updateSavingHint(true);
                    mCameraState = CAMAPP_STATE_SAVING;
                    mJpgSaving.start();
                } else {
                    mImageCapture.storeImage(jpegData, camera, mLocation);
                }

                dumpPictureTakenLogs();
                if (testOpt == 1) {
                    mHandler.sendEmptyMessage(STORE_DONE);
                }

                // Calculate this in advance of each shot so we don't add to shutter
                // latency. It's true that someone else could write to the SD card in
                // the mean time and fill it, but that could have happened between the
                // shutter press and saving the JPEG too.
                calculatePicturesRemaining();
                
            } else if (mCaptureMode.equals(Parameters.CAPTURE_MODE_EV_BRACKET_SHOT)) {
                mHandler.sendEmptyMessage(MSG_FIRE_EV_SELECTOR);
            }
            updateUIduringCapture(View.VISIBLE);
            // the switcher will short and cannot set to correct position if callback during capture
            // reset it here.
            if (!isImageCaptureIntent()) {
                mSwitcher.resetSwitch(SWITCH_CAMERA);
            }
        }

        private void logJpegCallbackTime (){
            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
            if (mPostViewPictureCallbackTime != 0) {
                mShutterToPictureDisplayedTime =
                        mPostViewPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mPostViewPictureCallbackTime;
            } else {
                mShutterToPictureDisplayedTime =
                        mRawPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mRawPictureCallbackTime;
            }
            Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                    + mPictureDisplayedToJpegCallbackTime + "ms");
        }
    }

    private void test_dumpJpegDataToFile(final byte [] jpegData, boolean a) {
        FileOutputStream fo;

        try {
            if (a) {
                fo = new FileOutputStream("/sdcard/before.jpg");
            } else {
                fo = new FileOutputStream("/sdcard/after.jpg");
            }
            fo.write(jpegData);
            fo.close();
        } catch (FileNotFoundException ex) {
            // do nothing (test only)
        } catch (IOException e) {
        }
    }

    private void dumpPictureTakenLogs() {

        if (!ENABLE_CAMERA_PROFILING_LOG) return;

        long currentTime = System.currentTimeMillis();
        Log.v("camera-pn", String.format("!!Time, %03d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d ",
                                testcnt,
                                mFocusStartTime,
                                mFocusCallbackTime,
                                mCaptureStartTime,
                                mShutterCallbackTime,
                                mRawPictureCallbackTime,
                                mJpegPictureCallbackTime,
                                mSetLastPicTime1,
                                mSetLastPicTime2,
                                ImageManager.mSaveTime1,
                                ImageManager.mSaveTime2,
                                currentTime ));
        Log.v(TAG, "       Count   Focus   Start  Shutter  Raw    Jpeg    B-Thumb  A-Thumb  B-Save  A-Save  End,  Capture, Total \n");
        Log.v(TAG, String.format("!!Time, %03d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d \n",
                            testcnt,
                            (mFocusCallbackTime - mFocusStartTime),
                            (mCaptureStartTime - mFocusStartTime),
                            (mShutterCallbackTime - mFocusStartTime),
                            (mRawPictureCallbackTime - mFocusStartTime),
                            (mJpegPictureCallbackTime - mFocusStartTime),
                            (mSetLastPicTime1 - mFocusStartTime),
                            (mSetLastPicTime2 - mFocusStartTime),
                            (ImageManager.mSaveTime1 - mFocusStartTime),
                            (ImageManager.mSaveTime2 - mFocusStartTime),
                            (currentTime - mFocusStartTime)));
        Log.v(TAG, String.format("!!Diff, %03d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d, %6d \n\n",
                        testcnt,
                        (mFocusCallbackTime - mFocusStartTime),
                        (mCaptureStartTime - mFocusCallbackTime),
                        (mShutterCallbackTime - mCaptureStartTime),
                        (mRawPictureCallbackTime - mShutterCallbackTime),
                        (mJpegPictureCallbackTime - mRawPictureCallbackTime),
                        (mSetLastPicTime1 - mJpegPictureCallbackTime),
                        (mSetLastPicTime2 - mSetLastPicTime1),
                        (ImageManager.mSaveTime1 - mSetLastPicTime2),
                        (ImageManager.mSaveTime2 - ImageManager.mSaveTime1),
                        (System.currentTimeMillis() - ImageManager.mSaveTime2),
                        mJpegPictureCallbackTime - mCaptureStartTime,
                        (currentTime - mCaptureStartTime) + (mFocusCallbackTime - mFocusStartTime)));
        if (testcnt >= 1) {
            testcnt++;
        }
    }

    private final class AutoFocusCallback
            implements android.hardware.Camera.AutoFocusCallback {
        public void onAutoFocus(
                boolean focused, android.hardware.Camera camera) {


            if (mPausing || mCameraDevice == null) {
                Log.e(TAG, "onAutoFocus() when pausing or CameraDevice is gone");
                return;
            }
            
            mFocusCallbackTime = System.currentTimeMillis();
            mAutoFocusTime = mFocusCallbackTime - mFocusStartTime;
            Log.v(TAG, "mAutoFocusTime = " + mAutoFocusTime + "ms");
            Parameters params;
            params = mCameraDevice.getParameters();
            mFocusRectangle.setFocusWinStr(params.get("focus-win"));
            if (mFocusState == FOCUSING_SNAP_ON_FINISH) {
                // Take the picture no matter focus succeeds or fails. No need
                // to play the AF sound if we're about to play the shutter
                // sound.
                if (focused) {
                    mFocusState = FOCUS_SUCCESS;
                } else {
                    mFocusState = FOCUS_FAIL;
                }
                mFocusRectangle.clearFocusWin();
                mImageCapture.onSnap();
            } else if (mFocusState == FOCUSING_SELFTIMER_ON_FINISH) {
                if (focused) {
                    mFocusState = FOCUS_SUCCESS;
                } else {
                    mFocusState = FOCUS_FAIL;
                }
                selfTimerStart();

            } else if (mFocusState == FOCUSING) {
                // User is half-pressing the focus key. Play the focus tone.
                // Do not take the picture now.
                ToneGenerator tg = mFocusToneGenerator;
                if (tg != null) {
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
                }
                if (focused) {
                    mFocusState = FOCUS_SUCCESS;
                } else {
                    mFocusState = FOCUS_FAIL;
                }
            } else if (mFocusState == FOCUS_NOT_STARTED) {
                // User has released the focus key before focus completes.
                // Do nothing.
            }
            updateFocusIndicator();
        }
    }

    private final class ErrorCallback
        implements android.hardware.Camera.ErrorCallback {
        public void onError(int error, android.hardware.Camera camera) {
            if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {
                 mMediaServerDied = true;
                 Log.v(TAG, "media server died");
            }
        }
    }

    private final class ZoomCallback
            implements android.hardware.Camera.ZoomCallback {
        public void onZoomUpdate(int zoomValue, boolean stopped,
                                 android.hardware.Camera camera) {
            Log.v(TAG, "ZoomCallback: zoom value=" + zoomValue + ". stopped="
                    + stopped);
            mZoomValue = zoomValue;
            // Keep mParameters up to date. We do not getParameter again in
            // takePicture. If we do not do this, wrong zoom value will be set.
            mParameters.setZoom(zoomValue);
            // We only care if the zoom is stopped. mZooming is set to true when
            // we start smooth zoom.
            if (stopped) {
                mZooming = false;
                if (mDoubleTapZooming && (zoomValue == 0 || zoomValue == mZoomMax)) {
                    mDoubleTapZooming = false;
                }
            }
            updateZoomButtonsEnabled();
        }
    }

    private class ImageCapture {

        private boolean mCancel = false;

        private Uri mLastContentUri;

        byte[] mCaptureOnlyData;

        // Returns the rotation degree in the jpeg header.
        private int storeImage(byte[] data, Location loc) {
            try {
                long dateTaken = System.currentTimeMillis();
                String name = createName(dateTaken) + ".jpg";
                int[] degree = new int[1];
                mLastContentUri = ImageManager.addImage(
                        mContentResolver,
                        name,
                        dateTaken,
                        loc, // location from gps/network
                        ImageManager.CAMERA_IMAGE_BUCKET_NAME, name,
                        null, data,
                        degree);
                if (mLastContentUri == null) {
                    // this means we got an error
                    mCancel = true;
                }
                if (!mCancel) {
                    /*
                    ImageManager.setImageSize(mContentResolver, mLastContentUri,
                            new File(ImageManager.CAMERA_IMAGE_BUCKET_NAME,
                            name).length());
                    */
                    long tt1 = System.currentTimeMillis();
                    ImageManager.setImageSize(mContentResolver, mLastContentUri,
                            new File(ImageManager.CAMERA_IMAGE_BUCKET_NAME,
                            name).length());
                    long tt2 = System.currentTimeMillis();
                    Log.v(TAG, "ImageManager.setImageSize needs " + (tt2 - tt1));
                }
                return degree[0];
            } catch (Exception ex) {
                Log.e(TAG, "Exception while compressing image.", ex);
                return 0;
            }
        }

        public void storeImage(final byte[] data, android.hardware.Camera camera, Location loc) {
            if (!mIsImageCaptureIntent) {

                int degree = storeImage(data, loc);
                sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", mLastContentUri));
                mDegree = degree;
                /*
                setLastPictureThumb(data, degree,
                        mImageCapture.getLastCaptureUri());
                mThumbController.updateDisplayIfNeeded();
                */
            } else {
                mCaptureOnlyData = data;
                showPostCaptureAlert();
            }
        }

        // for EV bracket shot
        public String storeImage(String [] inFiles, String newFilePath, Location loc) {


            long dateTaken = System.currentTimeMillis();
            String prefix = createName(dateTaken);
            int i = 0;
            for (String s : inFiles) {
                if (s == null) {
                    continue;
                }
                String name = prefix + "." + i + ".jpg";
                int[] degree = new int[1];
                mLastContentUri = ImageManager.addImage(
                        mContentResolver,
                        name,
                        dateTaken,
                        loc, // location from gps/network
                        ImageManager.CAMERA_IMAGE_BUCKET_NAME, name,
                        s,
                        degree);
                mDegree = degree[0];
                i++;
            }
            return prefix;
        }



        /**
         * Initiate the capture of an image.
         */
        public void initiate() {
            if (mCameraDevice == null) {
                return;
            }

            mCancel = false;

            capture();
        }

        public Uri getLastCaptureUri() {
            return mLastContentUri;
        }

        public byte[] getLastCaptureData() {
            return mCaptureOnlyData;
        }

        private void capture() {
            mCaptureOnlyData = null;

            // Set rotation.
            int orientation = mLastOrientation;
            if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
                orientation += 90;
            }
            orientation = ImageManager.roundOrientation(orientation);
            Log.v(TAG, "mLastOrientation = " + mLastOrientation
                    + ", orientation = " + orientation);
            mParameters.setRotation(orientation);

            // Clear previous GPS location from the parameters.
            mParameters.removeGpsData();

            // Set GPS location.
            Location loc = mRecordLocation ? getCurrentLocation() : null;
            if (loc != null) {
                double lat = loc.getLatitude();
                double lon = loc.getLongitude();
                boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

                if (hasLatLon) {
                    mParameters.setGpsLatitude(lat);
                    mParameters.setGpsLongitude(lon);
                    if (loc.hasAltitude()) {
                        mParameters.setGpsAltitude(loc.getAltitude());
                    } else {
                        // for NETWORK_PROVIDER location provider, we may have
                        // no altitude information, but the driver needs it, so
                        // we fake one.
                        mParameters.setGpsAltitude(0);
                    }
                    if (loc.getTime() != 0) {
                        // Location.getTime() is UTC in milliseconds.
                        // gps-timestamp is UTC in seconds.
                        long utcTimeSeconds = loc.getTime() / 1000;
                        mParameters.setGpsTimestamp(utcTimeSeconds);
                    }
                } else {
                    loc = null;
                }
            }

            mCameraDevice.setParameters(mParameters);

            mCameraDevice.takePicture(mShutterCallback, mRawPictureCallback,
                    mPostViewPictureCallback, new JpegPictureCallback(loc));
            mPreviewing = false;
        }

        public void onSnap() {

            Log.v(TAG, "onSnap(), Status:" + mStatus
                    + " selfTimer:" + mSelfTimerState
                    + " state:" + mCameraState);
            // If we are already in the middle of taking a snapshot then ignore.
            if (mPausing
                || mStatus == SNAPSHOT_IN_PROGRESS
                || mSelfTimerState == STATE_SELF_TIMER_COUNTING) {
                return;
            }

            mCaptureStartTime = System.currentTimeMillis();
            mPostViewPictureCallbackTime = 0;

            // Don't check the filesystem here, we can't afford the latency.
            // Instead, check the cached value which was calculated when the
            // preview was restarted.

            if (updateStorageHint(mPicturesRemaining) < 1) {
                return;
            }

            if (mCameraState == CAMAPP_STATE_SAVING) {
                //Log.v(TAG,"[CAMAPP] saving, onSnap() ignored");
                return;
            }

            mStatus = SNAPSHOT_IN_PROGRESS;

            mImageCapture.initiate();
        }

        private void clearLastData() {
            mCaptureOnlyData = null;
        }
    }

    public boolean saveDataToFile(String filePath, byte[] data) {
        FileOutputStream f = null;
        try {
            f = new FileOutputStream(filePath);
            f.write(data);
        } catch (IOException e) {
            return false;
        } finally {
            MenuHelper.closeSilently(f);
        }
        return true;
    }

    /*
    private void setLastPictureThumb(byte[] data, int degree, Uri uri) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 16;
        Bitmap lastPictureThumb =
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
        lastPictureThumb = Util.rotate(lastPictureThumb, degree);
        mThumbController.setData(uri, lastPictureThumb);
    }*/
    private Bitmap decodeLastPictureThumb(byte[] data) {
        BitmapFactory.Options options = new BitmapFactory.Options();       
        if (mLcdSize == LCD_SIZE_WVGA && mParameters.getPictureSize().width == 1024) {
            options.inSampleSize = 8;
        } else {           
            options.inSampleSize = 16;
        }
        Bitmap lastPictureThumb =
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
        Log.e("@@@@@@@@@@@@@@@@@@@@@@","lastPictureThumb is :"+lastPictureThumb);
        return lastPictureThumb;
    }

    private Bitmap decodeLastPictureThumb(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (mLcdSize == LCD_SIZE_WVGA && mParameters.getPictureSize().width == 1024) {
            options.inSampleSize = 8;
        } else {           
            options.inSampleSize = 16;
        }
        Bitmap lastPictureThumb =
                BitmapFactory.decodeFile(filePath, options);
        Log.e("@@@@@@@@@@@@@@34@@@@@@@@","lastPictureThumb is :"+lastPictureThumb);
        return lastPictureThumb;
    }

    private void setLastPictureThumb(Bitmap lastPictureThumb, int degree, Uri uri) {
        lastPictureThumb = Util.rotate(lastPictureThumb, degree);
        mThumbController.setData(uri, lastPictureThumb);
        mThumbController.updateDisplayIfNeeded();
        
        // update to preference
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(KEY_LAST_THUMB_URI, uri.toString());
        editor.commit();
    }

    private static String createName(long dateTaken) {
        return DateFormat.format("yyyy-MM-dd kk.mm.ss", dateTaken).toString();
    }

    private static int getLcdMapping(int width, int height) {

        int w;
        int h;
        if (width > height) {
            w = width;
            h = height;
        } else {
            w = height;
            h = width;
        }
        
        switch (h) {
            case 240:
                if (w == 320) {
                    return LCD_SIZE_QVGA;
                } else if (w == 400){
                    return LCD_SIZE_WQVGA;
                }
                break;
                
            case 320:
                if (w == 480) {
                    return LCD_SIZE_HVGA;
                }
                break;
                
            case 480:
                if (w == 800) {
                    return LCD_SIZE_WVGA;
                }
                break;
                
            default:
                break;
        }

        return LCD_SIZE_UNKNOWN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        Log.v(TAG, "onCreate()");

/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:33:36 +0800
 * init main sensor ensure to startup activity
 */
        mSrcDev = 0;
// End of Vanzo:zhouwenjie
        super.onCreate(icicle); 
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.camera);
        mSurfaceView = (SurfaceView) findViewById(R.id.camera_preview);

        mIsImageCaptureIntent = isImageCaptureIntent();
        if (mIsImageCaptureIntent) {
            setupCaptureParams();
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (isFromLauncher() || mIsImageCaptureIntent || !isFromVideoCamera()) {
            CameraSettings.resetPreferences(mPreferences);
        } else if (isFromVideoCamera()) {
            mResetSceneMode = true;
        }
//        CameraSettings.upgradePreferences(mPreferences);

        /*
         * To reduce startup time, we start the preview in another thread.
         * We make sure the preview is started at the end of onCreate.
         */
        Thread startPreviewThread = new Thread(new Runnable() {
            public void run() {
                try {
                    mStartPreviewFail = false;
                    startPreview();
                } catch (CameraHardwareException e) {
                    // In eng build, we throw the exception so that test tool
                    // can detect it and report it
                    if ("eng".equals(Build.TYPE)) {
                        throw new RuntimeException(e);
                    }
                    mStartPreviewFail = true;
                }
            }
        });
        startPreviewThread.start();

        // don't set mSurfaceHolder here. We have it set ONLY within
        // surfaceChanged / surfaceDestroyed, other parts of the code
        // assume that when it is set, the surface is also set.
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        WindowManager windowManager =
            (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int LcdWidth = windowManager.getDefaultDisplay().getWidth();
        int LcdHeight = windowManager.getDefaultDisplay().getHeight();
        mLcdSize = getLcdMapping (LcdWidth, LcdHeight);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup rootView;

        View frameLayout = findViewById(R.id.frame_layout);
        if (frameLayout instanceof com.android.camera.PreviewFrameLayout) {            
            rootView = (ViewGroup) findViewById(R.id.camera);
        } else {
            rootView = (ViewGroup) findViewById(R.id.frame);
        }
        
        if (mIsImageCaptureIntent) {
            View controlBar = inflater.inflate(
                    R.layout.attach_camera_control, rootView);
            controlBar.findViewById(R.id.btn_cancel).setOnClickListener(this);
            controlBar.findViewById(R.id.btn_retake).setOnClickListener(this);
            controlBar.findViewById(R.id.btn_done).setOnClickListener(this);
        } else {
            inflater.inflate(R.layout.camera_control, rootView);
            mSwitcher = ((Switcher) findViewById(R.id.camera_switch));
            mSwitcher.setOnSwitchListener(this);
            mSwitcher.addTouchView(findViewById(R.id.camera_switch_set));
        }
        findViewById(R.id.btn_gripper)
                .setOnTouchListener(new GripperTouchListener());

        mFlashIndicator = (IconIndicator) findViewById(R.id.flash_icon);
        mFocusIndicator = (IconIndicator) findViewById(R.id.focus_icon);
        mSceneModeIndicator = (IconIndicator) findViewById(R.id.scenemode_icon);
        mWhitebalanceIndicator = (IconIndicator) findViewById(R.id.whitebalance_icon);
        mSelftimerIndicator = (IconIndicator) findViewById(R.id.selftimer_icon);
        mExposureValueIndicator = (IconIndicator) findViewById(R.id.ev_icon);
        
        mPicturesRemainView = (TextView) findViewById(R.id.remain_pictures);

/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:35:17 +0800
 * add ImageButton on main/sub view & onClick event
 */
        mFunctionPictureButton1 = (ImageView) findViewById(R.id.function_button_1);
        mFunctionPictureButton1.setImageResource(R.drawable.camera_switch);
        mFunctionPictureButton1.setVisibility(View.VISIBLE);
        mFunctionPictureButton1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mCount >= 2) {
                    mSrcDev = 1;
                    mCount = 2;
                    cameraSwitch();
                    mIsSubEnabled = true;
                } else {
                    mSrcDev = 0;
                    mCount = 1;
                    cameraSwitch();
                    mIsSubEnabled = false;
                    initializeZoom();
                }
            }
       });
// End of Vanzo:zhouwenjie
        // Make sure preview is started.
        try {
            startPreviewThread.join();
            if (mStartPreviewFail) {
                showCameraErrorAndFinish();
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }
        removeUnsupportedIndicators();
    }

    private void removeUnsupportedIndicators() {
        List<String> modes;

        modes = mParameters.getSupportedFocusModes();
        if (modes == null || modes.size() <= 1) {
            mFocusIndicator.setVisibility(View.GONE);
            mFocusSupported = false;
        } else {
            mFocusSupported = true;
        }

        modes = mParameters.getSupportedWhiteBalance();
        if (modes == null || modes.size() <= 1) {
            mWhitebalanceIndicator.setVisibility(View.GONE);
        }
        modes = mParameters.getSupportedFlashModes();
        if (modes == null || modes.size() <= 1) {
            mFlashIndicator.setVisibility(View.GONE);
        }

        modes = mParameters.getSupportedSceneModes();
        if (modes == null || modes.size() <= 1) {
            mSceneModeIndicator.setVisibility(View.GONE);
        }

        modes = null;
    }

    private class GripperTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:36:38 +0800
 * control show settings base MotionEvent
                    showOnScreenSettings();
 */
                    choiceShowOnScreenSettings();
// End of Vanzo:zhouwenjie
                    return true;
            }
            return false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        /*
        if (!mIsImageCaptureIntent) {
            mSwitcher.setSwitch(SWITCH_CAMERA);
        }*/
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaProviderClient != null) {
            mMediaProviderClient.release();
            mMediaProviderClient = null;
        }
    }

    private void checkStorage() {
        if (ImageManager.isMediaScannerScanning(getContentResolver())) {
            mPicturesRemaining = MenuHelper.NO_STORAGE_ERROR;
        } else {
            calculatePicturesRemaining();
        }
        updateStorageHint(mPicturesRemaining);
    }


    private void showOnScreenSettings() {

        if (!isCameraIdle()) return;

        if (mSettings == null) {
            mSettings = new OnScreenSettings(
                    findViewById(R.id.camera_preview));
            CameraSettings helper =
                    new CameraSettings(this, mInitParameters);
            mSettings.setPreferenceScreen(helper
                    .getPreferenceScreen(R.xml.camera_preferences));
            mSettings.setOnVisibilityChangedListener(this);

            String sceneMode = mParameters.getSceneMode();
            if (sceneMode == null || Parameters.SCENE_MODE_AUTO.equals(sceneMode)) {
                // If scene mode is auto, cancel override in settings
                mSettings.overrideSettings(CameraSettings.KEY_EDGE, null);
                mSettings.overrideSettings(CameraSettings.KEY_SATURATION, null);
                mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, null);
                mSettings.overrideSettings(CameraSettings.KEY_ISO, null);

            } else {
                // If scene mode is not auto, override the value in settings
                mSettings.overrideSettings(CameraSettings.KEY_EDGE, mParameters.getEdgeMode());
                mSettings.overrideSettings(CameraSettings.KEY_SATURATION, mParameters.getSaturationMode());
                mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, mParameters.getContrastMode());
                mSettings.overrideSettings(CameraSettings.KEY_ISO, mParameters.getISOSpeed());
            }

            String faceDetection = mPreferences.getString(
                                    CameraSettings.KEY_FD_MODE,
                                    getString(R.string.pref_camera_fdmode_default));
            String focusMeter = mParameters.getFocusMeter();
            if (faceDetection.equals(getString(R.string.pref_camera_fdmode_default))) {
                mSettings.overrideSettings(CameraSettings.KEY_FOCUS_METER, null);
            } else {
                mSettings.overrideSettings(CameraSettings.KEY_FOCUS_METER, focusMeter);
            }

            String isoSpeed = mPreferences.getString(
                                CameraSettings.KEY_ISO,
                                getString(R.string.pref_camera_iso_default));
            if (isoSpeed.equals(CameraSettings.ISO_SPEED_1600)
                    || isoSpeed.equals(CameraSettings.ISO_SPEED_800)) {

                mSettings.overrideSettings(CameraSettings.KEY_PICTURE_SIZE,
                        CameraSettings.IMG_SIZE_FOR_HIGH_ISO);
            } else {
                mSettings.overrideSettings(CameraSettings.KEY_PICTURE_SIZE, null);
            }

            if (mIsImageCaptureIntent && isShowingPostCaptureAlert()) {
/* Vanzo:zhouwenjie on: Mon, 01 Nov 2010 14:29:14 +0800
 * modify back camera name
                setCameraParameters();      // keep camera parameters sync with preference
 */
                setCameraParameters_rear();      // keep camera parameters sync with preference
// End of Vanzo:zhouwenjie
            }
        }
        mSettings.setVisible(true);
        // user open setting to set something, should always apply new setting afterwards. 
        mFromOnResume = false;
    }

/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:38:07 +0800
 * add sub sensor settings
 */
    private void showOnScreenSettings_front() {
        if (!isCameraIdle()) return;
        if (mSettings == null) {
            mSettings = new OnScreenSettings(
                    findViewById(R.id.camera_preview));
            CameraSettings helper =
                    new CameraSettings(this, mInitParameters);
            mSettings.setPreferenceScreen(helper
                    .getPreferenceScreen(R.xml.camera_preview_front));
            mSettings.setOnVisibilityChangedListener(this);

            String sceneMode = mParameters.getSceneMode();
            if (sceneMode == null || Parameters.SCENE_MODE_AUTO.equals(sceneMode)) {
                // If scene mode is auto, cancel override in settings
                mSettings.overrideSettings(CameraSettings.KEY_EDGE, null);
                mSettings.overrideSettings(CameraSettings.KEY_SATURATION, null);
                mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, null);
                mSettings.overrideSettings(CameraSettings.KEY_ISO, null);

            } else {
                // If scene mode is not auto, override the value in settings
                mSettings.overrideSettings(CameraSettings.KEY_EDGE, mParameters.getEdgeMode());
                mSettings.overrideSettings(CameraSettings.KEY_SATURATION, mParameters.getSaturationMode());
                mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, mParameters.getContrastMode());
                mSettings.overrideSettings(CameraSettings.KEY_ISO, mParameters.getISOSpeed());
            }

            String faceDetection = mPreferences.getString(
                    CameraSettings.KEY_FD_MODE,
                    getString(R.string.pref_camera_fdmode_default));
            String focusMeter = mParameters.getFocusMeter();
            if (faceDetection.equals(getString(R.string.pref_camera_fdmode_default))) {
                mSettings.overrideSettings(CameraSettings.KEY_FOCUS_METER, null);
            } else {
                mSettings.overrideSettings(CameraSettings.KEY_FOCUS_METER, focusMeter);
            }

            String isoSpeed = mPreferences.getString(
                    CameraSettings.KEY_ISO,
                    getString(R.string.pref_camera_iso_default));
            if (isoSpeed.equals(CameraSettings.ISO_SPEED_1600)
                    || isoSpeed.equals(CameraSettings.ISO_SPEED_800)) {

                mSettings.overrideSettings(CameraSettings.KEY_PICTURE_SIZE0,
                        CameraSettings.IMG_SIZE_FOR_HIGH_ISO);
            } else {
                mSettings.overrideSettings(CameraSettings.KEY_PICTURE_SIZE0, null);
            }

            if (mIsImageCaptureIntent && isShowingPostCaptureAlert()) {
/* Vanzo:zhouwenjie on: Mon, 01 Nov 2010 14:30:22 +0800
 * modify back camera name
                setCameraParameters(); // keep camera parameters sync with preference
 */
                setCameraParameters_rear(); // keep camera parameters sync with preference
// End of Vanzo:zhouwenjie
            }
        }
        mSettings.setVisible(true);
        // user open setting to set something, should always apply new setting afterwards.
        mFromOnResume = false;
    }
// End of Vanzo:zhouwenjie
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_retake:
                mFocusRectangle.setBackgroundDrawable(null);
                hidePostCaptureAlert();
                restartPreview();
                if (!mFirstTimeInitialized) {
                    initializeFirstTime();
                } else if (mFocusToneGenerator == null) {
                    initializeSecondTime();
                }
                break;
            case R.id.review_thumbnail:
                mFocusRectangle.setBackgroundDrawable(null);
                if (isCameraIdle()) {
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 20:20:08 +0800
 * control main/sub sensor review last picture
 */
                    updateDevStatus();
// End of Vanzo:zhouwenjie
                    viewLastImage();
                }
                break;
            case R.id.btn_done:
                mFocusRectangle.setBackgroundDrawable(null);
                doAttach();
                break;
            case R.id.btn_cancel:
                doCancel();
        }
    }

    private Bitmap createCaptureBitmap(byte[] data) {
        // This is really stupid...we just want to read the orientation in
        // the jpeg header.
        String filepath = ImageManager.getTempJpegPath();
        int degree = 0;
        if (saveDataToFile(filepath, data)) {
            degree = ImageManager.getExifOrientation(filepath);
            new File(filepath).delete();
        }

        // Limit to 50k pixels so we can return it in the intent.
        Bitmap bitmap = Util.makeBitmap(data, 50*1024);
        bitmap = Util.rotate(bitmap, degree);
        return bitmap;
    }

    private void doAttach() {
        if (mPausing) {
            return;
        }

        byte[] data = mImageCapture.getLastCaptureData();

        if (mCropValue == null) {
            // First handle the no crop case -- just return the value.  If the
            // caller specifies a "save uri" then write the data to it's
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (mSaveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mContentResolver.openOutputStream(mSaveUri);
                    outputStream.write(data);
                    outputStream.close();

                    setResult(RESULT_OK);
                    finish();
                } catch (IOException ex) {
                    // ignore exception
                } finally {
                    Util.closeSilently(outputStream);
                }
            } else {
                Bitmap bitmap = createCaptureBitmap(data);
                setResult(RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
                finish();
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = getFileStreamPath(sTempCropFilename);
                path.delete();
                tempStream = openFileOutput(sTempCropFilename, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            } catch (IOException ex) {
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            } finally {
                Util.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (mCropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (mSaveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
            } else {
                newExtras.putBoolean("return-data", true);
            }

            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            startActivityForResult(cropIntent, CROP_MSG);
        }
    }

    private void doCancel() {
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }

    public void onShutterButtonFocus(ShutterButton button, boolean pressed) {
        if (mPausing) {
            return;
        }
        switch (button.getId()) {
            case R.id.shutter_button:
                doFocus(pressed);
                break;
        }
    }

    public void onShutterButtonClick(ShutterButton button) {
        if (mPausing) {
            return;
        }
        switch (button.getId()) {
            case R.id.shutter_button:
                doSnap();
                break;
        }
    }


    // update storage hint and return updated photo remain count
    private int updateStorageHint(int remaining) {
        String noStorageText = null;

        remaining = updateRemainImageView(remaining);

        if (remaining == MenuHelper.NO_STORAGE_ERROR) {
            String state = Environment.getExternalStorageState();
            if (state == Environment.MEDIA_CHECKING ||
                    ImageManager.isMediaScannerScanning(getContentResolver())) {
                noStorageText = getString(R.string.preparing_sd);
            } else {
                noStorageText = getString(R.string.no_storage);
            }
        } else if (remaining < 1) {
            noStorageText = getString(R.string.not_enough_space);
        }

        if (noStorageText != null) {
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(this, noStorageText);
            } else {
                mStorageHint.setText(noStorageText);
            }
            if (hasWindowFocus()) {     //workaround for landscape/portrait switch issue.
                mStorageHint.show();
            } else {
                mStorageHint.cancel();
            }
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }

        return remaining;
    }


    private int updateRemainImageView(int remaining) {
        if (!mCaptureMode.equals(Parameters.CAPTURE_MODE_NORMAL) && remaining > 0) {
            if (remaining > 3) {
                remaining  -= 2;
            } else {
                remaining = 0;
            }
        }
        
        if (remaining > 0) {
            mPicturesRemainView.setText(new Integer(remaining).toString());
            mPicturesRemainView.setVisibility(View.VISIBLE);
        } else {
            mPicturesRemainView.setVisibility(View.GONE);
        }

        return remaining;
    }

    /* onScreen hint for saving */

    private OnScreenHint mSavingHint;

    private void updateSavingHint(boolean bSaving) {
        String savingText = null;

        mHandler.removeMessages(MSG_SAVING_DONE);
        if (bSaving) {
            savingText = "Saving..."; // TODO: change to Resource

            mSavingHint = OnScreenHint.makeText(this, savingText);
            mSavingHint.show();
        } else if (mSavingHint != null) {
            mSavingHint.cancel();
            mSavingHint = null;
        }
    }

    private void installIntentFilter() {
        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter =
                new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
        mDidRegister = true;
    }

    private void initializeFocus() {
        initializeFocusTone();
        // update surface offset for mHal to calculate accurate focus area
/* Vanzo:zhouwenjie on: Thu, 28 Oct 2010 19:33:51 +0800
 * add main/sub sensor init Focus
        setCameraParameters();
 */
        if (!mIsSubEnabled) {
            setCameraParameters_rear();
        } else {
            setCameraParameters_front();
        }
// End of Vanzo:zhouwenjie
    }

    private void initializeFocusTone() {
        // Initialize focus tone generator.
        try {
            mFocusToneGenerator = new ToneGenerator(
                    AudioManager.STREAM_SYSTEM, FOCUS_BEEP_VOLUME);
        } catch (Throwable ex) {
            Log.w(TAG, "Exception caught while creating tone generator: ", ex);
            mFocusToneGenerator = null;
        }
    }

    private void readPreference() {
        mRecordLocation = RecordLocationPreference.get(
                mPreferences, getContentResolver());
        mFocusMode = mPreferences.getString(
                CameraSettings.KEY_FOCUS_MODE,
                getString(R.string.pref_camera_focusmode_default));
        if (mFocusSupported == false) {
            mFocusMode = Parameters.FOCUS_MODE_INFINITY;
        }
        /*
        mSelfTimerMode = Integer.parseInt(mPreferences.getString(
            CameraSettings.KEY_SELF_TIMER,
            getString(R.string.pref_camera_selftimer_default))); */
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");    
        super.onResume();

        mFromOnResume = mPausing;
        
        mPausing = false;
        mJpegPictureCallbackTime = 0;
        mZoomValue = 0;
        mImageCapture = new ImageCapture();

        if (!mIsImageCaptureIntent) {
            mSwitcher.resetSwitch(SWITCH_CAMERA);
        }

        if (mHandler.hasMessages(MSG_EV_SEL_DONE)) {
            return;
        }

        // Start the preview if it is not started.
        if (!mPreviewing && !mStartPreviewFail) {
            try {
/* Vanzo:zhouwenjie on: Sat, 23 Oct 2010 16:05:38 +0800
 * back camera activity when others activity startup
 */
                updateDevStatus();
// End of Vanzo:zhouwenjie
                startPreview();
            } catch (CameraHardwareException e) {
                showCameraErrorAndFinish();
                return;
            }
        }

        if (mSurfaceHolder != null) {
            // If first time initialization is not finished, put it in the
            // message queue.
            if (!mFirstTimeInitialized) {
                mHandler.sendEmptyMessage(FIRST_TIME_INIT);
            } else {
                initializeSecondTime();
            }
        }
        keepScreenOnAwhile();
        
        // do a fast calculatiion without checking media provider.
        calculatePicturesRemaining();
        updateRemainImageView(mPicturesRemaining);
        updateIndicators();
    }

    private static ImageManager.DataLocation dataLocation() {
        return ImageManager.DataLocation.EXTERNAL;
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause()");
        mPausing = true;
        mCameraState = CAMAPP_STATE_IDLE;
        stopPreview();
        
        // Close the camera now because other activities may need to use it.
        closeCamera();
        resetScreenOn();

        if (mSettings != null) {
            mSettings.setVisible(false);
            // remove mSettings since we cannot sync on screen settings with preferrence
            // in complicated cases
            // such as enter camera enter message -> than back to camera
            // or enter message->capture -> home -> enter camera-> adjust setting -> home
            // -> go message again
            mSettings = null;
        }

        if (mFirstTimeInitialized) {
            mOrientationListener.disable();
            if (mRecordLocation) {
                mGpsIndicator.setMode(GPS_MODE_OFF);
            }
            hidePostCaptureAlert();
        }

        if (mDidRegister) {
            unregisterReceiver(mReceiver);
            mDidRegister = false;
        }
        stopReceivingLocationUpdates();

        if (mFocusToneGenerator != null) {
            mFocusToneGenerator.release();
            mFocusToneGenerator = null;
        }

        if (mSelfTimerTone != null) {
            mSelfTimerTone.release();
            mSelfTimerTone = null;
        }

        if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }

        // This is necessary to make the ZoomButtonsController unregister
        // its configuration change receiver.
        if (mZoomButtons != null) {
            mZoomButtons.setVisible(false);
        }

        // Remove the messages in the event queue.
        mHandler.removeMessages(RESTART_PREVIEW);
        mHandler.removeMessages(FIRST_TIME_INIT);
        mHandler.removeMessages(MSG_SECOND_TIME_INIT);
        mHandler.removeMessages(MSG_SELFTIMER_TIMEOUT);

        if (mJpgSaving != null && !(mHandler.hasMessages(MSG_SAVING_DONE) 
                    || mHandler.hasMessages(MSG_EV_SAVING_DONE))) {
            try {
                mJpgSaving.join();
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Saving interrupted!");
            }
        }
        mHandler.removeMessages(MSG_SAVING_DONE);
        mHandler.removeMessages(MSG_EV_SAVING_DONE);

        if (mFirstTimeInitialized) {
            if (!mIsImageCaptureIntent) {
                // still have to update thumbnail for this image
                if (mLastPictureThumb != null) {
                    setLastPictureThumb(
                            mLastPictureThumb, mDegree, mImageCapture.getLastCaptureUri());
                }
                mThumbController.storeData(
                        ImageManager.getLastImageThumbPath());
            }
        }
        mJpgSaving = null;
        mLastPictureThumb = null;
        // If we are in an image capture intent and has taken
        // a picture, we just clear it in onPause.
        // remove mImageCapture at last since it will be used when onPause + saving image
        mImageCapture.clearLastData();
        mImageCapture = null;

        // an workaround of in an extereme case that visibilty change listener 
        // will be invoked before item click event
        mHandler.removeMessages(MSG_UNREGISTER_PREF_CHG_HDLR);
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
            Log.v(TAG, "onActivityResult(): req:" + requestCode + " res:" + resultCode );
        switch (requestCode) {
            case CROP_MSG: {
                Intent intent = new Intent();
                if (data != null) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        intent.putExtras(extras);
                    }
                }
                setResult(resultCode, intent);
                finish();

                File path = getFileStreamPath(sTempCropFilename);
                path.delete();

                break;
            }

            case EV_SELECT: {
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if (extras == null) {
                        Log.e(TAG, "onActivityResult(), EV_SELECT, extra == null!");
                        return;
                    }
                    mEvImageSelected = extras.getStringArray(PicturePicker.FILE_PATHS);
                    mHandler.sendEmptyMessage(MSG_EV_SEL_DONE);
                    // let handler do the job.
                } else {
                    String evPrefix = mIsImageCaptureIntent ? INTENT_EV_IMG_PREFIX : EV_IMG_PREFIX;
                    for (int i = 0; i < 3; i++) {
                        String s = new String(ImageManager.CAMERA_IMAGE_BUCKET_NAME + evPrefix + i);
                        new File(s).delete();
                    }
                }
            }
        }
    }

    private void handleEvSelectCallback() throws IOException{
        Log.v(TAG, "handleEvSelectCallback()");
        if (!mIsImageCaptureIntent) {
            String thumbFilePath = null;

            int thumbIndex = 0;
            for (String s : mEvImageSelected) {
                if (s != null) {
                    thumbIndex++;
                }
            }
            if (thumbIndex > 0) {
                thumbIndex -= 1;
            }

            if (mEvImageSelected[thumbIndex] != null) {
                mLastPictureThumb = decodeLastPictureThumb(mEvImageSelected[thumbIndex]);
            } 

            try {
                startPreview();
            } catch (CameraHardwareException e) {
                showCameraErrorAndFinish();
                return;
            }

            // initialize first, since it is possible that member variable is null
            // when back from EV callback (e.g. language setting changed)
            if (!mFirstTimeInitialized) {
                initializeFirstTime();
            } else {
                initializeSecondTime();
            }

            mCameraState = CAMAPP_STATE_SAVING;
            mHandler.sendEmptyMessageDelayed(MSG_SHOW_SAVING_HINT, 200);
            mJpgSaving = new Thread(new Runnable() {
                public void run() {
                    mImageCapture.storeImage(mEvImageSelected, null, mLastJpegLoc);
                    mHandler.removeMessages(MSG_SHOW_SAVING_HINT);
                    mHandler.sendEmptyMessage(MSG_EV_SAVING_DONE);
                    mEvImageSelected = null;
                }
            });
            mJpgSaving.start();

            keepScreenOnAwhile();
        } else {

            if (mPreviewing) {
                // if user doing somthing which makes camera restarted. have to 
                // stop preview here.
                stopPreview();
                showPostCaptureAlert();
            }

            String s = mEvImageSelected[0];
            File f = new File(s).getAbsoluteFile();
            BufferedInputStream bf = null;
            byte [] jpgData = null;
            try {
                bf = new BufferedInputStream( new FileInputStream(f));
                jpgData  =  new byte[bf.available()];
                bf.read(jpgData);
                mImageCapture.storeImage(jpgData, mCameraDevice, mLastJpegLoc);
            } catch (IOException e) {
                // TODO: capture failed. return?
            } finally {
                if (bf != null) {
                    bf.close();
                }
            }
            mEvImageSelected = null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bmp =
                    BitmapFactory.decodeByteArray(jpgData, 0, jpgData.length, options);
            Drawable d = new BitmapDrawable(bmp);
            if (mFocusRectangle == null) {
                mFocusRectangle = (FocusRectangle) findViewById(R.id.focus_rectangle);
            }
            mFocusRectangle.setBackgroundDrawable(d);

            // required for doAttach().
            if (mContentResolver == null) {
                mContentResolver = getContentResolver();
            }
            
            try {
                ensureCameraDevice();
            } catch (CameraHardwareException e) {
                // must run in UI thread
                showCameraErrorAndFinish();
            }
        }
    }


    private boolean canTakePicture() {
        return (isCameraIdle()
                && mPreviewing
                && (mPicturesRemaining > 0)
                && (mSelfTimerState == STATE_SELF_TIMER_IDLE)
                && (mStatus != CAMAPP_STATE_SAVING));
    }

    private void autoFocus() {
        // Initiate autofocus only when preview is started and snapshot is not
        // in progress.
        if (canTakePicture()) {
            Log.v(TAG, "Start autofocus.");
            if (mZoomButtons != null) mZoomButtons.setVisible(false);
            mFocusStartTime = System.currentTimeMillis();
            mFocusState = FOCUSING;
            Parameters params;
            params = mCameraDevice.getParameters();
            mFocusRectangle.setFocusWinStr(params.get("focus-win"));
            updateFocusIndicator();
            mCameraDevice.autoFocus(mAutoFocusCallback);
        }
    }

    private void cancelAutoFocus() {

        // User releases half-pressed focus key.

        if (mSelfTimerState != STATE_SELF_TIMER_IDLE) return;   //self timer counting, ignore

        if (mFocusState == FOCUSING
            || mFocusState == FOCUS_SUCCESS
            || mFocusState == FOCUS_FAIL) {
            Log.v(TAG, "Cancel autofocus.");
            mCameraDevice.cancelAutoFocus();
        }
        if (mFocusState != FOCUSING_SNAP_ON_FINISH
            && mFocusState != FOCUSING_SELFTIMER_ON_FINISH) {
            clearFocusState();
        }
    }

    private void clearFocusState() {
        mFocusState = FOCUS_NOT_STARTED;
        updateFocusIndicator();
    }

    private void updateFocusIndicator() {
        if (mFocusRectangle == null) return;

        if (mFocusState == FOCUSING || mFocusState == FOCUSING_SNAP_ON_FINISH) {
            mFocusRectangle.showStart();
        } else if (mFocusState == FOCUS_SUCCESS) {
            mFocusRectangle.showSuccess();
        } else if (mFocusState == FOCUS_FAIL) {
            mFocusRectangle.showFail();
        } else {
            mFocusRectangle.clear();
        }
    }

    @Override
    public void onBackPressed() {
        if (!isCameraIdle() && mSelfTimerState == STATE_SELF_TIMER_IDLE) {
            // ignore backs while we're taking a picture
            // but don't ignore back when self timer is counting
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    doFocus(true);
                }
                return true;
            case KeyEvent.KEYCODE_CAMERA:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    doSnap();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // If we get a dpad center event without any focused view, move
                // the focus to the shutter button and press it.
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    // Start auto-focus immediately to reduce shutter lag. After
                    // the shutter button gets the focus, doFocus() will be
                    // called again but it is fine.
                    doFocus(true);
                    if (mShutterButton.isInTouchMode()) {
                        mShutterButton.requestFocusFromTouch();
                    } else {
                        mShutterButton.requestFocus();
                    }
                    mShutterButton.setPressed(true);
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (event.isLongPress()) {
                    return true;    // consume LongPress to prevent VK popup.
                }
                return false;
            case KeyEvent.KEYCODE_SEARCH:
                // do nothing since we don't want search box which may cause camera UI crash
                // TODO: mapping to other useful feature
                return true;

        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
                if (mFirstTimeInitialized && mSelfTimerState == STATE_SELF_TIMER_IDLE) {
                    doFocus(false);
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (mIsImageCaptureIntent && mSelfTimerState == STATE_SELF_TIMER_IDLE) {
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:41:32 +0800
 * control main/sub sensor opt settings
                    showOnScreenSettings();
 */
                    choiceShowOnScreenSettings();
// End of Vanzo:zhouwenjie
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_SEARCH:
                // do nothing since we don't want search box which may cause camera UI crash
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }


/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:42:40 +0800
 * add main/sub sensor settings
 */
    public void choiceShowOnScreenSettings() {
        mSettings = null;
        if (mIsSubEnabled) {
            showOnScreenSettings_front();
        } else {
            showOnScreenSettings();
        }
    }
// End of Vanzo:zhouwenjie
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        // stop if the window does not have focus
        // e.g. task switcher has been bring to the foreground
        if (!hasFocus && !isShowingPostCaptureAlert()) {
            selfTimerStop();        
            if (mFirstTimeInitialized) {
                doFocus(false);
                clearFocusState();  // force clear, to avoid  FOCUSING_SNAP_ON_FINISH
            }
        }

        // an workaround of that window width is portrait mode when cam activity is not focused
        // so pending the storage time until window is focused.
        if (hasFocus && mStorageHint != null) {
            mStorageHint.show();
        }
        
        super.onWindowFocusChanged(hasFocus);
    }
    

    private void doSnap() {
        Log.v(TAG, "doSnap: mFocusState=" + mFocusState);
        // If the user has half-pressed the shutter and focus is completed, we
        // can take the photo right away. If the focus mode is infinity, we can
        // also take the photo.
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:43:33 +0800
 * beacause sub sensor not support zoom funciton
        if (isZooming()) {
            return;
        }
 */
// End of Vanzo:zhouwenjie
        if (!mPreviewing) {
            return;
        }

        if (mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY)
                || (mFocusState == FOCUS_SUCCESS || mFocusState == FOCUS_FAIL)) {
            if (mZoomButtons != null) mZoomButtons.setVisible(false);

            if (mSelfTimerMode != 0 && mSelfTimerState == STATE_SELF_TIMER_IDLE) {
                // align behavior, don't clear focus when self timer started.
                selfTimerStart();
            } else {
                mFocusRectangle.clearFocusWin();            
                mImageCapture.onSnap();
            }
        } else if (mFocusState == FOCUSING) {
            // Half pressing the shutter (i.e. the focus button event) will
            // already have requested AF for us, so just request capture on
            // focus here.
            mFocusState =
                (mSelfTimerMode == 0) ? FOCUSING_SNAP_ON_FINISH : FOCUSING_SELFTIMER_ON_FINISH;
        } else if (mFocusState == FOCUS_NOT_STARTED) {
            // Focus key down event is dropped for some reasons. Just ignore.
        }

    }


    private void doFocus(boolean pressed) {
        if (!mFocusMode.equals(Parameters.FOCUS_MODE_INFINITY) 
                && !isShowingPostCaptureAlert()) {
            if (pressed) {
                autoFocus();
            } else {
                cancelAutoFocus();
            }
        }
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }

        Log.v(TAG, "surfaceChanged()");

        // We need to save the holder for later use, even when the mCameraDevice
        // is null. This could happen if onResume() is invoked after this
        // function.
        mSurfaceHolder = holder;

        // The mCameraDevice will be null if it fails to connect to the camera
        // hardware. In this case we will show a dialog and then finish the
        // activity, so it's OK to ignore it.
        if (mCameraDevice == null) return;

        // Sometimes surfaceChanged is called after onPause or before onResume.
        // Ignore it.
        if (mPausing || isFinishing()) return;

        // it is found that in monkey test that it will call surface changed during 
        // snapshot, Ignore it.
        if (mStatus != IDLE) return;

        if (mHandler.hasMessages(MSG_EV_SEL_DONE)
                ||  (mIsImageCaptureIntent && isShowingPostCaptureAlert())) {
            return;
        }

        if (mPreviewing && holder.isCreating()) {
            // Set preview display if the surface is being created and preview
            // was already started. That means preview display was set to null
            // and we need to set it now.
            setPreviewDisplay(holder);
        } else {
            // 1. Restart the preview if the size of surface was changed. The
            // framework may not support changing preview display on the fly.
            // 2. Start the preview now if surface was destroyed and preview
            // stopped.
            restartPreview();
        }

        // If first time initialization is not finished, send a message to do
        // it later. We want to finish surfaceChanged as soon as possible to let
        // user see preview first.
        if (!mFirstTimeInitialized) {
            mHandler.sendEmptyMessage(FIRST_TIME_INIT);
        } else {
            initializeSecondTime();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        mSurfaceHolder = null;
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
/* Vanzo:zhouwenjie on: Wed, 20 Oct 2010 16:57:22 +0800
 * add control main sensor after closeCamera
 */
            if (mIsSubEnabled) {
                mSrcDev = 0;
            }
// End of Vanzo:zhouwenjie
            CameraHolder.instance().release();
            if (mZoomButtons != null) mCameraDevice.setZoomCallback(null);
            mCameraDevice = null;
            mPreviewing = false;
        }
    }

    private void ensureCameraDevice() throws CameraHardwareException {
        if (mCameraDevice == null) {
            mCameraDevice = CameraHolder.instance().open();
            mInitParameters = CameraHolder.instance().getParameters();
        }
    }

    private void updateLastImage() {
        IImageList list = ImageManager.makeImageList(
            mContentResolver,
            dataLocation(),
            ImageManager.INCLUDE_IMAGES,
            ImageManager.SORT_ASCENDING,
            ImageManager.CAMERA_IMAGE_BUCKET_ID);
        int count = list.getCount();
        if (count > 0) {
            IImage image = list.getImageAt(count - 1);
            Uri uri = image.fullSizeImageUri();
            mThumbController.setData(uri, image.miniThumbBitmap());
        } else {
            mThumbController.setData(null, null);
        }
        list.close();
    }


    private boolean isUriPathValid(Uri Uri2Check) {
        Log.v(TAG, "isUriPathValid()");

        if (Uri2Check == null) return false;

        long tStart = System.currentTimeMillis();
        IImageList list = ImageManager.makeImageList(
            mContentResolver,
            dataLocation(),
            ImageManager.INCLUDE_IMAGES,
            ImageManager.SORT_ASCENDING,
            ImageManager.CAMERA_IMAGE_BUCKET_ID);
        long tEnd = System.currentTimeMillis();        
        Log.v(TAG, "Image list cost " + (tEnd - tStart) + " ms");     
        
        IImage img = list.getImageForUri(Uri2Check);

        boolean valid = ((img != null) ? true : false);

        if (!valid) {
            Log.v(TAG, "Path is not valid");
        }
            
        return valid;
    }


    private void showCameraErrorAndFinish() {
        Resources ress = getResources();
        Util.showFatalErrorAndFinish(Camera.this,
                ress.getString(R.string.camera_error_title),
                ress.getString(R.string.cannot_connect_camera));
    }

/* Vanzo:zhouwenjie on: Mon, 01 Nov 2010 14:36:33 +0800
 * add state option function
 */
    private int updateDevStatus() {
        if (mCount <= 6 && mCount >= 2) {
            mSrcDev = 0;
        } else {
            mSrcDev = 1;
        }
        return mSrcDev;
    }
// End of Vanzo:zhouwenjie

    private void restartPreview() {
        updateDevStatus();
       try {
           startPreview();
        } catch (CameraHardwareException e) {
            showCameraErrorAndFinish();
        }
    }

    private void setPreviewDisplay(SurfaceHolder holder) {
        try {
            mCameraDevice.setPreviewDisplay(holder);
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("setPreviewDisplay failed", ex);
        }
    }

    private void startPreview() throws CameraHardwareException {
        if (mPausing || isFinishing()) return;

        ensureCameraDevice();

        // If we're previewing already, stop the preview first (this will blank
        // the screen).
        if (mPreviewing) stopPreview();
        
        //reget real current camera parameter to decide wheather to set correct scene mode 
        // or not
        mParameters = mCameraDevice.getParameters();

/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:45:56 +0800
 * add main/sub sensor opt function
        setPreviewDisplay(mSurfaceHolder);
        setCameraParameters();

 */
        mSrcDev = 1-mSrcDev;
        if (1 == mSrcDev) {
            mParameters.set("src-dev", "sensor-main");
            mCount++;
            if (mCount > 5) {
                mCount = 2;
            }
            setPreviewDisplay(mSurfaceHolder);
            setCameraParameters_rear();
        } else {
            mParameters.set("src-dev", "sensor-sub");
            setPreviewDisplay(mSurfaceHolder);
            setCameraParameters_front();
            mCount--;
            if (mCount < -5) {
                mCount = 0;
            }
        }
// End of Vanzo:zhouwenjie
        final long wallTimeStart = SystemClock.elapsedRealtime();
        final long threadTimeStart = Debug.threadCpuTimeNanos();

        if (ENABLE_CAMERA_PROFILING_LOG) {
            // Set one shot preview callback for latency measurement.
            mCameraDevice.setOneShotPreviewCallback(mOneShotPreviewCallback);
        }
        mCameraDevice.setErrorCallback(mErrorCallback);

        try {
            mCameraDevice.startPreview();
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("startPreview failed", ex);
        }
        mPreviewing = true;
        mZooming = false;
        mDoubleTapZooming = false;
        mStatus = IDLE;
        mSelfTimerState = STATE_SELF_TIMER_IDLE;

        long threadTimeEnd = Debug.threadCpuTimeNanos();
        long wallTimeEnd = SystemClock.elapsedRealtime();
        if ((wallTimeEnd - wallTimeStart) > 3000) {
            Log.w(TAG, "startPreview() to " + (wallTimeEnd - wallTimeStart)
                    + " ms. Thread time was"
                    + (threadTimeEnd - threadTimeStart) / 1000000 + " ms.");
        }
    }

    private void stopPreview() {
        if (mCameraDevice != null && mPreviewing) {
            mCameraDevice.stopPreview();
        }
        mPreviewing = false;
        // If auto focus was in progress, it would have been canceled.
        clearFocusState();
    }

    private Size getOptimalPreviewSize(List<Size> sizes, double targetRatio) {
        final double ASPECT_TOLERANCE = 0.05;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of mSurfaceView. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size

        Display display = getWindowManager().getDefaultDisplay();
        int targetHeight = Math.min(display.getHeight(), display.getWidth());

        if (targetHeight <= 0) {
            // We don't know the size of SurefaceView, use screen height
            WindowManager windowManager = (WindowManager)
                    getSystemService(Context.WINDOW_SERVICE);
            targetHeight = windowManager.getDefaultDisplay().getHeight();
        }

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            Log.v(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        Log.v(TAG, String.format(
                "Optimal preview size is %sx%s",
                optimalSize.width, optimalSize.height));
        return optimalSize;
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }


    private void setSceneModeIndependentParameters() {

        // Reset preview frame rate to the maximum because it may be lowered by
        // video camera application.
        List<Integer> frameRates = mParameters.getSupportedPreviewFrameRates();
        if (frameRates != null) {
            Integer max = Collections.max(frameRates);
            mParameters.setPreviewFrameRate(max);
        }

        // Set the preview frame aspect ratio according to the picture size.
        Size size = mParameters.getPictureSize();

        // the layout object is different for QVGA and other resolutions
        View frameLayout = findViewById(R.id.frame_layout);

        if (frameLayout instanceof com.android.camera.PreviewFrameLayout) {
            ((PreviewFrameLayout) frameLayout).setAspectRatio(
                    (double) size.width / size.height);
        } else {
            ((FullPreviewFrameLayout) frameLayout).setAspectRatio(
                    (double) size.width / size.height);
        }

        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizes = mParameters.getSupportedPreviewSizes();
        Size optimalSize = getOptimalPreviewSize(
                sizes, (double) size.width / size.height);
        if (optimalSize != null) {
            mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
        }

        // Set JPEG quality.
        String jpegQuality = mPreferences.getString(
                CameraSettings.KEY_JPEG_QUALITY,
                getString(R.string.pref_camera_jpegquality_default));
        mParameters.setJpegQuality(getQualityNumber(jpegQuality));

        // Set zoom.
/* Vanzo:zhouwenjie on: Mon, 01 Nov 2010 17:05:00 +0800
 * bugfix zoom in sub sensor
 */
        if (!mIsSubEnabled) {
            mZoomValue = 0;
        }
        if (mParameters.isZoomSupported()) {
            mParameters.setZoom(mZoomValue);
        }
// End of Vanzo:zhouwenjie

        // self timer
        mSelfTimerMode = Integer.parseInt(mPreferences.getString(
            CameraSettings.KEY_SELF_TIMER,
            getString(R.string.pref_camera_selftimer_default)));

        String antibanding = mPreferences.getString(
                CameraSettings.KEY_ANTIBANDING_MODE,
                getString(R.string.pref_camera_antibanding_default));
        if (isSupported(antibanding, mParameters.getSupportedAntibanding())){
            mParameters.setAntibanding(antibanding);
        }

        String flashMode = mPreferences.getString(
                            CameraSettings.KEY_FLASH_MODE,
                            getString(R.string.pref_camera_flashmode_default));
        List<String> supportedFlash = mParameters.getSupportedFlashModes();
        if (isSupported(flashMode, supportedFlash)) {
            mParameters.setFlashMode(flashMode);
        } else {
            flashMode = mParameters.getFlashMode();
            if (flashMode == null) {
                flashMode = getString(
                        R.string.pref_camera_flashmode_no_flash);
            } else {
                // the default flash mode is not supported.
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(CameraSettings.KEY_FLASH_MODE, flashMode);
                editor.commit();
            }
        }

    }


    private void loadCameraParametersToPreference(String sceneMode) {

        // Code body
        mFocusMode = mParameters.getFocusMode();
        if (mFocusMode == null) {
            mFocusMode = Parameters.FOCUS_MODE_INFINITY;
        }
        
        String exposure = mParameters.getExposure();
        String exposureMeter = mParameters.getExposureMeter();
        String colorEffect = mParameters.getColorEffect();
        String whiteBalance = mParameters.getWhiteBalance();
        String focusMeter = mParameters.getFocusMeter();
        String faceDetection = mParameters.getFDMode();
        String hue = mParameters.getHueMode();
        String brightness = mParameters.getBrightnessMode();
        String edge = mParameters.getEdgeMode();
        String saturation = mParameters.getSaturationMode();
        String contrast = mParameters.getContrastMode();
        String isoSpeed = mParameters.getISOSpeed();


        // EV, Focus Mode, Color Effect, white balance, AE Meter, Focus Meter, Face detection
        // Hue,  Brightness
        if (mSettings == null) {
            SharedPreferences.Editor edt = mPreferences.edit();

            edt.putString(CameraSettings.KEY_EXPOSURE, exposure);
            edt.putString(CameraSettings.KEY_EXPOSURE_METER, exposureMeter);
            edt.putString(CameraSettings.KEY_FOCUS_MODE, mFocusMode);
            edt.putString(CameraSettings.KEY_COLOR_EFFECT, colorEffect);
            edt.putString(CameraSettings.KEY_WHITE_BALANCE, whiteBalance);
            edt.putString(CameraSettings.KEY_HUE, hue);
            edt.putString(CameraSettings.KEY_BRIGHTNESS, brightness);
            edt.putString(CameraSettings.KEY_FD_MODE, faceDetection);

            if (faceDetection.equals(getString(R.string.pref_camera_fdmode_default))) {
                edt.putString(CameraSettings.KEY_FOCUS_METER, focusMeter);
            }

            if (mParameters.SCENE_MODE_AUTO.equals(sceneMode)) {
                edt.putString(CameraSettings.KEY_EDGE, edge);
                edt.putString(CameraSettings.KEY_SATURATION, saturation);
                edt.putString(CameraSettings.KEY_CONTRAST, contrast);
                edt.putString(CameraSettings.KEY_ISO, isoSpeed);
            }
            edt.commit();
        } else {
            mPreferences.unregisterOnSharedPreferenceChangeListener(this);
            mSettings.updateSetting(CameraSettings.KEY_EXPOSURE, exposure);
            mSettings.updateSetting(CameraSettings.KEY_EXPOSURE_METER, exposureMeter);
            mSettings.updateSetting(CameraSettings.KEY_FOCUS_MODE, mFocusMode);
            mSettings.updateSetting(CameraSettings.KEY_COLOR_EFFECT, colorEffect);
            mSettings.updateSetting(CameraSettings.KEY_WHITE_BALANCE, whiteBalance);
            mSettings.updateSetting(CameraSettings.KEY_HUE, hue);
            mSettings.updateSetting(CameraSettings.KEY_BRIGHTNESS, brightness);
            mSettings.updateSetting(CameraSettings.KEY_FD_MODE, faceDetection);

            if (faceDetection.equals(getString(R.string.pref_camera_fdmode_default))) {
                mSettings.overrideSettings(CameraSettings.KEY_FOCUS_METER, null);
                mSettings.updateSetting(CameraSettings.KEY_FOCUS_METER, focusMeter);
            } else {
                mSettings.overrideSettings(CameraSettings.KEY_FOCUS_METER, focusMeter);
            }

            if (mParameters.SCENE_MODE_AUTO.equals(sceneMode)) {
                mSettings.overrideSettings(CameraSettings.KEY_EDGE, null);
                mSettings.overrideSettings(CameraSettings.KEY_SATURATION, null);
                mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, null);
                mSettings.overrideSettings(CameraSettings.KEY_ISO, null);

                mSettings.updateSetting(CameraSettings.KEY_EDGE, edge);
                mSettings.updateSetting(CameraSettings.KEY_SATURATION, saturation);
                mSettings.updateSetting(CameraSettings.KEY_CONTRAST, contrast);
                mSettings.updateSetting(CameraSettings.KEY_ISO, isoSpeed);
                // TODO: add the option for picture size

            } else {
                // Scene mode is not "Auto", disable following settings.
                mSettings.overrideSettings(CameraSettings.KEY_EDGE, edge);
                mSettings.overrideSettings(CameraSettings.KEY_SATURATION, saturation);
                mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, contrast);
                mSettings.overrideSettings(CameraSettings.KEY_ISO, isoSpeed);
            }
            mPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }


    private void setCameraParametersFromPreference(String sceneMode) {

        /*
         * local variables
         */
        String edge;
        String saturation;
        String contrast;
        String isoSpeed;

        /*
         *  code
         */
        String exposure = mPreferences.getString(
                            CameraSettings.KEY_EXPOSURE,
                            getString(R.string.pref_camera_exposure_default));
        if (isSupported(exposure, mParameters.getSupportedExposure())){
            mParameters.setExposure(exposure);
        }

        String exposureMeter = mPreferences.getString(
                                CameraSettings.KEY_EXPOSURE_METER,
                                getString(R.string.pref_camera_exposuremeter_default));
        if (isSupported(exposureMeter, mParameters.getSupportedExposureMeter())){
            mParameters.setExposureMeter(exposureMeter);
        }

        mFocusMode = mPreferences.getString(
                        CameraSettings.KEY_FOCUS_MODE,
                        getString(R.string.pref_camera_focusmode_default));
        if (isSupported(mFocusMode, mParameters.getSupportedFocusModes())) {
            mParameters.setFocusMode(mFocusMode);
        } else {
            mFocusMode = mParameters.getFocusMode();
            if (mFocusMode == null) {
                mFocusMode = Parameters.FOCUS_MODE_INFINITY;
            }
        }

        String colorEffect = mPreferences.getString(
                                CameraSettings.KEY_COLOR_EFFECT,
                                getString(R.string.pref_camera_coloreffect_default));
        if (isSupported(colorEffect, mParameters.getSupportedColorEffects())) {
            mParameters.setColorEffect(colorEffect);
        }

        String whiteBalance = mPreferences.getString(
                                CameraSettings.KEY_WHITE_BALANCE,
                                getString(R.string.pref_camera_whitebalance_default));
        if (isSupported(whiteBalance, mParameters.getSupportedWhiteBalance())) {
            mParameters.setWhiteBalance(whiteBalance);
        } else {
            whiteBalance = mParameters.getWhiteBalance();
            if (whiteBalance == null) {
                whiteBalance = Parameters.WHITE_BALANCE_AUTO;
            }
        }

        String hue = mPreferences.getString(
                        CameraSettings.KEY_HUE,
                        getString(R.string.pref_camera_hue_default));
        if (isSupported(hue, mParameters.getSupportedHueMode())){
            mParameters.setHueMode(hue);
        }

        String brightness = mPreferences.getString(
                                CameraSettings.KEY_BRIGHTNESS,
                                getString(R.string.pref_camera_brightness_default));
        if (isSupported(brightness, mParameters.getSupportedBrightnessMode())){
            mParameters.setBrightnessMode(brightness);
        }

        // Set FD mode
        String fdMode = mPreferences.getString(
                CameraSettings.KEY_FD_MODE,
                getString(R.string.pref_camera_fdmode_default));
        if (isSupported(fdMode, mParameters.getSupportedFDMode())){
            mParameters.setFDMode(fdMode);
        }

        // Set focus meter parameter.
        // Overwrite focuse meter to "single zone (spot)" if FD is on
        String focusMeter = mPreferences.getString(
                CameraSettings.KEY_FOCUS_METER,
                getString(R.string.pref_camera_focusmeter_default));
        if (isSupported(focusMeter, mParameters.getSupportedFocusMeter())){
            mParameters.setFocusMeter(focusMeter);  // still apply focus meter setting
            if (mSettings != null){
                if (fdMode.equals(getString(R.string.pref_camera_fdmode_default))) {  
                    // fd off
                    mSettings.overrideSettings(CameraSettings.KEY_FOCUS_METER, null);
                } else {
                    mSettings.overrideSettings(
                        CameraSettings.KEY_FOCUS_METER, CameraSettings.FOCUS_METER_SPOT);
                }
            }
        }

        if (Parameters.SCENE_MODE_AUTO.equals(sceneMode)) {
            if (mSettings != null) {
                mSettings.overrideSettings(CameraSettings.KEY_EDGE, null);
                mSettings.overrideSettings(CameraSettings.KEY_SATURATION, null);
                mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, null);
            }
            edge = mPreferences.getString(
                    CameraSettings.KEY_EDGE,
                    getString(R.string.pref_camera_edge_default));
            if (isSupported(edge, mParameters.getSupportedEdgeMode())){
                mParameters.setEdgeMode(edge);
            }
            saturation = mPreferences.getString(
                            CameraSettings.KEY_SATURATION,
                            getString(R.string.pref_camera_saturation_default));
            if (isSupported(saturation, mParameters.getSupportedSaturationMode())){
                mParameters.setSaturationMode(saturation);
            }
            contrast = mPreferences.getString(
                        CameraSettings.KEY_CONTRAST,
                        getString(R.string.pref_camera_edge_default));
            if (isSupported(contrast, mParameters.getSupportedContrastMode())){
                mParameters.setContrastMode(contrast);
            }

            isoSpeed = mPreferences.getString(
                            CameraSettings.KEY_ISO,
                            getString(R.string.pref_camera_iso_default));
            if (isSupported(isoSpeed, mParameters.getSupportedISOSpeed())){
                mParameters.setISOSpeed(isoSpeed);
            }

        } else if (mSettings != null) {
            edge = mParameters.getEdgeMode();
            saturation = mParameters.getSaturationMode();
            contrast = mParameters.getContrastMode();
            isoSpeed = mParameters.getISOSpeed();
            mSettings.overrideSettings(CameraSettings.KEY_EDGE, edge);
            mSettings.overrideSettings(CameraSettings.KEY_SATURATION, saturation);
            mSettings.overrideSettings(CameraSettings.KEY_CONTRAST, contrast);
            mSettings.overrideSettings(CameraSettings.KEY_ISO, isoSpeed);
        }


    }

    private void setCameraParameters_rear() {
        /*
         * Check and set parameters to camera, since some attributes will be effected by scene mode
         * 1. set scenemode first, and load default camera parameters back if scene mode changed
         * 2. overwrite parameters which will not depends on scene mode (from current user setting)
         * 3. update parameter to Preference or overwrite parameter to camera
         * 4. check ISO/ image resolution
         * 5. update capture mode parameters
         */
        boolean sceneModeChanged = false;
        if (mParameters == null) {
            Log.v(TAG, "reload parameters");
            mParameters = mCameraDevice.getParameters();      // TODO: do not reload parameter
        }

        // Since change scene mode may change supported values,
        // Set scene mode first,
        String sceneMode = mPreferences.getString(
                            CameraSettings.KEY_SCENE_MODE,
                            getString(R.string.pref_camera_scenemode_default));
        if (isSupported(sceneMode, mParameters.getSupportedSceneModes())) {
            Log.v(TAG, "current pref scenemode: " + sceneMode + 
                " mResetSceneMode:" + mResetSceneMode + " mFromOnResume: " + mFromOnResume);
            if (!mParameters.getSceneMode().equals(sceneMode) || mResetSceneMode) {

                mResetSceneMode = false;
                
                mParameters.setSceneMode(sceneMode);
                mCameraDevice.setParameters(mParameters);
                // Setting scene mode will change all camera settings
                // so we read back here
                mParameters = mCameraDevice.getParameters();
                if (!mFromOnResume) {
                    sceneModeChanged = true;
                } else {
                    // when back from history, cam app is not exited, but HW will reset parameter
                    // in this case, apply preference values to camera parameter.
                    mFromOnResume = false;
                }
            }
        } else {
            sceneMode = mParameters.getSceneMode();
            if (sceneMode == null) {
                sceneMode = Parameters.SCENE_MODE_AUTO;
            }
        }


        //frame rate, aspect ratio, preview size, JPEG quality, zoom,
        //self timer, antibanding
        setSceneModeIndependentParameters();

        // EV, Focus Mode, Color Effect, white balance, AE Meter, Focus Meter, Face detection
        // Hue,  Brightness, edge, saturation, contrast, iso
        if (sceneModeChanged) {
            loadCameraParametersToPreference(sceneMode);
        } else {
            setCameraParametersFromPreference(sceneMode);
        }

        // ISO & Picture size
        setPictureSizeAndIsoSpeed();

        // Capture mode
        mCaptureMode= mPreferences.getString(
                CameraSettings.KEY_CAPTURE_MODE,
                getString(R.string.pref_camera_capturemode_default));
        mParameters.setCaptureMode(mCaptureMode);
        if (mCaptureMode != null) {
            if (mCaptureMode.equals(Parameters.CAPTURE_MODE_EV_BRACKET_SHOT)
                    || mCaptureMode.equals(Parameters.CAPTURE_MODE_BEST_SHOT)) {

                String evPath;
                if (mIsImageCaptureIntent) {
                    evPath = ImageManager.CAMERA_IMAGE_BUCKET_NAME + INTENT_EV_IMG_PREFIX + "0";
                } else {
                    evPath = ImageManager.CAMERA_IMAGE_BUCKET_NAME + EV_IMG_PREFIX + "0";
                }
                
                Log.v(TAG,"EV storage path: " + evPath);
                mParameters.setCapturePath(evPath);
                ImageManager.ensureImageBucketFolder();         // make sure the file path exists
            }
        } else {
            mCaptureMode = Parameters.CAPTURE_MODE_NORMAL;
        }

        mBurstNo = 1;
        mParameters.set(KEY_NUMBER_OF_BURST_SHOT, mBurstNo);
        mParameters.set(KEY_CAMERA_MODE, CAMERA_MODE_IMAGE_PREVIEW); // Cam mode is preview

        if (mSurfaceHolder != null) {
            //get preview offset and update
            View preview = findViewById(R.id.camera_preview);
            int [] loc = new int[2];
            preview.getLocationOnScreen(loc);
            mPreviewRect.set(loc[0], loc[1], loc[0] + preview.getWidth(), loc[1] + preview.getHeight());
            // for FD
            Log.v(TAG, "preview: " + rectToCameraString(mPreviewRect));
            // Should be the value before rotate
            mParameters.set("disp-y", mPreviewRect.left);
            mParameters.set("disp-x", mPreviewRect.top);
            mParameters.set("disp-h", mPreviewRect.width());
            mParameters.set("disp-w", mPreviewRect.height());
            mParameters.set("disp-rotate", 1);
        }

        mCameraDevice.setParameters(mParameters);

        // this is tricky, we don't want loading OSD time delay the preview start.
        // but we also want to update it in post capture screen (which preview is stopped)
        // so use these two flags instead.
        if (mPreviewing || mFirstTimeInitialized) {
            // update OSD indicator
            // We post the runner because this function can be called from
            // non-UI thread (i.e., startPreviewThread).
            final String finalWhiteBalance = mParameters.getWhiteBalance();
            final String finalFlashMode = mParameters.getFlashMode();
            final String finalSceneMode = sceneMode;
            final String finalSelftimerMode = Integer.toString(mSelfTimerMode);
            final String finalEvMode = mParameters.getExposure();


            mHandler.post(new Runnable() {
                public void run() {
                    if (mFocusSupported) {
                        mFocusIndicator.setMode(mFocusMode);
                    }
                    mWhitebalanceIndicator.setMode(finalWhiteBalance);
                    mSceneModeIndicator.setMode(finalSceneMode);
                    mFlashIndicator.setMode(finalFlashMode);
                    mSelftimerIndicator.setMode(finalSelftimerMode);
                    mExposureValueIndicator.setMode(finalEvMode);

                    if (mPreviewing || isShowingPostCaptureAlert()) {
                        checkStorage();
                    }
                }
            });
        }
    }
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:48:18 +0800
 * add sub sensor setCameraParameters
 */
    private void setCameraParameters_front() {
        /*
         * Check and set parameters to camera, since some attributes will be effected by scene mode
         * 1. set scenemode first, and load default camera parameters back if scene mode changed
         * 2. overwrite parameters which will not depends on scene mode (from current user setting)
         * 3. update parameter to Preference or overwrite parameter to camera
         * 4. check ISO/ image resolution
         * 5. update capture mode parameters
         */
        boolean sceneModeChanged = false;
        if (mParameters == null) {
            mParameters = mCameraDevice.getParameters();      // TODO: do not reload parameter
        }

        // Since change scene mode may change supported values,
        // Set scene mode first,
        String sceneMode = mPreferences.getString(
                        CameraSettings.KEY_SCENE_MODE,
                        getString(R.string.pref_camera_scenemode_default));
        if (isSupported(sceneMode, mParameters.getSupportedSceneModes())) {
            if (!mParameters.getSceneMode().equals(sceneMode) || mResetSceneMode) {

                mResetSceneMode = false;
                mParameters.setSceneMode(sceneMode);
                mCameraDevice.setParameters(mParameters);
                // Setting scene mode will change all camera settings
                // so we read back here
                mParameters = mCameraDevice.getParameters();
                if (!mFromOnResume) {
                    sceneModeChanged = true;
                } else {
                    // when back from history, cam app is not exited, but HW will reset parameter
                    // in this case, apply preference values to camera parameter.
                    mFromOnResume = false;
                }
            }
        } else {
            sceneMode = mParameters.getSceneMode();
            if (sceneMode == null) {
                sceneMode = Parameters.SCENE_MODE_AUTO;
            }
        }


        //frame rate, aspect ratio, preview size, JPEG quality, zoom,
        //self timer, antibanding
        setSceneModeIndependentParameters();

        // EV, Focus Mode, Color Effect, white balance, AE Meter, Focus Meter, Face detection
        // Hue,  Brightness, edge, saturation, contrast, iso
        if (sceneModeChanged) {
            loadCameraParametersToPreference(sceneMode);
        } else {
            setCameraParametersFromPreference(sceneMode);
        }

        // ISO & Picture size
        setPictureSizeAndIsoSpeed_front();

        // Capture mode
        mCaptureMode= mPreferences.getString(
                CameraSettings.KEY_CAPTURE_MODE,
                getString(R.string.pref_camera_capturemode_default));
        mParameters.setCaptureMode(mCaptureMode);
        if (mCaptureMode != null) {
            if (mCaptureMode.equals(Parameters.CAPTURE_MODE_EV_BRACKET_SHOT)
                    || mCaptureMode.equals(Parameters.CAPTURE_MODE_BEST_SHOT)) {

                String evPath;
                if (mIsImageCaptureIntent) {
                    evPath = ImageManager.CAMERA_IMAGE_BUCKET_NAME + INTENT_EV_IMG_PREFIX + "0";
                } else {
                    evPath = ImageManager.CAMERA_IMAGE_BUCKET_NAME + EV_IMG_PREFIX + "0";
                }
                Log.v(TAG,"EV storage path: " + evPath);
                mParameters.setCapturePath(evPath);
                ImageManager.ensureImageBucketFolder();         // make sure the file path exists
            }
        } else {
            mCaptureMode = Parameters.CAPTURE_MODE_NORMAL;
        }

        mBurstNo = 1;
        mParameters.set(KEY_NUMBER_OF_BURST_SHOT, mBurstNo);
        mParameters.set(KEY_CAMERA_MODE, CAMERA_MODE_IMAGE_PREVIEW); // Cam mode is preview

        if (mSurfaceHolder != null) {
            //get preview offset and update
            View preview = findViewById(R.id.camera_preview);
            int [] loc = new int[2];
            preview.getLocationOnScreen(loc);
            mPreviewRect.set(loc[0], loc[1], loc[0] + preview.getWidth(), loc[1] + preview.getHeight());
            // for FD
            // Should be the value before rotate
            mParameters.set("disp-y", mPreviewRect.left);
            mParameters.set("disp-x", mPreviewRect.top);
            mParameters.set("disp-h", mPreviewRect.width());
            mParameters.set("disp-w", mPreviewRect.height());
            mParameters.set("disp-rotate", 1);
        }

        mCameraDevice.setParameters(mParameters);

        // this is tricky, we don't want loading OSD time delay the preview start.
        // but we also want to update it in post capture screen (which preview is stopped)
        // so use these two flags instead.
        if (mPreviewing || mFirstTimeInitialized) {
            // update OSD indicator
            // We post the runner because this function can be called from
            // non-UI thread (i.e., startPreviewThread).
            final String finalWhiteBalance = mParameters.getWhiteBalance();
            final String finalFlashMode = mParameters.getFlashMode();
            final String finalSceneMode = sceneMode;
            final String finalSelftimerMode = Integer.toString(mSelfTimerMode);
            final String finalEvMode = mParameters.getExposure();


            mHandler.post(new Runnable() {
                public void run() {
                    if (mFocusSupported) {
                        mFocusIndicator.setMode(mFocusMode);
                    }
                    mWhitebalanceIndicator.setMode(finalWhiteBalance);
                    mSceneModeIndicator.setMode(finalSceneMode);
                    mFlashIndicator.setMode(finalFlashMode);
                    mSelftimerIndicator.setMode(finalSelftimerMode);
                    mExposureValueIndicator.setMode(finalEvMode);

                    if (mPreviewing || isShowingPostCaptureAlert()) {
                        checkStorage();
                    }
                }
            });
        }
    }
// End of Vanzo:zhouwenjie
    private void setPictureSizeAndIsoSpeed() {
        // Picture size & ISO speed are updated here. 
        // hardware limitation: if ISO == 800 || ISO == 1600, picture resolution can only be 1M
        String pictureSize = mPreferences.getString(CameraSettings.KEY_PICTURE_SIZE, null);
        Log.d("################","pictureSize is :"+pictureSize);
        if (pictureSize == null) {
            CameraSettings.initialCameraPictureSize(this, mParameters);
        } else {
            List<Size> supported = mParameters.getSupportedPictureSizes();
            CameraSettings.setCameraPictureSize(
                    pictureSize, supported, mParameters); // picture size may change
        }

        // use actual camera ISO value instead.
        String isoSpeed = mParameters.getISOSpeed();
        if (isoSpeed == null) {
            isoSpeed = getString(R.string.pref_camera_iso_default);
        }
        if (isoSpeed.equals(CameraSettings.ISO_SPEED_1600)
                || isoSpeed.equals(CameraSettings.ISO_SPEED_800)) {
            List<Size> supported = mParameters.getSupportedPictureSizes();
            CameraSettings.setCameraPictureSize(
                    CameraSettings.IMG_SIZE_FOR_HIGH_ISO, supported, mParameters);
            if (mSettings != null) {
                mSettings.overrideSettings(
                    CameraSettings.KEY_PICTURE_SIZE, CameraSettings.IMG_SIZE_FOR_HIGH_ISO);
            }
        } else if (mSettings != null) {
            mSettings.overrideSettings(CameraSettings.KEY_PICTURE_SIZE, null);
        }
    }
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:49:27 +0800
 * add sub sensor pictureSize set
 */
    private void setPictureSizeAndIsoSpeed_front() {
        // Picture size & ISO speed are updated here.
        // hardware limitation: if ISO == 800 || ISO == 1600, picture resolution can only be 1M
        String pictureSize = mPreferences.getString(CameraSettings.KEY_PICTURE_SIZE0, null);
        if (pictureSize == null) {
            CameraSettings.initialCameraPictureSize_front(this, mParameters);
        } else {
            List<Size> supported = mParameters.getSupportedPictureSizes();
            CameraSettings.setCameraPictureSize_front(
                    pictureSize, supported, mParameters); // picture size may change
        }


        // use actual camera ISO value instead.
        String isoSpeed = mParameters.getISOSpeed();
        if (isoSpeed == null) {
            isoSpeed = getString(R.string.pref_camera_iso_default);
        }
        if (isoSpeed.equals(CameraSettings.ISO_SPEED_1600)
                || isoSpeed.equals(CameraSettings.ISO_SPEED_800)) {
            List<Size> supported = mParameters.getSupportedPictureSizes();
            CameraSettings.setCameraPictureSize(
                    CameraSettings.IMG_SIZE_FOR_HIGH_ISO, supported, mParameters);
            if (mSettings != null) {
                mSettings.overrideSettings(
                    CameraSettings.KEY_PICTURE_SIZE0, CameraSettings.IMG_SIZE_FOR_HIGH_ISO);
            }
        } else if (mSettings != null) {
            mSettings.overrideSettings(CameraSettings.KEY_PICTURE_SIZE0, null);
        }
    }
// End of Vanzo:zhouwenjie
    private void updateIndicators() {
        // update OSD indicator
        // We post the runner because this function can be called from
        // non-UI thread (i.e., startPreviewThread).
        final String finalWhiteBalance = mParameters.getWhiteBalance();
        final String finalFlashMode = mParameters.getFlashMode();
        final String finalSceneMode = mParameters.getSceneMode();
        final String finalSelftimerMode = Integer.toString(mSelfTimerMode);
        final String finalEvMode = mParameters.getExposure();

        mHandler.post(new Runnable() {
            public void run() {
                if (mFocusSupported) {
                    mFocusIndicator.setMode(mFocusMode);
                }
                mWhitebalanceIndicator.setMode(finalWhiteBalance);
                mSceneModeIndicator.setMode(finalSceneMode);
                mFlashIndicator.setMode(finalFlashMode);
                mSelftimerIndicator.setMode(finalSelftimerMode);
                mExposureValueIndicator.setMode(finalEvMode);
            }
        });
    }


    private String rectToCameraString(Rect r) {
        StringBuilder sb = new StringBuilder(32);

        sb.append(r.left);
        sb.append("x");
        sb.append(r.top);
        sb.append("x");
        sb.append(r.right);
        sb.append("x");
        sb.append(r.bottom);
        return sb.toString();
    }

    private void gotoGallery() {
        MenuHelper.gotoCameraImageGallery(this);
    }

    private void viewLastImage() {
        if (mThumbController.isUriValid()) {
            Uri targetUri = mThumbController.getUri();
            targetUri = targetUri.buildUpon().appendQueryParameter(
                    "bucketId", ImageManager.CAMERA_IMAGE_BUCKET_ID).build();
            Intent intent = new Intent(this, ReviewImage.class);
            intent.setData(targetUri);
            intent.putExtra(MediaStore.EXTRA_FULL_SCREEN, true);
            intent.putExtra(MediaStore.EXTRA_SHOW_ACTION_ICONS, true);
            intent.putExtra("com.android.camera.ReviewMode", true);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Log.e(TAG, "review image fail", ex);
            }
        } else {
            Log.e(TAG, "Can't view last image.");
        }
    }

    private void startReceivingLocationUpdates() {
        if (mLocationManager != null) {
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1000,
                        0F,
                        mLocationListeners[1]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "provider does not exist " + ex.getMessage());
            }
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000,
                        0F,
                        mLocationListeners[0]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "provider does not exist " + ex.getMessage());
            }
        }
    }

    private void stopReceivingLocationUpdates() {
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private Location getCurrentLocation() {
        // go in best to worst order
        for (int i = 0; i < mLocationListeners.length; i++) {
            Location l = mLocationListeners[i].current();
            if (l != null) return l;
        }
        return null;
    }

    private boolean isCameraIdle() {
        return (mStatus == IDLE && mFocusState == FOCUS_NOT_STARTED 
                && mSelfTimerState == STATE_SELF_TIMER_IDLE && !isZooming());
    }

    private boolean isImageCaptureIntent() {
        String action = getIntent().getAction();
        return (MediaStore.ACTION_IMAGE_CAPTURE.equals(action));
    }

    private boolean isFromLauncher() {
        String action = getIntent().getAction();
        return (Intent.ACTION_MAIN.equals(action));
    }

    private boolean isFromVideoCamera() {
        String action = getIntent().getAction();
        return (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action) 
                && MenuHelper.isCameraModeSwitching(getIntent()));
    }

    private void setupCaptureParams() {
        Bundle myExtras = getIntent().getExtras();
        if (myExtras != null) {
            mSaveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            mCropValue = myExtras.getString("crop");
        }
    }

    private void showPostCaptureAlert() {
        if (mIsImageCaptureIntent) {
            mSelfTimerState = STATE_SELF_TIMER_IDLE;
            mStatus = IDLE;
            findViewById(R.id.shutter_button).setVisibility(View.INVISIBLE);
            int[] pickIds = {R.id.btn_retake, R.id.btn_done};
            for (int id : pickIds) {
                View button = findViewById(id);
                ((View) button.getParent()).setVisibility(View.VISIBLE);
            }
        }
    }

    private void hidePostCaptureAlert() {
        if (mIsImageCaptureIntent) {
            findViewById(R.id.shutter_button).setVisibility(View.VISIBLE);
            int[] pickIds = {R.id.btn_retake, R.id.btn_done};
            for (int id : pickIds) {
                View button = findViewById(id);
                ((View) button.getParent()).setVisibility(View.GONE);
            }
        }
    }

    private boolean isShowingPostCaptureAlert() {
        
        View button = findViewById(R.id.btn_done);

        if (button != null) {
            return ((View) button.getParent()).getVisibility() == View.VISIBLE;
        }
        return false;
    }

    private int calculatePicturesRemaining() {
        /* new */
        Size size = mParameters.getPictureSize();
        String pictureFormat = 
                size.width + "x" + size.height + "-" 
                + getQualityString(mParameters.getJpegQuality());
        mPicturesRemaining = MenuHelper.calculatePicturesRemaining(pictureFormat);

        return mPicturesRemaining;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Only show the menu when camera is idle.
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(isCameraIdle());
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isCameraIdle()) return true;

        super.onCreateOptionsMenu(menu);

        if (mIsImageCaptureIntent) {
            // No options menu for attach mode.
            return false;
        } else {
            addBaseMenuItems(menu);
        }
        return true;
    }

    private void addBaseMenuItems(Menu menu) {
        MenuItem gallery = menu.add(Menu.NONE, Menu.NONE,
                MenuHelper.POSITION_GOTO_GALLERY,
                R.string.camera_gallery_photos_text)
                .setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
/* Vanzo:zhouwenjie on: Sat, 23 Oct 2010 16:03:38 +0800
 * store current value when gotoGallery
 */
                updateDevStatus();
// End of Vanzo:zhouwenjie
                gotoGallery();
                return true;
            }
        });
        gallery.setIcon(android.R.drawable.ic_menu_gallery);
        mGalleryItems.add(gallery);

        MenuItem item = menu.add(Menu.NONE, Menu.NONE,
                MenuHelper.POSITION_CAMERA_SETTING, R.string.settings)
                .setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {

/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:50:54 +0800
 * add main/sub sensor setting opt
                showOnScreenSettings();
 */
                choiceShowOnScreenSettings();
// End of Vanzo:zhouwenjie
                return true;
            }
        });
        item.setIcon(android.R.drawable.ic_menu_preferences);
    }

    public boolean onSwitchChanged(Switcher source, boolean onOff) {
        if (onOff == SWITCH_VIDEO) {
            if (!isCameraIdle()){
                mSwitcher.resetSwitch(SWITCH_CAMERA);
                return false;
            } else {
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 20:35:21 +0800
 * when camera switch recording switch default main sensor
 */
                mSrcDev = 0;
// End of Vanzo:zhouwenjie
                MenuHelper.gotoVideoMode(this);
                finish();
            }
        }
        return true;
    }

    public void onSharedPreferenceChanged(
            SharedPreferences preferences, String key) {
        // ignore the events after "onPause()"
        if (mPausing) return;

        if (CameraSettings.KEY_RECORD_LOCATION.equals(key)) {
            mRecordLocation = RecordLocationPreference.get(
                    preferences, getContentResolver());
            if (mRecordLocation) {
                startReceivingLocationUpdates();
            } else {
                stopReceivingLocationUpdates();
                mHandler.post(new Runnable() {
                    public void run() {
                        mGpsIndicator.setMode(GPS_MODE_OFF);
                    }
                });
            }
        } else if (CameraSettings.KEY_SELF_TIMER.equals(key)){
            mSelfTimerMode = Integer.parseInt(preferences.getString(
                                        CameraSettings.KEY_SELF_TIMER,
                                        getString(R.string.pref_camera_selftimer_default)));
            mHandler.post(new Runnable() {
                public void run() {
                    mSelftimerIndicator.setMode(Integer.toString(mSelfTimerMode));
                }
            });
        } else {
            // All preferences except RECORD_LOCATION are camera parameters.
            // Call setCameraParameters to take effect now.
/* Vanzo:zhouwenjie on: Tue, 19 Oct 2010 19:52:22 +0800
 * add two sensors setParameters opt
            setCameraParameters();
 */
        if (mIsSubEnabled) {
            setCameraParameters_front();
        } else {
            setCameraParameters_rear();
        }
// End of Vanzo:zhouwenjie
        }
    }


    private String getMaxSupportPictureSize(String isoSpeed) {
        List<Size> supported = mParameters.getSupportedPictureSizes();
        String [] supportSizes = getResources()
                                .getStringArray(R.array.pref_camera_picturesize_entryvalues);
        String [] candidates = new String [supportSizes.length - 1];
        int index;
        int width;
        int height;
        boolean skipFirst = false;

        if (isoSpeed != null && isoSpeed.equals(CameraSettings.MAX_ISO_SPEED)) {
            // remove the first one (largest one)
            for (int i = 1 ;  i < supportSizes.length; i++) {
                candidates[i-1] = supportSizes[i];
            }
            skipFirst = true;
        } else {
            candidates = supportSizes;
        }

        if (supported == null) return null;

        for(String candidate : candidates) {
            index = candidate.indexOf('x');
            if (index == -1) return null;

            width = Integer.parseInt(candidate.substring(0, index));
            height = Integer.parseInt(candidate.substring(index + 1));

            for (Size size: supported) {
                if (size.width == width && size.height == height) {
                    if (skipFirst) {
                        skipFirst = false;
                        continue;
                    }
                    Log.v(TAG, "getMaxSupportPictureSize():" + candidate);
                    return candidate;
                }
            }
        }
        Log.v(TAG, "getMaxSupportPictureSize(): failed");
        return null;
    }


    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        keepScreenOnAwhile();
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private static String[] mQualityStrings = {"superfine", "fine", "normal"};
    private static String[] mQualityNumbers = SystemProperties.get(
            "ro.media.enc.jpeg.quality", "85,75,65").split(",");
    private static int DEFAULT_QUALITY = 85;

    // Translate from a quality string to a quality number using the system
    // properties.
    private static int getQualityNumber(String jpegQuality) {
        // Find the index of the input string
        int index = Util.indexOf(mQualityStrings, jpegQuality);

        if (index == -1 || index > mQualityNumbers.length - 1) {
            return DEFAULT_QUALITY;
        }

        try {
            return Integer.parseInt(mQualityNumbers[index]);
        } catch (NumberFormatException ex) {
            return DEFAULT_QUALITY;
        }
    }

    // mQualityNumbers -> mQualityStrings;
    private static String getQualityString(int QualityNumber) {
        int i = Util.indexOf(mQualityNumbers, new Integer(QualityNumber).toString());

        if (i == -1 || i > mQualityStrings.length - 1) {
            i = 0;
        }

        return mQualityStrings[i];
    }
    
   /* Android open new codes */

    /**************/
    /* self timer */
    /**************/
    private ToneGenerator mSelfTimerTone;
    private static final int SELF_TIMER_VOLUME = 100;
    private long mTimeSelfTimerStart;

    private void initSelfTimerTone() {
        try {
            mSelfTimerTone = new ToneGenerator(
                    AudioManager.STREAM_SYSTEM, SELF_TIMER_VOLUME);
        } catch (Throwable ex) {
            Log.w(TAG, "Exception caught while creating tone generator: ", ex);
            mSelfTimerTone = null;
        }
    }

    private void selfTimerStart() {
        if (mSelfTimerState != STATE_SELF_TIMER_IDLE
                || mHandler.hasMessages(MSG_SELFTIMER_TIMEOUT)
                || updateStorageHint(mPicturesRemaining) < 1) {
            return;
        }

        updateUIduringCapture(View.INVISIBLE);
        
        mTimeSelfTimerStart = System.currentTimeMillis();
        mSelfTimerState = STATE_SELF_TIMER_COUNTING;
        selfTimerTimeout();
    }


    private void selfTimerStop() {
        
        if (mSelfTimerState == STATE_SELF_TIMER_IDLE) {
            return;
        }
        doFocus(false); // cancel auto focus
        mHandler.removeMessages(MSG_SELFTIMER_TIMEOUT);
        mSelfTimerState = STATE_SELF_TIMER_IDLE;

        updateUIduringCapture(View.VISIBLE);
    }


    private void updateUIduringCapture(int visiblility) {

        if (visiblility != View.INVISIBLE && visiblility != View.GONE 
                && visiblility != View.VISIBLE) {
            return;
        }
        
        View switchView = findViewById(R.id.camera_switch_set);
        if (switchView != null) switchView.setVisibility(visiblility);
        View gripper = findViewById(R.id.btn_gripper);
        if (gripper != null) gripper.setVisibility(visiblility);
    } 


    private void selfTimerTimeout(){
        long msDelay;
        long msDelta = System.currentTimeMillis() - mTimeSelfTimerStart;
        long msTimeLeft;

        if (mPausing) return;

        msTimeLeft = (mSelfTimerMode > msDelta) ? mSelfTimerMode - msDelta : 0;
        //Log.d(TAG, "[SelfTimer]selfTimerTimeout(), " + msTimeLeft + "ms left");

        if (msTimeLeft >= SELF_TIMER_SHORT_BOUND){
            msDelay = msTimeLeft - SELF_TIMER_SHORT_BOUND;
        } else if (msTimeLeft != 0){
            msDelay = SELF_TIMER_INTERVAL;
        } else { /* timeout */
            mSelfTimerState = STATE_SELF_TIMER_SNAP;
            doSnap();
            mSelfTimerState = STATE_SELF_TIMER_IDLE;
            return;
        }
        mHandler.sendEmptyMessageDelayed(MSG_SELFTIMER_TIMEOUT, msDelay);

        if (mSelfTimerTone != null) mSelfTimerTone.startTone(ToneGenerator.TONE_DTMF_9, 100);
    }


    private void fireEvSelector(){
        if (mPausing) {
            return;
        }

        Intent picImgIntent = new Intent(this, PicturePicker.class);
        Bundle param = new Bundle();
        
        String evPrefix = mIsImageCaptureIntent ? INTENT_EV_IMG_PREFIX : EV_IMG_PREFIX;
        String [] paths = new String[3];
        for (int i = 0; i < 3; i++) {
            paths[i] = new String(ImageManager.CAMERA_IMAGE_BUCKET_NAME + evPrefix + i);
        }
        param.putStringArray(PicturePicker.FILE_PATHS, paths);

        if (mIsImageCaptureIntent) {
            param.putInt(PicturePicker.PICTURES_TO_PICK, 1);
            //align intent capture behavior
            picImgIntent.setFlags(picImgIntent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        }
        
        picImgIntent.putExtras(param);

        startActivityForResult(picImgIntent, EV_SELECT);
    }

}


class FocusWin {
    public FocusWin(int x0, int y0, int x1, int y1, int state) {
        mX0 = x0;
        mY0 = y0;
        mX1 = x1;
        mY1 = y1;
        mState = state;
        //Log.v("FocusWin", "[FocusWin]: (x0,y0,x1,y1,S) " + x0 + "," + y0 + "," + x1 + "," + y1 + "," + state + "\n");
    }
    public int mX0;
    public int mY0;
    public int mX1;
    public int mY1;
    public int mState;
};

class FocusRectangle extends View {

    @SuppressWarnings("unused")
    private static final String TAG = "FocusRectangle";
    private String mFocusWinStr = "";
    private Paint mPaint;
    private FocusWin[] mFocusWin = new FocusWin[MAX_FOCUS_FRAME];
    private int mFocusWinCnt = 0;

    private static final int STATE_FOCUSING = 0;
    private static final int STATE_FOCUSED = 1;
    private static final int STATE_FOCUS_FAILED = 2;

    private static final int MAX_FOCUS_FRAME = 9;

    private Rect mRectDirty = new Rect(0,0,0,0);


    public FocusRectangle(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFocusWinStr = "";
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(1);
        mPaint.setStyle(Paint.Style.STROKE);
        mFocusWinCnt = 0;

    }

    private void setDrawable(int resid) {
        //setBackgroundDrawable(getResources().getDrawable(resid));
        if (mRectDirty.isEmpty()) {
            invalidate();
        } else {
            invalidate(mRectDirty);
            Log.d(TAG, "[focus] setDrawable(), rect:" + mRectDirty.left + " " + mRectDirty.top + " "
                + mRectDirty.right + " " + mRectDirty.bottom);
        }
    }

    public void showStart() {
        for (int i = 0; i < mFocusWinCnt; i++) {
            mFocusWin[i].mState = STATE_FOCUSING;
        }
      setDrawable(R.drawable.focus_focusing);
    }

    public void showSuccess() {
        setDrawable(R.drawable.focus_focused);
    }

    public void showFail() {
        setDrawable(R.drawable.focus_focus_failed);
    }

    public void clear() {
        clearFocusWin();
        setBackgroundDrawable(null);
    }

    public void clearFocusWin() {
        mFocusWinStr = "";
        mRectDirty.setEmpty();
        for (int i = 0; i < mFocusWinCnt; i++) {
            mFocusWin[i] = null;
        }
        mFocusWinCnt = 0;
    }

    public void setFocusWinStr(String str) {    // TODO: str may be null.
        clearFocusWin();
        mFocusWinStr = str;
        Log.v(TAG, "setFocusWinStr(): "  + str);

        mRectDirty.setEmpty();
        // Use StringTokenizer because it is faster than split.
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        while (tokenizer.hasMoreElements()) {
            mFocusWin[mFocusWinCnt] = strToFocusWin(tokenizer.nextToken());
            mRectDirty.union(
                        mFocusWin[mFocusWinCnt].mX0,
                        mFocusWin[mFocusWinCnt].mY0,
                        mFocusWin[mFocusWinCnt].mX1,
                        mFocusWin[mFocusWinCnt].mY1);
            mFocusWinCnt++;
        }
        Log.v(TAG, "mFocusWinCnt: " + mFocusWinCnt);
    }

    // Parses a string (ex: "128x144x192x92x0") to FocusWin object.
    // Return null if the passing string is null.
    private FocusWin strToFocusWin(String str) {
        if (str == null) return null;

        String[] dims = str.split("x");
        if (dims.length != 5)
            return null;

        return new FocusWin(Integer.parseInt(dims[0]),
                            Integer.parseInt(dims[1]),
                            Integer.parseInt(dims[2]),
                            Integer.parseInt(dims[3]),
                            Integer.parseInt(dims[4]));
    }

    private void repaint() {
        SurfaceHolder surfaceHolder = null;//this.getHolder();
        Canvas c = null;

        if (mFocusWinCnt == 0) return;

        try {
            c = surfaceHolder.lockCanvas();
            drawFocusWin(c);
        } finally {
            if (c != null) {
                surfaceHolder.unlockCanvasAndPost(c);
            }
        }
    }

    private void drawFocusWin(Canvas canvas) {
        if (mFocusWinCnt == 0) return;

        Drawable dFocusing, dFocused,dFocusFail;
        dFocusing = getResources().getDrawable(R.drawable.focus_focusing);
        dFocused = getResources().getDrawable(R.drawable.focus_focused);
        dFocusFail = getResources().getDrawable(R.drawable.focus_focus_failed);
        
        Drawable dI;    
        for (int i = 0; i < mFocusWinCnt; i++) {
            if (mFocusWin[i].mState == STATE_FOCUSING) {
                dI = dFocusing;
            }
            else if (mFocusWin[i].mState == STATE_FOCUSED) {
                dI = dFocused;
            }
            else {
                dI = dFocusFail;
            }
            dI.setBounds(mFocusWin[i].mX0, mFocusWin[i].mY0, mFocusWin[i].mX1, mFocusWin[i].mY1);
            dI.draw(canvas);
        }
    }

    protected void onDraw(Canvas canvas) {
        drawFocusWin(canvas);
    }
}

