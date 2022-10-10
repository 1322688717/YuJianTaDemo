package com.yiliaodemo.chat.im;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.opensource.svgaplayer.SVGADrawable;
import com.opensource.svgaplayer.SVGAImageView;
import com.opensource.svgaplayer.SVGAParser;
import com.opensource.svgaplayer.SVGAVideoEntity;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.PersonInfoActivity;
import com.yiliaodemo.chat.activity.ReportActivity;
import com.yiliaodemo.chat.activity.VipCenterActivity;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.ActorInfoBean;
import com.yiliaodemo.chat.bean.AudioUserBean;
import com.yiliaodemo.chat.bean.ChargeBean;
import com.yiliaodemo.chat.bean.CoverUrlBean;
import com.yiliaodemo.chat.bean.CustomMessageBean;
import com.yiliaodemo.chat.bean.InfoRoomBean;
import com.yiliaodemo.chat.bean.LabelBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.dialog.GiftDialog;
import com.yiliaodemo.chat.dialog.InputRemarkDialog;
import com.yiliaodemo.chat.dialog.LookNumberDialog;
import com.yiliaodemo.chat.dialog.ProtectDialog;
import com.yiliaodemo.chat.dialog.VipDialog;
import com.yiliaodemo.chat.helper.ChargeHelper;
import com.yiliaodemo.chat.helper.IMFilterHelper;
import com.yiliaodemo.chat.helper.ImageHelper;
import com.yiliaodemo.chat.helper.SharedPreferenceHelper;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.AudioVideoRequester;
import com.yiliaodemo.chat.net.BlackRequester;
import com.yiliaodemo.chat.net.FocusRequester;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.DensityUtil;
import com.yiliaodemo.chat.util.LogUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.tencent.imsdk.TIMCallBack;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMCustomElem;
import com.tencent.imsdk.TIMElem;
import com.tencent.imsdk.TIMElemType;
import com.tencent.imsdk.TIMFriendshipManager;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMMessageListener;
import com.tencent.imsdk.friendship.TIMFriend;
import com.tencent.qcloud.tim.uikit.base.BaseFragment;
import com.tencent.qcloud.tim.uikit.component.AudioPlayer;
import com.tencent.qcloud.tim.uikit.component.TitleBarLayout;
import com.tencent.qcloud.tim.uikit.modules.chat.ChatLayout;
import com.tencent.qcloud.tim.uikit.modules.chat.base.ChatInfo;
import com.tencent.qcloud.tim.uikit.modules.chat.interfaces.ISend;
import com.tencent.qcloud.tim.uikit.modules.chat.interfaces.OnSend;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.ImCustomMessage;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.MessageLayout;
import com.tencent.qcloud.tim.uikit.modules.conversation.ConversationManagerKit;
import com.tencent.qcloud.tim.uikit.modules.message.MessageInfo;
import com.tencent.qcloud.tim.uikit.modules.message.MessageInfoUtil;
import com.zhihu.matisse.Matisse;
import com.zhy.http.okhttp.OkHttpUtils;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

public class ChatFragment extends BaseFragment {

