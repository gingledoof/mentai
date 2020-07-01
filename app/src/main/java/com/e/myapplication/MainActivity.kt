package com.e.myapplication

import RequestTools.SingletonManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.request.ImageRequest
import com.android.volley.request.JsonArrayRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.URLEncoder
import java.util.*

class MainActivity : AppCompatActivity() {

    val TAG = "RUNNING"

    val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
    var RequestManager = SingletonManager.getInstance(this)
    //val temp_path = Environment.getDataDirectory().absolutePath + "/MENTAI/temp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VolleyLog.DEBUG = false
        //Request Permissions
        req()

        val fav_dir = File(filesDir.absolutePath, "fav.txt")

        if (!fav_dir.exists()) {fav_dir.createNewFile()}

        setContentView(R.layout.activity_main)


        //Initialize with all tags
        pageRequest(gelbooru_api, "", RequestManager)

    }

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

        val fav_dir = File(filesDir.absolutePath, "fav.txt")

        var lin_params = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )

        val intent = Intent(this@MainActivity, PostActivity::class.java)
        val fav_intent = Intent(this@MainActivity, Favorites::class.java)

        val rows = findViewById<ViewGroup>(R.id.Rows)
        var row = LinearLayout(this)
        row.layoutParams = lin_params
        row.gravity = Gravity.CENTER_HORIZONTAL
        rows.addView(LinearLayout(this))

        val img_params = LinearLayout.LayoutParams(
            350,
            350
        )
        img_params.gravity = Gravity.CENTER_VERTICAL
        img_params.setMargins(10, 0, 10, 0)


        var posts = Stack<Post>()
        var imageViews = Stack<ImageView>()
        //Setting thumbnail url
        //Don't make fun of me for iterating this way. JSONArrays have no iterator
        for (i in 0 until jsonArray.length()) {

            val json = jsonArray[i] as JSONObject
            var post = Post(json)
            json.put("thumnail_url", post.thumbnail_url)

            var imageView = ImageView(this)
            imageView.layoutParams = img_params

            imageViews.add(imageView)

            //Thumbnail request
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
                    Log.e(com.e.myapplication.TAG, "ERROR")
                }
            )

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

            RequestManager.addToRequestQueue(imagereq)

        }

        var args = Bundle()
        args.putSerializable("posts", posts as Serializable)
        intent.putExtra("BUNDLE", args)


        for (i in 0 until imageViews.size) {
            //Needs to be changed to lambda
            imageViews[i].setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    Log.e(TAG, "CLICKED")
                    intent.putExtra("init_post", i)
                    startActivity(intent)
                }
            })

            imageViews[i].setOnLongClickListener { v: View -> Unit
                v.setPadding(1,1,1,1)
                v.setBackgroundColor(Color.CYAN)
                Log.e("wfdwf", "WRITTEN")
                fav_dir.appendText(posts[i].id.toString() + "\n")
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
            val fav_intent = Intent(this@MainActivity, Favorites::class.java)
            startActivity(fav_intent)
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

}
