package com.e.myapplication

import RequestTools.*
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.LruCache
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.core.view.marginLeft
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import org.json.XML
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.reflect.typeOf

class MainActivity : AppCompatActivity() {


    val TAG = "RUNNING"

    val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
    var RequestManager = SingletonManager.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialize with all tags
        pageRequest(gelbooru_api, "", RequestManager, ::setThumb)

    }



    //Old, don't use or FUCK YOU
    private class HTTPSGet : AsyncTask<String, Int, JSONArray>() {

        protected fun onProgressUpdate(vararg progress: Int) {
            println(progress)
        }

        protected fun xml_formatted_json(xml: StringBuffer): JSONArray{
            val json = XML.toJSONObject(xml.toString())
            var json2 = json["posts"]
            json2 = JSONObject(json2.toString())
            val json3 = json2["post"]
            var string = json3.toString().replace("[", "").replace("]", "")
            var strings = string.replace("},", "}},")
            var stringi = strings.split("},")
            var json_arr = JSONArray()
            for (string in stringi) {
                json_arr.put(JSONObject(string.toString()))
            }
            return json_arr
        }

        override fun onPostExecute(result: JSONArray) {
            println("COMPLETED")
        }

        override fun doInBackground(vararg tags: String): JSONArray {
            val response = StringBuffer()

            var reqParam = URLEncoder.encode("tags", "UTF-8") + "=" + URLEncoder.encode(tags[0], "UTF-8")
            reqParam += "&" + URLEncoder.encode("json", "UTF-8") + "=" + URLEncoder.encode("0", "UTF-8")

            val mURL = URL("https://gelbooru.com/index.php?page=dapi&s=post&q=index&" + reqParam)

            with(mURL.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "GET"

                println("URL : $url")
                println("Response Code : $responseCode")

                BufferedReader(InputStreamReader(inputStream)).use {

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()
                    println("Response : $response")
                }
            }
            return xml_formatted_json(response)
        }
    }


    public fun setThumb(jsonObject: JSONObject) {

        val thumbnail = jsonObject["thumbnail"] as Bitmap

        val img_params = LinearLayout.LayoutParams(
            350,
            350
        )
        img_params.gravity = Gravity.CENTER_VERTICAL

        img_params.setMargins(10, 0, 10, 0)

        var lin_params = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )
        lin_params.addRule(Gravity.CENTER_HORIZONTAL)


        val imageView = ImageView(this)
        imageView.layoutParams = img_params
        imageView.setImageBitmap(thumbnail)
        imageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@MainActivity, PostActivity::class.java)
                intent.putExtra("postJSON", jsonObject.toString())
            }
        })

        //Relative Layout that encapsulates all Linear Layouts
        val rel_layout = findViewById<ViewGroup>(R.id.Relative)

        //Number of Linear Layout Views (rows)
        val rows = rel_layout.childCount
        Log.e("Number of ROWS: ", rows.toString())

        //Get Last row
        if (rows == 0) {
            val new_row = LinearLayout(this)
            new_row.layoutParams = lin_params
            new_row.id = R.id.Relative
            rel_layout.addView(new_row)
            new_row.addView(imageView)
            return
        }

        var sub_lay = rel_layout.getChildAt(rows - 1) as ViewGroup

        //Get # of image view in row
        val img_views = sub_lay.childCount
        Log.e("Number of IMGVIEWS: ", img_views.toString())


        //var lin_params = sub_lay.layoutParams

        if (img_views == 3) {
            Log.e("ADDING NEW ROW...", "ADDING NEW ROW...")
            //Create new Empty Row
            val new_row = LinearLayout(this)
            println(sub_lay.id)
            lin_params.addRule(Gravity.CENTER_HORIZONTAL)
            lin_params.addRule(RelativeLayout.BELOW, sub_lay.id)
            new_row.layoutParams = lin_params
            new_row.id = sub_lay.id + 1
            new_row.gravity = Gravity.CENTER_HORIZONTAL
            rel_layout.addView(new_row)
            new_row.addView(imageView)
            return
        } else {
            sub_lay.addView(imageView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)

        var menuItem = menu.findItem(R.id.search_bar)
        var searchView = menuItem.actionView as SearchView

        searchView.queryHint = "Search for tags"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val rel_layout = findViewById<ViewGroup>(R.id.Relative)
                rel_layout.removeAllViews()

                pageRequest(gelbooru_api, query.toString(), RequestManager, ::setThumb)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        return true
    }

}
