package com.e.myapplication
import RequestTools.SingletonManager
import android.app.DownloadManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
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
import java.util.*


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
        var jsonArray = JSONArray(intent.getStringExtra("jsonArray"))
        val init_post = intent.getIntExtra("init_post", 0)
        val temp_path = applicationContext.cacheDir.absolutePath

        //CacheManager(File(temp_path), 100000000).execute()

        for (i in 0 until jsonArray.length()){

            var relativeLayout = RelativeLayout(this)
            relativeLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )

            var post = Post(jsonArray[i] as JSONObject)
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

class Post(jsonObject: JSONObject) {
    val source: String
    val directory: String
    val hash: String
    val height: Int
    val id: Int
    val image: String
    val change: Int
    val owner: String
    val rating: String
    val score: Int
    val tags: List<String>
    val width: Int
    val file_url: String
    val created_at: String
    lateinit var view: View
    var local_file: String = ""
    lateinit var request: DownloadRequest
    var init_post = false
    var loaded = false
    lateinit var thumbnail_url: String
    lateinit var RequestMangager: SingletonManager
    lateinit var listener: Response.Listener<Any>
    lateinit var player: SimpleExoPlayer
    lateinit var mediaSource: MediaSource
    lateinit var context: Context
    lateinit var EncapView: ViewGroup
    lateinit var progressBar: ProgressBar

    init {
        this.source = jsonObject["source"] as String
        this.directory = jsonObject["directory"] as String
        this.hash = jsonObject["hash"] as String
        this.height = jsonObject["height"] as Int
        this.id = jsonObject["id"] as Int
        this.image = jsonObject["image"] as String
        this.change =  jsonObject["change"] as Int
        this.owner = jsonObject["owner"] as String
        this.rating = jsonObject["rating"] as String
        this.score = jsonObject["score"] as Int
        this.tags = (jsonObject["tags"] as String).split(" ")
        this.width = jsonObject["width"] as Int
        this.file_url = jsonObject["file_url"] as String
        this.created_at = jsonObject["created_at"] as String
        getThumbnailUrl(jsonObject)
    }

    fun getThumbnailUrl(json: JSONObject){
        val file = json["image"] as String
        val thumbnail = json["file_url"].toString()
            .replace("images", "thumbnails")
            .replace(file, "thumbnail_" + file)
            .replace("img2", "img1")
            .replaceAfterLast(".", "jpg")
        this.thumbnail_url = thumbnail
    }

    fun load(){
        progressBar.visibility = View.GONE

        if (PostFileHandler.handler(image) == PostFileHandler.VIDEO){
            player.playWhenReady = false
            player.seekTo(0, 0)
            player.prepare(mediaSource, false, false)
        }
        else if (PostFileHandler.handler(image) == PostFileHandler.IMAGE){
            if (File(local_file).exists()){
                (view as ImageView).setImageBitmap(BitmapFactory.decodeFile(local_file))
            }
            else {
                RequestMangager.addToRequestQueue(request)
                progressBar.visibility = View.VISIBLE
                progressBar.bringToFront()
            }
            return
        }
        else if (PostFileHandler.handler(image) == PostFileHandler.GIF){
            if (File(local_file).exists()){
                Glide.with(context).load(local_file).into(view as ImageView)
            }
            else {
                RequestMangager.addToRequestQueue(request)
                progressBar.visibility = View.VISIBLE
                progressBar.bringToFront()
            }
            return
        }

    }

}

public object PostFileHandler {
    public val IMAGE = 0
    public val VIDEO = 1
    public val GIF = 2

    private val imageFileExtensions = arrayOf(
        "jpg",
        "png",
        "jpeg"
    )

    private val videoFileExtensions = arrayOf(
        "mp4",
        "webm"
    )


    fun handler(filename: String): Int {
        for (extension in imageFileExtensions) {
            if (filename.toLowerCase().endsWith(extension)) {
                return IMAGE
            }
        }
        for (extension in videoFileExtensions) {
            if (filename.toLowerCase().endsWith(extension)) {
                return VIDEO
            }
        }
        if(filename.toLowerCase().endsWith("gif")){
            return GIF
        }

        return -1
    }

}

/*
class FileGet : AsyncTask<String, Int, String> {
    var downloadManager:DownloadManager
    var context: Context
    var post: Post
    val temp_path = Environment.getDataDirectory().absolutePath + "/MENTAI/temp"

    constructor(context: Context , downloadManager: DownloadManager, post: Post){
        this.downloadManager = downloadManager
        this.context = context
        this.post = post
    }

    var progress = 0

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
    }

    override fun onPostExecute(result: String) {
        post.loaded = true
        Log.e("DOWNLOADING", "SUCCESS!")
        Log.e("DONE", result)
        var bit = BitmapFactory.decodeStream(context.openFileInput("files/h.jpg"))
    }


    override fun doInBackground(vararg params: String?): String? {

        val req = DownloadManager.Request(Uri.parse(post.file_url))
            //.setDestinationInExternalFilesDir(context, "cache", post.image)
            .setDestinationInExternalFilesDir(context , "", "h.jpg")
            .setAllowedOverMetered(true)

        val downloadID = downloadManager.enqueue(req)
        var path=""
        var finishDownload = false
        while (!finishDownload) {
            val cursor: Cursor =
                downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
            if (cursor.moveToFirst()) {
                val status: Int =
                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                when (status) {
                    DownloadManager.STATUS_FAILED -> {
                        finishDownload = true
                    }
                    DownloadManager.STATUS_PAUSED -> {
                    }
                    DownloadManager.STATUS_PENDING -> {
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        val total: Long =
                            cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total >= 0) {
                            val downloaded: Long = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            progress = (downloaded * 100L / total).toInt()
                            publishProgress(progress)
                        }
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        progress = 100
                        publishProgress(100)
                        finishDownload = true
                        Log.e("DOWNLOAD", "Getting path...")
                        val path = File(DIRECTORY_DOWNLOADS, post.image).absolutePath
                        Log.e("Download: ", "COMPLETE: " + path)
                        post.loaded = true
                    }
                }
            }
        }
        return path
    }
}
*/

