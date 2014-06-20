
Image Scrubber View
======================

Provides a mechanism for scrubbing through an array of images via a seekbar.  Additionally,
provides a means to pan and zoom on the image itself.  Compatible with Android versions down to Gingerbread (API 9)

Download
======================
Currently the only way to get this to work is to pull the repo and reference it in your library,
either as a gradle dependency or a module dependency.

Usage
=====================

Currently, you may create the view as usual:

```
<net.photonmed.imagescrubber.app.ImageScrubberView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/daScrubber" />
 ```  
 
 This library uses the Picasso library, but at the moment in only can load images via a collection of 
 string.  The strings get resolved via a URI Builder, so they can be local or remote URIs.
 
 Once you have those, simply call setImageUris(files) and the awesomeness will commence, for example
 
 ```java
imageScrubber.setImageUris(myFileCollection); 
```

The test app included in the source shows how to implement this has a good example of this.

Known problems
===================

* There some weirdness when double tapping to zoom back out in the right hand corner.

Feel free to submit issues with anything else you see.
