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
import androidx.viewpager.widget.PagerAdapter
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
import java.util.*


val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
val TAG = "Request Tools"

//Currently only sets thumbnail url on passed jsonarray




