package com.yiliaodemo.chat.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.yiliaodemo.chat.BuildConfig;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.DownloadBean;
import com.yiliaodemo.chat.bean.UnReadBean;
import com.yiliaodemo.chat.bean.UnReadMessageBean;
import com.yiliaodemo.chat.bean.UpdateBean;
import com.yiliaodemo.chat.bean.UserCenterBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.dialog.CloseYoungModeDialog;
import com.yiliaodemo.chat.dialog.FreeImDialog;
import com.yiliaodemo.chat.dialog.SetYoungModeDialog;
import com.yiliaodemo.chat.fragment.FindFragment;
import com.yiliaodemo.chat.fragment.HomeFragment;
import com.yiliaodemo.chat.fragment.MessageFragment;
import com.yiliaodemo.chat.fragment.MineFragment;
import com.yiliaodemo.chat.fragment.RankGroupFragment;
import com.yiliaodemo.chat.helper.IMFilterHelper;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.helper.LocationHelper;
import com.yiliaodemo.chat.helper.MessageHelper;
import com.yiliaodemo.chat.helper.RingVibrateManager;
import com.yiliaodemo.chat.helper.SharedPreferenceHelper;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.BadgeNumberUtil;
import com.yiliaodemo.chat.util.LogUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.util.permission.PermissionUtil;
import com.yiliaodemo.chat.util.permission.floating.IBgStartImpl;
import com.yiliaodemo.chat.util.permission.floating.api.PermissionLisenter;
import com.yiliaodemo.chat.view.tab.FragmentParamBuilder;
import com.yiliaodemo.chat.view.tab.MainMineTabViewHolder;
import com.yiliaodemo.chat.view.tab.MainMsgTabViewHolder;
import com.yiliaodemo.chat.view.tab.MainTabViewHolder;
import com.yiliaodemo.chat.view.tab.TabFragmentAdapter;
import com.yiliaodemo.chat.view.tab.TabPagerLayout;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMMessageListener;
import com.zhy.http.okhttp.OkHttpUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import cn.jpush.android.api.JPushInterface;
import okhttp3.Call;

/*
 * Copyright (C) 2018
 * ????????????
 *
 * ?????????????????????
 * ?????????
 * ???????????????2018/6/14
 *
 * ????????????
 * ???????????????
 * ????????????
 */
public class MainActivity extends BaseActivity implements TIMMessageListener {

    @BindView(R.id.content_vp)
    ViewPager mContentVp;

    @BindView(R.id.tabpager)
    TabPagerLayout tabPagerLayout;

//    @BindView(R.id.reward_view)
//    RewardView rewardView;

    MainMsgTabViewHolder mainMsgTabViewHolder;
    MainMineTabViewHolder mainMineTabViewHolder;


    //?????????????????????
    private int mSystemMessageCount;

