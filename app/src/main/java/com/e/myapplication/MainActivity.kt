package com.e.myapplication

import RequestTools.SingletonManager
import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.request.DownloadRequest
import com.android.volley.request.ImageRequest
import com.android.volley.request.JsonArrayRequest
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.Serializable
import java.net.URLEncoder
import java.util.*

class MainActivity : AppCompatActivity() {

    val TAG = "RUNNING"

    val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
    var RequestManager = SingletonManager.getInstance(this)
    lateinit var ids:MutableList<String>
    var paused = false
    lateinit var fav_dir:File
    private lateinit var mPager: ViewPager
    lateinit var downloadManager: DownloadManager

    val lin_params = RelativeLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        400
    )

    val img_params = LinearLayout.LayoutParams(
        350,
        350
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        img_params.gravity = Gravity.CENTER_VERTICAL
        img_params.setMargins(10, 0, 10, 0)

        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        VolleyLog.DEBUG = false
        //Request Permissions
        req()
        Log.e("ONCREATE", "HERE")

        fav_dir = File(filesDir.absolutePath, "fav.txt")
        if (!fav_dir.exists()) {fav_dir.createNewFile()}

        ids = fav_dir.readLines().toMutableList()
        for (id in ids){
            Log.e("ID", id)
        }

        setContentView(R.layout.activity_main)


        //Initialize with all tags
        pageRequest(gelbooru_api, "", RequestManager)

    }

    //Requests permissions
    fun req() {
        var det = (
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                )
        if(det)
        {
            //Not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 101)
            while (det) { }
        }

    }


    //Needs to create custom thumbnail object instead of JSON
    fun setThumbnails(jsonArray: JSONArray, RequestManager: SingletonManager) {

        val rows = findViewById<ViewGroup>(R.id.Rows)
        var row = LinearLayout(this)
        row.layoutParams = lin_params
        row.gravity = Gravity.CENTER_HORIZONTAL
        rows.addView(LinearLayout(this))


        var posts = Stack<Post>()
        var imageViews = Stack<ImageView>()
        //Setting thumbnail url
        //Don't make fun of me for iterating this way. JSONArrays have no iterator
        for (i in 0 until jsonArray.length()) {

            var imageView = ImageView(this)
            imageView.layoutParams = img_params

            val json = jsonArray[i] as JSONObject
            var post = Post(json)
            post.thumbnailView = imageView
            thumbReq(post, contentResolver, imageView, RequestManager)


            if(ids.contains(post.id.toString())){
                imageView.setBackgroundResource(R.drawable.image_border)
                post.favorite = true
            }

            imageViews.add(post.thumbnailView)

            val images = row.childCount

            //Make new row
            if (images == 3){
                row = LinearLayout(this)
                row.layoutParams = lin_params
                row.gravity = Gravity.CENTER_HORIZONTAL
                rows.addView(row)
            }

            row.addView(imageView)
            posts.add(post)

        }


        for (i in 0 until posts.size) {
            //Needs to be changed to lambda
            posts[i].thumbnailView.setOnClickListener { v ->
                Log.e(TAG, "CLICKED")
                init(posts, i)
            }

            posts[i].thumbnailView.setOnLongClickListener { v: View -> Unit

                if(posts[i].favorite){
                    //Remove favorite
                    ids.remove(posts[i].id.toString())
                    posts[i].favorite = false
                    //Remove border
                    v.setBackgroundResource(0)
                }
                else {
                    posts[i].favorite = true
                    ids.add(posts[i].id.toString())
                    v.setBackgroundResource(R.drawable.image_border)
                    Log.e("wfdwf", "WRITTEN")
                }
                true
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        var menuItem = menu.findItem(R.id.search_bar)
        var nav_bar = menu.findItem(R.id.favorites)
        var searchView = menuItem.actionView as SearchView

        searchView.queryHint = "Search for tags"

        nav_bar.setOnMenuItemClickListener { item: MenuItem -> Unit
            rewrite()
            val fav_intent = Intent(this, Favorites::class.java)
            fav_intent.putExtra("fav", File(filesDir.absolutePath, "fav.txt").absolutePath)
            startActivityForResult(fav_intent, 1)

            false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val rel_layout = findViewById<ViewGroup>(R.id.Rows)
                rel_layout.removeAllViews()

                pageRequest(gelbooru_api, query.toString(), RequestManager)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        return true
    }

    fun updateFavs(){
        ids = fav_dir.readLines().toMutableList()
        for (id in ids){
            Log.e("ID", id)
        }
    }

    fun rewrite(){
        fav_dir.delete()
        fav_dir.createNewFile()
        for (id in ids){
            fav_dir.appendText(id + "\n")
        }
    }


    override fun onResume() {
        Log.e("RESUMED", "HERE")
        super.onResume()
    }


    override fun onPause() {
        paused = true
        super.onPause()
    }

    //Gets thumbnail urls, sets thumbnails for first MaxImagesPerPage number of images
    fun pageRequest(api_url: String, tags: String, RequestManager: SingletonManager){

        var reqParam = URLEncoder.encode("tags", "UTF-8") + "=" + URLEncoder.encode(tags, "UTF-8")
        reqParam += "&" + URLEncoder.encode("json", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")

        val mURL = api_url + reqParam

        val jsonObjectRequest = JsonArrayRequest(Request.Method.GET, mURL, null,
            Response.Listener { response ->
                if (response.length() > 0){
                    setThumbnails(response, RequestManager)
                }
                else{
                    Log.e(com.e.myapplication.TAG, "No Posts")
                }
            },
            Response.ErrorListener { error ->
                Log.e(com.e.myapplication.TAG, "error")
                Log.e(com.e.myapplication.TAG, error.message)
                // TODO: Handle error
            }
        )
        RequestManager.addToRequestQueue(jsonObjectRequest)
    }

    override fun onDestroy() {
        cacheDir.delete()
        super.onDestroy()
    }

    fun thumbReq(post: Post, contentResolver: ContentResolver, imageView: ImageView, RequestManager: SingletonManager){
        val imagereq = ImageRequest(
            post.thumbnail_url,
            null,
            contentResolver,
            Response.Listener { result ->
                //Set Thumbnails to ImageView
                imageView.setImageBitmap(result)
            },
            1000,
            1000,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            Response.ErrorListener { error ->
                Log.e("IMAGEREQ", "ERROR")
            }
        )

        RequestManager.addToRequestQueue(imagereq)
    }

    override fun onBackPressed() {
        setContentView(R.layout.activity_main)
        super.onBackPressed()
    }

    fun createProgressBar(context: Context): ProgressBar{
        var progressBar = ProgressBar(this, null, 0, R.style.Widget_AppCompat_ProgressBar_Horizontal)
        progressBar.max = 100
        progressBar.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT)
        progressBar.visibility = View.VISIBLE
        return progressBar
    }

    fun init(posts : Stack<Post>, init_post: Int){
        val temp_path = cacheDir.absolutePath

        for (i in 0 until posts.size){
            var post = posts[i]
            var relativeLayout = RelativeLayout(this)
            relativeLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )

            post.context = this
            post.RequestMangager = RequestManager

            post.EncapView = relativeLayout

            post.progressBar = createProgressBar(this)

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

            post.EncapView.addView(post.progressBar)
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
}

fun DownloadReq( url: String, path: String, listener: Response.Listener<String>, progressBar: ProgressBar) : DownloadRequest {
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