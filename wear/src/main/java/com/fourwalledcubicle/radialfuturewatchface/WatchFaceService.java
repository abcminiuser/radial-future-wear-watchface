package com.fourwalledcubicle.radialfuturewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.view.SurfaceHolder;
import android.text.format.Time;

import java.util.TimeZone;

public class WatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private class InflatableRectF extends RectF {
            public InflatableRectF(RectF f) { super(f); }
            public InflatableRectF(Rect r) { super(r); }

            public void inflate(float amount) {
                this.left   += amount / 2;
                this.top    += amount / 2;
                this.right  -= amount / 2;
                this.bottom -= amount / 2;
            }
        }

        static final int MSG_UPDATE_TIME = 0;
        static final int INTERACTIVE_UPDATE_RATE_MS = 1000;

        private Time mTime;
        private Paint mPaint;
        private boolean mRegisteredTimeZoneReceiver;

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();

                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }

                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setAntiAlias(true);

            mTime = new Time();
            mTime.setToNow();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int colors[] = {Color.RED, Color.GREEN, Color.BLUE, 0xC32E96, Color.YELLOW};
            float values[] = {mTime.second / 60.0f, mTime.minute / 60.0f, mTime.hour / 24.0f, mTime.yearDay / 365.0f};

            for (int i = 0; i < values.length; i++) {
                float h = (360.0f / values.length) * i;
                float hsv[] = {h, .75f, .7f};
                colors[i] = Color.HSVToColor(hsv);
            }

            final int width = bounds.width() / values.length;

            InflatableRectF currentBounds = new InflatableRectF(bounds);
            currentBounds.inflate(width / 2);

            mPaint.setStrokeWidth((int)(.70 * width / 2));

            for (int i = 0; i < values.length; i++) {
                mPaint.setColor(colors[i]);

                float degrees = Math.min(360 * values[i], 360);
                canvas.drawArc(currentBounds, 270, degrees, false, mPaint);

                currentBounds.inflate(width);
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode), so we may need to start or stop the timer
            updateTimer();
        }
    }
}