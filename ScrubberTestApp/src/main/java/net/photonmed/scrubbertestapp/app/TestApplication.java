package net.photonmed.scrubbertestapp.app;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.*;

/**
 * Created by erikbuttram on 5/20/14.
 */
public class TestApplication extends Application{

    public static final String TAG = TestApplication.class.getPackage() + " " + TestApplication.class.getSimpleName();

    private boolean saveFile(String newFilePath, InputStream rawFile) {
        try {

            OutputStream stream = new FileOutputStream(newFilePath);
            byte[] buffer = new byte[2048];
            int length;
            while ((length = rawFile.read()) > 0) {
                stream.write(buffer, 0, length);
            }

            stream.flush();
            stream.close();
            rawFile.close();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, String.format("wtf: %s", ex.getMessage()));
            return false;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        AssetManager assetManager = getAssets();
        File baseFilePath = getExternalCacheDir();
        try {
            String[] allFiles = getAssets().list("");
            for (String rawFile : allFiles) {
               if (rawFile.endsWith("jpeg") || rawFile.endsWith("jpg")) {
                   InputStream iStream = assetManager.open(rawFile);
                   Bitmap bmp = BitmapFactory.decodeStream(iStream);
                   int fileEnd = rawFile.indexOf(".");
                   String fileName = rawFile.substring(0, fileEnd);
                   String newFile = String.format("%s/%s.png", baseFilePath, fileName);
                   FileOutputStream out = new FileOutputStream(newFile);
                   bmp.compress(Bitmap.CompressFormat.PNG, 95, out);
                   out.flush();
                   out.close();
               }
            }
        } catch (IOException ioEx) {
            Log.e(TAG, String.format("Bad file saving stuff went down: %s", ioEx.getMessage()));
        }
    }
}
