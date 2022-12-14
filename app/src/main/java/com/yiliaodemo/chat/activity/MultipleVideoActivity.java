package com.yiliaodemo.chat.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.opensource.svgaplayer.SVGADrawable;
import com.opensource.svgaplayer.SVGAImageView;
import com.opensource.svgaplayer.SVGAParser;
import com.opensource.svgaplayer.SVGAVideoEntity;
import com.yiliaodemo.chat.BuildConfig;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.adapter.MultipleMessageRecyclerAdapter;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.base.BaseBean;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.CustomMessageBean;
import com.yiliaodemo.chat.bean.GiftBean;
import com.yiliaodemo.chat.bean.MansionUserInfoBean;
import com.yiliaodemo.chat.bean.MultipleChatInfo;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.dialog.InputDialogFragment;
import com.yiliaodemo.chat.dialog.InvalidChatDialog;
import com.yiliaodemo.chat.dialog.Invite1v2Dialog;
import com.yiliaodemo.chat.dialog.MultipleGiftDialog;
import com.yiliaodemo.chat.glide.GlideCircleTransform;
import com.yiliaodemo.chat.helper.IMFilterHelper;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.rtc.RtcEngineEventHandler;
import com.yiliaodemo.chat.rtc.RtcManager;
import com.yiliaodemo.chat.rtc.RtcVideoConsumer;
import com.yiliaodemo.chat.socket.SocketMessageManager;
import com.yiliaodemo.chat.socket.domain.Mid;
import com.yiliaodemo.chat.socket.domain.SocketResponse;
import com.yiliaodemo.chat.ttt.QiNiuChecker;
import com.yiliaodemo.chat.util.LogUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.util.permission.PermissionUtil;
import com.yiliaodemo.chat.view.recycle.AbsRecycleAdapter;
import com.yiliaodemo.chat.view.recycle.ViewHolder;
import com.tencent.imsdk.TIMCallBack;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMCustomElem;
import com.tencent.imsdk.TIMElem;
import com.tencent.imsdk.TIMGroupManager;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMMessageListener;
import com.tencent.imsdk.TIMTextElem;
import com.tencent.imsdk.TIMValueCallBack;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.ImCustomMessage;
import com.tencent.qcloud.tim.uikit.modules.message.MessageInfo;
import com.tencent.qcloud.tim.uikit.modules.message.MessageInfoUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import okhttp3.Call;
import okhttp3.Request;


/**
 * ????????????
 */
public class MultipleVideoActivity extends BaseActivity {

    @BindView(R.id.time_ch)
    Chronometer timeCh;

    @BindView(R.id.gif_sv)
    protected SVGAImageView mGifSv;

    @BindView(R.id.view_rv)
    protected RecyclerView viewRv;

    @BindView(R.id.message_rv)
    protected RecyclerView mMessageRv;

    @BindView(R.id.room_tv)
    protected TextView roomTv;

    protected MultipleMessageRecyclerAdapter messageAdapter;

    protected RtcManager rtcManager;

    protected EventHandler eventHandler;

    private Dialog illegalAlert;

    private final MessageEvent messageEvent = new MessageEvent();

    /**
     * ??????????????????????????????15s
     */
    final int illegalTime = 15000;

    /**
     * ????????????
     */
    protected RoleAnchor roleManager = new RoleAnchor();

    /**
     * ???????????????
     */
    protected Role selfRole;

    protected List<Role> roleList = new ArrayList<>();

    protected AbsRecycleAdapter adapter;

    protected MultipleChatInfo chatInfo;

    protected boolean isManager;


    public static void start(final Context context, final MultipleChatInfo chatInfo, final boolean isAnchor) {
        if (chatInfo == null) {
            return;
        }

        PermissionUtil.requestPermissions(context, new PermissionUtil.OnPermissionListener() {
            @Override
            public void onPermissionGranted() {
                Intent starter = new Intent(context, chatInfo.isAudioChat() ?
                        MultipleAudioActivity.class : MultipleVideoActivity.class);
                starter.putExtra("isAnchor", isAnchor);
                starter.putExtra("chatInfo", chatInfo);
                context.startActivity(starter);
            }

            @Override
            public void onPermissionDenied() {
                ToastUtil.showToast("?????????????????????????????????????????????????????????");
            }
        }, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);

    }

