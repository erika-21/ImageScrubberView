package net.photonmed.imagescrubber.app.utils;

import android.content.Context;
import android.graphics.*;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.OverScroller;

/**
 * TODO:
 * Panning
 * Save Image location on screen rotation (need to stash the saved index somewhere.)
 * For some reason fullscreen mode doesn't dismiss the
 * EdgeCompat Animations
 * Double tap zoomer
 */
public class InteractiveImageView extends ImageView {

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1.f;
    private Rect contentRect = new Rect();
    private PointF viewportFocus = new PointF();
    private RectF currentViewport;
    private OverScroller scroller;

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        scaleDetector = new ScaleGestureDetector(context, scaleListener);
        gestureDetector = new GestureDetector(context, gestureListener);

        scroller = new OverScroller(context);

    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        //Get the values for our image size for transformation
        contentRect.set(getLeft(), getTop(), getRight(), getBottom());
        currentViewport = new RectF(getLeft(), getTop(), getRight(), getBottom());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean retVal = scaleDetector.onTouchEvent(ev);
        retVal = gestureDetector.onTouchEvent(ev) || retVal;
        return retVal || super.onTouchEvent(ev);
    }

    @Override
    public void onDraw(Canvas canvas) {
        int restore = canvas.save();
        canvas.scale(scaleFactor, scaleFactor, viewportFocus.x, viewportFocus.y);
        super.onDraw(canvas);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        boolean invalidateCanvas = false;
        // The scroller isn't finished, meaning a fling or programmatic pan operation is
        // currently active.

        PLog.l("YIPES", PLog.LogLevel.DEBUG, "COMPUTE SCROLL MUTHA FUCKA!");
        if (scroller.computeScrollOffset()) {

            PLog.l("YIPES", PLog.LogLevel.DEBUG, String.format("YAY WE SHOULD FUCKING MOVE NOW!! %s", currentViewport));
            //setSurfaceSizeBuffer(surfaceSizeBuffer);

            int currX = scroller.getCurrX();
            int currY = scroller.getCurrY();

            boolean canScrollX = (currentViewport.left > contentRect.left
                    || currentViewport.right < contentRect.right);
            boolean canScrollY = (currentViewport.top > contentRect.top
                    || currentViewport.bottom < contentRect.bottom);

            if (canScrollX
                    && currX < 0) {
                    //&& mEdgeEffectLeft.isFinished()
                   // && !mEdgeEffectLeftActive) {
                //mEdgeEffectLeft.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                //mEdgeEffectLeftActive = true;
                invalidateCanvas = true;
            } else if (canScrollX)
                    //&& mEdgeEffectRight.isFinished()
                    //&& !mEdgeEffectRightActive
                      {
                //mEdgeEffectRight.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                //mEdgeEffectRightActive = true;
                invalidateCanvas = true;
            }

            if (canScrollY
                    && currY < 0
                 //   && mEdgeEffectTop.isFinished()
                 //   && !mEdgeEffectTopActive
                 ) {
                //mEdgeEffectTop.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                //mEdgeEffectTopActive = true;
                invalidateCanvas = true;
            } else if (canScrollY)
                    //&& mEdgeEffectBottom.isFinished()
                    //&& !mEdgeEffectBottomActive
                     {
                //mEdgeEffectBottom.onAbsorb((int) OverScrollerCompat.getCurrVelocity(mScroller));
                //mEdgeEffectBottomActive = true;
                invalidateCanvas = true;
            }


            //float currXRange = surfaceSizeBuffer.x > 0 ? currX / surfaceSizeBuffer.x : currX;
            //float currYRange = surfaceSizeBuffer.y > 0 ? currY / surfaceSizeBuffer.y : currY;
            //setViewportBottomLeft(currXRange, currYRange);

            if (invalidateCanvas) {
                scrollTo(currX, currY);
                ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            }
        }
    }

    GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            //releaseEdgeEffects
            scroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent ev) {

            //todo:  Initiate zoom
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
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float distanceX, float distanceY) {
            float oldX = motionEvent.getX();
            float newX = motionEvent2.getX();
            float oldY = motionEvent.getY();
            float newY = motionEvent2.getY();

            float currentWidth = currentViewport.width();
            float currentHeight = currentViewport.height();
            PLog.l("YIPES", PLog.LogLevel.DEBUG, String.format("YAY HERE WE ARE %f %f Current Viewport %s", distanceX,
                    distanceY, currentViewport.toString()));

            if (oldX < newX) {
                //pan left
                PLog.l("YIPES", PLog.LogLevel.DEBUG, "Panning left");
                currentViewport.left = Math.max(currentViewport.left - Math.abs(distanceX), contentRect.left);
                currentViewport.right = currentViewport.left + currentWidth;
            } else {
                //pan right
                PLog.l("YIPES", PLog.LogLevel.DEBUG, "Panning right");
                currentViewport.right = Math.min(currentViewport.right + distanceX, contentRect.right);
                currentViewport.left = currentViewport.right - currentWidth;
            }

            if (oldY < newY) {
                //pan up
                currentViewport.top = Math.max(currentViewport.top - Math.abs(distanceY), contentRect.top);
                currentViewport.bottom = currentViewport.top + currentHeight;
            } else {
                //pan down
                currentViewport.bottom = Math.min(currentViewport.bottom + distanceY, contentRect.bottom);
                currentViewport.top = currentViewport.bottom - currentHeight;
            }

            //ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float velocityX, float velocityY) {
            //setSurfaceSizeBuffer(surfaceSizeBuffer);
            return false;
            /*
            int startX = (int) (surfaceSizeBuffer.x * currentViewport.left);
            int startY = (int) (surfaceSizeBuffer.y * currentViewport.bottom);
            scroller.forceFinished(true);
            scroller.fling(
                    startX,
                    startY,
                    (int)velocityX,
                    (int)velocityY,
                    0, surfaceSizeBuffer.x - contentRect.width(),
                    0, surfaceSizeBuffer.y - contentRect.height(),
                    contentRect.width() / 2,
                    contentRect.height() / 2);

            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
            */
        }
    };

    private final ScaleGestureDetector.OnScaleGestureListener scaleListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private float lastSpanX;
        private float lastSpanY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            lastSpanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector);
            lastSpanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float spanX = ScaleGestureDetectorCompat.getCurrentSpanX(scaleGestureDetector);
            float spanY = ScaleGestureDetectorCompat.getCurrentSpanY(scaleGestureDetector);

            //The ratio of the start to the current spans scaled by the current viewport dimensions
            float newWidth = (lastSpanX/spanX) * currentViewport.width();
            float newHeight = (lastSpanY/spanY) * currentViewport.height();

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();

            if (contentRect.contains((int)focusX, (int)focusY)) {
                viewportFocus.set(contentRect.left + contentRect.width() * (focusX - contentRect.left)/contentRect.width(),
                        contentRect.top + contentRect.height() * (focusY - contentRect.top)/contentRect.height());
            }

            currentViewport.set(viewportFocus.x - newWidth * (focusX - contentRect.left)/contentRect.width(),
                    viewportFocus.y - newHeight * (focusY - contentRect.top)/contentRect.height(),
                    0,
                    0);
            currentViewport.right = currentViewport.left + newWidth;
            currentViewport.bottom = currentViewport.top + newHeight;
            scaleFactor *= scaleGestureDetector.getScaleFactor();

            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f));
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);

            lastSpanX = spanX;
            lastSpanY = spanY;
            return true;
        }
    };
}
