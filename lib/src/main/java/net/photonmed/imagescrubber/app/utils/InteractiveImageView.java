package net.photonmed.imagescrubber.app.utils;

import android.content.Context;
import android.graphics.*;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.OverScroller;

/**
 * TODO:
 * Save Image location on screen rotation (need to stash the saved index somewhere.)
 * For some reason fullscreen mode doesn't dismiss the action bar on certain devices
 * Double tap zoomer
 *TODO:  Need to add a focal point adjustment for when the viewport gets constrained.  I think this can be done,
 *TODO   Via referencing the revised constraints on the viewport.  combine that with the previous values in
 *TODO   some way and that should get it working properly this should be done in constrain viewport since
 *TODO   ZOOM needs these values as well.
 */
public class InteractiveImageView extends ImageView {

    public static final String TAG = InteractiveImageView.class.getPackage() + " " + InteractiveImageView.class.getSimpleName();

    private static final float ZOOM_INCREMENT = 1.25f;
    private static final float X_MIN = -1f;
    private static final float X_MAX = 1f;
    private static final float Y_MIN = -1f;
    private static final float Y_MAX = 1f;

    private float maxScale = .99f;
    private float minScale = 5.0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1.f;
    private float lastScaleFactor;//for double tap to zoom
    private Rect contentRect = new Rect();
    private PointF viewportFocus = new PointF();//abstraction for the currentviewport tracking
    private PointF zoomViewportFocus = new PointF();
    private PointF zoomFocus = new PointF();
    private Point surfaceSizeBuffer = new Point();
    private OverScroller scroller;
    private Zoomer zoomer;//heavily influenced by AOSP samples

    private boolean edgeEffectLeftActive;
    private boolean edgeEffectRightActive;
    private boolean edgeEffectTopActive;
    private boolean edgeEffectBottomActive;

    //Need a viewport to keep frame of reference
    private RectF currentViewport = new RectF(X_MIN, Y_MIN, X_MAX, Y_MAX);
    //And for flinging we need a starting frame of reference
    private RectF scrollerStartViewport = new RectF();

    // Edge effect / overscroll tracking objects.
    private EdgeEffectCompat edgeEffectTop;
    private EdgeEffectCompat edgeEffectBottom;
    private EdgeEffectCompat edgeEffectLeft;
    private EdgeEffectCompat edgeEffectRight;

    public InteractiveImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        scaleDetector = new ScaleGestureDetector(context, scaleListener);
        gestureDetector = new GestureDetector(context, gestureListener);

        scroller = new OverScroller(context);
        zoomer = new Zoomer(context, scaleFactor);

        lastScaleFactor = scaleFactor;

        edgeEffectBottomActive = false;
        edgeEffectLeftActive = false;
        edgeEffectRightActive = false;
        edgeEffectTopActive = false;

