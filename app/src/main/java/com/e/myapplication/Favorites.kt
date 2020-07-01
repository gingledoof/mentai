package com.e.myapplication

import RequestTools.SingletonManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
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
import kotlin.collections.ArrayList

class Favorites : AppCompatActivity(){

    val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
    var RequestManager = SingletonManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("Fav", "STARTED")
        VolleyLog.DEBUG = false

        setContentView(R.layout.favorites)

        var ids = File(filesDir.absolutePath, "fav.txt").readLines()
        for (id in ids){
            Log.e("dwdw", id)
        }
        favRequest(gelbooru_api, ids, RequestManager)
        Log.e("asdasd", "done")



        //Initialize with all tags

    }

    fun setThumbnails(post: Post, RequestManager: SingletonManager, imageView: ImageView, post_num: Int) {

        val intent = Intent(this@Favorites, PostActivity::class.java)

        //Setting thumbnail url
        //Don't make fun of me for iterating this way. JSONArrays have no iterator

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


        imageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                Log.e(TAG, "CLICKED")
                intent.putExtra("init_post", post_num)
                startActivity(intent)
            }
        })

        RequestManager.addToRequestQueue(imagereq)


        //var args = Bundle()
        //args.putSerializable("posts", posts as Serializable)
        //intent.putExtra("BUNDLE", args)


    }

    fun favRequest(api_url: String, ids: List<String>, RequestManager: SingletonManager) {

        var lin_params = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )

        val img_params = LinearLayout.LayoutParams(
            350,
            350
        )
        img_params.gravity = Gravity.CENTER_VERTICAL
        img_params.setMargins(10, 0, 10, 0)

        val rows = findViewById<ViewGroup>(R.id.fav_Rows)
        var row = LinearLayout(this)
        row.layoutParams = lin_params
        row.gravity = Gravity.CENTER_HORIZONTAL
        rows.addView(LinearLayout(this))


        var posts = Stack<Post>()
        var imageViews = Stack<ImageView>()
        for (i in ids.indices) {
            val id = ids[i]
            var imageView = ImageView(this)
            imageView.layoutParams = img_params


            val images = row.childCount

            //Make new row
            if (images == 3){
                row = LinearLayout(this)
                row.layoutParams = lin_params
                row.gravity = Gravity.CENTER_HORIZONTAL
                rows.addView(row)
            }
            row.addView(imageView)

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
                        var post = Post(response[0] as JSONObject)
                        posts.add(post)
                        setThumbnails(post, RequestManager, imageView, i)
                    } else {
                        Log.e(TAG, "No Posts")
                    }
                },
                Response.ErrorListener { error ->
                    Log.e(TAG, "error")
                    posts.add(null)
                    Log.e(TAG, error.message)
                    // TODO: Handle error
                }
            )
            RequestManager.addToRequestQueue(jsonObjectRequest)
        }

        for (i in 0 until imageViews.size) {
            //Needs to be changed to lambda
            imageViews[i].setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    Log.e(TAG, "CLICKED")
                    intent.putExtra("init_post", i)
                    startActivity(intent)
                }
            })

        }

    }

}
