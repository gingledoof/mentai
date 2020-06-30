package com.e.myapplication

import android.content.Context
import android.media.Image
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter


class Pager : PagerAdapter {

    lateinit var context: Context
    lateinit var posts:List<Post>

    constructor(posts: List<Post>, context: Context){
        this.context = context
        this.posts = posts
    }

    fun getView(position: Int): View? {
        return posts[position].EncapView
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return posts.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val post = posts[position]
        if (post.init_post and !post.loaded){
            post.load()
            container.addView(post.EncapView)
        }
        else if (!post.loaded){
            post.load()
            container.addView(post.EncapView)
        }
        return post.EncapView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {

        return
        //super.destroyItem(container, position, `object`)
    }

    override fun getItemPosition(`object`: Any): Int {
        for (index in 0 until count) {
            if (`object` as View === posts[index].EncapView) {
                return index
            }
        }
        return POSITION_NONE
    }


}