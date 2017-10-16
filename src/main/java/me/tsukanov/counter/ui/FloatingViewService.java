package me.tsukanov.counter.ui;

/**
 * Created by admin on 10/14/17.
 */


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import me.tsukanov.counter.CounterApplication;
import me.tsukanov.counter.R;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class FloatingViewService extends Service implements View.OnClickListener {

    //    public static final int MAX_VALUE = 9999;
    public static final int MAX_VALUE = 10000;
    public static final int MIN_VALUE = 0;
    public static final int DEFAULT_VALUE = MIN_VALUE;
    private static final long DEFAULT_VIBRATION_DURATION = 30; // Milliseconds

    // copied from counterfragment.java
    private WindowManager mWindowManager;
    private View mFloatingView;
    private View collapsedView;
    private View expandedView;
    private String name = null;
    private int value = DEFAULT_VALUE;
    private CounterApplication app;
    private SharedPreferences settings;
    private Vibrator vibrator;
    private SoundPool soundPool;
    private SparseIntArray soundsMap;
    private TextView counterName;
    private TextView counterValue;
    private int sideLength;

    private boolean touchMoved = false;

//    private Button incrementButton;
//    private Button decrementButton;


    private GestureDetector gestureDetector;

    public FloatingViewService() {
    }

    @SuppressLint("ValidFragment")
    public FloatingViewService(String name) {
        this.name = name;
    }

    @SuppressLint("ValidFragment")
    public FloatingViewService(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public String getCounterName() {
        return name;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleStart(intent, startId);
        return START_NOT_STICKY;
    }

    private void handleStart(Intent intent, int startId) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
        } else {
            this.name = extras.getString("name");
            this.value = Integer.parseInt(extras.getString("value"));

            counterName.setText(name);
            counterValue.setText(Integer.toString(value));

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(
                getBaseContext(),
                getResources().getText(R.string.toast_wipe_success),
                Toast.LENGTH_SHORT
        ).show();

        //getting the widget layout from xml using layout inflater
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        //setting the layout parameters
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //getting windows services and adding the floating view to it
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        counterName = (TextView) mFloatingView.findViewById(R.id.counterName);
        counterValue = (TextView) mFloatingView.findViewById(R.id.counterValue);

        //getting the collapsed and expanded view from the floating view
        collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
        expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

        //adding click listener to close button and expanded view
        mFloatingView.findViewById(R.id.buttonCloseCollapsed).setOnClickListener(this);
        mFloatingView.findViewById(R.id.buttonCloseExpanded).setOnClickListener(this);
//        expandedView.setOnClickListener(this); // commenting


        View.OnTouchListener someOnTouchListener = new View.OnTouchListener() {

            boolean isExpanded;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean hasMoved = false;
            private int halfScreenWidth = getScreenWidth() / 2;
            private int halfScreenHeight = getScreenHeight() / 2;
            private int halfSideLength;
//            private boolean isPortrait =

//            private int halfSideLength = sideLength / 2;


            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        hasMoved = false;

                        if (expandedView.getVisibility() == View.VISIBLE) isExpanded = true;
                        else isExpanded = false;

                        return true;

                    case MotionEvent.ACTION_UP:
                        //when the drag is ended switching the state of the widget
                        if (!hasMoved) {
                            if (isExpanded) {
                                increment();
                            } else {
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                                isExpanded = false;
                                hasMoved = false;
                            }

                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        //this code is helping the widget to move around the screen with fingers
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        if (getApplicationContext().getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE){

                            if (params.y < -(halfScreenWidth - halfSideLength))
                                params.y = -(halfScreenWidth - halfSideLength);

                            if (params.y > (halfScreenWidth - halfSideLength))
                                params.y = halfScreenWidth - halfSideLength;

                            if (params.x < -(halfScreenHeight - halfSideLength))
                                params.x = -(halfScreenHeight - halfSideLength);

                            if (params.x > (halfScreenHeight - halfSideLength))
                                params.x = halfScreenHeight - halfSideLength;

                        }else{

                            if (params.x < -(halfScreenWidth - halfSideLength))
                                params.x = -(halfScreenWidth - halfSideLength);

                            if (params.x > (halfScreenWidth - halfSideLength))
                                params.x = halfScreenWidth - halfSideLength;

                            if (params.y < -(halfScreenHeight - halfSideLength))
                                params.y = -(halfScreenHeight - halfSideLength);

                            if (params.y > (halfScreenHeight - halfSideLength))
                                params.y = halfScreenHeight - halfSideLength;

                        }


                        if (isExpanded) halfSideLength = sideLength / 2;
                        else halfSideLength = 0;


                        int distanceMovedX = (int) (event.getRawX() - initialTouchX);
                        int distanceMovedY = (int) (event.getRawY() - initialTouchY);
                        int distanceMoved = (int) Math.sqrt(distanceMovedX * distanceMovedX + distanceMovedY * distanceMovedY);

                        // value is to be specified based on human tremor
                        // could be different

                        if (distanceMoved > 70)
                            hasMoved = true;

                        if (isExpanded && distanceMoved < 70) {
                        } else {
                            mWindowManager.updateViewLayout(mFloatingView, params);
                        }
                        return true;
                }
                return false;
            }

        };

        //adding an touchlistener to make drag movement of the floating widget
        mFloatingView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(someOnTouchListener);
//        expandedView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(someOnTouchListener);


        // copied from counterfragment.java
        app = (CounterApplication) getApplication();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        /** Setting up sounds */
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        soundsMap = new SparseIntArray(2);
        soundsMap.put(FloatingViewService.Sound.INCREMENT_SOUND.ordinal(), soundPool.load(getApplicationContext(), R.raw.increment_sound, 1));
        soundsMap.put(FloatingViewService.Sound.DECREMENT_SOUND.ordinal(), soundPool.load(getApplicationContext(), R.raw.decrement_sound, 1));


        // width
        sideLength = Math.min(getScreenHeight(), getScreenWidth());
        sideLength = sideLength * Integer.parseInt(settings.getString("widgetScreenPercentage", "50")) / 100;
        int transparency = Integer.parseInt(settings.getString("widgetTransparency", "50"));
        int alpha = (100 - transparency) * 255 / 100;

        expandedView.getLayoutParams().height = sideLength; // settings.getInt("widget_sideLength",100);
        expandedView.getLayoutParams().width = sideLength; // settings.getInt("widget_sideLength",100);
        expandedView.getBackground().setAlpha(alpha);


//        tv_value.setTextColor(settings.getInt("widget_frontColor", 0xddffffff));
//        tv_value.setBackgroundColor(settings.getInt("widget_backColor", 0xaa00ff00));
//        tv_value.getBackground().setAlpha((100 - settings.getInt("widget_alpha", 50)) * 255 / 100);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layoutExpanded:
                //switching views
//                collapsedView.setVisibility(View.VISIBLE);
//                expandedView.setVisibility(View.GONE);
                increment();
                break;

            case R.id.layoutCollapsed:
                //switching views
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
                break;

            case R.id.buttonCloseExpanded:
                //closing the widget
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
                break;

            case R.id.buttonCloseCollapsed:
                //closing the widget
                stopSelf();
                break;
        }
    }


    // from now on
    // copied from increment

    private void showResetConfirmationDialog() {
        Dialog dialog = new AlertDialog.Builder(getApplicationContext())
                .setMessage(getResources().getText(R.string.dialog_reset_title))
                .setCancelable(false)
                .setPositiveButton(getResources().getText(R.string.dialog_button_reset),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                reset();
                            }
                        })
                .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
                .create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

