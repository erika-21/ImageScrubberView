package net.photonmed.imagescrubber.app;

import android.content.Context;
import android.widget.LinearLayout;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Primary layout view for Scrubber
 */
public class ImageScrubberView extends LinearLayout {

    public void setImageUris(LinkedHashSet<String> imageUris) {
        this.imageUris = imageUris;
    }

    private LinkedHashSet<String> imageUris;

    public ImageScrubberView(Context context) {
        super(context);
    }

    public ImageScrubberView(Context context, Collection<String> imageUris) {
        this(context);
        this.imageUris = new LinkedHashSet<String>(imageUris);

    }
}
