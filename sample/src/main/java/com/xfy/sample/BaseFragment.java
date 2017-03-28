package com.xfy.sample;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.xfy.scrolllayout.OnChangeListener;
import com.xfy.scrolllayout.ScrollLayout;

/**
 * Created by XiongFangyu on 17/1/23.
 */

public abstract class BaseFragment extends Fragment implements View.OnClickListener, OnChangeListener {

    protected View rootView;
    protected ScrollLayout scrollLayout;
    protected EditText gotoEdit;
    private Toast toast;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = createRootView(inflater, savedInstanceState);
        initViews();
        initEvents();
        return rootView;
    }

    protected View createRootView(LayoutInflater inflater, Bundle savedInstanceState) {
        final int id = layoutId();
        if (id > 0) {
            return inflater.inflate(id, null);
        }
        return null;
    }

    protected void initViews() {
        scrollLayout = findViewById(R.id.flip_layout);
        gotoEdit = findViewById(R.id.goto_edit);
    }

    protected void initEvents() {
        findViewById(R.id.goto_button).setOnClickListener(this);
        findViewById(R.id.goto_smooth_button).setOnClickListener(this);
        findViewById(R.id.next_button).setOnClickListener(this);
        findViewById(R.id.next_smooth_button).setOnClickListener(this);
        findViewById(R.id.pre_button).setOnClickListener(this);
        findViewById(R.id.pre_smooth_button).setOnClickListener(this);
        findViewById(R.id.reset).setOnClickListener(this);
        scrollLayout.setOnChangeListener(this);
    }

    protected abstract int layoutId();

    protected <T extends View> T findViewById(int id) {
        return rootView != null ? (T) rootView.findViewById(id) : null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.goto_button:
                onGotoButtonClick(false);
                break;
            case R.id.goto_smooth_button:
                onGotoButtonClick(true);
                break;
            case R.id.next_button:
                scrollLayout.toNext(false);
                break;
            case R.id.pre_button:
                scrollLayout.toPre(false);
                break;
            case R.id.next_smooth_button:
                scrollLayout.toNext(true);
                break;
            case R.id.pre_smooth_button:
                scrollLayout.toPre(true);
                break;
            case R.id.reset:
                scrollLayout.reset();
                break;
        }
    }

    private void onGotoButtonClick(boolean smooth) {
        String s = gotoEdit.getText().toString();
        try {
            int index = Integer.parseInt(s);
            final int c = scrollLayout.getChildCount();
            if (index < 0 || index > c) {
                toast("请输入[0, " + c + ")之间的整数.");
                clearEdit();
            } else {
                scrollLayout.gotoChild(index, smooth);
            }
        } catch (Exception e) {
            e.printStackTrace();
            toast("请输入整数.");
            clearEdit();
        }
    }

    @Override
    public void changeTo(View child, int index) {
        toast("change to " + index);
    }

    protected void toast(String s) {
        if (toast != null)
            toast.cancel();
        toast = Toast.makeText(getActivity(), s, Toast.LENGTH_LONG);
        toast.show();
    }

    private void clearEdit() {
        gotoEdit.setText("");
    }
}
