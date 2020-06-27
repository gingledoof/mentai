package com.e.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject

class PostFragment : Fragment() {

    companion object {

        fun newInstance(json: JSONObject): Fragment {

            val args = Bundle()
            args.putString("json", json.toString())
            //args.putSerializable("testData", testData)
            val fragment = PostFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{

        val json = JSONObject(arguments?.getString("test"))

        var view:View = inflater.inflate(R.layout.slider_item, container, false)

        var textView = view.findViewById<TextView>(R.id.test_view)
        textView.setText(json["thumbnail_url"] as String)

        return view
        //return super.onCreateView(inflater, container, savedInstanceState)
    }
}