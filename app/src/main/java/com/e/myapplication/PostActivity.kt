package com.e.myapplication
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import org.json.JSONObject


class PostActivity : AppCompatActivity() {

    val TAG = "Post View"

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        var postJSON = JSONObject(intent.getStringExtra("postJSON"))

        setContentView(R.layout.post_activity)

        //Request for image
        val imagereq = ImageRequest(
            postJSON["file_url"] as String,
            Response.Listener<Bitmap>{ result ->
                var postView = findViewById<ImageView>(R.id.post_view)
                postView.setImageBitmap(result)
            },
            1000,
            1000,
            ImageView.ScaleType.CENTER_CROP,
            Bitmap.Config.RGB_565,
            Response.ErrorListener { error ->
                Log.e(TAG, "ERROR")
            }
        )

    }

    fun onBackPressed(context: Context) {
        setContentView(R.layout.activity_main)
        super.onBackPressed()
    }
}