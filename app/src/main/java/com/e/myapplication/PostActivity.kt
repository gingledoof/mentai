package com.e.myapplication
import RequestTools.SingletonManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.telephony.mbms.DownloadRequest
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import kotlinx.android.synthetic.main.post_activity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class PostActivity : AppCompatActivity() {

    var RequestManager = SingletonManager.getInstance(this)

    val TAG = "Post View"
    private lateinit var mPager: ViewPager
    var posts = Stack<Post>()
    //var downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var jsonArray = JSONArray(intent.getStringExtra("jsonArray"))
        val init_post = intent.getIntExtra("init_post", 0)
        val temp_path = File(intent.getStringExtra("temp_path"))
        val file_names = intent.getSerializableExtra("file_names") as ArrayList<String>
        val file_urls = intent.getSerializableExtra("file_urls") as ArrayList<String>

        for (i in 0 until file_urls.size){

            val file_url = file_urls[i]
            var view = ImageView(this)

            val imagereq = ImageRequest(
                file_url,
                Response.Listener<Bitmap>{ result ->
                    Log.e(TAG, "SUCCESS")
                    view.setImageBitmap(result)
                },
                1000,
                1000,
                ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.RGB_565,
                Response.ErrorListener { error ->
                    Log.e(TAG, "ERROR")
                }
            )

            /*val file = File(temp_path, file_names[i])
            val d = DownloadManager.Request(Uri.parse(file_url))
                .setDestinationUri(Uri.fromFile(temp_path))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)

            val d_id = downloadManager.enqueue(d)

            */



            posts.add(Post(view, imagereq, RequestManager))
        }

        posts[init_post].init_post = true

        var pagerAdapter = Pager(posts, this)

        setContentView(R.layout.post_activity)

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager)

        // The pager adapter, which provides the pages to the view pager widget.
        mPager.adapter = pagerAdapter
        mPager.currentItem = init_post
    }

}

class Post {
    lateinit var view: View
    lateinit var request: ImageRequest
    lateinit var reqManager: SingletonManager
    var init_post = false
    var loaded = false
    constructor(imageView: View, request: ImageRequest, reqManager: SingletonManager){
        this.view = imageView
        this.request = request
        this.reqManager = reqManager
    }
    fun load(){
        reqManager.addToRequestQueue(request)
        loaded = true
    }
}