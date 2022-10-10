package com.yiliaodemo.chat.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;

/**
 * 奖励规则dialog
 */
public class RewardRuleDialog extends android.app.Dialog implements View.OnClickListener {

    private BaseActivity context;

    private String content;

    public RewardRuleDialog(@NonNull BaseActivity context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_reward_rule);

        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        win.setGravity(Gravity.CENTER);
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        win.setAttributes(lp);

        TextView contentTv = findViewById(R.id.info_tv);
        contentTv.setText(content);

        findViewById(R.id.confirm_tv).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    @Override
    public void show() {

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        OkHttpUtils.post().url(ChatApi.getSystemConfig())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<JSONObject>>() {
            @Override
            public void onResponse(BaseResponse<JSONObject> response, int id) {
                if (context.isFinishing())
                    return;
                if (response != null && response.m_istatus == NetCode.SUCCESS && response.m_object != null) {
                    content = response.m_object.getString("t_rank_award_rules");
                    RewardRuleDialog.super.show();
                }
            }

            @Override
            public void onAfter(int id) {
                if (context.isFinishing())
                    return;
                context.dismissLoadingDialog();
            }

            @Override
            public void onBefore(Request request, int id) {
                if (context.isFinishing())
                    return;
                context.showLoadingDialog();
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                ToastUtil.showToast("获取失败");
            }
        });
    }
}