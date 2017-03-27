package com.xfy.sample;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

/**
 * Created by XiongFangyu on 17/1/23.
 */

public class MainActivity extends Activity implements View.OnClickListener{

    private static final int CONTAINER = R.id.container;
    private Fragment showFragment;
    private View buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.actvity_main);
        buttonContainer = findViewById(R.id.scroll_view);
        initEvents();
    }

    private void initEvents() {
        findViewById(R.id.one   ).setOnClickListener(this);
        findViewById(R.id.two   ).setOnClickListener(this);
        findViewById(R.id.three ).setOnClickListener(this);
        findViewById(R.id.four  ).setOnClickListener(this);
        findViewById(R.id.five  ).setOnClickListener(this);
        findViewById(R.id.six   ).setOnClickListener(this);
        findViewById(R.id.seven ).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.one  :
                TestNormalFlip3DFragment f = new TestNormalFlip3DFragment();
                getFragmentManager().beginTransaction().add(CONTAINER, f).commit();
                showFragment = f;
                buttonContainer.setVisibility(View.GONE);
                break;
            case R.id.two  :
                break;
            case R.id.three:
                break;
            case R.id.four :
                break;
            case R.id.five :
                break;
            case R.id.six  :
                break;
            case R.id.seven:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (showFragment != null) {
            getFragmentManager().beginTransaction().remove(showFragment).commit();
            buttonContainer.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}
