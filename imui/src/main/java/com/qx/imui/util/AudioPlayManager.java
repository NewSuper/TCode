package com.qx.imui.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.qx.imui.adapter.IAudioPlayListener;

import java.io.FileInputStream;
import java.io.IOException;

import static android.media.AudioManager.MODE_IN_CALL;
import static android.media.AudioManager.MODE_NORMAL;
import static android.media.AudioManager.MODE_RINGTONE;

public class AudioPlayManager implements SensorEventListener {
    private static final String TAG = "AudioPlayManager";
    private MediaPlayer mMediaPlayer;
    private IAudioPlayListener _playListener;
    private Uri mUriPlaying;
    private Sensor _sensor;
    private SensorManager _sensorManager;
    private AudioManager mAudioManager;
    private PowerManager _powerManager;
    private PowerManager.WakeLock _wakeLock;
    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private Context mContext;
    private Handler handler;
    private final Object mLock;
    private boolean isVOIPMode;

    private AudioPlayManager() {
        this.mLock = new Object();
        this.isVOIPMode = false;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static AudioPlayManager getInstance() {
        return AudioPlayManager.SingletonHolder.sInstance;
    }

    @TargetApi(11)
    public void onSensorChanged(SensorEvent event) {
        synchronized(this.mLock) {
            float range = event.values[0];
            Log.d("AudioPlayManager", "onSensorChanged. range:" + range + "; max range:" + event.sensor.getMaximumRange());
            double rangeJudgeValue = 0.0D;
            if (this._sensor != null && this.mMediaPlayer != null && this.mAudioManager != null) {
                boolean judge = this.judgeCondition(event, range, rangeJudgeValue);
                if (this.mMediaPlayer.isPlaying()) {
                    FileInputStream fis = null;
                    if (judge) {
                        if (this.mAudioManager.getMode() == MODE_NORMAL) {
                            return;
                        }

                        this.mAudioManager.setMode(MODE_NORMAL);
                        this.mAudioManager.setSpeakerphoneOn(true);
                        final int positions = this.mMediaPlayer.getCurrentPosition();

                        try {
                            this.mMediaPlayer.reset();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                @SuppressLint("WrongConstant") AudioAttributes attributes = (new AudioAttributes.Builder()).setUsage(MODE_RINGTONE).build();
                                this.mMediaPlayer.setAudioAttributes(attributes);
                            } else {
                                this.mMediaPlayer.setAudioStreamType(3);
                            }

                            this.mMediaPlayer.setVolume(1.0F, 1.0F);
                            fis = new FileInputStream(this.mUriPlaying.getPath());
                            this.mMediaPlayer.setDataSource(fis.getFD());
                            this.mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer mp) {
                                    mp.seekTo(positions);
                                }
                            });
                            this.mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                                public void onSeekComplete(MediaPlayer mp) {
                                    mp.start();
                                }
                            });
                            this.mMediaPlayer.prepareAsync();
                        } catch (IOException var20) {
                            Log.e("AudioPlayManager", "onSensorChanged", var20);
                        } finally {
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (IOException var19) {
                                    Log.e("AudioPlayManager", "startPlay", var19);
                                }
                            }

                        }

                        this.setScreenOn();
                    } else {
                        if (!Build.BRAND.equals("samsung") || !Build.MODEL.equals("SM-N9200")) {
                            this.setScreenOff();
                        }

                        if (Build.VERSION.SDK_INT >= 11) {
                            if (this.mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) {
                                return;
                            }

                            this.mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        } else {
                            if (this.mAudioManager.getMode() == AudioManager.MODE_IN_CALL) {
                                return;
                            }

                            this.mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                        }

                        this.mAudioManager.setSpeakerphoneOn(false);
                        this.replay();
                    }
                } else if ((double)range > 0.0D) {
                    if (this.mAudioManager.getMode() == MODE_NORMAL) {
                        return;
                    }

                    this.mAudioManager.setMode(MODE_NORMAL);
                    this.mAudioManager.setSpeakerphoneOn(true);
                    this.setScreenOn();
                }

            }
        }
    }

    private boolean judgeCondition(SensorEvent event, float range, double rangeJudgeValue) {
        synchronized(this.mLock) {
            boolean judge;
            if (Build.BRAND.equalsIgnoreCase("HUAWEI")) {
                judge = range >= event.sensor.getMaximumRange();
            } else {
                if (Build.BRAND.equalsIgnoreCase("ZTE")) {
                    rangeJudgeValue = 1.0D;
                } else if (Build.BRAND.equalsIgnoreCase("nubia")) {
                    rangeJudgeValue = 3.0D;
                }

                judge = (double)range > rangeJudgeValue;
            }

            return judge;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setScreenOff() {
        synchronized(this.mLock) {
            if (this._wakeLock == null && this._powerManager != null) {
                this._wakeLock = this._powerManager.newWakeLock(32, "AudioPlayManager:wakelockTag");
            }

            if (this._wakeLock != null && !this._wakeLock.isHeld()) {
                this._wakeLock.acquire(600000L);
            }

        }
    }

    private void setScreenOn() {
        synchronized(this.mLock) {
            if (this._wakeLock != null && this._wakeLock.isHeld()) {
                this._wakeLock.setReferenceCounted(false);
                this._wakeLock.release();
                this._wakeLock = null;
            }

        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @SuppressLint("WrongConstant")
    private void replay() {
        synchronized(this.mLock) {
            if (this.mMediaPlayer != null) {
                FileInputStream fis = null;

                try {
                    this.mMediaPlayer.reset();
                    AudioAttributes attributes;
                    if (Build.BRAND.equals("samsung") && Build.MODEL.equals("SM-N9200")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            attributes = (new AudioAttributes.Builder()).setUsage(MODE_IN_CALL).build();
                            this.mMediaPlayer.setAudioAttributes(attributes);
                        } else {
                            this.mMediaPlayer.setAudioStreamType(0);
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        attributes = (new AudioAttributes.Builder()).setUsage(MODE_RINGTONE).build();
                        this.mMediaPlayer.setAudioAttributes(attributes);
                    } else {
                        this.mMediaPlayer.setAudioStreamType(3);
                    }

                    this.mMediaPlayer.setVolume(1.0F, 1.0F);
                    fis = new FileInputStream(this.mUriPlaying.getPath());
                    this.mMediaPlayer.setDataSource(fis.getFD());
                    this.mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        public void onPrepared(MediaPlayer mp) {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException var3) {
                                Log.e("AudioPlayManager", "replay", var3);
                                Thread.currentThread().interrupt();
                            }

                            mp.start();
                        }
                    });
                    this.mMediaPlayer.prepareAsync();
                } catch (IOException var14) {
                    Log.e("AudioPlayManager", "replay", var14);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException var13) {
                            Log.e("AudioPlayManager", "replay", var13);
                        }
                    }

                }

            }
        }
    }

    public void startPlay(final Context context, Uri audioUri, IAudioPlayListener playListener) {
        synchronized(this.mLock) {
            if (context != null && audioUri != null) {
                this.mContext = context;
                if (this._playListener != null && this.mUriPlaying != null) {
                    this._playListener.onStop(this.mUriPlaying);
                }

                this.resetMediaPlayer();
                this.afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                    public void onAudioFocusChange(int focusChange) {
                        synchronized(AudioPlayManager.this.mLock) {
                            Log.d("AudioPlayManager", "OnAudioFocusChangeListener " + focusChange);
                            if (AudioPlayManager.this.mAudioManager != null && focusChange == -1) {
                                AudioPlayManager.this.mAudioManager.abandonAudioFocus(AudioPlayManager.this.afChangeListener);
                                AudioPlayManager.this.afChangeListener = null;
                                AudioPlayManager.this.handler.post(new Runnable() {
                                    public void run() {
                                        synchronized(AudioPlayManager.this.mLock) {
                                            if (AudioPlayManager.this._playListener != null) {
                                                AudioPlayManager.this._playListener.onComplete(AudioPlayManager.this.mUriPlaying);
                                                AudioPlayManager.this._playListener = null;
                                            }

                                        }
                                    }
                                });
                                AudioPlayManager.this.reset();
                            }

                        }
                    }
                };
                FileInputStream fis = null;
                ((Activity)context).getWindow().addFlags(128);

                try {
                    this._powerManager = (PowerManager)context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                    this.mAudioManager = (AudioManager)context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                    if (!this.isHeadphonesPlugged(this.mAudioManager)) {
                        this._sensorManager = (SensorManager)context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
                        if (this._sensorManager != null) {
                            this._sensor = this._sensorManager.getDefaultSensor(8);
                            this._sensorManager.registerListener(this, this._sensor, 3);
                        }
                    }

                    this.muteAudioFocus(this.mAudioManager, true);
                    this._playListener = playListener;
                    this.mUriPlaying = audioUri;
                    this.mMediaPlayer = new MediaPlayer();
                    this.mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        public void onCompletion(MediaPlayer mp) {
                            synchronized(AudioPlayManager.this.mLock) {
                                if (AudioPlayManager.this._playListener != null) {
                                    AudioPlayManager.this._playListener.onComplete(AudioPlayManager.this.mUriPlaying);
                                    AudioPlayManager.this._playListener = null;
                                }

                                AudioPlayManager.this.reset();
                                ((Activity)context).getWindow().clearFlags(128);
                            }
                        }
                    });
                    this.mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            synchronized(AudioPlayManager.this.mLock) {
                                AudioPlayManager.this.reset();
                                return true;
                            }
                        }
                    });
                    Log.d(TAG, "startPlay: 音频播放路径为：" + audioUri.getPath());
                    fis = new FileInputStream(audioUri.getPath());
                    this.mMediaPlayer.setDataSource(fis.getFD());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        @SuppressLint("WrongConstant") AudioAttributes attributes = (new AudioAttributes.Builder()).setUsage(MODE_RINGTONE).build();
                        this.mMediaPlayer.setAudioAttributes(attributes);
                    } else {
                        this.mMediaPlayer.setAudioStreamType(3);
                    }

                    this.mMediaPlayer.prepare();
                    this.mMediaPlayer.start();
                    if (this._playListener != null) {
                        this._playListener.onStart(this.mUriPlaying);
                    }
                } catch (Exception var17) {
                    Log.e("AudioPlayManager", "startPlay", var17);
                    if (this._playListener != null) {
                        this._playListener.onStop(audioUri);
                        this._playListener = null;
                    }

                    this.reset();
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException var16) {
                            Log.e("AudioPlayManager", "startPlay", var16);
                        }
                    }

                }

            } else {
                Log.e("AudioPlayManager", "startPlay context or audioUri is null.");
            }
        }
    }

    private boolean isHeadphonesPlugged(AudioManager audioManager) {
        synchronized(this.mLock) {
            if (audioManager == null) {
                return false;
            } else if (Build.VERSION.SDK_INT < 23) {
                return audioManager.isWiredHeadsetOn();
            } else {
                AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
                AudioDeviceInfo[] var4 = audioDevices;
                int var5 = audioDevices.length;

                for(int var6 = 0; var6 < var5; ++var6) {
                    AudioDeviceInfo deviceInfo = var4[var6];
                    if (deviceInfo.getType() == 4 || deviceInfo.getType() == 3) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public void setPlayListener(IAudioPlayListener listener) {
        synchronized(this.mLock) {
            this._playListener = listener;
        }
    }

    public void stopPlay() {
        synchronized(this.mLock) {
            if (this.mContext != null) {
                ((Activity)this.mContext).getWindow().clearFlags(128);
            }

            if (this._playListener != null && this.mUriPlaying != null) {
                this._playListener.onStop(this.mUriPlaying);
            }

            this.reset();
        }
    }

    private void reset() {
        this.resetMediaPlayer();
        this.resetAudioPlayManager();
    }

    private void resetAudioPlayManager() {
        if (this.mAudioManager != null) {
            this.mAudioManager.setMode(MODE_NORMAL);
            this.muteAudioFocus(this.mAudioManager, false);
        }

        if (this._sensorManager != null) {
            this.setScreenOn();
            this._sensorManager.unregisterListener(this);
        }

        this._sensorManager = null;
        this._sensor = null;
        this._powerManager = null;
        this.mAudioManager = null;
        this._wakeLock = null;
        this.mUriPlaying = null;
        this._playListener = null;
    }

    private void resetMediaPlayer() {
        synchronized(this.mLock) {
            if (this.mMediaPlayer != null) {
                try {
                    this.mMediaPlayer.stop();
                    this.mMediaPlayer.reset();
                    this.mMediaPlayer.release();
                    this.mMediaPlayer = null;
                } catch (IllegalStateException var4) {
                    Log.e("AudioPlayManager", "resetMediaPlayer", var4);
                }
            }

        }
    }

    public Uri getPlayingUri() {
        synchronized(this.mLock) {
            return this.mUriPlaying != null ? this.mUriPlaying : Uri.EMPTY;
        }
    }

    @TargetApi(8)
    private void muteAudioFocus(AudioManager audioManager, boolean bMute) {
        synchronized(this.mLock) {
            if (audioManager != null) {
                if (bMute) {
                    audioManager.requestAudioFocus(this.afChangeListener, AudioManager.GET_DEVICES_ALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                } else {
                    audioManager.abandonAudioFocus(this.afChangeListener);
                    this.afChangeListener = null;
                }

            }
        }
    }

    public boolean isInNormalMode(Context context) {
        synchronized(this.mLock) {
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager)context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            }

            return this.mAudioManager != null && this.mAudioManager.getMode() == MODE_NORMAL;
        }
    }

    public boolean isInVOIPMode(Context context) {
        return this.isVOIPMode;
    }

    public void setInVoipMode(boolean isVOIPMode) {
        this.isVOIPMode = isVOIPMode;
    }

    public boolean isPlaying() {
        synchronized(this.mLock) {
            return this.mMediaPlayer != null && this.mMediaPlayer.isPlaying();
        }
    }

    static class SingletonHolder {
        static AudioPlayManager sInstance = new AudioPlayManager();

        SingletonHolder() {
        }
    }
}

