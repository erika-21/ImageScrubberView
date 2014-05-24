package net.photonmed.imagescrubber.app.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.widget.OverScroller;

public class OverScrollerCompat {

    private OverScrollerCompat() {

    }

    /**
     * @see android.view.ScaleGestureDetector#getCurrentSpanY()
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static float getCurrVelocity(OverScroller overScroller) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return overScroller.getCurrVelocity();
        } else {
            return 0;
        }
    }
}
