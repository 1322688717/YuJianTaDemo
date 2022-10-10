package com.yiliaodemo.chat.activity;

import android.view.View;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.BaseActivity;

public class MyInviteActivity extends BaseActivity {

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_my_invite);
    }

    @Override
    protected void onContentAdded() {
        setTitle("我的邀请");
    }

}