//    private void showEditDialog() {
//        EditDialog dialog = EditDialog.newInstance(name, value);
//        dialog.show(getFragmentManager(), EditDialog.TAG);
//    }
//
//    private void showDeleteDialog() {
//        DeleteDialog dialog = DeleteDialog.newInstance(name);
//        dialog.show(getFragmentManager(), DeleteDialog.TAG);
//    }

    public void increment() {
        if (value < MAX_VALUE) {
//            setValue(++value);
            // ADDING CODE
            int countAmount = Integer.parseInt(settings.getString("countAmount", "1"));
            int newValue = value + countAmount;
            if (value > MAX_VALUE) {
                value = MAX_VALUE;
            } else if (value < MIN_VALUE) {
                value = MIN_VALUE;
            }
            setValue(newValue);

//            vibrateTick(DEFAULT_VIBRATION_DURATION);
            vibrate();
            playSound(FloatingViewService.Sound.INCREMENT_SOUND);
        }
    }

    public void decrement() {

        if (value > MIN_VALUE) {
//            setValue(--value);
            // ADDING CODE
            int countAmount = Integer.parseInt(settings.getString("countAmount", "1"));
            int newValue = value - countAmount;
            if (value > MAX_VALUE) {
                value = MAX_VALUE;
            } else if (value < MIN_VALUE) {
                value = MIN_VALUE;
            }
            setValue(newValue);


//            vibrateTick(DEFAULT_VIBRATION_DURATION + 20);
            vibrate();
            playSound(FloatingViewService.Sound.DECREMENT_SOUND);
        }
    }

    public void reset() {
        setValue(DEFAULT_VALUE);
    }

    public void setValue(int value) {
        if (value > MAX_VALUE) value = MAX_VALUE;
        else if (value < MIN_VALUE) value = MIN_VALUE;
        this.value = value;
        counterValue.setText(Integer.toString(value));
        checkStateOfButtons();
        saveValue();
    }

    private void saveValue() {
        app.counters.remove(name);
        app.counters.put(name, value);
    }

    public String getName() {
        return name;
    }

    private void checkStateOfButtons() {
//        if (value >= MAX_VALUE) incrementButton.setEnabled(false);
//        else incrementButton.setEnabled(true);
//        if (value <= MIN_VALUE) decrementButton.setEnabled(false);
//        else decrementButton.setEnabled(true);

        // todo: implement functionality to service

        if (value >= MAX_VALUE) expandedView.setEnabled(false);
        else expandedView.setEnabled(true);

        if (value <= MIN_VALUE) expandedView.setEnabled(false);
        else expandedView.setEnabled(true);

    }

    private void vibrateTick(long duration) {
        if (settings.getBoolean("vibrationOn", true)) {
            vibrator.vibrate(duration);
        }
    }

    // ADDING CODE
    private void vibrateCheckpoint(long duration) {
        if (settings.getBoolean("checkpointVibrationOn", true)) {
            vibrator.vibrate(duration);
        }
    }

    private void vibrate() {

        int checkpointValue = Integer.parseInt(settings.getString("checkpointValue", "100"));
        boolean vibrationOn = settings.getBoolean("vibrationOn", true);
        boolean checkpointVibrationOn = settings.getBoolean("checkpointVibrationOn", true);
        int vibrationTime = Integer.parseInt(settings.getString("vibrationTime", "30"));
        int checkpointVibrationTime = Integer.parseInt(settings.getString("checkpointVibrationTime", "90"));

        if (value % checkpointValue == 0) { // checkpoint case
            if (checkpointVibrationOn) vibrator.vibrate(checkpointVibrationTime);
        } else { // normal case
            if (vibrationOn) vibrator.vibrate(vibrationTime);
        }
    }

    private void playSound(FloatingViewService.Sound sound) {
        if (settings.getBoolean("soundsOn", false)) {
//            AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
            AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            float volume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            soundPool.play(soundsMap.get(sound.ordinal()), volume, volume, 1, 0, 1f);
        }
    }

    private enum Sound {
        INCREMENT_SOUND, DECREMENT_SOUND
    }


    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

}


// https://stackoverflow.com/a/28106474
// todo: implement circular widget option
/*
Thank you for your all support, based on your support I have done by below way and it is working perfect
        ImageView imgView = (ImageView) findViewById(R.id.imageView1);
        imgView.setOnTouchListener(new View.OnTouchListener() {

@Override
public boolean onTouch(View v, MotionEvent event) {

        //CIRCLE :      (x-a)^2 + (y-b)^2 = r^2
        float centerX, centerY, touchX, touchY, radius;
        centerX = v.getWidth() / 2;
        centerY = v.getHeight() / 2;
        touchX = event.getX();
        touchY = event.getY();
        radius = centerX;
        System.out.println("centerX = "+centerX+", centerY = "+centerY);
        System.out.println("touchX = "+touchX+", touchY = "+touchY);
        System.out.println("radius = "+radius);
        if (Math.pow(touchX - centerX, 2)
        + Math.pow(touchY - centerY, 2) < Math.pow(radius, 2)) {
        System.out.println("Inside Circle");
        return false;
        } else {
        System.out.println("Outside Circle");
        return true;
        }
        }
        });*/