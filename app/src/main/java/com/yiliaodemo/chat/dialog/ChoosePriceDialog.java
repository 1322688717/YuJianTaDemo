package com.yiliaodemo.chat.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.adapter.SetChargeRecyclerAdapter;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.layoutmanager.PickerLayoutManager;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 相册设置价格
 */
public class ChoosePriceDialog extends Dialog {

    private Activity activity;
    private boolean isVideo;
    private String[] datas;
    private String mSelectContent;

    /**
     * @param context
     */
    public ChoosePriceDialog(@NonNull Activity context, boolean isVideo) {
        super(context, R.style.DialogStyle_Dark_Background);
        this.activity = context;
        this.isVideo = isVideo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (activity.isFinishing()) {
            dismiss();
        }

        setContentView(R.layout.dialog_set_charge_layout);

        setCanceledOnTouchOutside(true);
        getWindow().setGravity(Gravity.BOTTOM);
        getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getDecorView().setPadding(0, 0, 0, 0);

        TextView cancel_tv = findViewById(R.id.cancel_tv);
        cancel_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        TextView title_tv = findViewById(R.id.title_tv);
        title_tv.setText(getContext().getString(isVideo ? R.string.private_video : R.string.private_image));
        title_tv.append(getContext().getString(R.string.gold_des));

        final List<String> beans = Arrays.asList(datas);

        SetChargeRecyclerAdapter adapter = new SetChargeRecyclerAdapter(getContext());

        final RecyclerView content_rv = findViewById(R.id.content_rv);

        final PickerLayoutManager pickerLayoutManager = new PickerLayoutManager(
                getContext(),
                content_rv,
                PickerLayoutManager.VERTICAL,
                false,
                beans.size(),
                0.4f,
                true);

        content_rv.setLayoutManager(pickerLayoutManager);
        content_rv.setAdapter(adapter);

        adapter.loadData(beans);
        pickerLayoutManager.setOnSelectedViewListener(new PickerLayoutManager.OnSelectedViewListener() {
            @Override
            public void onSelectedView(View view, int position) {
                if (position < beans.size()) {
                    mSelectContent = beans.get(position);
                }
            }
        });

        TextView confirm_tv = findViewById(R.id.confirm_tv);
        confirm_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mSelectContent)) {
                    mSelectContent = datas[0];
                }
                dismiss();
                execute(mSelectContent);
            }
        });

    }

    public void execute(String data) {

    }

    @Override
    public void show() {
        getData();
    }

    private String getUserId() {
        return AppManager.getInstance().getUserInfo().t_id + "";
    }

    private void getData() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        OkHttpUtils.post().url(isVideo ? ChatApi.GET_PRIVATE_VIDEO_MONEY() : ChatApi.GET_PRIVATE_PHOTO_MONEY())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<String>>() {
            @Override
            public void onResponse(BaseResponse<String> response, int id) {
                if (activity.isFinishing())
                    return;
                boolean ok = false;
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    String m_object = response.m_object;
                    if (!TextUtils.isEmpty(m_object)) {
                        datas = m_object.split(",");
                        ok = true;
                        ChoosePriceDialog.super.show();
                    }
                }
                if (!ok) {
                    handleError();
                }
            }
        });
    }

    private void handleError() {
        ToastUtil.showToast(activity, R.string.data_get_error);
        dismiss();
    }
}