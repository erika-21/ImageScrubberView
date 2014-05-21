package net.photonmed.imagescrubber.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import com.squareup.picasso.Picasso;
import net.photonmed.imagescrubber.app.utils.PLog;
import net.photonmed.imagescrubber.app.utils.SystemUiHider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Primary layout view for Scrubber
 */
public class ImageScrubberView extends FrameLayout implements SeekBar.OnSeekBarChangeListener {

    public static final String TAG = ImageScrubberView.class.getPackage() + " " + ImageScrubberView.class.getSimpleName();

    private ArrayList<String> imageUris;
    private ImageView imageView;
    private Activity activity;
    private SeekBar seekBar;
    private Context context;
    private int totalSize;
    private SystemUiHider systemUiHider;
    private int currentIndex;

    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    public void setImageUris(Collection<String> imageUris, int index) {
        this.imageUris = new ArrayList<String>(imageUris);
        this.totalSize = this.imageUris.size();
        this.seekBar.setMax(totalSize - 1);
        if (index < totalSize) {
            Picasso.with(this.context).load(this.imageUris.get(index)).into(imageView);
        } else if (totalSize > 0) {
            Picasso.with(this.context).load(this.imageUris.get(0)).into(imageView);
        } else {
            PLog.l(TAG, PLog.LogLevel.WARNING, "It appears the image uris are empty, scrubber is unstable.");
        }
    }

    public ImageScrubberView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        this.context = context;
        this.activity = (Activity)context;

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.main_scrubber, this, true);

        imageView = (ImageView)findViewById(R.id.imageView);
        seekBar = (SeekBar)findViewById(R.id.seekBar);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);

        systemUiHider = systemUiHider.getInstance(this.activity, imageView, HIDER_FLAGS);
        systemUiHider.setup();
        systemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {

                                                   // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });
        delayedHide(2000);

        this.seekBar.setOnSeekBarChangeListener(this);
    }

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            systemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (progress != currentIndex) {
                currentIndex = progress;
                Picasso.with(this.context).load(this.imageUris.get(progress)).into(this.imageView);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
