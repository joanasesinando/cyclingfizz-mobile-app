package pt.ulisboa.tecnico.cmov.cyclingfizz;

import android.util.Log;
import android.util.LruCache;

import java.util.Arrays;

public class Cache {
    private final static String TAG = "Cycling_Fizz@Cache";

    private static Cache INSTANCE = null;

    private final LruCache<String, byte[]> lruCache;

    private Cache() {
        int cacheSize = 10 * 1024 * 1024; // 10MiB

        lruCache = new LruCache<>(cacheSize);
    };

    public static Cache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Cache();
        }
        return(INSTANCE);
    }


    public void save(String key, byte[] bytes) {
        Log.d(TAG, "Saved " + key + " result in cache");
        Log.d(TAG, "Saved value -> " + Arrays.toString(bytes));
        lruCache.put(key, bytes);
    }

    public byte[] get(String key) {
        return lruCache.get(key);
    }

}
