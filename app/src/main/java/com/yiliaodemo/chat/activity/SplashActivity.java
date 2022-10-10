package com.yiliaodemo.chat.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.gyf.barlibrary.ImmersionBar;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseBean;
import com.yiliaodemo.chat.bean.ChatUserInfo;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.dialog.AgreementDialog;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.helper.RingVibrateManager;
import com.yiliaodemo.chat.helper.SharedPreferenceHelper;
import com.yiliaodemo.chat.socket.ConnectHelper;

/*
 * Copyright (C) 2018
 * 版权所有
 *
 * 功能描述：启动页面
 * 作者：
 * 创建时间：2018/6/14
 *
 * 修改人：
 * 修改描述：
 * 修改日期
 */
public class SplashActivity extends Activity {

    private ChatUserInfo chatUserInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppManager.getInstance().getActivityObserve().isActive()) {
            finish();
            return;
        }

        chatUserInfo = SharedPreferenceHelper.getAccountInfo(AppManager.getInstance());

        ImmersionBar.with(this).statusBarDarkFont(true).navigationBarColor(R.color.black).init();

        setContentView(R.layout.activity_splash_layout);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                toIntent();
            }
        }, 2000);

        if (chatUserInfo.t_id > 0) {
            IMHelper.checkLogin();
            AppManager.getInstance().refreshMyInfo();
        }

//        OkHttpUtils.post()
//                .url(ChatApi.getProtectAppVersion())
//                .build()
//                .execute(new AjaxCallback<BaseResponse<UrlResponse>>() {
//
//                    @Override
//                    public void onResponse(BaseResponse<UrlResponse> response, int id) {
//                        if (response != null && response.m_object != null) {
//                            UrlResponse urlResponse = response.m_object;
//                            if (urlResponse.request_status == 1) {
//                                setUrl(urlResponse);
//                                saveUrl(urlResponse);
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onError(Call call, Exception e, int id) {
//                        String localUrlData = PreferenceManager.getDefaultSharedPreferences(AppManager.getInstance())
//                                .getString("local_url_data", null);
//                        if (!TextUtils.isEmpty(localUrlData)) {
//                            try {
//                                UrlResponse urlResponse = JSON.parseObject(localUrlData, UrlResponse.class);
//                                setUrl(urlResponse);
//                            } catch (Exception e2) {
//                                e2.printStackTrace();
//                            }
//                        }
//                    }
//
//                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        RingVibrateManager.syncSwitch();
    }

    private void setUrl(UrlResponse response) {
        if (!TextUtils.isEmpty(response.request_url) && response.request_url.startsWith("http")) {
            ChatApi.setUrl(response.request_url);
        }
        if (!TextUtils.isEmpty(response.request_socket)) {
            ConnectHelper.get().setUrl(response.request_socket);
        }
    }

    private void saveUrl(UrlResponse response) {
        PreferenceManager.getDefaultSharedPreferences(AppManager.getInstance())
                .edit().putString("local_url_data", response == null ? null : JSON.toJSONString(response))
                .apply();
    }

    private void toIntent() {
        boolean finish = true;
        if (chatUserInfo.t_id > 0) {
            if (chatUserInfo.t_sex == 2) {//还没有选择性别
                startActivity(new Intent(getApplicationContext(), ChooseGenderActivity.class));
            } else {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            if (sp.getBoolean("agree", false)) {
                startActivity(new Intent(getApplicationContext(), ScrollLoginActivity.class));
            } else {
                finish = false;
                new AgreementDialog(this).show();
            }
        }
        if (finish) {
            finish();
        }
    }

    static class UrlResponse extends BaseBean {

        public String request_url;

        public String request_socket;

        public int request_status;

    }

}