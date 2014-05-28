package net.photonmed.imagescrubber.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.util.AttributeSet;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import com.squareup.picasso.Picasso;
import net.photonmed.imagescrubber.app.utils.InteractiveImageView;
import net.photonmed.imagescrubber.app.utils.PLog;
import net.photonmed.imagescrubber.app.utils.SystemUiHider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Primary layout view for Scrubber
 */
public class ImageScrubberView extends FrameLayout {

    public static final String TAG = ImageScrubberView.class.getPackage() + " " + ImageScrubberView.class.getSimpleName();

    private ArrayList<String> imageUris;
    private InteractiveImageView imageView;
    private Activity activity;
    private SeekBar seekBar;
    private Context context;
    private int totalSize;
    private SystemUiHider systemUiHider;
    private int currentIndex = 0;

    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    public void setImageUris(Collection<String> imageUris, int index) {
        this.imageUris = new ArrayList<String>(imageUris);
        this.totalSize = this.imageUris.size();
        this.seekBar.setMax(totalSize - 1);
        if (index < totalSize) {
            currentIndex = index;
            Picasso.with(this.context).load(this.imageUris.get(index)).into(imageView);
        } else if (totalSize > 0) {
            Picasso.with(this.context).load(this.imageUris.get(0)).into(imageView);
        } else {
            PLog.l(TAG, PLog.LogLevel.WARNING, "It appears the image uris are empty, scrubber is unstable.");
        }
    }

    public void setImageUris(Collection<String> imageUris) {
        setImageUris(imageUris, currentIndex);
    }

    public ImageScrubberView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        this.context = context;
        this.activity = (Activity) context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.main_scrubber, this, true);

        imageView = (InteractiveImageView) findViewById(R.id.imageView);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

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

        this.seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        this.seekBar.setOnTouchListener(delayHideTouchListener);
    }

    Handler hideHandler = new Handler();
    Runnable hideRunnable = new Runnable() {
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
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, delayMillis);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener delayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
            return false;
        }
    };

    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && progress != currentIndex) {
                currentIndex = progress;
                Picasso.with(context).load(imageUris.get(progress)).into(imageView);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            //UNUSED
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //UNUSED
        }

    };

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentIndex = currentIndex;
        savedState.imageStrings = imageUris;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState)state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentIndex = savedState.currentIndex;
        ArrayList<String> rawStrings = savedState.imageStrings;
        imageUris = new ArrayList<String>(rawStrings);
        Picasso.with(this.context).load(this.imageUris.get(currentIndex)).into(imageView);
    }

    public static class SavedState extends BaseSavedState {

        private Integer currentIndex;
        private ArrayList<String> imageStrings;

        public SavedState(Parcelable superState) {
            super(superState);
            imageStrings = new ArrayList<String>();
        }

        SavedState(Parcel in) {
            super(in);
            currentIndex = in.readInt();
            in.readStringList(imageStrings);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentIndex);
            dest.writeStringList(imageStrings);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
            = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        });



    }



}
