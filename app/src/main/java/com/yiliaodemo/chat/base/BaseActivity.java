package com.yiliaodemo.chat.base;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.gyf.barlibrary.ImmersionBar;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.socket.SocketMessageManager;
import com.yiliaodemo.chat.socket.domain.Mid;
import com.yiliaodemo.chat.socket.domain.ReceiveFloatingBean;
import com.yiliaodemo.chat.socket.domain.SocketResponse;
import com.yiliaodemo.chat.util.ActivityManager;
import com.yiliaodemo.chat.util.DialogUtil;
import com.yiliaodemo.chat.util.FloatingManagerOne;
import com.zhy.http.okhttp.OkHttpUtils;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/*
 * Copyright (C) 2018
 * 版权所有
 *
 * 功能描述： Activity基类
 * 作者：
 * 创建时间：2018/6/14
 *
 * 修改人：
 * 修改描述：
 * 修改日期
 */
public abstract class BaseActivity extends FragmentActivity {

    //根布局
    protected RelativeLayout mBaseLayout;

    protected FrameLayout mBaseContent;

    //标题栏
    protected View mHeadLayout;

    protected View mLeftFl;

    protected ImageView mLeftIv;

    protected TextView mTvTitle;

    protected TextView mRightTv;

    protected View mHeadLineV;

    //沉浸式状态栏
    protected ImmersionBar mImmersionBar;

    //注解
    private Unbinder mUnbinder;

    protected BaseActivity mContext;

    //加载dialog
    private Dialog mDialogLoading;

    //判断当前是否前台
    private boolean mIsActivityFront = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        ActivityManager.getInstance().addActivity(this);

        SocketMessageManager.get().subscribe(baseSubscribe, Mid.RECEIVE_GIFT);

        mContext = this;