    @Override
    protected boolean isImmersionBarEnabled() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        return true;
    }

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_multiple_activity);
    }

    @Override
    protected void onContentAdded() {

        SocketMessageManager.get().subscribe(subscription, Subscriptions);

        isManager = getIntent().getBooleanExtra("isAnchor", isManager);
        chatInfo = (MultipleChatInfo) getIntent().getSerializableExtra("chatInfo");

        QiNiuChecker.get().checkEnable();
        QiNiuChecker.get().setAlertListener(new OnCommonListener<Boolean>() {
            @Override
            public void execute(Boolean aBoolean) {
                if (isFinishing()) {
                    QiNiuChecker.get().setAlertListener(null);
                    return;
                }
                if (aBoolean) {
                    sendMessage(ImCustomMessage.buildIllegalVideo(), null);
                    IllegalVideo(selfRole.uid);
                    if (illegalAlert == null) {
                        illegalAlert = new InvalidChatDialog(mContext, getString(R.string.illegal_info_self));
                        illegalAlert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                illegalAlert = null;
                            }
                        });
                    }
                    illegalAlert.show();
                }
            }
        });

        needHeader(false);
        initChatRv();
        initRoles();
        initMsgRv();
        initRoom();
        joinRoom();
    }

    /**
     * ????????????
     */
    private void IllegalVideo(int id) {
        if (id <= 0) {
            return;
        }
        ViewHolder viewHolder = null;
        for (Role role : roleList) {
            if (role.uid == id && role.viewHolder != null) {
                viewHolder = role.viewHolder;
                break;
            }
        }
        if (viewHolder != null) {
            boolean isSelf = id == selfRole.uid;
            TextView illegalTv = viewHolder.getView(R.id.cover_tv);
            illegalTv.setVisibility(View.VISIBLE);
            illegalTv.setText(isSelf ? R.string.illegal_info_self : R.string.illegal_info_other);
            illegalTv.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) {
                        return;
                    }
                    illegalTv.setVisibility(View.GONE);
                    if (isSelf && illegalAlert != null) {
                        illegalAlert.dismiss();
                        illegalAlert = null;
                    }
                }
            }, illegalTime);
        }

    }

    private void destroy() {
        if (rtcManager == null) {
            return;
        }

        QiNiuChecker.get().setAlertListener(null);

        destroyRtc();

        if (eventHandler.alertDialog != null) {
            eventHandler.alertDialog.dismiss();
        }

        stopTimer();

        SocketMessageManager.get().unsubscribe(subscription);

        hangUp();

        mGifSv.pauseAnimation();

        registerIm(false);
    }

    private void destroyRtc() {
        if (rtcManager != null) {
            rtcManager.stopCamera();
            rtcManager.removeRtcHandler(eventHandler);
            rtcManager.rtcEngine().leaveChannel();
            rtcManager = null;
        }
    }

    @Override
    protected void onDestroy() {
        destroy();
        super.onDestroy();
        IMHelper.syncGroup(null);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(mContext)
                .setMessage("????????????????????????")
                .setNegativeButton("??????", null)
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).create().show();
    }

    @OnClick({
            R.id.finish_btn,
            R.id.gift_iv,
            R.id.input_tv
    })
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.finish_btn:
                onBackPressed();
                break;

            /*
             * ?????????
             */
            case R.id.gift_iv:
                getMansionInfo(new OnCommonListener<MansionInfo>() {
                    @Override
                    public void execute(MansionInfo mansionInfo) {
                        List<MansionUserInfoBean> list = new ArrayList<>();
                        if (mansionInfo != null && mansionInfo.userInfo.t_id != AppManager.getInstance().getUserInfo().t_id) {
                            list.add(mansionInfo.userInfo);
                        }
                        if (mansionInfo != null && mansionInfo.anochorInfo != null && mansionInfo.anochorInfo.size() > 0) {
                            for (MansionUserInfoBean userInfoBean : mansionInfo.anochorInfo) {
                                if (userInfoBean.t_id != AppManager.getInstance().getUserInfo().t_id) {
                                    list.add(userInfoBean);
                                }
                            }
                        }
                        if (list.size() > 0) {
                            new MultipleGiftDialog(mContext, list) {
                                @Override
                                public void sendOk(GiftBean giftBean, int gold, List<MansionUserInfoBean> list) {
                                    //??????????????????
                                    for (MansionUserInfoBean userInfoBean : list) {
                                        CustomMessageBean bean = CustomMessageBean.transformGift(
                                                AppManager.getInstance().getUserInfo().t_id,
                                                userInfoBean.t_id, giftBean, userInfoBean.t_nickName);
                                        String json = JSON.toJSONString(bean);
                                        TIMCustomElem elem = new TIMCustomElem();
                                        elem.setData(json.getBytes());
                                        elem.setDesc(getString(R.string.get_a_gift));
                                        sendMessage(elem, null);
                                    }
                                }
                            }.show();
                        } else {
                            ToastUtil.showToast("???????????????????????????");
                        }
                    }
                });
                break;

            /*
             * ????????????
             */
            case R.id.input_tv: {
                final InputDialogFragment inputDialogFragment = new InputDialogFragment();
                inputDialogFragment.setOnTextSendListener(new InputDialogFragment.OnTextSendListener() {
                    @Override
                    public void onTextSend(String text) {
                        if (!TextUtils.isEmpty(text)) {
                            text = IMFilterHelper.getInstance().switchFilterWord(getApplicationContext(), text);
                            TIMTextElem textElem = new TIMTextElem();
                            textElem.setText(text);
                            sendMessage(textElem, null);
                            inputDialogFragment.dismiss();
                        } else {
                            ToastUtil.showToast(R.string.please_input_text_message);
                        }
                    }
                });
                inputDialogFragment.show(getSupportFragmentManager(), "tag");
                break;
            }
        }
    }

    /**
     * ????????????adapter
     */
    protected void initChatRv() {
        adapter = new AbsRecycleAdapter(
                new AbsRecycleAdapter.Type(R.layout.item_multiple_video_anchor, RoleAnchor.class),
                new AbsRecycleAdapter.Type(R.layout.item_multiple_video_anchor2, RoleBroadcaster.class)) {

            @Override
            public void convert(ViewHolder holder, Object t) {

                Role role = (Role) t;

                role.viewHolder = holder;

                boolean isSelf = role.uid == AppManager.getInstance().getUserInfo().t_id;

                if (role.isJoinRoom && role.operate == Role.OperateBind) {

                    //??????????????????
                    FrameLayout frameLayout = holder.getView(R.id.root_view);
                    if (role.surfaceView != null) {
                        frameLayout.removeView(role.surfaceView);
                    }

                    View surfaceView;
                    if (role == selfRole) {
                        surfaceView = new TextureView(holder.itemView.getContext());
                        rtcManager.getCameraManager().setLocalPreview((TextureView) surfaceView);
                        rtcManager.startCamera();
                    } else {
                        surfaceView = RtcEngine.CreateRendererView(holder.itemView.getContext());
                        VideoCanvas videoCanvas = new VideoCanvas(
                                (SurfaceView) surfaceView,
                                Constants.RENDER_MODE_HIDDEN,
                                chatInfo.mansionRoomId + "",
                                role.uid,
                                Constants.VIDEO_MIRROR_MODE_ENABLED);
                        rtcManager.rtcEngine().setupRemoteVideo(videoCanvas);
                    }
                    frameLayout.addView(surfaceView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                    frameLayout.requestLayout();
                    role.surfaceView = surfaceView;

                    //????????????
                    if (role.getClass() == RoleBroadcaster.class) {
                        Chronometer chronometer = holder.getView(R.id.time_ch);
                        if (isManager || isSelf) {
                            chronometer.setVisibility(View.VISIBLE);
                            chronometer.setFormat("%s");
                            chronometer.setBase(SystemClock.elapsedRealtime());
                            chronometer.start();
                        }
                    }

                }

                if (role.getClass() == RoleBroadcaster.class) {

                    //??????btn??????
                    holder.getView(R.id.remove_btn).setVisibility(View.GONE);
                    if (isManager && role.isJoinRoom) {
                        holder.getView(R.id.remove_btn).setVisibility(View.VISIBLE);
                    }

                    //????????????
                    Chronometer chronometer = holder.getView(R.id.time_ch);
                    if (!role.isJoinRoom || (!isManager && !isSelf)) {
                        chronometer.setVisibility(View.INVISIBLE);
                        chronometer.stop();
                    }
                }

                //??????????????????
                holder.getView(R.id.person_ll).setVisibility(role.isJoinRoom ? View.VISIBLE : View.GONE);

                //????????????
                if (role.userInfo != null) {
                    Glide.with(mContext)
                            .load(role.userInfo.t_handImg)
                            .error(R.drawable.default_head)
                            .transform(new GlideCircleTransform(mContext))
                            .into(holder.<ImageView>getView(R.id.head_iv));
                    holder.<TextView>getView(R.id.name_tv).setText(role.getNickName());
                } else {
                    holder.<ImageView>getView(R.id.head_iv).setImageResource(0);
                    holder.<TextView>getView(R.id.name_tv).setText(null);
                }

                //??????btn
                holder.getView(R.id.speaker_btn).setVisibility(!isSelf && role.isJoinRoom ? View.VISIBLE : View.GONE);
                holder.<ImageView>getView(R.id.speaker_btn).setImageResource(role.mutedAudio ?
                        R.drawable.multiple_chat_speaker_selected : R.drawable.multiple_chat_speaker_unselected);

                //??????btn
                holder.getView(R.id.mute_btn).setVisibility(isSelf && role.isJoinRoom ? View.VISIBLE : View.GONE);
                holder.<ImageView>getView(R.id.mute_btn).setImageResource(role.muted ?
                        R.drawable.multiple_chat_mute_selected : R.drawable.multiple_chat_mute_unselected);

                //???????????????
                holder.getView(R.id.camera_btn).setVisibility(isSelf && role.isJoinRoom ? View.VISIBLE : View.GONE);
            }

            @Override
            public void setViewHolder(final ViewHolder viewHolder) {

                //????????????
                View inviteBtn = viewHolder.itemView.findViewById(R.id.invite_btn);
                if (inviteBtn != null) {
                    if (isManager) {
                        inviteBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Role role = (Role) getData().get(viewHolder.getRealPosition());
                                if (!role.isJoinRoom) {
                                    new Invite1v2Dialog(mContext, chatInfo).show();
                                }
                            }
                        });
                    } else {
                        inviteBtn.setVisibility(View.GONE);
                    }
                }

                //????????????
                View removeBtn = viewHolder.itemView.findViewById(R.id.remove_btn);
                if (removeBtn != null) {
                    if (isManager) {
                        removeBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Role role = (Role) getData().get(viewHolder.getRealPosition());
                                eventHandler.remove(role);
                            }
                        });
                    } else {
                        removeBtn.setVisibility(View.GONE);
                    }
                }

                //??????
                final ImageView muteBtn = viewHolder.itemView.findViewById(R.id.mute_btn);
                if (muteBtn != null) {
                    muteBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Role role = (Role) getData().get(viewHolder.getRealPosition());
                            eventHandler.mute(role, muteBtn);
                        }
                    });
                }

                //???????????????
                final View cameraBtn = viewHolder.itemView.findViewById(R.id.camera_btn);
                if (cameraBtn != null) {
                    cameraBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            rtcManager.getCameraManager().switchCamera();
                        }
                    });
                }

                //?????????
                final ImageView speakerBtn = viewHolder.itemView.findViewById(R.id.speaker_btn);
                if (speakerBtn != null) {
                    speakerBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Role role = (Role) getData().get(viewHolder.getRealPosition());
                            eventHandler.speaker(role, speakerBtn);
                        }
                    });
                }
            }
        };
        final GridLayoutManager layoutManager = new GridLayoutManager(mContext, 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                return i == 0 ? layoutManager.getSpanCount() : 1;
            }
        });
        viewRv.setLayoutManager(layoutManager);
        viewRv.setAdapter(adapter);
    }

    /**
     * ???????????????????????????????????????????????????
     * ???????????????????????????
     */
    private void initRoles() {
        roleList.add(roleManager);
        roleList.add(new RoleBroadcaster());
        roleList.add(new RoleBroadcaster());
        adapter.setDatas(roleList);
    }

    private void initMsgRv() {
        messageAdapter = new MultipleMessageRecyclerAdapter(mContext);
        mMessageRv.setLayoutManager(new LinearLayoutManager(this));
        mMessageRv.setAdapter(messageAdapter);
    }

    /**
     * ?????????
     */
    private void initRoom() {
        eventHandler = new EventHandler();
        rtcManager = RtcManager.get();
        rtcManager.addRtcHandler(eventHandler);
        if (chatInfo.isAudioChat()) {
            rtcManager.rtcEngine().muteLocalAudioStream(false);
            rtcManager.rtcEngine().muteLocalVideoStream(true);
        } else {
            QiNiuChecker.get().checkEnable();
            VideoEncoderConfiguration configuration = new VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT);
            rtcManager.rtcEngine().setVideoEncoderConfiguration(configuration);
            rtcManager.rtcEngine().setVideoSource(new RtcVideoConsumer());
            rtcManager.rtcEngine().enableVideo();
            rtcManager.startCamera();
            rtcManager.rtcEngine().muteLocalAudioStream(false);
            rtcManager.rtcEngine().muteLocalVideoStream(false);
        }
        roomTv.setText(chatInfo.roomName);
    }

    /**
     * ???????????????
     */
    private void startTimer() {

        timeCh.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                //30s??????
                String time = chronometer.getText().toString();
                if (!time.equals("00:00") && (time.endsWith("00") || time.endsWith("30"))) {
                    checkChatState();
                }

                if (!chatInfo.isAudioChat()) {
                    //????????????
                    int second = (int) ((SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000);
                    QiNiuChecker.get().checkTime(second, roleManager.uid, selfRole.uid, chatInfo.roomId, chatInfo.mansionRoomId);
                }
            }
        });
        timeCh.setFormat("%s");
        timeCh.setBase(SystemClock.elapsedRealtime());
        timeCh.start();
    }

    /**
     * ???????????????
     * ??????????????????
     */
    private void checkChatState() {
        if (isManager || roleManager.uid == 0 || chatInfo.roomId == 0)
            return;
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("roomId", chatInfo.roomId);
        paramMap.put("videoUserId", roleManager.uid);
        paramMap.put("videoCoverUserId", getUserId());
        OkHttpUtils.post().url(ChatApi.getVideoStatus())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<Integer>>() {
            @Override
            public void onResponse(BaseResponse<Integer> response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS && response.m_object != null) {
                    if (response.m_object != 1) {
                        ToastUtil.showToast("?????????");
                        finish();
                    }
                }
            }
        });
    }

    /**
     * ???????????????
     */
    protected final void stopTimer() {
        if (timeCh != null) {
            timeCh.stop();
            timeCh.setOnChronometerTickListener(null);
        }
    }

    /**
     * ??????IM??????
     */
    private void registerIm(boolean register) {
        final TIMCallBack timCallBack = new TIMCallBack() {
            @Override
            public void onError(int i, String s) {
                LogUtil.i("TIM: " + i + ": " + s);
            }

            @Override
            public void onSuccess() {

            }
        };
        if (register) {
            if (isManager) {
                TIMGroupManager.CreateGroupParam param = new TIMGroupManager.CreateGroupParam(
                        "ChatRoom",
                        "" + chatInfo.mansionRoomId);
                param.setGroupId("" + chatInfo.mansionRoomId);
                TIMGroupManager.getInstance().createGroup(param, new TIMValueCallBack<String>() {

                    @Override
                    public void onError(int code, String desc) {
                        ToastUtil.showToast("??????????????????" + code);
                    }

                    @Override
                    public void onSuccess(String s) {
                    }
                });
            } else {
                TIMGroupManager.getInstance().applyJoinGroup(
                        "" + chatInfo.mansionRoomId,
                        "reason",
                        timCallBack);
            }
            TIMManager.getInstance().addMessageListener(messageEvent);
        } else {
            //??????????????????
            TIMElem timElem = ImCustomMessage.buildRoomMessage(ImCustomMessage.Type_leaved,
                    AppManager.getInstance().getUserInfo().t_id, 0, "");
            sendMessage(timElem, new OnCommonListener<Boolean>() {
                @Override
                public void execute(Boolean aBoolean) {
                    if (isManager) {
                        TIMGroupManager.getInstance().deleteGroup("" + chatInfo.mansionRoomId, timCallBack);
                    } else {
                        TIMGroupManager.getInstance().quitGroup("" + chatInfo.mansionRoomId, timCallBack);
                    }
                }
            });
            TIMManager.getInstance().removeMessageListener(messageEvent);
        }
    }

    /**
     * ?????????????????????
     * ????????????
     */
    private void joinChannel() {
        rtcManager.rtcEngine().setEnableSpeakerphone(selfRole.speaker);
        rtcManager.rtcEngine().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        rtcManager.rtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        rtcManager.rtcEngine().joinChannel(
                chatInfo.sign,
                String.valueOf(chatInfo.mansionRoomId),
                null,
                AppManager.getInstance().getUserInfo().t_id);
        registerIm(true);
    }

    /**
     * ????????????
     */
    private void joinRoom() {

        //????????????
        if (isManager) {
            roleManager.uid = AppManager.getInstance().getUserInfo().t_id;
            selfRole = roleManager;
            getMansionInfo(new OnCommonListener<MansionInfo>() {
                @Override
                public void execute(MansionInfo mansionInfo) {
                    if (mansionInfo != null) {
                        roleManager.setUserInfo(mansionInfo.userInfo);
                    } else {
                        MansionUserInfoBean userInfoBean = new MansionUserInfoBean();
                        userInfoBean.t_id = AppManager.getInstance().getUserInfo().t_id;
                        userInfoBean.t_nickName = AppManager.getInstance().getUserInfo().t_nickName;
                        userInfoBean.t_handImg = AppManager.getInstance().getUserInfo().headUrl;
                        roleManager.setUserInfo(userInfoBean);
                    }
                    joinChannel();
                }
            });
        }

        //????????????
        else {
            //????????????Id
            roleManager.uid = chatInfo.connectUserId;
            getMansionInfo(new OnCommonListener<MansionInfo>() {
                @Override
                public void execute(MansionInfo mansionInfo) {
                    if (mansionInfo == null) {
                        new AlertDialog.Builder(mContext).setMessage("??????????????????").setCancelable(false)
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                })
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        joinRoom();
                                    }
                                }).create().show();
                    } else {
                        chatInfo.roomName = mansionInfo.mansionRoomName;
                        roomTv.setText(chatInfo.roomName);
                        roleManager.setUserInfo(mansionInfo.userInfo);
                        selfRole = findNext();
                        for (MansionUserInfoBean userInfoBean : mansionInfo.anochorInfo) {
                            if (userInfoBean.t_id == AppManager.getInstance().getUserInfo().t_id) {
                                selfRole.setUserInfo(userInfoBean);
                                break;
                            }
                        }
                        if (selfRole != null) {
                            joinChannel();
                        } else {
                            new AlertDialog.Builder(mContext).setMessage("??????????????????").setCancelable(false)
                                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            finish();
                                        }
                                    }).create().show();
                        }
                    }
                }
            });
        }

    }

    private Role findNext() {
        for (Role role : roleList) {
            if (role == roleManager)
                continue;
            if (!role.isJoinRoom) {
                return role;
            }
        }
        return null;
    }

    /**
     * ????????????????????????
     */
    private void getMansionInfo(final OnCommonListener<MansionInfo> commonListener) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("mansionRoomId", chatInfo.mansionRoomId);
        OkHttpUtils.post().url(ChatApi.getMansionHouseVideoInfo())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<MansionInfo>>() {

            @Override
            public void onResponse(BaseResponse<MansionInfo> response, int id) {
                if (isFinishing())
                    return;
                if (response != null && response.m_istatus == 1
                        && response.m_object != null && response.m_object.userInfo != null) {
                    if (commonListener != null) {
                        commonListener.execute(response.m_object);
                    }
                } else {
                    onError(null, null, 0);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                if (isFinishing())
                    return;
                if (commonListener != null) {
                    commonListener.execute(null);
                }
            }

            @Override
            public void onBefore(Request request, int id) {
                showLoadingDialog();
            }

            @Override
            public void onAfter(int id) {
                if (isFinishing())
                    return;
                dismissLoadingDialog();
            }
        });
    }

    /**
     * ???????????? breakMansionLink
     * ???????????? closeMansionLink
     */
    private void hangUp() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("mansionRoomId", chatInfo.mansionRoomId);
        String method = ChatApi.breakMansionLink();
        if (isManager) {
            method = ChatApi.closeMansionLink();
        } else {
            paramMap.put("breakUserId", getUserId());
            paramMap.put("roomId", chatInfo.roomId);
            paramMap.put("breakType", chatInfo.chatType);
        }
        OkHttpUtils.post().url(method).addParams("param", ParamUtil.getParam(paramMap)).build()
                .execute(new AjaxCallback<BaseResponse>() {
                    @Override
                    public void onResponse(BaseResponse response, int id) {
                    }
                });
    }

    /**
     * ??????Im??????
     */
    private void sendMessage(TIMElem timElem, final OnCommonListener<Boolean> onCommonListener) {
        TIMMessage msg = new TIMMessage();
        msg.addElement(timElem);
        TIMConversation timConversation = TIMManager.getInstance().getConversation(
                TIMConversationType.Group,
                "" + chatInfo.mansionRoomId);
        if (timConversation != null) {
            if (timElem != null) {
                timConversation.sendMessage(msg, new TIMValueCallBack<TIMMessage>() {
                    @Override
                    public void onError(int code, String desc) {
//                        String content = code + getString(R.string.send_fail) + desc;
//                        ToastUtil.showToast(content);
                        IMHelper.resLogin(code);
                        if (onCommonListener != null) {
                            onCommonListener.execute(false);
                        }
                    }

                    @Override
                    public void onSuccess(TIMMessage msg) {
                        messageEvent.addNewMessage(msg);
                        if (onCommonListener != null) {
                            onCommonListener.execute(true);
                        }
                    }
                });
            } else {
                ToastUtil.showToast(R.string.element_send_fail);
                if (onCommonListener != null) {
                    onCommonListener.execute(false);
                }
            }
        } else {
            if (onCommonListener != null) {
                onCommonListener.execute(false);
            }
            ToastUtil.showToast(R.string.tim_send_fail);
        }
    }

    /**
     * ????????????????????????
     */
    @Override
    protected void receiveGift(SocketResponse response) {
    }

    /**
     * ??????Socket Message
     */
    int[] Subscriptions = {

            //???????????????
            Mid.video_brokenLineRes,

            //??????
            Mid.BEAN_SUSPEND,

            //????????????
            Mid.invalidChat
    };

    OnCommonListener<SocketResponse> subscription = new OnCommonListener<SocketResponse>() {

        @Override
        public void execute(SocketResponse response) {

            switch (response.mid) {

                case Mid.video_brokenLineRes:
                    if (!isManager) {
                        finish();
                    }
                    break;

                case Mid.BEAN_SUSPEND:
                    finish();
                    AppManager.getInstance().exit(response.message, true);
                    break;

                case Mid.invalidChat:
                    new InvalidChatDialog(MultipleVideoActivity.this, response.content).show();
                    break;
            }
        }

    };

    class MessageEvent implements TIMMessageListener {

        @Override
        public boolean onNewMessages(List<TIMMessage> list) {
            for (TIMMessage timMessage : list) {
                TIMConversation conversation = timMessage.getConversation();
                if (conversation != null
                        && conversation.getType() == TIMConversationType.Group
                        && (chatInfo.mansionRoomId + "").equals(conversation.getPeer())) {
                    addNewMessage(timMessage);
                }
            }
            return false;
        }

        /**
         * ??????????????????
         */
        void addNewMessage(TIMMessage msg) {
            if (msg == null || isFinishing()) {
                return;
            }
            List<MessageInfo> messageList = MessageInfoUtil.TIMMessage2MessageInfoIgnore(msg, true);
            if (messageList != null && messageList.size() > 0) {
                for (MessageInfo messageInfo : messageList) {
                    if (messageInfo != null && (messageInfo.getMsgType() == MessageInfo.MSG_TYPE_TEXT
                            || (messageInfo.getExtra() != null && messageInfo.getExtra() instanceof ImCustomMessage))) {
                        if (messageInfo.getExtra() instanceof ImCustomMessage) {
                            ImCustomMessage customMessage = (ImCustomMessage) messageInfo.getExtra();
                            if (ImCustomMessage.Type_gift.equals(customMessage.type)) {
                                startAnim(customMessage.gift_gif_url);
                            } else if (ImCustomMessage.Type_kickUser.equals(customMessage.type)) {
                                eventHandler.onUserKicked(customMessage.otherId, 0);
                            } else if (ImCustomMessage.Type_pulp.equals(customMessage.type)) {
                                int id = 0;
                                try {
                                    id = Integer.parseInt(messageInfo.getFromUser());
                                } catch (Exception ignore) {
                                }
                                IllegalVideo(id - 10000);
                                return;
                            } else if (ImCustomMessage.Type_leaved.equals(customMessage.type)) {
                                try {
                                    int id = Integer.parseInt(messageInfo.getFromUser()) - 10000;
                                    if (id > 0) {
                                        Log.d("EngineEvent", "newMessage: " + id);
                                        eventHandler.onUserOffline(id, 2);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        messageAdapter.add(messageInfo, mMessageRv);
                    }
                }
            }
        }

    }

    /**
     * ??????????????????
     */
    private void startAnim(String gift_gif_url) {
        if (!TextUtils.isEmpty(gift_gif_url)) {
            SVGAParser parser = new SVGAParser(mContext);
            try {
                URL url = new URL(gift_gif_url);
                parser.parse(url, new SVGAParser.ParseCompletion() {
                    @Override
                    public void onComplete(@NotNull SVGAVideoEntity videoItem) {
                        if (isFinishing()) {
                            return;
                        }
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

    /**
     * ????????????
     */
    class EventHandler implements RtcEngineEventHandler {

        //----------------????????????------------------

        /**
         * ????????????
         */
        void remove(final Role role) {

            new AlertDialog.Builder(mContext)
                    .setMessage("???????????????????????????")
                    .setNegativeButton("??????", null)
                    .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();

                            if (role.userInfo == null) {
                                getMansionInfo(new OnCommonListener<MansionInfo>() {
                                    @Override
                                    public void execute(MansionInfo mansionInfo) {
                                        if (mansionInfo != null && mansionInfo.anochorInfo != null) {
                                            for (MansionUserInfoBean mansionUserInfoBean : mansionInfo.anochorInfo) {
                                                if (role.uid == mansionUserInfoBean.t_id) {
                                                    role.userInfo = mansionUserInfoBean;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                });
                                ToastUtil.showToast("???????????????,???????????????");
                                return;
                            }

                            Map<String, Object> paramMap = new HashMap<>();
                            paramMap.put("userId", getUserId());
                            paramMap.put("mansionRoomId", chatInfo.mansionRoomId);
                            String method = ChatApi.breakMansionLink();
                            paramMap.put("breakUserId", role.uid);
                            paramMap.put("roomId", role.userInfo.roomId);
                            paramMap.put("breakType", chatInfo.chatType);
                            OkHttpUtils.post().url(method).addParams("param", ParamUtil.getParam(paramMap)).build()
                                    .execute(new AjaxCallback<BaseResponse>() {

                                        @Override
                                        public void onResponse(BaseResponse response, int id) {

                                            if (isFinishing()) {
                                                return;
                                            }

                                            if (response.m_istatus == NetCode.SUCCESS) {

                                                adapter.notifyItemChanged(roleList.indexOf(role));

                                                //????????????Im??????
                                                TIMElem timElem = ImCustomMessage.buildRoomMessage(
                                                        ImCustomMessage.Type_kickUser,
                                                        AppManager.getInstance().getUserInfo().t_id,
                                                        role.uid,
                                                        role.getNickName());
                                                sendMessage(timElem, null);

                                                role.reset();
                                            } else {
                                                onError(null, null, 0);
                                            }

                                        }

                                        @Override
                                        public void onError(Call call, Exception e, int id) {
                                            if (isFinishing()) {
                                                return;
                                            }
                                            ToastUtil.showToast("????????????????????????");
                                        }

                                        @Override
                                        public void onBefore(Request request, int id) {
                                            showLoadingDialog();
                                        }

                                        @Override
                                        public void onAfter(int id) {
                                            dismissLoadingDialog();
                                        }

                                    });

                        }
                    }).create().show();
        }

        /**
         * ??????
         */
        void mute(Role role, ImageView muteBtn) {
            role.setMuted(!role.muted);
            rtcManager.rtcEngine().muteLocalAudioStream(role.muted);
            muteBtn.setImageResource(role.muted ? R.drawable.multiple_chat_mute_selected : R.drawable.multiple_chat_mute_unselected);
        }

        /**
         * ??????
         */
        void speaker(Role role, ImageView speakerBtn) {
            role.setMutedAudio(!role.mutedAudio);
            rtcManager.rtcEngine().muteRemoteAudioStream(role.uid, role.mutedAudio);
            speakerBtn.setImageResource(role.mutedAudio ? R.drawable.multiple_chat_speaker_selected : R.drawable.multiple_chat_speaker_unselected);
        }


        //----------------????????????------------------

        //----------------????????????------------------

//        @Override
//        public void onLocalVideoFrameCapturedBytes(TTTVideoFrame frame) {
//            //????????????
//            if (!chatInfo.isAudioChat()) {
//                QiNiuChecker.get().checkTakeShot(frame.buf, frame.stride, frame.height);
//            }
//        }

        //----------------????????????------------------

        //----------------????????????------------------

        AlertDialog alertDialog;

        void alertError(final int errorType, final String message) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (alertDialog == null) {
                        alertDialog = new AlertDialog.Builder(mContext)
                                .setCancelable(false)
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                })
                                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        joinChannel();
                                    }
                                }).create();
                    }
                    alertDialog.setMessage(message + errorType);
                    alertDialog.show();
                }
            });
        }

        private void bindRole(final int userId, final Role newRole, final MansionUserInfoBean info) {
            if (newRole == null) {
                return;
            }
            for (Role r : roleList) {
                if (r == roleManager && roleManager.isJoinRoom)
                    continue;
                if (r.uid == userId && r.isJoinRoom) {
                    return;
                }
            }
            newRole.bind(userId);
            newRole.setUserInfo(info);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyItemChanged(roleList.indexOf(newRole));
                }
            });
        }

        /**
         * ???????????????
         */
        public void onUserKicked(int uid, int reason) {
            if (uid == selfRole.uid) {
                finish();
            } else {
                onUserOffline(uid, reason);
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startTimer();
                    if (!isManager) {
                        //??????????????????
                        TIMElem timElem = ImCustomMessage.buildRoomMessage(
                                ImCustomMessage.Type_joined,
                                AppManager.getInstance().getUserInfo().t_id, 0, "");
                        sendMessage(timElem, null);
                    }

                    selfRole.bind(AppManager.getInstance().getUserInfo().t_id);
                    adapter.notifyItemChanged(roleList.indexOf(selfRole));
                }
            });
        }

        @Override
        public void onUserOffline(final int uid, final int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (BuildConfig.DEBUG) {
                        ToastUtil.showToast(uid + "????????????" + reason);
                    }

                    //????????????
                    if (uid == roleManager.uid) {
                        finish();
                        return;
                    }

                    //??????????????????
                    for (Role role : roleList) {
                        if (role.uid == uid) {
                            role.reset();
                            adapter.notifyItemChanged(roleList.indexOf(role));
                            break;
                        }
                    }
                }
            });
        }

        @Override
        public void onUserJoined(final int uid, int elapsed) {

            //????????????
            if (uid == roleManager.uid) {
                if (!roleManager.isJoinRoom) {
                    bindRole(uid, roleManager, null);
                }
                return;
            }

            //??????????????????
            for (Role r : roleList) {
                if (r == roleManager && roleManager.isJoinRoom)
                    continue;
                if (r.uid == uid && r.isJoinRoom) {
                    return;
                }
            }

            //??????????????????
            getMansionInfo(new OnCommonListener<MansionInfo>() {
                @Override
                public void execute(MansionInfo mansionInfo) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MansionUserInfoBean info = null;
                            if (mansionInfo != null) {

                                //??????????????????
                                for (MansionUserInfoBean userInfoBean : mansionInfo.anochorInfo) {
                                    if (uid == userInfoBean.t_id) {
                                        info = userInfoBean;
                                        break;
                                    }
                                }

                                //??????????????????
                                for (Role role : roleList) {
                                    if (role == roleManager)
                                        continue;
                                    if (role.isJoinRoom) {
                                        boolean needReset = true;
                                        for (MansionUserInfoBean userInfoBean : mansionInfo.anochorInfo) {
                                            if (role.uid == userInfoBean.t_id) {
                                                needReset = false;
                                                break;
                                            }
                                        }
                                        if (needReset) {
                                            role.reset();
                                            adapter.notifyItemChanged(roleList.indexOf(selfRole));
                                        }
                                    }
                                }
                            }

                            //????????????
                            Role role = findNext();
                            bindRole(uid, role, info);
                        }
                    });
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            if (state == io.agora.rtc.Constants.REMOTE_VIDEO_STATE_DECODING) {
                onUserJoined(uid, elapsed);
            }
        }

        @Override
        public void onUserMuteVideo(int uid, boolean muted) {

        }

        //----------------????????????------------------
    }

    /**
     * ??????????????????
     */
    public static class MansionInfo extends BaseBean {
        public MansionUserInfoBean userInfo;
        public List<MansionUserInfoBean> anochorInfo;
        public String mansionRoomName;
    }

    /**
     * ??????
     */
    static class Role {

        /**
         * ????????????&data
         */
        static int OperateBind = 0x01;

        /**
         * ????????????
         */
        static int OperateReset = 0x03;

        int clientRole;

        boolean isJoinRoom;

        int operate;

        int uid;

        View surfaceView;

        MansionUserInfoBean userInfo;

        //???????????????/???????????????
        boolean muted;

        //??????????????????/??????????????????
        boolean mutedAudio;

        //??????/??????
        boolean speaker = true;

        ViewHolder viewHolder;

        Role(int clientRole) {
            this.clientRole = clientRole;
        }

        void setMuted(boolean muted) {
            this.muted = muted;
        }

        void setMutedAudio(boolean mutedAudio) {
            this.mutedAudio = mutedAudio;
        }

        void setUserInfo(MansionUserInfoBean userInfo) {
            if (userInfo == null) {
                return;
            }
            this.userInfo = userInfo;
        }

        void bind(int uid) {
            operate = OperateBind;
            this.uid = uid;
            this.isJoinRoom = true;
        }

        void reset() {
            uid = 0;
            isJoinRoom = false;
            operate = OperateReset;
            muted = false;
            mutedAudio = false;
            if (surfaceView != null && surfaceView.getParent() != null) {
                ViewGroup viewGroup = (ViewGroup) surfaceView.getParent();
                viewGroup.removeView(surfaceView);
            }
            surfaceView = null;
        }

        String getNickName() {
            if (userInfo != null)
                return userInfo.t_nickName;
            if (uid != 0)
                return "" + (10000 + uid);
            return null;
        }

        String getCover() {
            if (userInfo != null) {
                return TextUtils.isEmpty(userInfo.t_cover_img) ? userInfo.t_handImg : userInfo.t_cover_img;
            }
            return null;
        }

        String getHead() {
            if (userInfo != null) {
                return userInfo.t_handImg;
            }
            return null;
        }

    }

    /**
     * ????????????
     */
    static class RoleAnchor extends Role {
        RoleAnchor() {
            super(0);
        }
    }

    /**
     * ????????????
     */
    static class RoleBroadcaster extends Role {
        RoleBroadcaster() {
            super(0);
        }
    }

}