    private View mBaseView;
    private ChatLayout mChatLayout;
    private ChatInfo mChatInfo;
    private TitleBarLayout mTitleBar;
    private SVGAImageView mGifSv;
    private boolean isFollow;
    private List<String> topConversation;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.chat_fragment, container, false);
        return mBaseView;
    }

    private void initView() {

        mGifSv = mBaseView.findViewById(R.id.gif_sv);

        //从布局文件中获取聊天面板组件
        mChatLayout = mBaseView.findViewById(R.id.chat_layout);

        //单聊组件的默认UI和交互初始化
        mChatLayout.initDefault();

        //查询是否有备注
        TIMFriend timFriend = TIMFriendshipManager.getInstance().queryFriend(mChatInfo.getId());
        if (timFriend != null && !TextUtils.isEmpty(timFriend.getRemark())) {
            mChatInfo.setChatName(timFriend.getRemark());
        }

        //需要聊天的基本信息
        mChatLayout.setChatInfo(mChatInfo);

        //设置文字过滤
        mChatLayout.getInputLayout().setImFilter(IMFilterHelper.getInstance());

        //获取单聊面板的标题栏
        mTitleBar = mChatLayout.getTitleBar();

        int rightPadding = DensityUtil.dip2px(getActivity(), 6);
        mTitleBar.getRightIcon().setPadding(rightPadding, rightPadding, rightPadding, rightPadding);
        mTitleBar.setRightIcon(R.drawable.dian_black);

        //单聊面板标记栏返回按钮点击事件
        mTitleBar.setOnLeftClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        //更多
        mTitleBar.getRightIcon().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPop();
            }
        });

        //消息长按事件、头像点击事件
        mChatLayout.getMessageLayout().setOnItemClickListener(new MessageLayout.OnItemClickListener() {
            @Override
            public void onMessageLongClick(View view, int position, MessageInfo messageInfo) {
                //因为adapter中第一条为加载条目，位置需减1
                mChatLayout.getMessageLayout().showItemPopMenu(position - 1, messageInfo, view);
            }

            @Override
            public void onUserIconClick(View view, int position, MessageInfo messageInfo) {
                if (null == messageInfo || messageInfo.isSelf()) {
                    return;
                }
                if (mUserCenterBean == null) {
                    getActorInfo();
                    return;
                }
                PersonInfoActivity.start(getActivity(), getActorId());
            }
        });

        mChatLayout.getNoticeLayout().alwaysShow(true);
        mChatLayout.getNoticeLayout().getContentExtra()
                .setText("任何以可线下约会见面为由要求打赏礼物或者添加微信、QQ等第三方工具发红包的均是骗子。");

        ConversationManagerKit.getInstance().loadConversation(null);

        //监听消息事件
        TIMManager.getInstance().addMessageListener(timMessageListener);

        topConversation = SharedPreferenceHelper.getTop(getActivity());
    }

    TIMMessageListener timMessageListener = new TIMMessageListener() {
        @Override
        public boolean onNewMessages(List<TIMMessage> list) {
            for (TIMMessage timMessage : list) {
                TIMConversation conversation = timMessage.getConversation();
                if (conversation != null
                        && conversation.getType() == TIMConversationType.C2C
                        && mChatInfo.getId().equals(conversation.getPeer())) {
                    for (int i = 0; i < timMessage.getElementCount(); ++i) {
                        TIMElem elem = timMessage.getElement(i);
                        if (elem.getType() == TIMElemType.Custom) {
                            TIMCustomElem customElem = (TIMCustomElem) elem;
                            byte[] data = customElem.getData();
                            String json = new String(data);
                            CustomMessageBean bean = CustomMessageBean.parseBean(json);
                            if (bean != null) {
                                if (ImCustomMessage.Type_gift.equals(bean.type)) {
                                    //礼物
                                    LogUtil.i("接收到的礼物: " + bean.gift_name);
                                    startGif(bean.gift_gif_url);
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
    };

    /**
     * PopWindow
     */
    private void showPop() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.pop_chat_more, null);
        PopupWindow mPopWindow = new PopupWindow(v);
        mPopWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopWindow.setFocusable(true);
        mPopWindow.setOutsideTouchable(true);
        mPopWindow.showAsDropDown(mChatLayout.getTitleBar(), 0, 0, Gravity.END);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                switch (v.getId()) {

                    //加入黑名单
                    case R.id.black_btn: {
                        new AlertDialog.Builder(getActivity())
                                .setMessage(String.format(getString(R.string.black_alert), mChatInfo.getChatName()))
                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, int which) {
                                        new BlackRequester() {
                                            @Override
                                            public void onSuccess(BaseResponse response, boolean addToBlack) {
                                                ToastUtil.showToast(R.string.black_add_ok);
                                                dialog.dismiss();
                                            }
                                        }.post(getActorId(), true);
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create().show();
                        break;
                    }

                    //置顶
                    case R.id.top_btn: {
                        boolean isTop = isTop();
                        if (isTop) {
                            topConversation.remove(mChatInfo.getId());
                        } else {
                            topConversation.add(mChatInfo.getId());
                        }
                        SharedPreferenceHelper.setTop(getActivity(), mChatInfo.getId(), isTop);
                        break;
                    }

                    //关注
                    case R.id.follow_btn: {
                        final boolean setFollow = !isFollow;
                        new FocusRequester() {
                            @Override
                            public void onSuccess(BaseResponse response, boolean focus) {
                                if (getActivity() == null || getActivity().isFinishing())
                                    return;
                                setFollow(setFollow);
                            }
                        }.focus(getActorId(), setFollow);
                        break;
                    }

                    //投诉
                    case R.id.report_btn: {
                        Intent intent = new Intent(getActivity(), ReportActivity.class);
                        intent.putExtra(Constant.ACTOR_ID, getActorId());
                        startActivity(intent);
                        break;
                    }

                    //备注
                    case R.id.remark_btn: {
                        new InputRemarkDialog(getActivity()) {
                            @Override
                            protected void remark(String text) {
                                mTitleBar.setTitle(text, TitleBarLayout.POSITION.MIDDLE);
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put(TIMFriend.TIM_FRIEND_PROFILE_TYPE_KEY_REMARK, text);
                                TIMFriendshipManager.getInstance().modifyFriend(mChatInfo.getId(), hashMap, new TIMCallBack() {
                                    @Override
                                    public void onError(int i, String s) {
                                    }

                                    @Override
                                    public void onSuccess() {
                                    }
                                });
                            }
                        }.show();
                        break;
                    }

                    //查看主页
                    case R.id.person_btn: {
                        PersonInfoActivity.start(getActivity(), getActorId());
                        break;
                    }

                }
                mPopWindow.dismiss();
            }
        };

        ViewGroup vp = v.findViewById(R.id.pop_ll);
        for (int i = 0; i < vp.getChildCount(); i++) {
            vp.getChildAt(i).setOnClickListener(clickListener);
        }

        TextView followTv = v.findViewById(R.id.follow_btn);
        followTv.setText(isFollow ? "取消关注" : "关注");

        TextView topTv = v.findViewById(R.id.top_btn);
        topTv.setText(isTop() ? "取消置顶" : "消息置顶");
    }

    private boolean isTop() {
        return topConversation.contains(mChatInfo.getId());
    }

    /**
     * 开始GIF动画
     */
    private void startGif(String path) {
        if (!TextUtils.isEmpty(path)) {
            SVGAParser parser = new SVGAParser(getActivity());
            try {
                URL url = new URL(path);
                parser.parse(url, new SVGAParser.ParseCompletion() {
                    @Override
                    public void onComplete(@NotNull SVGAVideoEntity videoItem) {
                        if (getActivity() == null || getActivity().isFinishing())
                            return;
                        SVGADrawable drawable = new SVGADrawable(videoItem);
                        mGifSv.setImageDrawable(drawable);
                        mGifSv.startAnimation();
                    }

                    @Override
                    public void onError() {

                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mChatInfo = (ChatInfo) getActivity().getIntent().getSerializableExtra(ImConstants.CHAT_INFO);
        if (mChatInfo == null) {
            return;
        }
        initView();

        // TODO 通过api设置ChatLayout各种属性的样例
        ChatLayoutHelper helper = new ChatLayoutHelper(getActivity());
        helper.customizeChatLayout(mChatLayout);
        initBtn();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!AppManager.getInstance().getUserInfo().isVip()) {
            mChatLayout.getInputLayout().mAudioInputSwitchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new VipDialog(getActivity(), "VIP用户才可发送语音消息").show();
                }
            });
        } else {
            mChatLayout.getInputLayout().mAudioInputSwitchButton.setOnClickListener(mChatLayout.getInputLayout());
        }
    }

    /**
     * 点击事件
     */
    private void initBtn() {

        if (getView() == null)
            return;

        getActorInfo();

        //守护
        View protectBtn = getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.protect_btn);
        if (AppManager.getInstance().getUserInfo().isSexMan()) {
            protectBtn.setVisibility(View.VISIBLE);
            protectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new ProtectDialog(getActivity(), getActorId()).show();
                }
            });
        } else {
            protectBtn.setVisibility(View.GONE);
        }

        //vip
        View vipBtn = getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.vip_btn);
        if (!AppManager.getInstance().getUserInfo().isVip()) {
            vipBtn.setVisibility(View.VISIBLE);
            vipBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VipCenterActivity.start(getActivity(), false);
                }
            });
        } else {
            vipBtn.setVisibility(View.GONE);
        }

        //选择图片
        getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.btn_picture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AppManager.getInstance().getUserInfo().isVip()) {
                    new VipDialog(getActivity(), "VIP用户才能使用图片聊天功能").show();
                    return;
                }
                ImageHelper.openPictureChoosePage(getActivity(), Constant.REQUEST_CODE_CHOOSE);
            }
        });

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUserCenterBean != null) {
                    //同性别不能交流
                    if (mUserCenterBean.t_sex == getUserSex()) {
                        ToastUtil.showToast(getActivity(), R.string.sex_can_not_communicate);
                        return;
                    }
                    //判断双方是不是都是用户
                    if (mUserCenterBean.t_role == 0 && getUserRole() == 0) {
                        ToastUtil.showToast(getActivity(), R.string.can_not_communicate);
                        return;
                    }
                    AudioVideoRequester audioVideoRequester = new AudioVideoRequester(getActivity(),
                            mUserCenterBean.t_role == 1,
                            getActorId());
//                    if (v.getTag() != null) {
//                        String type = v.getTag().toString();
//                        if (ImCustomMessage.Call_Type_Video.equals(type)) {
                    audioVideoRequester.executeVideo();
//                        } else {
//                            audioVideoRequester.executeAudio();
//                        }
//                    } else {
//                        audioVideoRequester.execute();
//                    }
                } else {
                    getActorInfo();
                }
            }
        };

        //开启相机
        getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.btn_camera).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!AppManager.getInstance().getUserInfo().isVip()) {
                            new VipDialog(getActivity(), "VIP用户才能使用图片聊天功能").show();
                            return;
                        }
                        mChatLayout.getInputLayout().startCapture();
                    }
                });

        //发起音视频
        getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.btn_video).setTag(ImCustomMessage.Call_Type_Video);
        getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.btn_video).setOnClickListener(onClickListener);

        getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.btn_audio).setTag(ImCustomMessage.Call_Type_Audio);
        getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.btn_audio).setOnClickListener(onClickListener);

        //发送礼物
        getView().findViewById(com.tencent.qcloud.tim.uikit.R.id.btn_gift).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new GiftDialog(getActivity(), getActorId()).show();
            }
        });

        //拦截器
        mChatLayout.setCanSend(new ISend() {
            @Override
            public void send(final OnSend onSend) {

                if (mUserCenterBean == null) {
                    getActorInfo();
                    return;
                }

                //同性别不能交流
                if (mUserCenterBean.t_sex == getUserSex()) {
                    ToastUtil.showToast(getActivity(), R.string.sex_can_not_communicate);
                    return;
                }
                //判断双方是不是都是用户
                if (mUserCenterBean.t_role == 0 && getUserRole() == 0) {
                    ToastUtil.showToast(getActivity(), R.string.can_not_communicate);
                    return;
                }

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("userId", getUserId());
                paramMap.put("coverConsumeUserId", getActorId());
                OkHttpUtils.post().url(ChatApi.SEND_TEXT_CONSUME())
                        .addParams("param", ParamUtil.getParam(paramMap))
                        .build().execute(new AjaxCallback<BaseResponse>() {
                    @Override
                    public void onResponse(BaseResponse response, int id) {
                        if (response != null) {
                            if (response.m_istatus == NetCode.SUCCESS || response.m_istatus == 2) {
                                //扣费成功或者是VIP用户
                                onSend.canSend(true);
                            } else if (response.m_istatus == -1) {
                                //余额不足
                                ChargeHelper.showSetCoverDialog(getActivity());
                            } else if (response.m_istatus == 3) {
                                ToastUtil.showToast(getActivity(), response.m_strMessage);
                                onSend.canSend(true);
                            } else {
                                //其他错误直接提示
                                ToastUtil.showToast(getActivity(), response.m_strMessage);
                            }
                        } else {
                            ToastUtil.showToast(getActivity(), R.string.system_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        super.onError(call, e, id);
                        ToastUtil.showToast(getActivity(), R.string.system_error);
                    }
                });
            }
        });

        setSubTitleBar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //图片选择回调
        if (requestCode == Constant.REQUEST_CODE_CHOOSE && resultCode == Activity.RESULT_OK) {
            List<Uri> mSelectedUris = Matisse.obtainResult(data);
            if (mSelectedUris.size() > 0) {
                MessageInfo info = MessageInfoUtil.buildImageMessage(mSelectedUris.get(0), true);
                mChatLayout.sendMessage(info, false);
            }
        }
    }

    private AudioUserBean mUserCenterBean;

    /**
     * 获取对方信息
     * ++++++++++++++++接口更改++++++++++++++
     */
    private void getActorInfo() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        paramMap.put("coverUserId", String.valueOf(getActorId()));
        OkHttpUtils.post().url(ChatApi.getUserInfoById())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<AudioUserBean>>() {
            @Override
            public void onResponse(BaseResponse<AudioUserBean> response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    AudioUserBean bean = response.m_object;
                    if (bean != null) {
                        mUserCenterBean = bean;
                    }
                }
            }
        });
    }

    /**
     * 非vip用户顶部收费提示
     */
