package com.yiliaodemo.chat.net;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

/**
 * 关注&取消关注
 */
public class FocusRequester {

    public void focus(int actorId, final boolean focus) {

        Map<String, String> paramMap = new HashMap<>();

        String myId = AppManager.getInstance().getUserInfo().t_id + "";
        paramMap.put("userId", myId);

        if (focus) {
            /**
             *
             * saveFollow
             *
             * followUserId	是	int	关注人
             * coverFollowUserId	是	int	被关注人
             */
            paramMap.put("followUserId", myId);
            paramMap.put("coverFollowUserId", actorId + "");
        } else {
            /**
             *
             * delFollow
             *
             * followId	是	int	关注人编号
             * coverFollow	是	int	被关注人编号
             */
            paramMap.put("followId", myId);
            paramMap.put("coverFollow", actorId + "");
        }

        String url = focus ? ChatApi.SAVE_FOLLOW() : ChatApi.DEL_FOLLOW();

        OkHttpUtils.post().url(url)
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    onSuccess(response, focus);
                } else {
                    ToastUtil.showToast(AppManager.getInstance(), R.string.system_error);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                ToastUtil.showToast(AppManager.getInstance(), R.string.system_error);
            }
        });
    }

    public void onSuccess(BaseResponse response, boolean focus) {
        ToastUtil.showToast(AppManager.getInstance(), response.m_strMessage);
    }
}