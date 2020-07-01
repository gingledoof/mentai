package com.e.myapplication

import RequestTools.SingletonManager
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.request.ImageRequest
import com.android.volley.request.JsonArrayRequest
import com.android.volley.request.JsonObjectRequest
import com.bumptech.glide.RequestManager
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

class Favorites : AppCompatActivity(){

    val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
    var RequestManager = SingletonManager.getInstance(this)
    val img_params = LinearLayout.LayoutParams(350, 350)
    lateinit var ids:MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        img_params.gravity = Gravity.CENTER_VERTICAL
        img_params.setMargins(10, 0, 10, 0)

        Log.e("Fav", "STARTED")
        VolleyLog.DEBUG = false

        setContentView(R.layout.favorites)

        ids = File(filesDir.absolutePath, "fav.txt").readLines().toMutableList()
        for (id in ids){
            Log.e("ID", id)
        }
        favRequest(gelbooru_api, ids, RequestManager)



        //Initialize with all tags

    }

    fun favRequest(api_url: String, ids: List<String>, RequestManager: SingletonManager) {

        var requestsCounter = AtomicInteger(ids.size)

        val intent = Intent(this@Favorites, PostActivity::class.java)

        var lin_params = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )

        val rows = findViewById<ViewGroup>(R.id.fav_Rows)
        var row = LinearLayout(this)
        row.layoutParams = lin_params
        row.gravity = Gravity.CENTER_HORIZONTAL
        rows.addView(LinearLayout(this))


        var posts = Stack<Post>()
        var imageViews = Stack<ImageView>()

        var jsonArray = JSONArray()
        RequestManager.requestQueue.addRequestFinishedListener<JsonArrayRequest> {request ->
            requestsCounter.decrementAndGet()

            if (requestsCounter.get() == 0){
                //All JSON requests complete
                setThumbnails(jsonArray, RequestManager)
            }
        }

        for (i in ids.indices) {
            val id = ids[i]

            //Create JSON url
            var reqParam = URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(id, "UTF-8")
            reqParam += "&" + URLEncoder.encode("json", "UTF-8") + "=" + URLEncoder.encode(
                "1",
                "UTF-8"
            )

            val mURL = api_url + reqParam

            val jsonObjectRequest = JsonArrayRequest(
                Request.Method.GET, mURL, null,
                Response.Listener { response ->
                    if (response.length() > 0) {
                        jsonArray.put(i, response[0] as JSONObject)
                    } else {
                        Log.e(TAG, "No Posts")
                    }
                },
                Response.ErrorListener { error ->
                    Log.e(TAG, "error")
                    jsonArray.put(i, null as JSONObject)
                    Log.e(TAG, error.message)
                    // TODO: Handle error
                }
            )
            RequestManager.addToRequestQueue(jsonObjectRequest)
        }

    }

    fun setThumbnails(jsonArray: JSONArray, RequestManager: SingletonManager) {

        val fav_dir = File(filesDir.absolutePath, "fav.txt")

        var lin_params = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )

        val intent = Intent(this@Favorites, PostActivity::class.java)

        val rows = findViewById<ViewGroup>(R.id.fav_Rows)
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

            var imageView = ImageView(this)
            imageView.layoutParams = img_params

            val json = jsonArray[i] as JSONObject
            var post = Post(json)
            thumbReq(post, contentResolver, imageView, RequestManager)
            json.put("thumnail_url", post.thumbnail_url)

            imageViews.add(imageView)

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
                v.setBackgroundColor(Color.RED)
                ids.remove(posts[i].id.toString())
                Log.e("wfdwf", "WRITTEN")

                true
            }
        }

    }

    override fun onDestroy() {
        var file = File(filesDir.absolutePath, "fav.txt")
        file.delete()
        file.createNewFile()
        for (id in ids){
            file.appendText(id + "\n")
        }

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

}