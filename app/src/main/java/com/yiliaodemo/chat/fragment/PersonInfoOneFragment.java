package com.yiliaodemo.chat.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.CloseRankActivity;
import com.yiliaodemo.chat.activity.GiftPackActivity;
import com.yiliaodemo.chat.adapter.CloseGiftRecyclerAdapter;
import com.yiliaodemo.chat.adapter.InfoCommentRecyclerAdapter;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseListResponse;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.base.LazyFragment;
import com.yiliaodemo.chat.bean.ActorInfoBean;
import com.yiliaodemo.chat.bean.ChargeBean;
import com.yiliaodemo.chat.bean.ChatUserInfo;
import com.yiliaodemo.chat.bean.CommentBean;
import com.yiliaodemo.chat.bean.CoverUrlBean;
import com.yiliaodemo.chat.bean.InfoRoomBean;
import com.yiliaodemo.chat.bean.IntimateBean;
import com.yiliaodemo.chat.bean.IntimateDetailBean;
import com.yiliaodemo.chat.bean.LabelBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.helper.ChargeHelper;
import com.yiliaodemo.chat.helper.SharedPreferenceHelper;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Call;

/*
 * Copyright (C) 2018
 * ????????????
 *
 * ??????????????????????????????????????????Fragment One
 * ?????????
 * ???????????????2018/6/21
 *
 * ????????????
 * ???????????????
 * ????????????
 */
public class PersonInfoOneFragment extends LazyFragment implements View.OnClickListener {

    public PersonInfoOneFragment() {

    }

    protected Activity mActivity;

    //??????
    private TextView mCityTv;
    //?????????
    private TextView mRateTv;
    //??????
    private TextView mStarTv;
    //??????
    private TextView mWeightTv;
    //??????
    private TextView mHighTv;
    //ID
    private TextView mIdCardTv;
    //????????????
    private TextView mLastTimeTv;

