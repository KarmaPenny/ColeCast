package com.colecast.colecast;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

public class RepeatListener implements OnTouchListener {

    private Handler handler = new Handler();

    private int initialInterval;
    private final int normalInterval;
    private final OnClickListener clickListener;
    private View touchedView;

    private Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            if(touchedView.isEnabled()) {
                handler.postDelayed(this, normalInterval);
                clickListener.onClick(touchedView);
            } else {
                // if the view was disabled by the clickListener, remove the callback
                handler.removeCallbacks(handlerRunnable);
                touchedView.setPressed(false);
                touchedView = null;
            }
        }
    };

    /**
     * @param repeatDelay milliseconds until repeat starts
     * @param repeatInterval milliseconds between repeats
     *       events
     * @param clickListener The OnClickListener, that will be called
     *       periodically
     */
    public RepeatListener(int repeatDelay, int repeatInterval, OnClickListener clickListener) {
        if (clickListener == null)
            throw new IllegalArgumentException("null runnable");
        if (repeatDelay < 0 || repeatInterval < 0)
            throw new IllegalArgumentException("negative interval");

        initialInterval = repeatDelay;
        normalInterval = repeatInterval;
        this.clickListener = clickListener;
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(handlerRunnable);
                handler.postDelayed(handlerRunnable, initialInterval);
                touchedView = view;
                touchedView.setPressed(true);
                clickListener.onClick(view);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(handlerRunnable);
                touchedView.setPressed(false);
                touchedView = null;
                return true;
        }

        return false;
    }

}