package RequestTools

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
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.e.myapplication.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder


val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
val TAG = "Request Tools"

//Currently only sets thumbnail url on passed jsonarray
fun setJSONVals(jsonArray: JSONArray, RequestManager: SingletonManager, handler: (json: JSONObject) -> Unit) {

    //Setting thumbnail url
    //Don't make fun of me for iterating this way. JSONArrays have no iterator
    for (i in 0 until jsonArray.length()) {
        val json = jsonArray.getJSONObject(i)
        val file = json["image"] as String
        val thumbnail = json["file_url"].toString()
            .replace("images", "thumbnails")
            .replace(file, "thumbnail_" + file)
            .replace("img2", "img1")
            .replaceAfterLast(".", "jpg")
        //Append Thumbnail URL to JSON
        json.put("thumbnail_url", thumbnail)

        //Thumbnail request
        val imagereq = ImageRequest(
            json["thumbnail_url"] as String,
            Response.Listener<Bitmap> { result ->
                //Set Thumbnails to ImageView
                json.put("thumbnail", result as Bitmap)
                handler(json)
            },
            1000,
            1000,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            Response.ErrorListener { error ->
                Log.e(TAG, "ERROR")
            }
        )

        RequestManager.addToRequestQueue(imagereq)
    }
}

//Gets thumbnail urls, sets thumbnails for first MaxImagesPerPage number of images
fun pageRequest(api_url: String, tags: String, RequestManager: SingletonManager, handler: (json: JSONObject) -> Unit){

    var reqParam = URLEncoder.encode("tags", "UTF-8") + "=" + URLEncoder.encode(tags, "UTF-8")
    reqParam += "&" + URLEncoder.encode("json", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8")

    val mURL = gelbooru_api + reqParam



    val jsonObjectRequest = JsonArrayRequest(Request.Method.GET, mURL, null,
        Response.Listener { response ->
            setJSONVals(response, RequestManager, handler)
        },
        Response.ErrorListener { error ->
            Log.e(TAG, "error")
            Log.e(TAG, error.message)
            // TODO: Handle error
        }
    )
    RequestManager.addToRequestQueue(jsonObjectRequest)
}



