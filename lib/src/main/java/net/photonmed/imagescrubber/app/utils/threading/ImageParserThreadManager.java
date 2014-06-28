package net.photonmed.imagescrubber.app.utils.threading;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import com.squareup.okhttp.OkHttpClient;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Thread manager service that will manage concurrent image parser thread manager
 */
public class ImageParserThreadManager {

    public static final int MAX_MEMORY = (int)(Runtime.getRuntime().maxMemory() / 1024); //store in kb
    public final int cacheSize = ImageParserThreadManager.MAX_MEMORY/ 2;
    public static final String IMAGE_PARSING_COMPLETE = "net.photonmed.android.imageparser";
    public static final String IMAGE_PARSED = "net.photonmed.android.imageparsed";
    private static final int CONCURRENT_MAX = 4;
    private static ImageParserThreadManager manager;

    private List<ImageParserRunnable> runningParsers;
    private List<ImageParserRunnable> parsersToRun;
    private Map<Integer, byte[]> bitmapMap;
    private ThreadPoolExecutor executor;
    private Context context;
    private OkHttpClient client;

    public OkHttpClient getClient() {
        return client;
    }

    public HashMap<Integer, byte[]> getBitmapBytes() {
        HashMap<Integer, byte[]> copyOf =  new HashMap<Integer, byte[]>(bitmapMap);
        bitmapMap.clear();
        return copyOf;
    }

    public void emptyBitmapBytes() {
        bitmapMap.clear();
    }

    private ImageParserThreadManager(Context context) {
        this.runningParsers = Collections.synchronizedList(new ArrayList<ImageParserRunnable>());
        this.parsersToRun = Collections.synchronizedList(new ArrayList<ImageParserRunnable>());
        this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        this.context = context;
        this.bitmapMap = Collections.synchronizedMap(new HashMap<Integer, byte[]>());
        this.client = new OkHttpClient();
    }

    public void addTasksToRun(List<String> tasks) {
        //TODO:  Need to optimize this loop, seems micro, but we're dealing with a startup routine with a large collection
        int i = 0;
        for (String task : tasks) {
            ImageParserRunnable runnable = new ImageParserRunnable(task, i);
            i++;
            parsersToRun.add(runnable);
        }
    }

    public void removeTaskToRun(ImageParserRunnable task) {
        parsersToRun.remove(task);
    }

    public void removeRunningTask(ImageParserRunnable task, boolean success, byte[] bitmap) {
        runningParsers.remove(task);
        if (success) {
            removeTaskToRun(task);
            bitmapMap.put(task.getIndexNumber(), bitmap);
            Intent intent = new Intent(IMAGE_PARSED);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
        if (runningParsers.size() == 0) {
            executeTasks();
        }
    }

    public void executeTasks() {

        if (parsersToRun.size() == 0) {
            Intent intent = new Intent(IMAGE_PARSING_COMPLETE);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            return;
        }
        for (int i = 0; (i <= ImageParserThreadManager.CONCURRENT_MAX || i > parsersToRun.size()); i++) {
            ImageParserRunnable runnable = parsersToRun.get(i);
            runningParsers.add(runnable);
            try {
                executor.execute(runnable);
            } catch (RejectedExecutionException ex) {
                removeRunningTask(runnable, false, null);
            }
        }
    }

    public static ImageParserThreadManager getInstance(Context context) {
        if (manager == null) {
            manager = new ImageParserThreadManager(context.getApplicationContext());
        }
        return manager;
    }

}
