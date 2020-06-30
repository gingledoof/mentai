package RequestTools

import android.app.Activity
import com.e.myapplication.PostActivity
import com.e.myapplication.R

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.viewpager.widget.PagerAdapter
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import com.e.myapplication.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*


val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
val TAG = "Request Tools"

//Currently only sets thumbnail url on passed jsonarray

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
    val score: String
    val tags: List<String>
    val width: Int
    val file_url: String
    val created_at: String
    lateinit var view: View
    lateinit var request: Any
    lateinit var reqManager: SingletonManager
    var init_post = false
    var loaded = false
    lateinit var thumbnail_url: String

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
        this.score = jsonObject["score"] as String
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
}

