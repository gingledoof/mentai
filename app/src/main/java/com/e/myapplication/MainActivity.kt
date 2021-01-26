package com.e.myapplication

import RequestTools.SingletonManager
import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.*
import android.widget.*
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
import java.io.Serializable
import java.net.URLEncoder
import java.util.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.children

import androidx.core.view.get
import androidx.core.view.size
import androidx.core.widget.addTextChangedListener
import com.diegodobelo.expandingview.ExpandingList
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {

    val TAG = "RUNNING"

    val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
    val gelbooru_tag_api = "https://gelbooru.com/index.php?page=dapi&s=tag&q=index&"

    val test_tags = arrayOf("fate_(series)")
    val SFW = "rating:safe -loli -large_breasts -panties -girlfriend_(kari) -cleavage -ass -midriff -swimsuit -nude -spread_legs -bare_legs -flat_chest -bunny_ears"

    var RequestManager = SingletonManager.getInstance(this)
    lateinit var searchView:ArrayAdapterSearchView
    lateinit var ids:MutableList<String>
    var paused = false
    lateinit var fav_dir:File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.inflateMenu(R.menu.options_menu)

        //Initialize with all safe tags
        pageRequest(gelbooru_api, SFW, RequestManager, 0)
    }

    fun tagRequest(api_url: String, RequestManager: SingletonManager, pattern: String, page: Int){
        var reqParam = URLEncoder.encode("json", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")
        reqParam += "&" + URLEncoder.encode("name_pattern", "UTF-8") + "=" + URLEncoder.encode("$pattern%", "UTF-8")
        reqParam += "&" + URLEncoder.encode("order", "UTF-8") + "=" + URLEncoder.encode("DESC", "UTF-8")
        reqParam += "&" + URLEncoder.encode("orderby", "UTF-8") + "=" + URLEncoder.encode("count", "UTF-8")

        val mURL = api_url + reqParam

        val jsonObjectRequest = JsonArrayRequest(Request.Method.GET, mURL, null,
            { response ->
                if (response.length() > 0){

                    var tags = Array(response.length(), {
                            i -> (response.get(i) as JSONObject).get("tag") as String
                    })


                    val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this,
                        android.R.layout.simple_dropdown_item_1line, tags)

                    //val adapter = DelimiterAdapter(this, android.R.layout.simple_dropdown_item_1line, tags)
                    searchView.setAdapter(adapter)
                }
                else{
                    Log.e(com.e.myapplication.TAG, "No Tags")
                }
            },
            { error ->
                Log.e(com.e.myapplication.TAG, "error")
                Log.e(com.e.myapplication.TAG, error.message)
                // TODO: Handle error
            }
        )
        RequestManager.addToRequestQueue(jsonObjectRequest)
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
    fun setThumbnails(jsonArray: JSONArray, RequestManager: SingletonManager, tags: String, page: Int) {

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

            var imageView = ImageView(this)
            imageView.layoutParams = img_params

            val json = jsonArray[i] as JSONObject
            var post = Post(json)
            thumbReq(post, contentResolver, imageView, RequestManager)
            json.put("thumnail_url", post.thumbnail_url)

            if(ids.contains(post.id.toString())){
                imageView.setBackgroundResource(R.drawable.image_border)
                post.favorite = true
            }

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


        val button_row = LinearLayout(this)
        button_row.layoutParams = lin_params
        button_row.gravity = Gravity.CENTER_HORIZONTAL
        rows.addView(button_row)

        if (page != 0) {
            //add previous button
            val prev = Button(this)
            prev.text = "Previous"

            prev.setOnClickListener {
                rows.removeAllViews()
                pageRequest(gelbooru_api, tags, RequestManager, (page-1) ) }

            button_row.addView(prev)
        }

        if (imageViews.size == 100){
            //Adding next button
            val next = Button(this)
            next.text = "Next"
            next.setOnClickListener {
                rows.removeAllViews()
                pageRequest(gelbooru_api, tags, RequestManager, (page+1) ) }

            button_row.addView(next)

        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === 1) {
            if (resultCode === Activity.RESULT_OK) {
                ids = (data!!.getStringArrayExtra("ids")).toMutableList()
                for (id in ids){ Log.e("IDS", id)}
                rewrite()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        var scroll_bar = menu.findItem(R.id.test)
        var menuItem = menu.findItem(R.id.me)
        var nav_bar = menu.findItem(R.id.favorites)
        //var searchView = menuItem.actionView as ArrayAdapterSearchView

        var s = scroll_bar.actionView as HorizontalScrollView


        val mst_lay = RelativeLayout(this)
        mst_lay.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mst_lay.gravity = Gravity.CENTER

        val Lin = LinearLayout(this)
        Lin.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        Lin.gravity = Gravity.CENTER

        val se_Lin = LinearLayout(this)
        se_Lin.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        mst_lay.addView(se_Lin)
        mst_lay.addView(Lin)

        searchView = ArrayAdapterSearchView(this)

        Lin.addView(searchView)

        s.addView(mst_lay)

        val tagView = findViewById<LinearLayout>(R.id.tag_scroll)

        //val adapter = DelimiterAdapter(this, android.R.layout.simple_dropdown_item_1line, test_tags)
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this,
            android.R.layout.simple_dropdown_item_1line, test_tags)

        searchView.setAdapter(adapter)

        searchView.setOnItemClickListener { parent, view, position, id ->
            //Maybe Create custom view adder thing
            val textView = TagView(this, getDrawable(R.drawable.mentai_ic))
            val text = (view as TextView).text.toString()
            textView.setText(text)

            val img_params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            img_params.setMargins(10, 0, 10, 0)

            textView.layoutParams = img_params

            textView.setOnClickListener { v: View? -> tagView.removeView(v) }
            tagView.addView(textView)

            searchView.setText("")

            //var t = ImageView(this)
            //t.setImageDrawable(getDrawable(R.drawable.mentai_ic))

        }

        /*
        searchView.addTextChangedListener {
            val text = searchView.text.toString()

            if (text != null) {
                tagRequest(gelbooru_tag_api, RequestManager, text, 0)
            }
        }

        searchView.setTokenizer(SpaceTokenizer())
         */

        nav_bar.setOnMenuItemClickListener { item: MenuItem -> Unit
            rewrite()
            val fav_intent = Intent(this, Favorites::class.java)
            fav_intent.putExtra("fav", File(filesDir.absolutePath, "fav.txt").absolutePath)
            startActivityForResult(fav_intent, 1)

            false
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                var query = ""

                for (textView in tagView.children){
                    val text = (textView as TagView).getText()
                    query = "$query $text"
                }
                Log.e("QUERY", query)

                val rel_layout = findViewById<ViewGroup>(R.id.Rows)
                rel_layout.removeAllViews()
                pageRequest(gelbooru_api, query, RequestManager, 0)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                if (newText != null) {
                    tagRequest(gelbooru_tag_api, RequestManager, newText, 0)
                }
                return true
            }
        })

        return true
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
    fun pageRequest(api_url: String, tags: String, RequestManager: SingletonManager, page: Int){

        var reqParam = URLEncoder.encode("tags", "UTF-8") + "=" + URLEncoder.encode(tags, "UTF-8")
        reqParam += "&" + URLEncoder.encode("json", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")
        reqParam += "&" + URLEncoder.encode("pid", "UTF-8") + "=" + URLEncoder.encode(page.toString(), "UTF-8")

        val mURL = api_url + reqParam

        val jsonObjectRequest = JsonArrayRequest(Request.Method.GET, mURL, null,
            { response ->
                if (response.length() > 0){
                    Log.e("pageRequest", "Got Posts!")
                    setThumbnails(response, RequestManager, tags, page)
                }
                else{
                    Log.e(com.e.myapplication.TAG, "No Posts")
                }
            },
            { error ->
                Log.e(com.e.myapplication.TAG, "Page req error")
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
}
