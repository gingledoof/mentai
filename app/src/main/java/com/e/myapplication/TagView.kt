package com.e.myapplication

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.get
import java.util.*

class TagView(context: Context, drawable: Drawable?) : RelativeLayout(context) {

    var tags:Stack<String>

    private val textView = TextView(context)
    private val iconView = ImageView(context)
    init {
        this.setBackgroundColor(Color.RED)
        this.gravity = Gravity.CENTER
        this.addView(textView)

        var lp = RelativeLayout.LayoutParams(30,30)
        lp.addRule(RelativeLayout.LEFT_OF, textView.id)

        iconView.layoutParams = lp

        this.addView(iconView)
        this.setPadding(20,0,20,0)
        this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

        tags = Stack<String>()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
    }

    fun setText(text: String){
        textView.text = text
    }

    fun getText(): String {
        return this.textView.text.toString()
    }

}