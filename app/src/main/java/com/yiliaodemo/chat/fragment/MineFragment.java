package com.yiliaodemo.chat.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.AccountBalanceActivity;
import com.yiliaodemo.chat.activity.ActorVerifyingActivity;
import com.yiliaodemo.chat.activity.ApplyCompanyActivity;
import com.yiliaodemo.chat.activity.ApplyVerifyHandActivity;
import com.yiliaodemo.chat.activity.ChargeActivity;
import com.yiliaodemo.chat.activity.HelpCenterActivity;
import com.yiliaodemo.chat.activity.InviteActivity;
import com.yiliaodemo.chat.activity.ModifyUserInfoActivity;
import com.yiliaodemo.chat.activity.MyActorActivity;
import com.yiliaodemo.chat.activity.MyFollowActivity;
import com.yiliaodemo.chat.activity.MyInviteActivity;
import com.yiliaodemo.chat.activity.PhoneNaviActivity;
import com.yiliaodemo.chat.activity.PhoneVerifyActivity;
import com.yiliaodemo.chat.activity.SetBeautyActivity;
import com.yiliaodemo.chat.activity.SetChargeActivity;
import com.yiliaodemo.chat.activity.SettingActivity;
import com.yiliaodemo.chat.activity.UserSelfActiveActivity;
import com.yiliaodemo.chat.activity.VerifyOptionActivity;
import com.yiliaodemo.chat.activity.VipCenterActivity;
import com.yiliaodemo.chat.activity.WhoSawTaActivity;
import com.yiliaodemo.chat.activity.WithDrawActivity;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseFragment;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.ReceiveRedBean;
import com.yiliaodemo.chat.bean.RedCountBean;
import com.yiliaodemo.chat.bean.UserCenterBean;
import com.yiliaodemo.chat.bean.VerifyBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.dialog.CompanyInviteDialog;
import com.yiliaodemo.chat.glide.GlideCircleTransform;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.helper.SharedPreferenceHelper;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.CodeUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.view.recycle.AbsRecycleAdapter;
import com.yiliaodemo.chat.view.recycle.OnItemClickListener;
import com.yiliaodemo.chat.view.recycle.ViewHolder;
import com.robinhood.ticker.TickerView;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import okhttp3.Call;

/**
 * ??????
 */
public class MineFragment extends BaseFragment {

    private Unbinder unbinder;

    //????????????
    private UserCenterBean userCenterBean;

    //??????????????????
    private int actorVerifyState = -1;

    private MineMenu menuApply;

    @Override
    protected int initLayout() {
        return R.layout.fragment_mine;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //????????????????????????????????????
        if (AppManager.getInstance().getUserInfo().isSexMan()) {
            findViewById(R.id.verify_btn).setVisibility(View.GONE);
        }
        setFunction();
        refreshUser();
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
         * ???????????????????????????????????????????????????????????????
         */
        getInfo();
        getVerifyStatus();
        receiveRedPacket();
    }

