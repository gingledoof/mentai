package com.e.myapplication
import RequestTools.SingletonManager
import android.app.DownloadManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.net.Uri
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.android.volley.Response
import com.android.volley.request.DownloadRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.with
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList


class PostActivity : AppCompatActivity() {

    var RequestManager = SingletonManager.getInstance(this)

    val TAG = "Post View"
    private lateinit var mPager: ViewPager
    var posts = Stack<Post>()
    lateinit var downloadManager: DownloadManager

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var args = intent.getBundleExtra("BUNDLE")
        var posts = args.getSerializable("posts") as ArrayList<Post>
        val init_post = intent.getIntExtra("init_post", 0)
        val temp_path = applicationContext.cacheDir.absolutePath

        //CacheManager(File(temp_path), 100000000).execute()

        for (i in 0 until posts.size){
            var post = posts[i]
            var relativeLayout = RelativeLayout(this)
            relativeLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )

            post.context = this
            post.RequestMangager = RequestManager

            var progressBar = ProgressBar(this, null, 0, R.style.Widget_AppCompat_ProgressBar_Horizontal)
            post.EncapView = relativeLayout

            progressBar.max = 100
            progressBar.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT)
            progressBar.visibility = View.VISIBLE
            post.progressBar = progressBar

            val type = PostFileHandler.handler(post.image)

            if (type == PostFileHandler.IMAGE){
                post.view = ImageView(this)
                post.EncapView.addView(post.view)
                post.view.layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT)
                //Decode and set
                val path = temp_path + "/" + post.image
                post.local_file = path
                post.request = DownloadReq(post.file_url, path,
                    Response.Listener { result ->
                        //Returns path
                        File(result).setLastModified(System.currentTimeMillis())
                        post.progressBar.visibility = View.GONE
                        Log.e("DownloadFile", result)
                        var bitmap = BitmapFactory.decodeFile(result)
                        Log.e("REQUEST", bitmap.byteCount.toString())
                        (post.view as ImageView).setImageBitmap(bitmap)
                    },
                    post.progressBar
                )
            }

            else if(type == PostFileHandler.VIDEO){ initVideoStream(post) }

            else if(type == PostFileHandler.GIF) {
                post.view = ImageView(this)
                post.EncapView.addView(post.view)
                post.view.layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT)
                val path = temp_path + "/" + post.image
                post.local_file = path
                post.request = DownloadReq(post.file_url, path,
                    Response.Listener { result ->
                        post.local_file = result
                        File(result).setLastModified(System.currentTimeMillis())
                        Log.e("Request", "File is GIF")
                        post.progressBar.visibility = View.GONE
                        Glide.with(applicationContext)
                            .asGif()
                            .load(result)
                            .into(post.view as ImageView)
                    },
                post.progressBar
                )
            }

            else { Log.e("Assigning View Type", "ERROR") }

            post.EncapView.addView(progressBar)
            posts.add(post)
        }

        posts[init_post].init_post = true

        var pagerAdapter = Pager(posts, this)

        setContentView(R.layout.post_activity)

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager)
        mPager.offscreenPageLimit = 0
        // The pager adapter, which provides the pages to the view pager widget.
        mPager.adapter = pagerAdapter
        mPager.currentItem = init_post
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(this, "exoplayer-codelab")
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }

    fun initVideoStream(post: Post){
        post.view = PlayerView(this)
        post.view.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT)

        post.EncapView.addView(post.view)
        post.player = ExoPlayerFactory.newSimpleInstance(this)
        (post.view as PlayerView).player = post.player
        val uri = Uri.parse(post.file_url)
        post.mediaSource = buildMediaSource(uri)
    }

    override fun onDestroy() {
        mPager.removeAllViews()
        super.onDestroy()
    }

}

class CacheManager : AsyncTask<Int, Int, Int?>{
    val cacheDir:File
    val maxSize:Long

    constructor(cacheDir: File, maxSize: Long){
        this.cacheDir = cacheDir
        this.maxSize = maxSize
    }
    override fun doInBackground(vararg params: Int?): Int? {
        while(true){
            var size = getDirSize(cacheDir)
            if (size >= maxSize){
                //Get oldest files
                var files = cacheDir.listFiles()
                sortByDate(files)

                var i = 0
                while(size >= maxSize){
                    files[i].delete()
                    i+=1
                    size = getDirSize(cacheDir)
                }
                Log.e("CacheManager", "Deleted " + i.toString() + " Files")
            }
        }
    }

    fun getDirSize(dir: File): Long{
        var size:Long = 0
        for (file in cacheDir.listFiles()){
            size += file.length()
        }
        return size
    }

    fun sortByDate(files: Array<File>) {
        for (i in 1 until files.size){
            if (files[files.size-1-i].lastModified() > files[files.size-i].lastModified()){
                val temp = files[files.size-1-i]
                files[files.size-1-i] = files[files.size-i]
                files[files.size-i] = temp
            }
        }
    }

}


fun DownloadReq( url: String, path: String, listener: Response.Listener<String>, progressBar: ProgressBar) : DownloadRequest{
    var downloadRequest = DownloadRequest(url, path,
        listener,
        Response.ErrorListener { error ->
            Log.e("DownloadFile", "ERROR")
        })
    downloadRequest.setOnProgressListener { transferredBytes, totalSize ->
        val progress = ((transferredBytes * 100)/totalSize).toInt()
        progressBar.setProgress(progress)
    }
    return downloadRequest
}


