package net.photonmed.imagescrubber.app.utils.threading;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 *
 */
public class ImageParserRunnable implements Runnable{

    private String imageUri;
    //The index which this image belongs to wrt to the scrubber

    public int getIndexNumber() {
        return indexNumber;
    }

    int indexNumber = 0;

    public ImageParserRunnable(String uri, int index) {
        imageUri = uri;
        indexNumber = index;
    }

    @Override
    public void run() {
        try {
            Request request = new Request.Builder().url(imageUri).build();
            ImageParserThreadManager manager = ImageParserThreadManager.getInstance(null);
            Response response = manager.getClient().newCall(request).execute();
            byte[] byteArray = response.body().bytes();
            manager.removeRunningTask(this, true, byteArray);
        } catch (Exception ex) {
            ImageParserThreadManager.getInstance(null).removeRunningTask(this, false, null);
        }

    }
}
