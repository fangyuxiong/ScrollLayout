package com.xfy.sample;

import android.view.View;

/**
 * Created by XiongFangyu on 17/1/23.
 */

public class TestNormalFlip3DFragment extends BaseFragment {
    @Override
    protected int layoutId() {
        return R.layout.test_normal_flip_3d_fragment;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.view1:
                onViewClick(v, 0);
                return;
            case R.id.view2:
                onViewClick(v, 1);
                return;
            case R.id.view3:
                onViewClick(v, 2);
                return;
            case R.id.view4:
                onViewClick(v, 3);
                return;
        }
        super.onClick(v);
    }

    @Override
    protected void initEvents() {
        super.initEvents();
        findViewById(R.id.view1).setOnClickListener(this);
        findViewById(R.id.view2).setOnClickListener(this);
        findViewById(R.id.view3).setOnClickListener(this);
        findViewById(R.id.view4).setOnClickListener(this);
    }

    private void onViewClick(View v, int index) {
        toast("click view: " + index + " " + v.hashCode());
    }
}
