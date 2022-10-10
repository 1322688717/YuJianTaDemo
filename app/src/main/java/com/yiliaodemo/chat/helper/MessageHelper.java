package com.yiliaodemo.chat.helper;

import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.util.ParamUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 清空消息
 */
public class MessageHelper {

    /**
     * 清除通话记录
     *
     * @param listener
     */
    private static void clearCall(final OnCommonListener listener) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        paramMap.put("page", 1);
        OkHttpUtils.post().url(ChatApi.GET_CALL_LOG())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<String>>() {
            @Override
            public void onResponse(BaseResponse<String> response, int id) {
                clearSystemMessage(listener);
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                clearSystemMessage(listener);
            }
        });
    }

    /**
     * 清除系统消息
     *
     */
    private static void clearSystemMessage(final OnCommonListener listener) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        OkHttpUtils.post().url(ChatApi.SET_UP_READ())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                if (listener != null)
                    listener.execute(null);
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                if (listener != null)
                    listener.execute(null);
            }
        });
    }

    public static void execute(OnCommonListener listener) {
        clearCall(listener);
    }
}