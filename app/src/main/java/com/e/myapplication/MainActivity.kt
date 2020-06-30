package com.e.myapplication

import RequestTools.SingletonManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.request.ImageRequest
import com.android.volley.request.JsonArrayRequest
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*

class MainActivity : AppCompatActivity() {

    val TAG = "RUNNING"

    val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
    var RequestManager = SingletonManager.getInstance(this)
    //val temp_path = Environment.getDataDirectory().absolutePath + "/MENTAI/temp"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Request Permissions
        req()

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

    /*
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

                inputStream.use { input ->
                    this.outputStream.use { fileOut ->

                        while (true) {
                            val length = input.read(buffer)
                            if (length <= 0)
                                break
                            fileOut.write(buffer, 0, length)
                        }
                        fileOut.flush()
                    }
                }


            }
        }
    }*/
    /*
    private class DownloadFileFromURL : AsyncTask<String?, String?, String?>() {
        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        override fun onPreExecute() {
            super.onPreExecute()
        }

        /**
         * Downloading file in background thread
         */
        override fun doInBackground(vararg params: String?): String? {
            var count: Int
            try {
                val url = URL(f_url[0])
                val connection: URLConnection = url.openConnection()
                connection.connect()

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                val lenghtOfFile: Int = connection.getContentLength()

                // download the file
                val input: InputStream = BufferedInputStream(
                    url.openStream(),
                    8192
                )

                // Output stream
                val output: OutputStream = FileOutputStream(
                    Environment
                        .getExternalStorageDirectory().toString()
                            + "/2011.kml"
                )
                val data = ByteArray(1024)
                var total: Long = 0
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (total * 100 / lenghtOfFile).toInt())

                    // writing data to file
                    output.write(data, 0, count)
                }

                // flushing output
                output.flush()

                // closing streams
                output.close()
                input.close()
            } catch (e: Exception) {
                Log.e("Error: ", e.message)
            }
            return null
        }

        /**
         * Updating progress bar
         */
        override fun onProgressUpdate(vararg values: String?) {
            // setting progress percentage
            println(values[0]?.toInt())
            //pDialog.setProgress(values[0]?.toInt())
        }

        /**
         * After completing background task Dismiss the progress dialog
         */
        override fun onPostExecute(file_url: String?) {
            // dismiss the dialog after the file was downloaded
            //Done
        }
    }*/

    fun setThumbnails(jsonArray: JSONArray, RequestManager: SingletonManager) {

        var lin_params = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )

        val intent = Intent(this@MainActivity, PostActivity::class.java)

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


        var file_stack = Stack<String>()
        var file_names = Stack<String>()
        //Setting thumbnail url
        //Don't make fun of me for iterating this way. JSONArrays have no iterator
        for (i in 0 until jsonArray.length()) {

            val json = jsonArray[i] as JSONObject

            file_names.add(json["image"] as String)
            file_stack.add(json["file_url"] as String)


            var imageView = ImageView(this)
            imageView.layoutParams = img_params

            json.put("thumbnailView", imageView)

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
                    Log.e(RequestTools.TAG, "ERROR")
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

            RequestManager.addToRequestQueue(imagereq)

        }

        intent.putExtra("jsonArray", jsonArray.toString())
        intent.putExtra("file_urls", file_stack)
        //intent.putExtra("temp_path", temp_path.absolutePath)
        intent.putExtra("file_names", file_names)


        for (i in 0 until jsonArray.length()) {
            var json = jsonArray[i] as JSONObject
            (json["thumbnailView"] as ImageView).setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    Log.e(TAG, "CLICKED")
                    intent.putExtra("init_post", i)
                    startActivity(intent)
                }
            })
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        var menuItem = menu.findItem(R.id.search_bar)
        var searchView = menuItem.actionView as SearchView

        searchView.queryHint = "Search for tags"

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
                    Log.e(RequestTools.TAG, "No Posts")
                }
            },
            Response.ErrorListener { error ->
                Log.e(RequestTools.TAG, "error")
                Log.e(RequestTools.TAG, error.message)
                // TODO: Handle error
            }
        )
        RequestManager.addToRequestQueue(jsonObjectRequest)
    }

    fun getContext() : Context{
        return applicationContext
    }
}
