package com.e.myapplication;

import android.widget.MultiAutoCompleteTextView;

public class SpaceTokenizer implements MultiAutoCompleteTextView.Tokenizer {
    @Override
    public int findTokenStart(CharSequence text, int cursor) {

        String[] temp = text.toString().split("\n");
        int i=0;
        int det=0;
        while( det != temp.length-1){
            if  (text.charAt(i) == ' ') {det = det+1;}
        }

        return det;
    }

    @Override
    public int findTokenEnd(CharSequence text, int cursor) {
        return text.length();
    }

    @Override
    public CharSequence terminateToken(CharSequence text) {
        return text.toString() + ' ';
    }
}
