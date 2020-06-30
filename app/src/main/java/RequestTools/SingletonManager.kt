package RequestTools

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageCache
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley

class SingletonManager constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: SingletonManager? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SingletonManager(context).also {
                    INSTANCE = it
                }
            }
    }
    val imageLoader: ImageLoader by lazy {
        ImageLoader(requestQueue,
            object : ImageCache {
                private val cache = LruCache<String, Bitmap>(20)
                override fun clear() {
                    TODO("Not yet implemented")
                }

                override fun getBitmap(url: String): Bitmap {
                    return cache.get(url)
                }
                override fun putBitmap(url: String, bitmap: Bitmap) {
                    cache.put(url, bitmap)
                }

                override fun invalidateBitmap(url: String?) {
                    TODO("Not yet implemented")
                }
            })
    }
    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    fun <T> addToRequestQueue(vararg req: Request<T>) {
        for (r in req) {
            requestQueue.add(r)
        }
    }
}