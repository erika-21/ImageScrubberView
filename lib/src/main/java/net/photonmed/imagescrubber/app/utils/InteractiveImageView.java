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
 * Panning still isn't quite right when zoomed in.  I think it has something to do with how we're
 * constraining the bounds on onScroll, seems to happen regardless
 * Save Image location on screen rotation (need to stash the saved index somewhere.)
 * For some reason fullscreen mode doesn't dismiss the
 * Double tap zoomer
 */
public class InteractiveImageView extends ImageView {

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float scaleFactor = 1.f;
    private Rect contentRect = new Rect();
    private PointF viewportFocus = new PointF();
    private OverScroller scroller;

    private boolean edgeEffectLeftActive;
    private boolean edgeEffectRightActive;
    private boolean edgeEffectTopActive;
    private boolean edgeEffectBottomActive;

    private int positionX = 0;
    private int positionY = 0;

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

        edgeEffectBottomActive = false;
        edgeEffectLeftActive = false;
        edgeEffectRightActive = false;
        edgeEffectTopActive = false;

        edgeEffectBottom = new EdgeEffectCompat(context);
        edgeEffectLeft = new EdgeEffectCompat(context);
        edgeEffectRight = new EdgeEffectCompat(context);
        edgeEffectTop = new EdgeEffectCompat(context);
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
            edgeEffectRight.setSize(contentRect.width(), contentRect.height());
            if (edgeEffectRight.draw(canvas)) {
                invalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!edgeEffectTop.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(contentRect.left, contentRect.top);
            edgeEffectTop.setSize(contentRect.width(), contentRect.height());
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
        PLog.l("YIPES", PLog.LogLevel.DEBUG, String.format("Content rect %s", contentRect.toString()));
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean retVal = scaleDetector.onTouchEvent(ev);
        retVal = gestureDetector.onTouchEvent(ev) || retVal;
        return retVal || super.onTouchEvent(ev);
    }

    @Override
    public void onDraw(Canvas canvas) {

        canvas.scale(scaleFactor, scaleFactor, viewportFocus.x, viewportFocus.y);

        super.onDraw(canvas);

        drawEdgeEffects(canvas);

    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        boolean invalidateCanvas = false;
        // The scroller isn't finished, meaning a fling or programmatic pan operation is
        // currently active.

        if (scroller.computeScrollOffset()) {

            //setSurfaceSizeBuffer(surfaceSizeBuffer);

            positionX  = scroller.getCurrX();
            positionY = scroller.getCurrY();

            PLog.l("YIPES", PLog.LogLevel.DEBUG, String.format("Posish %d %d", positionX, positionY));
            scrollTo(positionX, positionY);

            if (positionX <= 0
                   && edgeEffectLeft.isFinished()
                    && !edgeEffectLeftActive) {

                edgeEffectLeft.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectLeftActive = true;
                invalidateCanvas = true;
            } else if (positionX >= contentRect.right
                    && edgeEffectRight.isFinished()
                    && !edgeEffectRightActive) {

                PLog.l("YIPES", PLog.LogLevel.DEBUG, "Position X > the right wall.");

                edgeEffectRight.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectRightActive = true;
                invalidateCanvas = true;
            }

            if (positionY < 0
                    && edgeEffectTop.isFinished()
                    && !edgeEffectTopActive
                 ) {
                edgeEffectTop.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectTopActive = true;
                invalidateCanvas = true;
            } else if (positionY >= contentRect.bottom
                    && edgeEffectBottom.isFinished()
                    && !edgeEffectBottomActive)
                     {
                edgeEffectBottom.onAbsorb((int) OverScrollerCompat.getCurrVelocity(scroller));
                edgeEffectBottomActive = true;
                invalidateCanvas = true;
            }

            if (invalidateCanvas) {
                ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            }
        }
    }

    GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {


        @Override
        public boolean onDown(MotionEvent motionEvent) {
            releaseEdgeEffects();
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
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float distanceX, float distanceY) {

            int dx = (int)distanceX;
            int dy = (int)distanceY;
            int newPositionX = positionX + dx;
            int newPositionY = positionY + dy;
            float initialTouchX = motionEvent.getX();
            float initialTouchY = motionEvent.getY();

            PLog.l("YIPES", PLog.LogLevel.DEBUG, String.format("initialX %f initialY %f", initialTouchX, initialTouchY));
            if (newPositionX < contentRect.left) {
                dx = 0;
                edgeEffectLeft.onPull(distanceX/contentRect.width());
            } else if ((initialTouchX + dx) > contentRect.width()) {
                dx = 0;
                edgeEffectRight.onPull(distanceX/contentRect.width());
            }
            if (newPositionY < contentRect.top) {
                dy -= newPositionY;
                edgeEffectTop.onPull(distanceY/contentRect.height());
            } else if (initialTouchY + dy > contentRect.bottom) {
                dy -= newPositionY - contentRect.bottom;
                edgeEffectBottom.onPull(distanceY/contentRect.height());
            }

            scroller.startScroll(positionX, positionY, dx, dy, 0);
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            PLog.l("YIPES", PLog.LogLevel.DEBUG, String.format("This is the Posish dawg %d %d with new posish %d %d " +
                            "and dx dy %d %d the float falues were %f %f",
                    newPositionX, newPositionY, positionX, positionY, dx, dy, distanceX, distanceY));

            return true;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float velocityX, float velocityY) {
            PLog.l("YIPES", PLog.LogLevel.DEBUG, "We are FLINGING!!!");
            scroller.forceFinished(true);
            scroller.fling(
                    positionX,
                    positionY,
                    (int)-velocityX,
                    (int)-velocityY,
                    contentRect.left, contentRect.right,
                    contentRect.top, contentRect.bottom,
                    0,
                    0);

            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);
            return true;
        }
    };

    private final ScaleGestureDetector.OnScaleGestureListener scaleListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();

            if (contentRect.contains((int)focusX, (int)focusY)) {
                viewportFocus.set(contentRect.left + contentRect.width() * (focusX - contentRect.left)/contentRect.width(),
                        contentRect.top + contentRect.height() * (focusY - contentRect.top)/contentRect.height());
            }

            scaleFactor *= scaleGestureDetector.getScaleFactor();

            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 3.0f));
            ViewCompat.postInvalidateOnAnimation(InteractiveImageView.this);

            return true;
        }
    };
}
