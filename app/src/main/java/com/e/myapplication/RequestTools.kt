package com.e.myapplication

import RequestTools.SingletonManager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.android.volley.Response
import com.android.volley.request.DownloadRequest
import com.android.volley.request.ImageRequest
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import org.json.JSONObject
import java.io.File
import java.io.Serializable


val gelbooru_api = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&"
val TAG = "Request Tools"

//Currently only sets thumbnail url on passed jsonarray

class Post(jsonObject: JSONObject) : Serializable {
    val source: String
    val directory: String
    val hash: String
    val height: Int
    val id: Int
    val image: String
    val change: Int
    val owner: String
    val rating: String
    val score: Int
    val tags: List<String>
    val width: Int
    val file_url: String
    val created_at: String
    lateinit var view: View
    var local_file: String = ""
    lateinit var request: DownloadRequest
    var init_post = false
    var loaded = false
    lateinit var thumbnail_url: String
    lateinit var listener: Response.Listener<Any>
    lateinit var player: SimpleExoPlayer
    lateinit var mediaSource: MediaSource
    lateinit var EncapView: ViewGroup
    lateinit var progressBar: ProgressBar
    lateinit var RequestMangager: SingletonManager
    lateinit var context: Context
    var favorite = false
    lateinit var thumbnailView: ImageView

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
        this.score = jsonObject["score"] as Int
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


    fun load(){
        progressBar.visibility = View.GONE

        if (PostFileHandler.handler(image) == PostFileHandler.VIDEO){
            player.playWhenReady = false
            player.seekTo(0, 0)
            player.prepare(mediaSource, false, false)
        }
        else if (PostFileHandler.handler(image) == PostFileHandler.IMAGE){
            if (File(local_file).exists()){
                (view as ImageView).setImageBitmap(BitmapFactory.decodeFile(local_file))
            }
            else {
                RequestMangager.addToRequestQueue(request)
                progressBar.visibility = View.VISIBLE
                progressBar.bringToFront()
            }
            return
        }
        else if (PostFileHandler.handler(image) == PostFileHandler.GIF){
            if (File(local_file).exists()){
                Glide.with(context).load(local_file).into(view as ImageView)
            }
            else {
                RequestMangager.addToRequestQueue(request)
                progressBar.visibility = View.VISIBLE
                progressBar.bringToFront()
            }
            return
        }

    }

}