//    private void setSubTitleBar() {
//        if (AppManager.getInstance().getUserInfo().t_is_vip != 0 && getUserRole() == 0) {
//            Map<String, Object> paramMap = new HashMap<>();
//            paramMap.put("userId", getUserId());
//            paramMap.put("anchorId", getActorId());
//            OkHttpUtils.post().url(ChatApi.GET_ACTOR_CHARGE_SETUP)
//                    .addParams("param", ParamUtil.getParam(paramMap))
//                    .build().execute(new AjaxCallback<BaseResponse<ChargeBean>>() {
//                @Override
//                public void onResponse(BaseResponse<ChargeBean> response, int id) {
//                    if (getActivity() == null) {
//                        return;
//                    }
//                    if (response != null && response.m_istatus == NetCode.SUCCESS && response.m_object != null) {
//                        ChargeBean bean = response.m_object;
//                        int textGold = bean.t_text_gold;
//                        if (textGold > 0) {
//                            ViewGroup viewGroup = getView().findViewById(R.id.subtitle_bar);
//                            View view = View.inflate(getActivity(), R.layout.top_price_layout, viewGroup);
//                            TextView textView = view.findViewById(R.id.first_tv);
//                            textView.setText(String.format(getString(R.string.im_chat_price), textGold + getResources().getString(R.string.gold)));
//                            getView().findViewById(R.id.vip_tv).setOnClickListener(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    Intent intent = new Intent(getActivity(), VipCenterActivity.class);
//                                    startActivity(intent);
//                                }
//                            });
//                        }
//                    }
//                }
//            });
//        }
//    }

    /**
     * 查看主播联系方式
     */
    private void setSubTitleBar() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("coverUserId", getActorId());
        OkHttpUtils.post().url(ChatApi.GET_ACTOR_INFO())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean>>>() {
            @Override
            public void onResponse(BaseResponse<ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean>> response, int id) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    if (response.m_object != null) {

                        //联系方式
//                        setContact(response.m_object);

                        //关注
                        setFollow(response.m_object.isFollow == 1);

                        mChatLayout.getMessageLayout().scrollToEnd();
                    }
                }
            }
        });
    }

    /**
     * 设置关注icon
     */
    private void setFollow(boolean isFollow) {
        this.isFollow = isFollow;
//        mChatLayout.getTitleBar().getRightIcon().setSelected(isFollow);
//        mChatLayout.getTitleBar().setRightIcon(isFollow ?
//                R.drawable.follow_selected : R.drawable.follow_unselected);
//        mChatLayout.getTitleBar().getRightIcon().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final boolean setFollow = !v.isSelected();
//                new FocusRequester() {
//                    @Override
//                    public void onSuccess(BaseResponse response, boolean focus) {
//                        if (getActivity() == null || getActivity().isFinishing())
//                            return;
//                        setFollow(setFollow);
//                    }
//                }.focus(getActorId(), setFollow);
//            }
//        });
    }

    /**
     * 查看联系方式
     */
    private void setContact(final ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> bean) {
        if (getView() == null)
            return;

        ViewGroup viewGroup = getView().findViewById(R.id.subtitle_bar);
        viewGroup.removeAllViews();

        View view = View.inflate(getActivity(), R.layout.top_contact_layout, viewGroup);

        View mWeixinTv = view.findViewById(R.id.weixin_tv);
        View mQqTv = view.findViewById(R.id.qq_tv);
        View mPhoneTv = view.findViewById(R.id.phone_tv);

        mWeixinTv.setTag(0);
        mPhoneTv.setTag(1);
        mQqTv.setTag(2);

        mWeixinTv.setOnClickListener(null);
        mPhoneTv.setOnClickListener(null);
        mQqTv.setOnClickListener(null);

        View.OnClickListener lookClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LookNumberDialog(getActivity(), bean, (int) v.getTag(), getActorId()).show();
            }
        };

        mWeixinTv.setAlpha(0.3f);
        mQqTv.setAlpha(0.3f);
        mPhoneTv.setAlpha(0.3f);

        if (bean.anchorSetup != null && bean.anchorSetup.size() > 0) {
            ChargeBean chargeBean = bean.anchorSetup.get(0);
            if (chargeBean.t_weixin_gold != 0 && !TextUtils.isEmpty(bean.t_weixin)) {
                mWeixinTv.setAlpha(1f);
                mWeixinTv.setOnClickListener(lookClickListener);
            }
            if (chargeBean.t_qq_gold != 0 && !TextUtils.isEmpty(bean.t_qq)) {
                mQqTv.setAlpha(1f);
                mQqTv.setOnClickListener(lookClickListener);
            }
            if (chargeBean.t_phone_gold != 0 && !TextUtils.isEmpty(bean.t_phone)) {
                mPhoneTv.setAlpha(1f);
                mPhoneTv.setOnClickListener(lookClickListener);
            }
        }
    }

    private int getActorId() {
        return Integer.parseInt(mChatInfo.getId()) - 10000;
    }

    private int getUserId() {
        return AppManager.getInstance().getUserInfo().t_id;
    }

    private int getUserSex() {
        return AppManager.getInstance().getUserInfo().t_sex;
    }

    private int getUserRole() {
        return AppManager.getInstance().getUserInfo().t_role;
    }

    @Override
    public void onPause() {
        super.onPause();
        AudioPlayer.getInstance().stopPlay();
    }

    @Override
    public void onDestroyView() {
        TIMManager.getInstance().removeMessageListener(timMessageListener);
        if (mGifSv != null) {
            mGifSv.pauseAnimation();
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatLayout != null) {
            mChatLayout.exitChat();
        }
    }
}