    //????????????
    private RelativeLayout mCloseRl;
    private ImageView mCloseIv;
    private TextView mCloseTv;
    private RecyclerView mCloseRv;
    //????????????
    private RelativeLayout mGiftRl;
    private ImageView mGiftIv;
    private TextView mGiftTv;
    private RecyclerView mGiftRv;
    //????????????
    private LinearLayout mSelfTagLl;
    //??????
    private TextView mNoCommentTv;
    private RecyclerView mCommentRv;
    //ID
    private int mActorId;
    private ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> mActorInfoBean;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        View view = LayoutInflater.from(mActivity).inflate(R.layout.fragment_person_info_one_layout,
                container, false);
        initView(view);
        return view;
    }

    /**
     * ?????????
     */
    protected void initView(View view) {
        mCityTv = view.findViewById(R.id.city_tv);
        mRateTv = view.findViewById(R.id.rate_tv);
        mStarTv = view.findViewById(R.id.star_tv);
        mWeightTv = view.findViewById(R.id.weight_tv);
        mHighTv = view.findViewById(R.id.high_tv);
        mIdCardTv = view.findViewById(R.id.id_card_tv);
        mLastTimeTv = view.findViewById(R.id.last_time_tv);
        mCloseRl = view.findViewById(R.id.close_rl);
        mCloseIv = view.findViewById(R.id.close_iv);
        mCloseTv = view.findViewById(R.id.close_tv);
        mCloseRv = view.findViewById(R.id.close_rv);
        mGiftRl = view.findViewById(R.id.gift_rl);
        mGiftIv = view.findViewById(R.id.gift_iv);
        mGiftTv = view.findViewById(R.id.gift_tv);
        mGiftRv = view.findViewById(R.id.gift_rv);
        mSelfTagLl = view.findViewById(R.id.self_tag_ll);
        mNoCommentTv = view.findViewById(R.id.no_comment_tv);
        mCommentRv = view.findViewById(R.id.comment_rv);
        mCommentRv.setNestedScrollingEnabled(false);
        mCloseRl.setOnClickListener(this);
        mGiftRl.setOnClickListener(this);
        mIsViewPrepared = true;
    }

    @Override
    protected void onFirstVisibleToUser() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            mActorId = bundle.getInt(Constant.ACTOR_ID);
            getIntimateAndGift(mActorId);
            getUserComment(mActorId);
        }
        mIsDataLoadCompleted = true;
    }

    /**
     * ????????????
     */
    public void loadData(ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> bean) {
        mActorInfoBean = bean;
        //??????
        mCityTv.setText(bean.t_city);
        //?????????
        mRateTv.setText(bean.t_reception);
        //??????
        mStarTv.setText(bean.t_constellation);
        //??????
        String weight = bean.t_weight + getString(R.string.body_des);
        mWeightTv.setText(weight);
        //??????
        String high = bean.t_height + getString(R.string.high_des);
        mHighTv.setText(high);
        //ID
        String id = getString(R.string.chat_number_one) + bean.t_idcard;
        mIdCardTv.setText(id);
        //????????????
        String lastTime = getString(R.string.last_time_des) + bean.t_login_time;
        mLastTimeTv.setText(lastTime);

        //????????????
        List<LabelBean> tagBeans = bean.lable;
        if (tagBeans != null && tagBeans.size() > 0) {
            setLabelView(tagBeans);
        }
    }

    /**
     * ????????????????????????
     */
    private void getIntimateAndGift(int actorId) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", String.valueOf(actorId));
        OkHttpUtils.post().url(ChatApi.GET_INITMATE_AND_GIFT())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<IntimateBean<IntimateDetailBean>>>() {
            @Override
            public void onResponse(BaseResponse<IntimateBean<IntimateDetailBean>> response, int id) {
                if (mActivity.isFinishing()) {
                    return;
                }
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    IntimateBean<IntimateDetailBean> bean = response.m_object;
                    if (bean != null) {
                        List<IntimateDetailBean> intimates = bean.intimates;
                        List<IntimateDetailBean> gifts = bean.gifts;
                        //?????????
                        if (intimates != null && intimates.size() > 0) {
                            mCloseTv.setVisibility(View.VISIBLE);
                            mCloseRl.setVisibility(View.VISIBLE);
                            int intimate = 0;//??????
                            setRecyclerView(mCloseRv, intimates, intimate);
                            if (intimates.size() >= 6) {
                                mCloseIv.setVisibility(View.VISIBLE);
                            }
                        } else {
                            mCloseTv.setVisibility(View.GONE);
                            mCloseRl.setVisibility(View.GONE);
                        }
                        //?????????
                        if (gifts != null && gifts.size() > 0) {
                            mGiftTv.setVisibility(View.VISIBLE);
                            mGiftRl.setVisibility(View.VISIBLE);
                            int gift = 1;//??????
                            setRecyclerView(mGiftRv, gifts, gift);
                            if (gifts.size() >= 6) {
                                mGiftIv.setVisibility(View.VISIBLE);
                            }
                        } else {
                            mGiftTv.setVisibility(View.GONE);
                            mGiftRl.setVisibility(View.GONE);
                        }
                    }
                }
            }
        });
    }

    /**
     * ??????RecyclerView
     */
    private void setRecyclerView(RecyclerView recyclerView, List<IntimateDetailBean> beans, int type) {
        GridLayoutManager manager = new GridLayoutManager(mActivity, 6);
        recyclerView.setLayoutManager(manager);
        CloseGiftRecyclerAdapter adapter = new CloseGiftRecyclerAdapter(mActivity, type);
        recyclerView.setAdapter(adapter);
        adapter.loadData(beans);
        adapter.setOnItemClickListener(new CloseGiftRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int type) {
                if (type == 0) {//??????
                    if (mCloseIv.getVisibility() == View.VISIBLE) {
                        if (mActorId > 0) {
                            Intent intent = new Intent(mActivity, CloseRankActivity.class);
                            intent.putExtra(Constant.ACTOR_ID, mActorId);
                            startActivity(intent);
                        }
                    }
                } else {
                    if (mGiftIv.getVisibility() == View.VISIBLE) {
                        if (mActorId > 0) {
                            Intent intent = new Intent(mActivity, GiftPackActivity.class);
                            intent.putExtra(Constant.ACTOR_ID, mActorId);
                            startActivity(intent);
                        }
                    }
                }
            }
        });
    }

    /**
     * ??????????????????
     */
    private void getUserComment(int actorId) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", String.valueOf(actorId));
        OkHttpUtils.post().url(ChatApi.GET_EVALUATION_LIST())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseListResponse<CommentBean>>() {
            @Override
            public void onResponse(BaseListResponse<CommentBean> response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    List<CommentBean> commentBeans = response.m_object;
                    if (commentBeans != null && commentBeans.size() > 0) {
                        List<CommentBean> loadBeans;
                        if (commentBeans.size() >= 10) {
                            loadBeans = commentBeans.subList(0, 10);
                        } else {
                            loadBeans = commentBeans;
                        }
                        mNoCommentTv.setVisibility(View.GONE);
                        mCommentRv.setVisibility(View.VISIBLE);
                        LinearLayoutManager manager = new LinearLayoutManager(mActivity);
                        mCommentRv.setLayoutManager(manager);
                        InfoCommentRecyclerAdapter adapter = new InfoCommentRecyclerAdapter(mActivity);
                        mCommentRv.setAdapter(adapter);
                        adapter.loadData(loadBeans);
                    } else {
                        mNoCommentTv.setVisibility(View.VISIBLE);
                        mCommentRv.setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                mNoCommentTv.setVisibility(View.VISIBLE);
                mCommentRv.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * ????????????View
     */
    private void setLabelView(List<LabelBean> labelBeans) {
        //????????????
        mSelfTagLl.removeAllViews();
        int[] backs = {R.drawable.shape_tag_one, R.drawable.shape_tag_two, R.drawable.shape_tag_three};
        if (labelBeans != null && labelBeans.size() > 0) {
            for (int i = 0; i < labelBeans.size(); i++) {
                @SuppressLint("InflateParams")
                View view = LayoutInflater.from(getContext()).inflate(R.layout.item_tag_user_info_layout, null);
                TextView textView = view.findViewById(R.id.content_tv);
                textView.setText(labelBeans.get(i).t_label_name);
                Random random = new Random();
                int index = random.nextInt(backs.length);
                textView.setBackgroundResource(backs[index]);
                if (i != 0) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = 20;
                    textView.setLayoutParams(params);
                }
                mSelfTagLl.addView(textView);
            }
            if (mSelfTagLl.getChildCount() > 0) {
                mSelfTagLl.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_rl: {//?????????
                if (mActorId > 0) {
                    Intent intent = new Intent(mActivity, CloseRankActivity.class);
                    intent.putExtra(Constant.ACTOR_ID, mActorId);
                    startActivity(intent);
                }
                break;
            }
            case R.id.gift_rl: {//?????????
                if (mActorId > 0) {
                    Intent intent = new Intent(mActivity, GiftPackActivity.class);
                    intent.putExtra(Constant.ACTOR_ID, mActorId);
                    startActivity(intent);
                }
                break;
            }
//            case R.id.see_wechat_tv: {//????????????
//                if (mActorInfoBean == null) {
//                    return;
//                }
//                if (getUserSex() == mActorInfoBean.t_sex) {
//                    ToastUtil.showToast(mActivity, R.string.sex_can_not_communicate);
//                    return;
//                }
//                int weChat = 0;
//                showSeeWeChatRemindDialog(weChat);
//                break;
//            }
//            case R.id.see_phone_tv: {//????????????
//                if (mActorInfoBean == null) {
//                    return;
//                }
//                if (getUserSex() == mActorInfoBean.t_sex) {
//                    ToastUtil.showToast(mActivity, R.string.sex_can_not_communicate);
//                    return;
//                }
//                int phone = 1;
//                showSeeWeChatRemindDialog(phone);
//                break;
//            }
        }
    }

    /**
     * ???????????????????????????
     */
    private void showSeeWeChatRemindDialog(int position) {
        final Dialog mDialog = new Dialog(mActivity, R.style.DialogStyle_Dark_Background);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_see_we_chat_number_layout, null);
        setDialogView(view, mDialog, position);
        mDialog.setContentView(view);
        Point outSize = new Point();
        mActivity.getWindowManager().getDefaultDisplay().getSize(outSize);
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = outSize.x;
            window.setGravity(Gravity.CENTER); // ??????????????????dialog???????????????
        }
        mDialog.setCanceledOnTouchOutside(false);
        if (!mActivity.isFinishing()) {
            mDialog.show();
        }
    }

    /**
     * ???????????????????????????view
     */
    private void setDialogView(View view, final Dialog mDialog, final int position) {
        //??????
        TextView see_des_tv = view.findViewById(R.id.see_des_tv);
        if (mActorInfoBean != null) {
            if (mActorInfoBean.anchorSetup != null && mActorInfoBean.anchorSetup.size() > 0) {
                ChargeBean chargeBean = mActorInfoBean.anchorSetup.get(0);
                if (position == 0) {//?????????
                    String content = getResources().getString(R.string.see_we_chat_number_des)
                            + chargeBean.t_weixin_gold + getResources().getString(R.string.gold);
                    see_des_tv.setText(content);
                } else {
                    String content = getResources().getString(R.string.see_we_phone_number_des)
                            + chargeBean.t_phone_gold + getResources().getString(R.string.gold);
                    see_des_tv.setText(content);
                }
            }
        }

        //??????
        TextView cancel_tv = view.findViewById(R.id.cancel_tv);
        cancel_tv.setOnClickListener(new View.OnClickListener() {
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
                seeWeChat(position);
                mDialog.dismiss();
            }
        });
    }

    /**
     * ??????????????????
     */
    private void seeWeChat(final int position) {
        String url;
        if (position == 0) {//??????
            url = ChatApi.SEE_WEI_XIN_CONSUME();
        } else {
            url = ChatApi.SEE_PHONE_CONSUME();
        }
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("coverConsumeUserId", String.valueOf(mActorId));
        OkHttpUtils.post().url(url)
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<String>>() {
            @Override
            public void onResponse(BaseResponse<String> response, int id) {
                if (mActivity.isFinishing()) {
                    return;
                }
                if (response != null) {
                    if (response.m_istatus == NetCode.SUCCESS || response.m_istatus == 2) {
                        String message = response.m_strMessage;
                        if (!TextUtils.isEmpty(message)) {
                            ToastUtil.showToast(mActivity, message);
                        } else {
                            if (response.m_istatus == 2) {
                                ToastUtil.showToast(mActivity, R.string.vip_free);
                            } else {
                                ToastUtil.showToast(mActivity, R.string.pay_success);
                            }
                        }
//                        if (position == 0) {//??????
//                            String weChat = getString(R.string.we_chat_num_des_one) + response.m_object;
//                            mWeChatTv.setText(weChat);
//                            mSeeWechatTv.setVisibility(View.GONE);
//                        } else {
//                            String phone = getString(R.string.phone_num_one) + response.m_object;
//                            mPhoneTv.setText(phone);
//                            mSeePhoneTv.setVisibility(View.GONE);
//                        }

                    } else if (response.m_istatus == -1) {//????????????
                        ChargeHelper.showSetCoverDialog(mActivity);
                    } else {
                        ToastUtil.showToast(mActivity, R.string.system_error);
                    }
                } else {
                    ToastUtil.showToast(mActivity, R.string.system_error);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                ToastUtil.showToast(mActivity, R.string.system_error);
            }
        });
    }

    /**
     * ??????????????????
     */
    private int getUserSex() {
        if (AppManager.getInstance() != null) {
            ChatUserInfo userInfo = AppManager.getInstance().getUserInfo();
            if (userInfo != null) {
                //0.??????1.???
                int sex = userInfo.t_sex;
                return sex != 2 ? sex : 0;
            } else {
                int sex = SharedPreferenceHelper.getAccountInfo(mActivity.getApplicationContext()).t_sex;
                return sex != 2 ? sex : 0;
            }
        }
        return 0;
    }

    /**
     * ??????UserId
     */
    private String getUserId() {
        String sUserId = "";
        if (AppManager.getInstance() != null) {
            ChatUserInfo userInfo = AppManager.getInstance().getUserInfo();
            if (userInfo != null) {
                int userId = userInfo.t_id;
                if (userId >= 0) {
                    sUserId = String.valueOf(userId);
                }
            } else {
                int id = SharedPreferenceHelper.getAccountInfo(mActivity.getApplicationContext()).t_id;
                sUserId = String.valueOf(id);
            }
        }
        return sUserId;
    }


}
