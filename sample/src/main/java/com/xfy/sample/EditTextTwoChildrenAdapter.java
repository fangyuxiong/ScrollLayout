package com.xfy.sample;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.xfy.scrolllayout.BaseTwoChildrenAdapter;
import com.xfy.scrolllayout.ScrollLayout;

/**
 * Created by XiongFangyu on 2017/3/28.
 */
public class EditTextTwoChildrenAdapter extends BaseTwoChildrenAdapter<EditText> {

    private boolean changed = true;

    @Override
    public @NonNull EditText cloneFirstView(ScrollLayout parent, EditText first) {
        EditText text = super.cloneFirstView(parent, first);
        first.addTextChangedListener(new TextChangedListener(parent, first));
        text.addTextChangedListener(new TextChangedListener(parent, text));
        return text;
    }

    @Override
    public @NonNull EditText cloneSecondView(ScrollLayout parent, EditText second) {
        EditText text = super.cloneSecondView(parent, second);
        second.addTextChangedListener(new TextChangedListener(parent, second));
        text.addTextChangedListener(new TextChangedListener(parent, text));
        return text;
    }

    @Override
    public void bindViewData(EditText res, EditText newView) {
        newView.setText(res.getText().toString());
    }

    private class TextChangedListener implements TextWatcher {
        private ScrollLayout scrollLayout;
        private EditText view;
        TextChangedListener(ScrollLayout scrollLayout, EditText editText) {
            this.scrollLayout = scrollLayout;
            view = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (changed) {
                changed = false;
                scrollLayout.notifyViewChanged(view);
            } else {
                changed = true;
            }
        }
    }
}
