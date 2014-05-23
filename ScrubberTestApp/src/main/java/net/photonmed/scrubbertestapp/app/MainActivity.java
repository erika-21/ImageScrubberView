package net.photonmed.scrubbertestapp.app;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import net.photonmed.imagescrubber.app.ImageScrubberView;
import net.photonmed.imagescrubber.app.utils.SystemUiHider;

import java.io.File;
import java.util.LinkedHashSet;


public class MainActivity extends ActionBarActivity {

    public static final String TAG = MainActivity.class.getPackage() + " " + MainActivity.class.getSimpleName();

    ImageScrubberView scrubberView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrubberView = (ImageScrubberView)findViewById(R.id.daScrub);

        LinkedHashSet<String> files = new LinkedHashSet<String>();
        File basePath = getExternalCacheDir();
        String[] allFiles = basePath.list();
        for (String file : allFiles) {
            if (file.endsWith(".png")) {
                files.add(String.format("file://%s/%s", basePath.getAbsolutePath(), file));
            }
        }

        scrubberView.setImageUris(files, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
