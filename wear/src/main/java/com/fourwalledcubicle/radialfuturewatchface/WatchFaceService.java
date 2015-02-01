package com.fourwalledcubicle.radialfuturewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.TimeZone;

public class WatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;
        static final int INTERACTIVE_UPDATE_RATE_MS = 1000;

        private Time mTime;
        private Paint mPaint;
        private Paint mTextPaint;
        private Paint mTextBGPaint;
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
            mPaint.setStrokeCap(Paint.Cap.BUTT);
            mPaint.setAntiAlias(true);

            mTextPaint = new Paint();
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mTextPaint.setColor(Color.WHITE);

            mTextBGPaint = new Paint();

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

            updateTimer();

            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int valuesCurrent[] = {mTime.second, mTime.minute, mTime.hour, mTime.monthDay, mTime.month + 1};
            int valuesMax[] = {mTime.getActualMaximum(mTime.SECOND), mTime.getActualMaximum(mTime.MINUTE), mTime.getActualMaximum(mTime.HOUR), mTime.getActualMaximum(mTime.MONTH_DAY), mTime.getActualMaximum(mTime.MONTH) + 1};
            int colorsFill[] = new int[valuesCurrent.length];
            int colorsRem[] = new int[valuesCurrent.length];

            for (int i = 0; i < valuesCurrent.length; i++) {
                float h = (360.0f / valuesCurrent.length) * i;
                float hsv[] = {h, isInAmbientMode() ? 0.0f : 1.0f, isInAmbientMode() ? 0.5f : 1.0f};

                colorsFill[i] = Color.HSVToColor(hsv);
                hsv[2] /= 3.0f;
                colorsRem[i] = Color.HSVToColor(hsv);
            }

            final int width = bounds.width() / valuesCurrent.length;

            RectF currentBounds = new RectF(bounds);
            currentBounds.inset(width / 4, width / 4);

            mPaint.setStrokeWidth((int) (.80 * width / 2));
            mTextPaint.setTextSize((mPaint.getStrokeWidth() / 2) - 2);

            canvas.drawColor(Color.BLACK);

            for (int i = 0; i < valuesCurrent.length; i++) {
                if (!isInAmbientMode() || i != 0) {
                    float degrees = Math.min(360 * ((float)valuesCurrent[i] / valuesMax[i]), 360);

                    mPaint.setColor(colorsRem[i]);
                    canvas.drawArc(currentBounds, degrees, 360 - degrees, false, mPaint);
                    mPaint.setColor(colorsFill[i]);
                    canvas.drawArc(currentBounds, 0, degrees, false, mPaint);

                    PointF textLocation = new PointF(
                            currentBounds.right,
                            currentBounds.top + (currentBounds.height() / 2));

                    mTextBGPaint.setColor(colorsFill[i]);
                    mTextBGPaint.setShader(
                            new RadialGradient(
                                    textLocation.x,
                                    textLocation.y,
                                    mPaint.getStrokeWidth(),
                                    Color.BLACK, colorsFill[i], Shader.TileMode.CLAMP));
                    canvas.drawCircle(
                            textLocation.x,
                            textLocation.y,
                            (mPaint.getStrokeWidth() / 2) - 2,
                            mTextBGPaint);
                    canvas.drawText(
                            Integer.toString(valuesCurrent[i]),
                            textLocation.x - 1,
                            textLocation.y + (mTextPaint.getTextSize() / 2) - 1,
                            mTextPaint);
                }

                currentBounds.inset(width / 2, width / 2);
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