package com.qd.recorder;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;

/**
 * Created by yangfeng on 14/12/22.
 */
public class BaseInjectActivity extends Activity {
    public final void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.inject(this);
    }

    public final void setContentView(View view) {
        super.setContentView(view);
        ButterKnife.inject(this);
    }
    public final void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        ButterKnife.inject(this);
    }
}
