package com.yiliaodemo.chat.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.base.BaseBean;
import com.yiliaodemo.chat.base.BaseListResponse;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.view.recycle.AbsRecycleAdapter;
import com.yiliaodemo.chat.view.recycle.OnItemClickListener;
import com.yiliaodemo.chat.view.recycle.ViewHolder;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 打招呼
 */
public class HelloDialog extends Dialog implements View.OnClickListener {

    private BaseActivity activity;
    private List<HelloBean> list;

    public HelloDialog(@NonNull BaseActivity context) {
        super(context);
        activity = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_hello);

        getWindow().setGravity(Gravity.BOTTOM);
        getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        setCanceledOnTouchOutside(true);
        setCancelable(true);

        findViewById(R.id.confirm_tv).setOnClickListener(this);

        final AbsRecycleAdapter absRecycleAdapter = new AbsRecycleAdapter(
                new AbsRecycleAdapter.Type(R.layout.item_hello_text, HelloBean.class)) {
            @Override
            public void convert(ViewHolder holder, Object t) {
                HelloBean helloBean = (HelloBean) t;
                holder.itemView.setBackgroundColor(helloBean.selected ? 0xffe5e5e5 : 0xffffffff);
                holder.<TextView>getView(R.id.content_tv).setText(helloBean.t_content);
            }
        };
        absRecycleAdapter.setData(list, true);
        absRecycleAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, Object obj, int position) {
                HelloBean helloBean = (HelloBean) obj;
                if (!helloBean.selected) {
                    helloBean.selected = true;
                    for (HelloBean bean : list) {
                        if (bean != helloBean) {
                            bean.selected = false;
                        }
                    }
                    absRecycleAdapter.notifyDataSetChanged();
                }
            }
        });

        RecyclerView recyclerView = findViewById(R.id.text_rv);
        DividerItemDecoration decoration = new DividerItemDecoration(getContext(), LinearLayoutManager.HORIZONTAL);
        decoration.setDrawable(getContext().getResources().getDrawable(R.drawable.divider_horizontal_gray));
        recyclerView.addItemDecoration(decoration);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(absRecycleAdapter);

        findViewById(R.id.dismiss_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

    }

    private void hello(HelloBean bean) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        paramMap.put("imId", bean.t_id);
        OkHttpUtils.post().url(ChatApi.sendIMToUserMes())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                if (response != null) {
                    ToastUtil.showToast(response.m_strMessage);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                ToastUtil.showToast("打招呼失败");
            }
        });
    }

    @Override
    public void onClick(View v) {
        HelloBean bean = null;
        for (HelloBean helloBean : list) {
            if (helloBean.selected) {
                bean = helloBean;
                break;
            }
        }
        if (bean == null) {
            ToastUtil.showToast("请选择打招呼消息");
            return;
        }

        dismiss();
        hello(bean);
    }

    @Override
    public void show() {
        getList();
    }

    private void getList() {
        activity.showLoadingDialog();
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        OkHttpUtils.post().url(ChatApi.getIMToUserMesList())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseListResponse<HelloBean>>() {
            @Override
            public void onResponse(BaseListResponse<HelloBean> response, int id) {
                if (activity.isFinishing()) {
                    return;
                }
                if (response != null
                        && response.m_istatus == NetCode.SUCCESS
                        && response.m_object != null
                        && response.m_object.size() > 0) {
                    list = response.m_object;
                    HelloDialog.super.show();
                }
            }

            @Override
            public void onAfter(int id) {
                if (!activity.isFinishing()) {
                    activity.dismissLoadingDialog();
                }
            }
        });
    }

    public static class HelloBean extends BaseBean {
        transient boolean selected;
        public int t_id;
        public String t_content;
    }
}