    @OnClick({
            R.id.modify_btn,
            R.id.verify_btn,
            R.id.company_iv,
            R.id.my_like_btn,
            R.id.each_like_btn,
            R.id.follow_btn,
            R.id.victor_btn,
            R.id.charge_btn,
            R.id.with_draw_btn,
            R.id.gold_ll,
            R.id.get_ll,
            R.id.copy_btn,
            R.id.my_dynamic_btn
    })
    public void onClick(View view) {

        switch (view.getId()) {

            //????????????
            case R.id.my_dynamic_btn:
                startActivity(new Intent(mContext, UserSelfActiveActivity.class));
                break;

            //??????id
            case R.id.copy_btn:
                if (checkInvalidBean())
                    return;
                ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData mClipData = ClipData.newPlainText("ID", String.valueOf(userCenterBean.t_idcard));
                if (cm != null) {
                    cm.setPrimaryClip(mClipData);
                    ToastUtil.showToast(R.string.copy_success);
                }
                break;

            //????????????
            case R.id.get_ll:
                startActivity(new Intent(getActivity(), MyInviteActivity.class));
                break;

            //??????
            case R.id.gold_ll:
                startActivity(new Intent(getActivity(), AccountBalanceActivity.class));
                break;

            //??????
            case R.id.company_iv:
                if (checkInvalidBean())
                    return;
                if (!TextUtils.isEmpty(userCenterBean.guildName)) {
                    ToastUtil.showToast(getContext(), String.format(getString(R.string.belong_company), userCenterBean.guildName));
                }
                break;

            //type 0???????????? 1???????????? 2????????????
            //????????????
            case R.id.my_like_btn:
                MyFollowActivity.start(getActivity(), "????????????", 0);
                break;

            //????????????
            case R.id.follow_btn:
                MyFollowActivity.start(getActivity(), "????????????", 1);
                break;

            //????????????
            case R.id.each_like_btn:
                MyFollowActivity.start(getActivity(), "????????????", 2);
                break;

            //??????
            case R.id.victor_btn:
                if (checkInvalidBean())
                    return;
                WhoSawTaActivity.start(getActivity());
                break;

            //????????????
            case R.id.modify_btn:
                startActivity(new Intent(getActivity(), ModifyUserInfoActivity.class));
                break;

            //????????????
            case R.id.verify_btn:

                //???????????????????????????
                if (AppManager.getInstance().getUserInfo().isSexMan()) {
                    ToastUtil.showToast(getContext(), R.string.male_not);
                    return;
                }

                if (!isGetState) {
                    ToastUtil.showToast("???????????????");
                    getVerifyStatus();
                    return;
                }

                //????????????
                if (actorVerifyState == -1 || actorVerifyState == 2) {
                    startActivity(new Intent(getContext(), ApplyVerifyHandActivity.class));
                }

                //?????????
                else if (actorVerifyState == 0) {
                    startActivity(new Intent(getContext(), ActorVerifyingActivity.class));
                }

                //????????????
                else if (actorVerifyState == 1) {
                    startActivity(new Intent(getContext(), SetChargeActivity.class));
                }

                break;

            //??????
            case R.id.charge_btn:
                startActivity(new Intent(getContext(), ChargeActivity.class));
                break;

            //??????
            case R.id.with_draw_btn:
                if (checkInvalidBean())
                    return;
                if (userCenterBean.phoneIdentity == 0) {
                    new AlertDialog.Builder(mContext)
                            .setMessage("?????????????????????????????????????????????")
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PhoneVerifyActivity.start(getActivity(), userCenterBean.t_phone);
                                    dialog.dismiss();
                                }
                            }).create().show();
                    return;
                }
                startActivity(new Intent(getContext(), WithDrawActivity.class));
                break;
        }
    }

    /**
     * ??????????????????
     */
    private void setFunction() {

        final List<MineMenu> list = Arrays.asList(

                new MineMenu(R.drawable.mine_function_invite, "????????????", InviteActivity.class),
                new MineMenu(R.drawable.mine_funciton_vip, "????????????", VipCenterActivity.class),
                menuApply = new MineMenu(R.drawable.mine_funciton_apply, "????????????", VerifyOptionActivity.class),

                new MineMenu(R.drawable.mine_funciton_verify, "????????????", MyActorActivity.class),
                new MineMenu(R.drawable.mine_funciton_beauty, "????????????", SetBeautyActivity.class),
                new MineMenu(R.drawable.mine_funciton_help, "????????????", HelpCenterActivity.class),

                new MineMenu(R.drawable.mine_funciton_call, "????????????", PhoneNaviActivity.class),
                new MineMenu(R.drawable.mine_funciton_sys, "????????????", SettingActivity.class),
                new MineMenu(R.drawable.mine_funciton_bind, "????????????", PhoneVerifyActivity.class));

        RecyclerView recyclerView = findViewById(R.id.mine_rv);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
        final AbsRecycleAdapter adapter = new AbsRecycleAdapter(
                new AbsRecycleAdapter.Type(R.layout.item_mine_function, MineMenu.class)) {
            @Override
            public void convert(ViewHolder holder, Object t) {
                MineMenu bean = (MineMenu) t;
                TextView textView = (TextView) holder.itemView;
                textView.setCompoundDrawablesWithIntrinsicBounds(0, bean.drawId, 0, 0);
                textView.setText(bean.menu);
                if (bean.clazz == PhoneNaviActivity.class) {
                    textView.setTextColor(0xffff0000);
                    textView.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    textView.setTextColor(0xff868686);
                    textView.setTypeface(Typeface.DEFAULT);
                }
            }
        };
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, Object obj, int position) {
                MineMenu bean = (MineMenu) adapter.getData().get(position);
                Class clazz = bean.clazz;
                if (clazz == Object.class) {
                    CodeUtil.jumpToQQ(mContext);
                } else if (clazz == VerifyOptionActivity.class) {

                    //???????????????????????????
                    if (AppManager.getInstance().getUserInfo().isSexMan()) {
                        ToastUtil.showToast(getContext(), R.string.male_not);
                        return;
                    }

                    if (!isGetState) {
                        ToastUtil.showToast("???????????????");
                        getVerifyStatus();
                        return;
                    }

                    //??????????????????
                    if (actorVerifyState == 1) {
                        startActivity(new Intent(getContext(), SetChargeActivity.class));
                    } else {
                        startActivity(new Intent(getContext(), VerifyOptionActivity.class));
                    }

                } else if (clazz == MyActorActivity.class) {
                    if (checkInvalidBean())
                        return;
                    //?????????????????? 0.????????? 1.????????? 2.?????????
                    if (userCenterBean.isGuild == 0) {
                        showCompanyDialog();
                    } else if (userCenterBean.isGuild == 1) {
                        ToastUtil.showToast(mContext, R.string.apply_company_ing_des);
                    } else {
                        //????????????
                        if (userCenterBean.isGuild == 3) {
                            //?????????
                            ToastUtil.showToast(getContext(), R.string.company_down);
                        } else {
                            Intent intent = new Intent(getContext(), MyActorActivity.class);
                            startActivity(intent);
                        }
                    }
                } else if (clazz == PhoneVerifyActivity.class) {
                    if (checkInvalidBean())
                        return;
                    PhoneVerifyActivity.start(getActivity(), userCenterBean.t_phone);
                } else {
                    startActivity(new Intent(mContext, clazz));
                }
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.setDatas(list);
    }

    /**
     * ????????????
     */
    private void showCompanyDialog() {
        final Dialog mDialog = new Dialog(mContext, R.style.DialogStyle_Dark_Background);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_connect_qq_layout, null);
        setDialogView(view, mDialog);
        mDialog.setContentView(view);
        Point outSize = new Point();
        mContext.getWindowManager().getDefaultDisplay().getSize(outSize);
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = outSize.x;
            window.setGravity(Gravity.CENTER); // ??????????????????dialog???????????????
        }
        mDialog.setCanceledOnTouchOutside(true);
        if (!mContext.isFinishing()) {
            mDialog.show();
        }
    }

    /**
     * ??????view
     */
    private void setDialogView(View view, final Dialog mDialog) {
        //??????
        ImageView cancel_iv = view.findViewById(R.id.cancel_iv);
        cancel_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        //??????
        TextView confirm_tv = view.findViewById(R.id.confirm_tv);
        confirm_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ApplyCompanyActivity.class);
                startActivity(intent);
                mDialog.dismiss();
            }
        });
    }

    /**
     * ????????????????????????
     * ???????????????
     */
    private boolean isGetState;

    private void getVerifyStatus() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", mContext.getUserId());
        OkHttpUtils.post().url(ChatApi.GET_VERIFY_STATUS())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<VerifyBean>>() {
            @Override
            public void onResponse(BaseResponse<VerifyBean> response, int id) {

                if (getActivity() == null || getActivity().isFinishing() || getView() == null)
                    return;

                // ???bean == null ?????????) ->state=-1  0.?????????  1.???????????? 2.????????????
                if (response != null) {

                    int state = -1;
                    if (response.m_object != null) {
                        state = response.m_object.t_certification_type;
                    }

                    if (state != actorVerifyState) {

                        actorVerifyState = state;

                        int[] stringIds = {R.string.apply_actor, R.string.actor_ing, R.string.set_money, R.string.apply_actor};
                        String text = getString(stringIds[++state]);
                        ((TextView) getView().findViewById(R.id.verify_btn)).setText(text);
                        menuApply.menu = text;
                        RecyclerView recyclerView = findViewById(R.id.mine_rv);
                        if (recyclerView.getAdapter() != null) {
                            recyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }

                }

                isGetState = true;
            }
        });
    }

    /**
     * ??????????????????
     */
    private void refreshUser() {

        if (getView() == null)
            return;

        //??????
        ((TextView) getView().findViewById(R.id.nick_name_tv))
                .setText(AppManager.getInstance().getUserInfo().t_nickName);

        //Vip/SVip
        boolean isVip = AppManager.getInstance().getUserInfo().isVip();
        getView().findViewById(R.id.v_iv).setVisibility(isVip ? View.VISIBLE : View.GONE);

        TextView ageTv = getView().findViewById(R.id.age_tv);

        //??????
        ageTv.setSelected(AppManager.getInstance().getUserInfo().isSexMan());

        //??????
        Glide.with(mContext)
                .load(AppManager.getInstance().getUserInfo().headUrl)
                .error(R.drawable.default_head)
                .transform(new GlideCircleTransform(mContext))
                .into((ImageView) getView().findViewById(R.id.head_iv));

        if (userCenterBean != null) {

            //??????????????????  1 = ??????
            if (AppManager.getInstance().getUserInfo().t_role != userCenterBean.t_role) {
                AppManager.getInstance().getUserInfo().t_role = userCenterBean.t_role;
                SharedPreferenceHelper.saveRoleInfo(mContext, userCenterBean.t_role);
            }

            //??????
            ageTv.setText(String.valueOf(userCenterBean.t_age));

            //????????????
            String sign = TextUtils.isEmpty(userCenterBean.t_autograph) ?
                    getString(R.string.lazy) : userCenterBean.t_autograph;
            ((TextView) getView().findViewById(R.id.sign_tv)).setText(sign);

            //ID
            ((TextView) getView().findViewById(R.id.id_tv))
                    .setText(String.format("1v1Demo???: %s", userCenterBean.t_idcard));

            //????????????
            ((TextView) getView().findViewById(R.id.fan_tv)).setText(String.valueOf(userCenterBean.likeMeCount));

            //????????????
            ((TextView) getView().findViewById(R.id.each_like_tv)).setText(String.valueOf(userCenterBean.eachLikeCount));

            //????????????
            ((TextView) getView().findViewById(R.id.follow_tv)).setText(String.valueOf(userCenterBean.myLikeCount));

            //????????????
            ((TextView) getView().findViewById(R.id.victor_tv)).setText(String.valueOf(userCenterBean.browerCount));

            //????????????
            ((TextView) getView().findViewById(R.id.my_dynamic_tv)).setText(String.valueOf(userCenterBean.dynamCount));

            //??????
            setTicker((TickerView) getView().findViewById(R.id.gold_tv), userCenterBean.amount);

            //??????
            setTicker((TickerView) getView().findViewById(R.id.gold_get_tv), userCenterBean.extractGold);

            //??????TIM??????
            IMHelper.checkTIMInfo(userCenterBean.nickName, userCenterBean.handImg);

            //????????????
            refreshSwitch();
        }
    }

    /**
     * ????????????
     */
    private void refreshSwitch() {

        View switchVideo = findViewById(R.id.video_chat_iv);
        View switchText = findViewById(R.id.im_chat_iv);
        View switchFloat = findViewById(R.id.disable_slide_iv);
        View switchRank = findViewById(R.id.disable_rank_iv);

        TextView videoTv = findViewById(R.id.video_chat_tv);
        TextView textTv = findViewById(R.id.im_chat_tv);
        TextView floatTv = findViewById(R.id.disable_slide_tv);
        TextView rankTv = findViewById(R.id.disable_rank_tv);

        videoTv.setText(userCenterBean.t_is_not_disturb == 1 ? "?????????????????????" : "?????????????????????");
        textTv.setText(userCenterBean.t_text_switch == 1 ? "?????????????????????" : "?????????????????????");
        floatTv.setText(userCenterBean.t_float_switch == 1 ? "?????????????????????" : "?????????????????????");
        rankTv.setText(userCenterBean.t_rank_switch == 1 ? "?????????????????????" : "?????????????????????");

        switchVideo.setSelected(userCenterBean.t_is_not_disturb == 1);
        switchText.setSelected(userCenterBean.t_text_switch == 1);
        switchFloat.setSelected(userCenterBean.t_float_switch == 1);
        switchRank.setSelected(userCenterBean.t_rank_switch == 1);

        if (switchVideo.getTag() != null) {
            return;
        }

        //chatType 1?????? 2 ?????? 3 ??????   4:???????????????  5:????????????
        switchVideo.setTag(1);
        switchText.setTag(3);
        switchFloat.setTag(5);
        switchRank.setTag(4);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                final int type = Integer.parseInt(view.getTag().toString());
                final boolean isSelected = !view.isSelected();
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("userId", mContext.getUserId());
                paramMap.put("chatType", type);
                paramMap.put("switchType", isSelected ? 1 : 0);
                OkHttpUtils.post().url(ChatApi.setUpChatSwitch())
                        .addParams("param", ParamUtil.getParam(paramMap))
                        .build().execute(
                        new AjaxCallback<BaseResponse<String>>() {

                            @Override
                            public void onResponse(BaseResponse response, int id) {
                                if (getActivity() == null || getActivity().isFinishing())
                                    return;
                                if (response != null) {
                                    if (response.m_istatus == NetCode.SUCCESS) {
                                        view.setSelected(isSelected);
                                        if (view.getId() == R.id.video_chat_iv) {
                                            videoTv.setText(view.isSelected() ? "?????????????????????" : "?????????????????????");
                                        } else if (view.getId() == R.id.im_chat_tv) {
                                            textTv.setText(view.isSelected() ? "?????????????????????" : "?????????????????????");
                                        } else if (view.getId() == R.id.disable_slide_iv) {
                                            floatTv.setText(view.isSelected() ? "?????????????????????" : "?????????????????????");
                                        } else if (view.getId() == R.id.disable_rank_iv) {
                                            rankTv.setText(view.isSelected() ? "?????????????????????" : "?????????????????????");
                                        }
                                    } else {
                                        ToastUtil.showToast(mContext, response.m_strMessage);
                                    }
                                }
                            }

                            @Override
                            public void onError(Call call, Exception e, int id) {
                                if (getActivity() == null || getActivity().isFinishing())
                                    return;
                                mContext.dismissLoadingDialog();
                            }
                        });
            }
        };
        switchVideo.setOnClickListener(onClickListener);
        switchText.setOnClickListener(onClickListener);
        switchFloat.setOnClickListener(onClickListener);
        switchRank.setOnClickListener(onClickListener);
    }

    /**
     * ????????????
     */
    private void setTicker(TickerView tickerView, int number) {
        int currentNumber = 0;
        try {
            if (!TextUtils.isEmpty(tickerView.getText()))
                currentNumber = Integer.parseInt(tickerView.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (number == currentNumber)
            return;
        if (number > currentNumber) {
            tickerView.setPreferredScrollingDirection(TickerView.ScrollingDirection.UP);
        } else {
            tickerView.setPreferredScrollingDirection(TickerView.ScrollingDirection.DOWN);
        }
        tickerView.setText("" + number);
    }

    /**
     * ?????????????????????
     */
    private void receiveRedPacket() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        OkHttpUtils.post().url(ChatApi.GET_RED_PACKET_COUNT())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<RedCountBean>>() {
            @Override
            public void onResponse(BaseResponse<RedCountBean> response, int id) {
                if (getActivity() == null || getActivity().isFinishing())
                    return;
                if (response != null && response.m_istatus == NetCode.SUCCESS && response.m_object != null) {
                    if (response.m_object.total > 0) {
                        Map<String, String> paramMap = new HashMap<>();
                        paramMap.put("userId", mContext.getUserId());
                        OkHttpUtils.post().url(ChatApi.RECEIVE_RED_PACKET())
                                .addParams("param", ParamUtil.getParam(paramMap))
                                .build().execute(new AjaxCallback<BaseResponse<ReceiveRedBean>>() {
                            @Override
                            public void onResponse(BaseResponse<ReceiveRedBean> response, int id) {
                                if (getActivity() == null || getActivity().isFinishing())
                                    return;
                                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                                    getInfo();
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * ??????bean????????????
     */
    private boolean checkInvalidBean() {
        boolean b = userCenterBean == null;
        if (b) {
            ToastUtil.showToast("???????????????");
            getInfo();
        }
        return b;
    }

    /**
     * ????????????????????????
     */
    private void getInfo() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        OkHttpUtils.post().url(ChatApi.INDEX())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<UserCenterBean>>() {
            @Override
            public void onResponse(BaseResponse<UserCenterBean> response, int id) {
                if (getActivity() == null || getActivity().isFinishing() || getView() == null)
                    return;
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    UserCenterBean bean = response.m_object;
                    userCenterBean = bean;
                    if (bean != null) {

                        //??????vip??????
                        SharedPreferenceHelper.saveUserVip(mContext, bean.t_is_vip);

                        //????????????
                        String imgUrl = SharedPreferenceHelper.getAccountInfo(mContext).headUrl;
                        if (TextUtils.isEmpty(imgUrl) || !imgUrl.equals(bean.handImg)) {
                            SharedPreferenceHelper.saveHeadImgUrl(mContext, bean.handImg);
                            if (!TextUtils.isEmpty(bean.handImg)) {
                                AppManager.getInstance().getUserInfo().headUrl = bean.handImg;
                            }
                        }

                        //????????????
                        String nickName = bean.nickName;
                        String saveNick = SharedPreferenceHelper.getAccountInfo(mContext).t_nickName;
                        if (TextUtils.isEmpty(saveNick) || !saveNick.equals(nickName)) {
                            SharedPreferenceHelper.saveUserNickName(mContext, nickName);
                            AppManager.getInstance().getUserInfo().t_nickName = nickName;
                        }

                        //???????????????????????????????????? 	?????????????????? 0.????????? 1.?????????
                        getView().findViewById(R.id.company_iv).setVisibility(bean.isApplyGuild == 0 ? View.GONE : View.VISIBLE);
                        if (bean.isApplyGuild == 0) {
                            CompanyInviteDialog.getCompanyInvite(getActivity());
                        }

                        //??????UI
                        refreshUser();
                    }
                }
            }
        });
    }

    private static class MineMenu {

        int drawId;
        String menu;
        Class clazz;

        public MineMenu(int drawId, String menu, Class clazz) {
            this.drawId = drawId;
            this.menu = menu;
            this.clazz = clazz;
        }
    }
}