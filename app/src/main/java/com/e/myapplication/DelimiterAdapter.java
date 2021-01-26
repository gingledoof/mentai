package com.e.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.TintableBackgroundView;

import java.util.ArrayList;
import java.util.List;

public class DelimiterAdapter extends ArrayAdapter<String> implements Filterable {

    private final LayoutInflater mInflater;
    private List<String> mSubStrings;

    private String mMainString;
    public String getMainString() { return mMainString; }

    private AmazingFilter mFilter;
    private String[] options;

    public DelimiterAdapter(Context context, int resource, String[] tags) {
        super(context, -1);
        mInflater = LayoutInflater.from(context);
        options = tags;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView tv;
        if (convertView != null) {
            tv = (TextView) convertView;
        } else {
            tv = (TextView) mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }
        tv.setText(getItem(position));
        return tv;
    }

    @Override
    public int getCount() {

        if (mSubStrings != null){
            return mSubStrings.size();
        }
        else {
            return 0;
        }
    }

    @Override
    public String getItem(int position) {
        return mSubStrings.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public Filter getFilter() {
        if(mFilter == null) {
            mFilter = new AmazingFilter();
        }
        return mFilter;
    }

    private class AmazingFilter extends Filter {

        private final static String DELIMITER = " ";

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults filterResults = new FilterResults();
            String request;
            mSubStrings = new ArrayList<String>();
            if(constraint != null) {
                request = constraint.toString();

                //cuts the string with delimiter
                if (request.contains(DELIMITER)) {
                    final String[] splitted = request.split(DELIMITER);
                    request = splitted[splitted.length - 1];

                }

                //checks for substring of any word in the dictionary
                for(String s : options) {
                    if(s.contains(request)) {
                        mSubStrings.add(s);
                    }
                }
            }
            filterResults.values = mSubStrings;
            filterResults.count = mSubStrings.size();
            return filterResults;
        }


        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            for (String request : (ArrayList<String>)results.values) {
                add(request);
            }
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}