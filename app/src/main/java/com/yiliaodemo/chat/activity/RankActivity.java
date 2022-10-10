package com.yiliaodemo.chat.activity;

import android.view.View;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.BaseActivity;

/**
 * 榜单页面
 */
public class RankActivity extends BaseActivity {

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_rank_layout);
    }

    @Override
    protected void onContentAdded() {
        needHeader(false);
    }

    @Override
    protected boolean isImmersionBarEnabled() {
        return true;
    }

}