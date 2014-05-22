package net.photonmed.imagescrubber.app.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/**
 */
public class InteractiveImageView extends ImageView {

    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.f;

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        scaleDetector = new ScaleGestureDetector(context, scaleListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        if (!scaleDetector.isInProgress()) {
           // focalX = ev.getX();
           // focalY = ev.getY();
        }
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {

        canvas.save();
       // canvas.scale(scaleFactor, scaleFactor, focalX, focalY);
        canvas.scale(scaleFactor, scaleFactor);
        super.onDraw(canvas);
    }

    GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
            return false;
        }
    };

    ScaleGestureDetector.OnScaleGestureListener scaleListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            scaleFactor *= scaleGestureDetector.getScaleFactor();

            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f));
            invalidate();
            return true;
        }

    };
}
