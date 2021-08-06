package com.qx.imui.view;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qx.imlib.qlog.QLog;
import com.qx.imui.QXConfigManager;
import com.qx.imui.R;

import java.io.File;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

public class RecordButton  extends AppCompatButton {
    private static final String TAG = "RecordButton";

    private MyRunnable myRunnable;

    public RecordButton(Context context) {
        super(context);
        init();
    }

    public RecordButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
   class MyHandler extends Handler
   {
       @Override
       public void handleMessage(@NonNull Message msg) {
           super.handleMessage(msg);
       }
   }
   class MyRunnable implements Runnable{
       @Override
       public void run() {
           long time = System.currentTimeMillis() - startTime;
           if (time > MAX_INTERVAL_TIME) {
               finishRecord();
               myHandler.removeCallbacks(myRunnable);
           } else {
               myHandler.postDelayed(myRunnable, 200);
           }
       }
   }

    private DialogInterface.OnDismissListener onDismiss = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            stopRecording();
        }
    };

    public interface OnFinishedRecordListener {
        void onFinishedRecord(String audioPath, int time);
    }

    private static long startTime;
    private Dialog recordDialog;
    private static int[] res = {R.drawable.ic_volume_0, R.drawable.ic_volume_1, R.drawable.ic_volume_2,
            R.drawable.ic_volume_3, R.drawable.ic_volume_4, R.drawable.ic_volume_5, R.drawable.ic_volume_6
            , R.drawable.ic_volume_7};

    private String mFile = "";

    private OnFinishedRecordListener finishedListener;
    /**
     * 最短录音时间
     **/
    private int MIN_INTERVAL_TIME = 1000;
    /**
     * 最长录音时间60m
     **/
    private float MAX_INTERVAL_TIME = 1000 * QXConfigManager.getQxFileConfig().getVoiceMessageMaxDuration(TimeUnit.SECONDS);
    private static View view;
    private TextView mStateTV;
    private ImageView mStateIV;
    private boolean isRecordOk = false;
    private MediaRecorder mRecorder;

    //控制采集声波的线程
    private ObtainDecibelThread mThread;
    private Handler volumeHandler;
    private float y;
    private MyHandler myHandler;
    private AnimationDrawable anim;


    private void init(){
        volumeHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -100){
                        stopRecording();
                        recordDialog.dismiss();
                    }else if (msg.what  != -1){
                        mStateIV.setImageResource(res[msg.what]);
                    }
            }
        };
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        y = event.getY();
        if (mStateTV != null && mStateIV != null && y < 0) {
            mStateTV.setText(R.string.qx_record_audio_release_cancel_send);
            mStateIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_volume_cancel));
        } else if (mStateTV != null) {
            mStateTV.setText(R.string.qx_record_audio_slideup_cancel_send);
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initDialogAndStartRecord();
                myHandler = new MyHandler();
                myRunnable = new MyRunnable();
                myHandler.postDelayed(myRunnable, 0);
                setText(R.string.qx_record_audio_release_send);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.setText(R.string.qx_record_audio_press);
                if (mThread != null) {
                    mThread.exit();
                }
                myHandler.removeCallbacks(myRunnable);
                long time = System.currentTimeMillis() - startTime;
                if (y >= 0 && (time < MAX_INTERVAL_TIME)) {
                    QLog.d(TAG, "结束录音");
                    finishRecord();
                } else if (y < 0) {  //当手指向上滑，会cancel
                    cancelRecord();
                }
                break;
        }
        return true;
    }
    /**
     * 初始化录音对话框 并 开始录音
     */
    private void initDialogAndStartRecord() {
        startTime = System.currentTimeMillis();
        recordDialog = new Dialog(getContext(), R.style.like_toast_dialog_style);
        // view = new ImageView(getContext());
        view = View.inflate(getContext(), R.layout.imui_record_button, null);
        mStateIV = (ImageView) view.findViewById(R.id.rc_audio_state_image);
        mStateTV = (TextView) view.findViewById(R.id.rc_audio_state_text);
        mStateIV.setImageDrawable(getResources().getDrawable(R.drawable.imui_anim_mic));
        anim = (AnimationDrawable) mStateIV.getDrawable();
        anim.start();
        mStateIV.setVisibility(View.VISIBLE);
        //mStateIV.setImageResource(R.drawable.ic_volume_1);
        mStateTV.setVisibility(View.VISIBLE);
        mStateTV.setText(R.string.qx_record_finger_slideup_cancel_send);
        recordDialog.setContentView(view, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        recordDialog.setOnDismissListener(onDismiss);
        WindowManager.LayoutParams lp = recordDialog.getWindow().getAttributes();
        lp.gravity = Gravity.CENTER;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (getContext() instanceof Activity) {
                Activity activity = (Activity) getContext();
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
            }
        } else {
            startRecording();
            recordDialog.show();
        }
    }

    /**
     * 放开手指，结束录音处理
     */
    private void finishRecord() {
        long intervalTime = System.currentTimeMillis() - startTime;
        if (intervalTime < MIN_INTERVAL_TIME) {
            volumeHandler.sendEmptyMessageDelayed(-100, 500);
            //view.setBackgroundResource(R.drawable.ic_voice_cancel);
            mStateIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_volume_wraning));
            mStateTV.setText(R.string.qx_record_time_short);
            anim.stop();
            File file = new File(mFile);
            file.delete();
        /*    stopRecording();
            recordDialog.dismiss();*/
            return;
        } else {
            stopRecording();
            recordDialog.dismiss();
        }
        QLog.d(TAG, "录音完成的路径:" + mFile);
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(mFile);
            mediaPlayer.prepare();
            mediaPlayer.getDuration();
            QLog.d(TAG, "获取到的时长:" + mediaPlayer.getDuration() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!isRecordOk && finishedListener != null && !TextUtils.isEmpty(mFile)) {
            isRecordOk = true;
            finishedListener.onFinishedRecord(mFile, mediaPlayer.getDuration() / 1000);
        }
    }

    /**
     * 取消录音对话框和停止录音
     */
    public void cancelRecord() {
        stopRecording();
        recordDialog.dismiss();
        File file = new File(mFile);
        file.delete();
    }

    //获取类的实例
    // ExtAudioRecorder extAudioRecorder; //压缩的录音（WAV）

    /**
     * 执行录音操作
     */
    //int num = 0 ;
    private void startRecording() {
        if (mRecorder != null) {
            mRecorder.reset();
        } else {
            mRecorder = new MediaRecorder();
        }

        try {
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mFile = getContext().getFilesDir() + "/" + "voice_" + System.currentTimeMillis() + ".mp3";
            isRecordOk = false;

            File file = new File(mFile);
            QLog.d(TAG, "创建文件的路径:" + mFile);
            QLog.d(TAG, "文件创建成功:" + file.exists());
            mRecorder.setOutputFile(mFile);
            mRecorder.prepare();
            mRecorder.start();
            mThread = new ObtainDecibelThread();
            mThread.start();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            QLog.d(TAG, "preparestart异常,重新开始录音:" + exception.toString());
            mRecorder.release();
            mRecorder = null;
        } catch (Exception e) {
            e.printStackTrace();
            QLog.d(TAG, "preparestart异常,重新开始录音:" + e.toString());
            mRecorder.release();
            mRecorder = null;
        }
    }


    public void setOnFinishedRecordListener(OnFinishedRecordListener listener) {
        finishedListener = listener;
    }


    private void stopRecording() {
        if (mThread != null) {
            mThread.exit();
            mThread = null;
        }
        if (mRecorder != null) {
            try {
                mRecorder.stop();//停止时没有prepare，就会报stop failed
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            } catch (RuntimeException pE) {
                pE.printStackTrace();
            } finally {
                if (recordDialog.isShowing()) {
                    recordDialog.dismiss();
                }
            }
        }
    }

    private class ObtainDecibelThread extends Thread {
        private volatile boolean running = true;

        public void exit() {
            running = false;
        }

        @Override
        public void run() {
            while (running) {
                if (mRecorder == null || !running) {
                    break;
                }
                int db = mRecorder.getMaxAmplitude() / 600;
                //Log.i("RecordButton_Tag","检测到的分贝002:"+db);
                if (db != 0 && y >= 0) {
                    int f = (int) (db / 5);
                    if (f == 0)
                        volumeHandler.sendEmptyMessage(0);
                    else if (f == 1)
                        volumeHandler.sendEmptyMessage(1);
                    else if (f == 2)
                        volumeHandler.sendEmptyMessage(2);
                    else if (f == 3)
                        volumeHandler.sendEmptyMessage(3);
                    else if (f == 4)
                        volumeHandler.sendEmptyMessage(4);
                    else if (f == 5)
                        volumeHandler.sendEmptyMessage(5);
                    else if (f == 6)
                        volumeHandler.sendEmptyMessage(6);
                    else
                        volumeHandler.sendEmptyMessage(7);
                }
                volumeHandler.sendEmptyMessage(-1);
//                if (System.currentTimeMillis() - startTime > MAX_INTERVAL_TIME) {
//                    finishRecord();
//                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }





}
