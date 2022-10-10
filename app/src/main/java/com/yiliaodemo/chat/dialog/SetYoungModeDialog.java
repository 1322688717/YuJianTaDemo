package com.yiliaodemo.chat.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.YoungModeActivity;


/*
 * Copyright (C) 2018
 * 版权所有
 *
 * 功能描述：设置未成年模式dialog
 * 作者：
 * 创建时间：2018/6/14
 *
 * 修改人：
 * 修改描述：
 * 修改日期
 */
public class SetYoungModeDialog extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_set_yound_mode_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //我知道了
        TextView i_know_tv = view.findViewById(R.id.i_know_tv);
        i_know_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        //设置模式
        TextView set_mode_tv = view.findViewById(R.id.set_mode_tv);
        set_mode_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), YoungModeActivity.class);
                    getActivity().startActivity(intent);
                }
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // 一定要设置Background，如果不设置，window属性设置无效
                window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
                DisplayMetrics dm = new DisplayMetrics();
                if (getActivity() != null) {
                    WindowManager windowManager = getActivity().getWindowManager();
                    if (windowManager != null) {
                        windowManager.getDefaultDisplay().getMetrics(dm);
                        WindowManager.LayoutParams params = window.getAttributes();
                        params.gravity = Gravity.CENTER;
                        // 使用ViewGroup.LayoutParams，以便Dialog 宽度充满整个屏幕
                        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        window.setAttributes(params);
                    }
                }
            }
        }
    }

}