    private UnReadBean<UnReadMessageBean> unReadBean;

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_main);
    }

    @Override
    protected boolean isImmersionBarEnabled() {
        return true;
    }

    @Override
    protected void onContentAdded() {

        needHeader(false);
        initIm();
        initViewPager();
        checkUpdate();

        LocationHelper.get().startLocation();

        if (!LocationHelper.get().isHasPerMission()) {
            PermissionUtil.requestPermissions(this, new PermissionUtil.OnPermissionListener() {

                @Override
                public void onPermissionGranted() {

                }

                @Override
                public void onPermissionDenied() {
//                    ToastUtil.showToast("????????????: ????????????????????????????????????????????????");
                }

            }, PermissionUtil.locationPermission);
        }

        IMFilterHelper.getInstance().updateImFilterWord();
        AppManager.getInstance().startService();
        new FreeImDialog(this).show();
        if (!BuildConfig.DEBUG) {
            new IBgStartImpl().requestStartPermission(this, new PermissionLisenter() {

                @Override
                public void onGranted() {
                }

                @Override
                public void cancel() {
                }

                @Override
                public void onDenied() {
                    ToastUtil.showToast("??????????????????????????????????????????");
                }
            });
        }
//        rewardView.setActivity(mContext);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        rewardView.getData();
        //??????
        if (JPushInterface.isPushStopped(getApplicationContext())) {
            JPushInterface.resumePush(getApplicationContext());
        }
    }

    //-------------------------------------????????????----------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LocationHelper.get().onRequestPermissionsResult();
    }

    private void initIm() {

        //?????????????????????
        TIMManager.getInstance().addMessageListener(this);

        //????????????????????????
        String saveAlisa = SharedPreferenceHelper.getJPushAlias(mContext);
        if (TextUtils.isEmpty(saveAlisa) || !saveAlisa.equals(getUserId())) {
            JPushInterface.setAlias(mContext, 1, getUserId());
        }
    }

    /**
     * ?????????viewPager
     */
    private void initViewPager() {

       TabFragmentAdapter adapter = new TabFragmentAdapter(getSupportFragmentManager(), mContentVp);

        adapter.init(

                FragmentParamBuilder.create()
                        .withClazz(HomeFragment.class)
                        .withName(getString(R.string.home))
                        .withViewHolder(new MainTabViewHolder(tabPagerLayout, R.drawable.selector_navigation_home_background))
                        .build(),

                FragmentParamBuilder.create()
                        .withClazz(FindFragment.class)
                        .withName(getString(R.string.dynamic))
                        .withViewHolder(new MainTabViewHolder(tabPagerLayout, R.drawable.selector_navigation_live))
                        .build(),

//                FragmentParamBuilder.create()
//                        .withClazz(RankGroupFragment.class)
//                        .withName(getString(R.string.main_rank))
//                        .withViewHolder(new MainTabViewHolder(tabPagerLayout, R.drawable.selector_navigation_home_top))
//                        .build(),
                FragmentParamBuilder.create()
                        .withClazz(MessageFragment.class)
                        .withName(getString(R.string.main_message))
                        .withViewHolder(mainMsgTabViewHolder = new MainMsgTabViewHolder(tabPagerLayout))
                        .build(),

                FragmentParamBuilder.create()
                        .withClazz(MineFragment.class)
                        .withName(getString(R.string.main_mine))
                        .withViewHolder(mainMineTabViewHolder = new MainMineTabViewHolder(tabPagerLayout))
                        .build()
        );

        //????????????????????????
        tabPagerLayout.init(mContentVp);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10086) {//??????????????????
            if (Build.VERSION.SDK_INT >= 26) {
                boolean b = getPackageManager().canRequestPackageInstalls();
                File apk = new File(Constant.UPDATE_DIR, Constant.UPDATE_APK_NAME);
                if (apk.exists() && b) {
                    installApk(apk);
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //??????????????????????????????????????????
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //---------------------------------????????????----------------------------

    /**
     * ????????????
     */
    private void checkUpdate() {
        AppManager.getInstance().refreshMyInfo(new OnCommonListener<UserCenterBean>() {
            @Override
            public void execute(UserCenterBean userCenterBean) {
                if (isFinishing() || userCenterBean == null) {
                    return;
                }
                Map<String, String> paramMap = new HashMap<>();
                paramMap.put("userId", getUserId());
                OkHttpUtils.post().url(ChatApi.GET_NEW_VERSION())
                        .addParams("param", ParamUtil.getParam(paramMap))
                        .build().execute(new AjaxCallback<BaseResponse<UpdateBean>>() {
                    @Override
                    public void onResponse(BaseResponse<UpdateBean> response, int id) {
                        if (isFinishing()) {
                            return;
                        }
                        if (response != null && response.m_istatus == NetCode.SUCCESS && response.m_object != null) {
                            UpdateBean bean = response.m_object;
                            String t_version = bean.t_version;//????????????
                            if (!TextUtils.isEmpty(t_version)) {
                                String originalVersionName = BuildConfig.VERSION_NAME;//????????????
                                if (!TextUtils.isEmpty(originalVersionName) && !originalVersionName.equals(t_version)) {
                                    showUpdateDialog(bean);
                                }
                            }
                        }
                    }
                });
            }
        });

    }

    /**
     * ????????????dialog
     */
    private void showUpdateDialog(UpdateBean bean) {
        final Dialog mDialog = new Dialog(MainActivity.this, R.style.DialogStyle_Dark_Background);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_update_layout, null);
        setUpdateDialogView(view, mDialog, bean);
        mDialog.setContentView(view);
        Point outSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(outSize);
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = outSize.x;
            window.setGravity(Gravity.CENTER); // ??????????????????dialog???????????????
        }
        mDialog.setCanceledOnTouchOutside(false);
        if (!BuildConfig.DEBUG)
            mDialog.setCancelable(false);
        if (!isFinishing()) {
            mDialog.show();
        }
    }

    private void setUpdateDialogView(View view, final Dialog mDialog, final UpdateBean bean) {
        //??????
        TextView des_tv = view.findViewById(R.id.des_tv);
        String des = bean.t_version_depict;
        if (!TextUtils.isEmpty(des)) {
            des_tv.setText(des);
        }
        //??????
        TextView title_tv = view.findViewById(R.id.title_tv);
        String version = bean.t_version;
        String content;
        if (!TextUtils.isEmpty(version)) {
            content = getResources().getString(R.string.new_version_des_one) + version;
        } else {
            content = getString(R.string.new_version_des);
        }
        title_tv.setText(content);
        //??????
        final TextView update_tv = view.findViewById(R.id.update_tv);
        update_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(bean.t_download_url)) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(bean.t_download_url);
                    intent.setData(content_url);
                    startActivity(intent);
                } else {
                    ToastUtil.showToast(getApplicationContext(), R.string.get_download_url_fail_one);
                }
            }
        });
        //??????????????????
        TextView click_tv = view.findViewById(R.id.click_tv);
        click_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDownloadUrl();
            }
        });
    }

    /**
     * ??????????????????
     */
    private void getDownloadUrl() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        OkHttpUtils.post().url(ChatApi.GET_DOLOAD_URL())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<DownloadBean>>() {
            @Override
            public void onResponse(BaseResponse<DownloadBean> response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    DownloadBean bean = response.m_object;
                    if (bean != null && !TextUtils.isEmpty(bean.t_android_download)) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse(bean.t_android_download);
                        intent.setData(content_url);
                        startActivity(intent);
                    } else {
                        ToastUtil.showToast(getApplicationContext(), R.string.get_download_url_fail);
                    }
                } else {
                    ToastUtil.showToast(getApplicationContext(), R.string.get_download_url_fail);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                ToastUtil.showToast(getApplicationContext(), R.string.get_download_url_fail);
            }
        });
    }

    //????????????
    private void installApk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //?????????7.0???????????????????????????uri?????????
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // ???????????????Activity???????????????Activity,?????????????????????
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri apkUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileProvider", apkFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile),
                    "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }

    //-----------------------????????????-------------------------
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable messageRun = new Runnable() {
        @Override
        public void run() {
            dealMessageCount(mSystemMessageCount);
        }
    };

    @Override
    public boolean onNewMessages(List<TIMMessage> list) {
        LogUtil.i("?????????TIM ?????????");

        handler.removeCallbacks(messageRun);
        handler.postDelayed(messageRun, 500);

        for (TIMMessage timMessage : list) {

            TIMConversation conversation = timMessage.getConversation();

            if (conversation == null || TextUtils.isEmpty(conversation.getPeer())) {
                continue;
            }

            //??????????????????
            if (timMessage.isSelf()) {
                continue;
            }

            //???????????????
            if (IMHelper.isGroupDismiss(timMessage)) {
                continue;
            }

            //????????????????????????
            if (IMHelper.filterC2CSex(conversation)) {
                continue;
            }

            RingVibrateManager.receiveMessage(timMessage);

        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        IMHelper.checkLogin();
        try {
            showSetYoungMode();
            dealUnReadCount();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //??????????????????
        TIMManager.getInstance().removeMessageListener(this);

        unReadBeanOnCommonListener = null;
    }

    private OnCommonListener<UnReadBean<UnReadMessageBean>> unReadBeanOnCommonListener;

    public void setUnReadBeanListener(OnCommonListener<UnReadBean<UnReadMessageBean>> listener) {
        this.unReadBeanOnCommonListener = listener;
        listener.execute(unReadBean);
    }

    /**
     * ??????????????????
     */
    public final void dealUnReadCount() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        OkHttpUtils.post().url(ChatApi.GET_UN_READ_MESSAGE())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<UnReadBean<UnReadMessageBean>>>() {
            @Override
            public void onResponse(BaseResponse<UnReadBean<UnReadMessageBean>> response, int id) {
                if (isFinishing())
                    return;
                if (response != null && response.m_istatus == NetCode.SUCCESS && response.m_object != null) {
                    unReadBean = response.m_object;
                    mSystemMessageCount = unReadBean.totalCount;
                    dealMessageCount(mSystemMessageCount);
                }
            }
        });
    }

    /**
     * ??????????????????
     */
    private void dealMessageCount(int systemCount) {
        int[] count = getTIMUnReadCount();
        int all = count[0] + count[1] + systemCount;
        mainMsgTabViewHolder.setRedCount(all);
        BadgeNumberUtil.setCount(all);
        if (unReadBean != null) {
            unReadBean.groupCount = count[1];
        }
        if (unReadBeanOnCommonListener != null) {
            unReadBeanOnCommonListener.execute(unReadBean);
        }
    }

    /**
     * ????????????
     */
    public final void resetRedPot() {
        MessageHelper.execute(new OnCommonListener() {
            @Override
            public void execute(Object o) {
                if (isFinishing())
                    return;
                dealUnReadCount();
            }
        });
    }

    /**
     * ??????IM???????????????
     */
    private int[] getTIMUnReadCount() {
        int count = 0;
        int count2 = 0;
        try {
            List<TIMConversation> conversationList = TIMManager.getInstance().getConversationList();
            if (conversationList != null && conversationList.size() > 0) {
                for (TIMConversation timConversation : conversationList) {
                    if (TextUtils.isEmpty(timConversation.getPeer())) {
                        continue;
                    }
                    if (timConversation.getType() == TIMConversationType.C2C) {
                        if (!IMHelper.filterC2CSex(timConversation)) {
                            count += timConversation.getUnreadMessageNum();
                        }
                    } else if (timConversation.getType() == TIMConversationType.Group
                            && IMHelper.isPublicGroup(timConversation.getPeer())) {
                        count2 += timConversation.getUnreadMessageNum();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new int[]{count, count2};
    }

    //---------------------------------???????????????--------------------------

    /**
     * ???????????????????????????
     */
    private void showSetYoungMode() {
        FragmentManager manager = getSupportFragmentManager();
        boolean haveShow = SharedPreferenceHelper.getHaveShow(getApplicationContext());
        if (!haveShow) {//??????????????????
            if (manager != null) {
                //???????????????
                SetYoungModeDialog dialog = (SetYoungModeDialog) manager.findFragmentByTag("SetYoungModeDialog");
                if (dialog != null && dialog.isVisible()) {
                    return;
                }
            }
            SharedPreferenceHelper.setHaveShow(getApplicationContext());
            SetYoungModeDialog dialog = new SetYoungModeDialog();
            dialog.show(getSupportFragmentManager(), "SetYoungModeDialog");
        } else {//??????????????????, ???????????????????????????????????????????????????
            String youngModePassword = SharedPreferenceHelper.getYoungPassword(getApplicationContext());
            if (!TextUtils.isEmpty(youngModePassword)) {//?????????????????????, ??????????????????
                if (manager != null) {
                    //???????????????
                    CloseYoungModeDialog dialog = (CloseYoungModeDialog) manager.findFragmentByTag("CloseYoungModeDialog");
                    if (dialog != null && dialog.isVisible()) {
                        return;
                    }
                }
                CloseYoungModeDialog dialog = new CloseYoungModeDialog();
                dialog.show(getSupportFragmentManager(), "CloseYoungModeDialog");
            } else {//?????????????????????, ?????????????????????
                if (manager != null) {
                    //????????????????????????
                    CloseYoungModeDialog dialog = (CloseYoungModeDialog) manager.findFragmentByTag("CloseYoungModeDialog");
                    if (dialog != null && dialog.isVisible()) {
                        dialog.dismiss();
                    }
                }
            }
        }
    }
}