        edgeEffectBottom = new EdgeEffectCompat(context);
        edgeEffectLeft = new EdgeEffectCompat(context);
        edgeEffectRight = new EdgeEffectCompat(context);
        edgeEffectTop = new EdgeEffectCompat(context);
    }

    private void constrainViewport() {
        currentViewport.left = Math.max(X_MIN, currentViewport.left);
        currentViewport.top = Math.max(Y_MIN, currentViewport.top);
        currentViewport.bottom = Math.max(Math.nextUp(currentViewport.top),
                Math.min(Y_MAX, currentViewport.bottom));
        currentViewport.right = Math.max(Math.nextUp(currentViewport.left),
                Math.min(X_MAX, currentViewport.right));
    }

    private void setViewportFocus(float x, float y) {
        viewportFocus.set(currentViewport.left + currentViewport.width() * (x - contentRect.left) / contentRect.width(),
                currentViewport.top + currentViewport.height() * (y - contentRect.top)/ contentRect.height());
    }

    private void setCurrentSurfaceSizeBuffer() {
        surfaceSizeBuffer.set((int) (contentRect.width() * (X_MAX - X_MIN) / currentViewport.width()),
                (int) (contentRect.height() * (Y_MAX - Y_MIN) / currentViewport.height()));
    }

    public void releaseEdgeEffects() {
        edgeEffectBottomActive = false;
        edgeEffectLeftActive = false;
        edgeEffectRightActive = false;
        edgeEffectTopActive = false;
        edgeEffectLeft.onRelease();
        edgeEffectRight.onRelease();
        edgeEffectTop.onRelease();
        edgeEffectBottom.onRelease();
    }

    private void setViewportTopLeft(float x, float y) {
        float currentWidth = currentViewport.width();
        float currentHeight = currentViewport.height();

        x = Math.max(X_MIN, Math.min(x, X_MAX - currentWidth));
        y = Math.max(Y_MIN, Math.min(y, Y_MAX - currentHeight));

        currentViewport.set(x, y, x + currentWidth, y + currentHeight);

    }

    private void drawEdgeEffects(Canvas canvas) {
        boolean invalidate = false;

        if (!edgeEffectLeft.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(contentRect.left, contentRect.bottom);
            canvas.rotate(-90, 0, 0);
            edgeEffectLeft.setSize(contentRect.height(), contentRect.width());
            if (edgeEffectLeft.draw(canvas)) {
                invalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeEffectRight.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(contentRect.right, contentRect.top);
            canvas.rotate(90, 0, 0);
            edgeEffectRight.setSize(contentRect.height(), contentRect.width());
            if (edgeEffectRight.draw(canvas)) {
                invalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeEffectTop.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(contentRect.left, contentRect.top);
            edgeEffectTop.setSize(contentRect.height(), contentRect.width());
            if (edgeEffectTop.draw(canvas)) {
                invalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeEffectBottom.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(2 * contentRect.left - contentRect.right, contentRect.bottom);
            canvas.rotate(180, contentRect.width(), 0);
            edgeEffectBottom.setSize(contentRect.width(), contentRect.height());
            if (edgeEffectBottom.draw(canvas)) {
                invalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (invalidate) {
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        //Get the values for our image size for transformation
        contentRect.set(getLeft(), getTop(), getRight(), getBottom());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean retVal = scaleDetector.onTouchEvent(ev);
        retVal = gestureDetector.onTouchEvent(ev) || retVal;
        return retVal || super.onTouchEvent(ev);
    }

    @Override
    public void onDraw(Canvas canvas) {

        canvas.scale(scaleFactor, scaleFactor, zoomFocus.x, zoomFocus.y);

        super.onDraw(canvas);

        drawEdgeEffects(canvas);

    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        boolean invalidateCanvas = false;
        if (scroller.computeScrollOffset()) {

            PLog.d(TAG, String.format("Current viewport now at SCROLLING %s", currentViewport.toString()));
            setCurrentSurfaceSizeBuffer();

            int positionX = scroller.getCurrX();
            int positionY = scroller.getCurrY();

            boolean scrollableX = (currentViewport.left > X_MIN || currentViewport.right < X_MAX);
            boolean scrollableY = (currentViewport.top > Y_MIN || currentViewport.bottom < Y_MAX);

            if (scrollableX && positionX < 0
                    && edgeEffectLeft.isFinished()
                    && !edgeEffectLeftActive) {

                edgeEffectLeft.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectLeftActive = true;
                invalidateCanvas = true;
            } else if (scrollableX && positionX > (surfaceSizeBuffer.x - contentRect.width())
                    && edgeEffectRight.isFinished()
                    && !edgeEffectRightActive) {

                edgeEffectRight.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectRightActive = true;
                invalidateCanvas = true;
            }

            if (positionY < 0 && scrollableY
                    && edgeEffectTop.isFinished()
                    && !edgeEffectTopActive
                    ) {
                edgeEffectTop.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectTopActive = true;
                invalidateCanvas = true;
            } else if (positionY > (surfaceSizeBuffer.y - contentRect.height())
                    && edgeEffectBottom.isFinished()
                    && !edgeEffectBottomActive) {
                edgeEffectBottom.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectBottomActive = true;
                invalidateCanvas = true;
            }

            float currentXRange = X_MIN + (X_MAX - X_MIN) * positionX / surfaceSizeBuffer.x;
            float currentYRange = Y_MIN + (Y_MAX - Y_MIN) * positionY / surfaceSizeBuffer.y;
            setViewportTopLeft(currentXRange, currentYRange);

            scrollTo(positionX, positionY);

        }

        if (zoomer.computeZoom()) {
            float newWidth = (1f - (zoomer.getCurrZoom() - lastScaleFactor)) * scrollerStartViewport.width();
            float newHeight = (1f - (zoomer.getCurrZoom() - lastScaleFactor)) * scrollerStartViewport.height();

            float viewportX = (zoomViewportFocus.x - scrollerStartViewport.left) / scrollerStartViewport.width();
            float viewportY = (zoomViewportFocus.y - scrollerStartViewport.top) / scrollerStartViewport.height();
            currentViewport.set(zoomViewportFocus.x - newWidth * viewportX,
                    zoomViewportFocus.y - newHeight * viewportY,
                    zoomViewportFocus.x + newWidth * (1 - viewportX),
                    zoomViewportFocus.y + newHeight * (1 - viewportY));

            PLog.d(TAG, String.format("Current viewport now at ZOOMING %s", currentViewport.toString()));
            constrainViewport();
            //PLog.d(TAG, String.format("Constrained viewport now at ZOOMING %s", currentViewport.toString()));
            scaleFactor = zoomer.getCurrZoom();
            invalidateCanvas = true;
        }

        if (invalidateCanvas) {
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
        }
    }

    GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            releaseEdgeEffects();
            scrollerStartViewport.set(currentViewport);
            scroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            zoomer.forceFinished(true);
            float useX = ev.getRawX();
            float useY = ev.getRawY();
            if (contentRect.contains((int)useX, (int)useY)) {
                zoomViewportFocus.set(currentViewport.left + currentViewport.width() * (useX - contentRect.left) / contentRect.width(),
                        currentViewport.top + currentViewport.height() * (useY - contentRect.top) / contentRect.height());
                zoomFocus.set(useX, useY);
                PLog.d(TAG, String.format("Double tap pressed zoom focus %s", zoomViewportFocus));
                float finalScaleFactor = scaleFactor * ZOOM_INCREMENT;
                finalScaleFactor = Math.min(finalScaleFactor, minScale);
                lastScaleFactor = scaleFactor;
                zoomer.startZoom(finalScaleFactor);
                ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float distanceX, float distanceY) {

            float viewportOffsetX = distanceX * currentViewport.width() / contentRect.width();
            float viewportOffsetY = distanceY * currentViewport.height() / contentRect.height();
            setCurrentSurfaceSizeBuffer();
            PLog.d(TAG, String.format("SURFACE BUFFER IS %s current viewport is %s", surfaceSizeBuffer.toString(), currentViewport.toString()));
            int scrolledX = (int) (surfaceSizeBuffer.x * (currentViewport.left + viewportOffsetX - X_MIN) / (X_MAX - X_MIN));
            int scrolledY = (int) (surfaceSizeBuffer.y * (currentViewport.top + viewportOffsetY - Y_MIN) / (Y_MAX - Y_MIN));

            boolean scrollableX = currentViewport.left > X_MIN || currentViewport.right < X_MAX;
            boolean scrollableY = currentViewport.top > Y_MIN || currentViewport.bottom < Y_MAX;

            PLog.d(TAG, String.format("The big haus scrolledX %d scrolledY %d", scrolledX, scrolledY));

            setViewportTopLeft(currentViewport.left + viewportOffsetX, currentViewport.top + viewportOffsetY);

            if (scrollableX && scrolledX < 0) {
                edgeEffectLeft.onPull(scrolledX / contentRect.width());
                edgeEffectLeftActive = true;
            }
            if (scrollableY && scrolledY < 0) {
                edgeEffectTop.onPull(scrolledY / contentRect.height());
                edgeEffectTopActive = true;
            }
            if (scrollableX && scrolledX > surfaceSizeBuffer.x - contentRect.width()) {
                edgeEffectRight.onPull((scrolledX - surfaceSizeBuffer.x + contentRect.width()) /
                        (float) contentRect.width());
                edgeEffectRightActive = true;
            }
            if (scrollableY && scrolledY > surfaceSizeBuffer.y - contentRect.height()) {
                edgeEffectBottom.onPull((scrolledY - surfaceSizeBuffer.y + contentRect.height()) /
                        (float) contentRect.height());
                edgeEffectBottomActive = true;
            }

            int startX = (int) (surfaceSizeBuffer.x * (currentViewport.left - X_MIN) / (X_MAX - X_MIN));
            int startY = (int) (surfaceSizeBuffer.y * (currentViewport.top - Y_MIN) / (Y_MAX - Y_MIN));

            scroller.startScroll(startX, startY, (int) distanceX, (int) distanceY, 100);
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
    }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float velocityX, float velocityY) {
            releaseEdgeEffects();
            setCurrentSurfaceSizeBuffer();
            scrollerStartViewport.set(currentViewport);
            int startX = (int)(surfaceSizeBuffer.x * (scrollerStartViewport.left - X_MIN) / (X_MAX - X_MIN));
            int startY = (int)(surfaceSizeBuffer.y * (scrollerStartViewport.top - Y_MIN) / (Y_MAX - Y_MIN));
            scroller.fling(
                    startX,
                    startY,
                    (int)-velocityX/2,
                    (int)-velocityY/2,
                    0, surfaceSizeBuffer.x - contentRect.width(),
                    0, surfaceSizeBuffer.y - contentRect.height(),
                    contentRect.width()/10,
                    contentRect.height()/10);
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
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

            float newWidth = lastSpanX / spanX * currentViewport.width();
            float newHeight = lastSpanY /spanY * currentViewport.height();

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();

            PLog.d(TAG, String.format("currentScale %f scaleFactor %f", scaleGestureDetector.getScaleFactor(), scaleFactor));

            if (contentRect.contains((int)focusX, (int)focusY)) {
                setViewportFocus(focusX, focusY);
            }

            currentViewport.set(viewportFocus.x - newWidth * (focusX - contentRect.left) / contentRect.width(),
                    viewportFocus.y - newHeight * (focusY - contentRect.top) / contentRect.height(),
                    0, 0);
            currentViewport.right = currentViewport.left + newWidth;
            currentViewport.bottom = currentViewport.top + newHeight;
            //PLog.l(TAG, PLog.LogLevel.DEBUG, String.format("Current viewport is now %s", currentViewport.toString()));

            constrainViewport();
            lastSpanX = spanX;
            lastSpanY = spanY;

            scaleFactor *= scaleGestureDetector.getScaleFactor();

            PLog.d(TAG, String.format("Gesture Detector scale Factor %f", scaleGestureDetector.getScaleFactor()));

            lastScaleFactor = scaleFactor = Math.max(maxScale, Math.min(scaleFactor, minScale));
            zoomFocus.set(focusX, focusY);
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);

            return true;
        }
    };
}