        boolean supportFullScr = supportFullScreen();
        if (supportFullScr) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.base_activity_base);

        //1.设置状态栏样式
        if (!supportFullScr) {
            setStatusBarStyle();
        }

        //2.设置是否屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //3.初始化http请求request集合，保证在activity结束的时候终止http请求
        //4.初始化view
        initView();
        //5.添加view到content容器中，子类实现
        addIntoContent(getContentView());
        //6.初始化view，设置onclick监听器
        //解决继承自BaseActivity且属于当前库(framework)的子类butterknife不能使用Bindview的注解，onclick的注解
        initSubView();
        //7.register eventbus
        //8.view已添加到container
        onContentAdded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityFront = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActivityFront = false;
    }

    @Override
    protected void onDestroy() {

        SocketMessageManager.get().unsubscribe(baseSubscribe);

        if (mImmersionBar != null) {
            mImmersionBar.destroy();
        }
        if (mUnbinder != null) {
            mUnbinder.unbind();
        }

        OkHttpUtils.getInstance().cancelTag(BaseActivity.this);
        ActivityManager.getInstance().removeActivity(this);
        dismissLoadingDialog();

        super.onDestroy();
    }

    /**
     * 初始化view
     */
    private void initView() {
        mDialogLoading = DialogUtil.showLoadingDialog(this);
        mBaseLayout = findViewById(R.id.base_layout);
        mBaseContent = findViewById(R.id.base_content);

        mHeadLayout = findViewById(R.id.head);
        //left
        mLeftFl = findViewById(R.id.left_fl);
        mLeftIv = findViewById(R.id.left_image);
        //middle
        mTvTitle = findViewById(R.id.middle_title);
        //right
        mRightTv = findViewById(R.id.right_text);
        //line
        mHeadLineV = findViewById(R.id.head_line_v);

        //默认处理title左上view的点击事件（back）
        mLeftFl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * 添加view到容器中
     */
    private void addIntoContent(View view) {
        if (view != null) {
            if (!attachMergeLayout()) {
                mBaseContent.removeAllViews();
                mBaseContent.addView(view);
            }
            mUnbinder = ButterKnife.bind(this);
        } else {
            try {
                throw new Exception("content view can not be null");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return view
     */
    protected abstract View getContentView();

    /**
     * 加载布局
     */
    protected View inflate(@LayoutRes int resource) {
        return LayoutInflater.from(this).inflate(resource, null);
    }

    /**
     * 加载布局
     */
    protected View inflate(@LayoutRes int resource, @Nullable ViewGroup root) {
        return LayoutInflater.from(this).inflate(resource, root);
    }

    /**
     * 初始化 subView，一般只用于在framework中BaseActivity子类，为了解决
     * 继承自BaseActivity且属于当前库(framework)的子类butterknife不能使用Bindview的注解，onclick的注解
     */
    protected void initSubView() {

    }

    /**
     * @return return true 添加的layout以merge标签作为根布局, false layout不以merge标签作为根布局
     */
    protected boolean attachMergeLayout() {
        return false;
    }

    /**
     * 添加view完成回调，用于初始化数据
     */
    protected abstract void onContentAdded();

    /**
     * 是否需要显示顶部栏
     */
    protected final void needHeader(boolean isNeed) {
        if (isNeed) {
            mHeadLayout.setVisibility(View.VISIBLE);
        } else {
            mHeadLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 设置页面title
     */
    @Override
    public final void setTitle(int res) {
        if (res > 0) {
            setTitle(getResources().getText(res));
        } else {
            mTvTitle.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 设置页面title
     */
    @Override
    public void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) {
            mTvTitle.setText(title);
            mTvTitle.setVisibility(View.VISIBLE);
        } else {
            mTvTitle.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 设置返回不可见
     */
    public void setBackVisibility(int visibility) {
        mLeftFl.setVisibility(visibility);
    }

    /**
     * 是否支持全屏
     */
    protected boolean supportFullScreen() {
        return false;
    }

    /**
     * 设置状态栏背景
     */
    protected void setStatusBarStyle() {
        if (!isImmersionBarEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果不是沉浸式,就设置为黑色字体
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            return;
        }
        mImmersionBar = ImmersionBar.with(this);
        mImmersionBar.statusBarDarkFont(true).navigationBarColor(R.color.black).init();
    }

    /**
     * 是否可以使用沉浸式
     */
    protected boolean isImmersionBarEnabled() {
        return false;
    }

    /**
     * 设置状态栏背景色资源id
     */
    protected int getStatusBarColorResId() {
        return R.color.white;
    }

    /**
     * 设置状态栏背景色
     */
    protected int getStatusBarColor() {
        if (Build.VERSION.SDK_INT > 22) {
            return getColor(getStatusBarColorResId());
        } else {
            return getResources().getColor(getStatusBarColorResId());
        }
    }

    /**
     * 显示请求网络数据进度条
     */
    public void showLoadingDialog() {
        try {
            if (!isFinishing() && mDialogLoading != null && !mDialogLoading.isShowing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mDialogLoading.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 关闭请求网络数据进度条
     */
    public void dismissLoadingDialog() {
        try {
            if (mDialogLoading != null && mDialogLoading.isShowing()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mDialogLoading.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置title右边文字
     */
    protected void setRightText(int resourceId) {
        if (resourceId > 0) {
            mRightTv.setVisibility(View.VISIBLE);
            mRightTv.setText(resourceId);
        }
    }

    /**
     * 获取UserId
     */
    public String getUserId() {
        return String.valueOf(AppManager.getInstance().getUserInfo().t_id);
    }

    protected void receiveGift(SocketResponse response){
        try {
            //处于前台的activity才显示动画
            if (mIsActivityFront) {
                ReceiveFloatingBean bean = JSON.parseObject(response.sourceData, ReceiveFloatingBean.class);
                FloatingManagerOne.receiveGift(BaseActivity.this, bean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    OnCommonListener<SocketResponse> baseSubscribe = new OnCommonListener<SocketResponse>() {
        @Override
        public void execute(SocketResponse response) {
            switch (response.mid) {
                case Mid.RECEIVE_GIFT:
                    receiveGift(response);
                    break;
            }

        }
    };

}