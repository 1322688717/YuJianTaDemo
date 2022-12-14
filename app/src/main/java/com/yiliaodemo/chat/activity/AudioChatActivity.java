package com.yiliaodemo.chat.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.opensource.svgaplayer.SVGADrawable;
import com.opensource.svgaplayer.SVGAImageView;
import com.opensource.svgaplayer.SVGAParser;
import com.opensource.svgaplayer.SVGAVideoEntity;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.AVChatBean;
import com.yiliaodemo.chat.bean.AudioUserBean;
import com.yiliaodemo.chat.bean.ChatMessageBean;
import com.yiliaodemo.chat.bean.CustomMessageBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.dialog.GiftDialog;
import com.yiliaodemo.chat.dialog.MoneyNotEnoughDialog;
import com.yiliaodemo.chat.dialog.VipDialog;
import com.yiliaodemo.chat.helper.ChargeHelper;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.helper.ImageLoadHelper;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.AudioVideoRequester;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.rtc.RtcEngineEventHandler;
import com.yiliaodemo.chat.rtc.RtcManager;
import com.yiliaodemo.chat.socket.SocketMessageManager;
import com.yiliaodemo.chat.socket.domain.Mid;
import com.yiliaodemo.chat.socket.domain.SocketResponse;
import com.yiliaodemo.chat.util.LogUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.SoundRing;
import com.yiliaodemo.chat.util.ToastUtil;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMCustomElem;
import com.tencent.imsdk.TIMElem;
import com.tencent.imsdk.TIMElemType;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMMessageListener;
import com.tencent.imsdk.TIMValueCallBack;
import com.zhy.http.okhttp.OkHttpUtils;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.agora.rtc.Constants;
import okhttp3.Call;


public class AudioChatActivity extends BaseActivity implements TIMMessageListener {

    //??????????????????----------------------------
    //??????
    @BindView(R.id.head_iv)
    ImageView headIv;
    //??????
    @BindView(R.id.name_tv)
    TextView nameTv;
    //??????
    @BindView(R.id.sign_tv)
    TextView mSignTv;

    //??????????????????----------------------------
    //????????????, ???????????????????????????
    @BindView(R.id.middle_hang_up_tv)
    TextView mMiddleHangUpTv;
    //????????????
    @BindView(R.id.left_hang_up_tv)
    TextView mLeftHangUpTv;
    //??????
    @BindView(R.id.answer_tv)
    TextView mAnswerTv;

    //??????????????????------------------------------
    @BindView(R.id.middle_action_ll)
    LinearLayout mMiddleActionLl;
    //??????
    @BindView(R.id.time_ch)
    Chronometer timeCh;
    //??????
    @BindView(R.id.mute_tv)
    TextView muteTv;
    //??????
    @BindView(R.id.speaker_tv)
    TextView speakerTv;
    //??????
    @BindView(R.id.focus_tv)
    TextView mFocusTv;
    //??????????????????
    @BindView(R.id.calling_des_tv)
    TextView mCallingDesTv;

    //????????????-------------------
    //gif????????????
    @BindView(R.id.gif_sv)
    SVGAImageView mGifSv;
    @BindView(R.id.gift_ll)
    LinearLayout mGiftLl;
    @BindView(R.id.gift_head_iv)
    ImageView mGiftHeadIv;
    @BindView(R.id.gift_des_tv)
    TextView mGiftDesTv;
    @BindView(R.id.gift_iv)
    ImageView mGiftIv;
    @BindView(R.id.gift_number_tv)
    TextView mGiftNumberTv;

    //????????????????????????
    protected static final int StateCall = 0;
    //???????????????
    protected static final int StateChatting = 1;
    //????????????????????????
    protected static final int StateReceive = 2;

    //????????????
    private int state;

    protected SoundRing soundRing;

    private CountDownTimer countDownTimer;

    //???????????????
    public static boolean isChatting;

    //????????????????????????????????????????????????
    private int mSingleTimeSendGiftCount = 0;
    //??????
    protected TIMConversation mConversation;
    //?????????
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private AVChatBean chatBean;

    private RtcManager rtcManager;

    /**
     * ??????????????????
     */
    public static void startCall(final Context context, final AVChatBean bean) {
        Intent starter = new Intent(context, AudioChatActivity.class);
        starter.putExtra("state", StateCall);
        starter.putExtra("bean", bean);
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    /**
     * ????????????????????????
     */
    public static void startReceive(final Context context, final AVChatBean bean) {
        AudioVideoRequester.getAgoraSign(bean.roomId, new OnCommonListener<String>() {
            @Override
            public void execute(String s) {
                if (TextUtils.isEmpty(s)) {
                    ToastUtil.showToast("????????????");
                } else {
                    bean.sign = s;
                    Intent starter = new Intent(context, AudioChatActivity.class);
                    starter.putExtra("state", StateReceive);
                    starter.putExtra("bean", bean);
                    try {
                        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, starter, PendingIntent.FLAG_UPDATE_CURRENT);
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
    }

    /**
     * ????????????
     */
    protected final void stopTime() {
        if (timeCh != null) {
            timeCh.stop();
        }
    }

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_audio_chat);
    }

    @Override
    protected boolean supportFullScreen() {
        return true;
    }

    @Override
    protected void onContentAdded() {
        isChatting = true;
        SocketMessageManager.get().subscribe(subscription, Subscriptions);
        needHeader(false);
        soundRing = getSoundRing();
        initData(getIntent());
        initIm();
    }

    protected SoundRing getSoundRing() {
        return new SoundRing();
    }

    private void initData(Intent intent) {
        state = intent.getIntExtra("state", 0);
        chatBean = (AVChatBean) getIntent().getSerializableExtra("bean");
        getUserInfo();
        initEngine();
        updateState(state);

        timeCh.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                //30s????????????
                String time = chronometer.getText().toString();
                if (!time.equals("00:00") && (time.endsWith("00") || time.endsWith("30"))) {
                    getChatState();
                }
            }
        });
    }

    private void initIm() {
        String peer = String.valueOf(10000 + chatBean.otherId);
        mConversation = TIMManager.getInstance().getConversation(TIMConversationType.C2C, peer);
        TIMManager.getInstance().addMessageListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initData(intent);
    }

    @OnClick({
            R.id.mute_tv,
            R.id.middle_hang_up_tv,
            R.id.speaker_tv,
            R.id.answer_tv,
            R.id.left_hang_up_tv,
            R.id.report_tv,
            R.id.focus_tv,
            R.id.charge_tv,
            R.id.gift_tv
    })
    public void onClick(View view) {
        switch (view.getId()) {

            //??????
            case R.id.left_hang_up_tv:
            case R.id.middle_hang_up_tv: {
                new AlertDialog.Builder(mContext)
                        .setMessage("???????????????????????????")
                        .setNegativeButton("????????????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).setPositiveButton("????????????", null).create().show();
                break;
            }

            //??????
            case R.id.answer_tv: {
                startTimer();
                break;
            }

            //??????
            case R.id.mute_tv: {
                boolean enable = !view.isSelected();
                rtcManager.rtcEngine().muteLocalAudioStream(enable);
                view.setSelected(enable);
                break;
            }

            //?????????
            case R.id.speaker_tv: {
                setSpeaker(!view.isSelected());
                break;
            }

            case R.id.report_tv: {//??????
                Intent intent = new Intent(getApplicationContext(), ReportActivity.class);
                intent.putExtra(Constant.ACTOR_ID, chatBean.otherId);
                startActivity(intent);
                break;
            }

            case R.id.focus_tv: {//??????
                if (chatBean.otherId > 0) {
                    String text = mFocusTv.getText().toString().trim();
                    if (text.equals(getString(R.string.focus))) {//?????????
                        saveFollow(chatBean.otherId);
                    } else {
                        cancelFollow(chatBean.otherId);
                    }
                }
                break;
            }

            case R.id.charge_tv: {//??????
                Intent intent = new Intent(getApplicationContext(), ChargeActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.gift_tv: {//??????
                new GiftDialog(mContext, chatBean.otherId).show();
                break;
            }

        }
    }

    private void setSpeaker(boolean b) {
        rtcManager.rtcEngine().setEnableSpeakerphone(b);
        findViewById(R.id.speaker_tv).setSelected(b);
    }

    /**
     * ????????????
     */
    private void saveFollow(int actorId) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());//?????????
        paramMap.put("coverFollowUserId", String.valueOf(actorId));//	????????????
        OkHttpUtils.post().url(ChatApi.SAVE_FOLLOW())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    String message = response.m_strMessage;
                    if (!TextUtils.isEmpty(message) && message.contains(getResources().getString(R.string.success_str))) {
                        ToastUtil.showToast(getApplicationContext(), message);
                        mFocusTv.setSelected(true);
                        mFocusTv.setText(getString(R.string.have_focus));
                    }
                } else {
                    ToastUtil.showToast(getApplicationContext(), R.string.system_error);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                ToastUtil.showToast(getApplicationContext(), R.string.system_error);
            }

        });
    }

    /**
     * ????????????
     */
    private void cancelFollow(int actorId) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());//?????????
        paramMap.put("coverFollow", String.valueOf(actorId));//	????????????
        OkHttpUtils.post().url(ChatApi.DEL_FOLLOW())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    String message = response.m_strMessage;
                    if (!TextUtils.isEmpty(message) && message.contains(getResources().getString(R.string.success_str))) {
                        ToastUtil.showToast(getApplicationContext(), message);
                        mFocusTv.setSelected(false);
                        mFocusTv.setText(getString(R.string.focus));
                    }
                } else {
                    ToastUtil.showToast(getApplicationContext(), R.string.system_error);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                ToastUtil.showToast(getApplicationContext(), R.string.system_error);
            }
        });
    }

    protected void onAudio(int type) {
        switch (type) {
            //????????????
            case Mid.onLineToVoiceRes:
                break;
            //????????????
            case Mid.anchorLinkUserToVoiceRes:
                break;
            //??????
            case Mid.brokenVoiceLineRes:
                otherBroken();
                break;
        }
    }

    protected void brokenVIPLineRes(int roomId, int breakId, int code) {

    }

    /**
     * ????????????
     */
    protected void otherBroken() {
        if (!isFinishing()) {
            finish();
        }
    }

    protected void moneyNotEnough() {
        new MoneyNotEnoughDialog(this).show();
    }

    @Override
    public void finish() {
        destroy();
        super.finish();
    }

    RtcEngineEventHandler rtcEngineEventHandler = new RtcEngineEventHandler() {

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d("EngineEvent", "onJoinChannelSuccess: " + uid);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d("EngineEvent", "onUserOffline: " + uid);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastUtil.showToast("???????????????");
                    finish();
                    hangUp();
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.d("EngineEvent", "onUserJoined: " + uid);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopCounter();
                    userJoined();
                    updateState(StateChatting);
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {

        }

        @Override
        public void onUserMuteVideo(int uid, boolean mute) {

        }

    };

    private void initEngine() {
        rtcManager = RtcManager.get();
        rtcManager.rtcEngine().enableAudio();
        rtcManager.rtcEngine().muteLocalAudioStream(false);
        rtcManager.rtcEngine().muteLocalVideoStream(true);
        rtcManager.addRtcHandler(rtcEngineEventHandler);
    }

    private void joinChannel() {
        rtcManager.rtcEngine().setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        rtcManager.rtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        rtcManager.rtcEngine().joinChannel(
                chatBean.sign,
                chatBean.roomId + "",
                null,
                AppManager.getInstance().getUserInfo().t_id);
    }

    private void destroy() {
        if (rtcManager == null) {
            return;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mGifSv != null) {
            mGifSv.pauseAnimation();
            mGifSv = null;
        }
        TIMManager.getInstance().removeMessageListener(this);
        soundRing.stop();
        stopTime();
        stopCounter();
        hangUp();
        destroyRtc();
        isChatting = false;
    }

    private void destroyRtc() {
        if (rtcManager != null) {
            rtcManager.removeRtcHandler(rtcEngineEventHandler);
            rtcManager.rtcEngine().leaveChannel();
            rtcManager = null;
        }
    }

    protected void userJoined() {
//        if (RecordUploader.get().isSaveRecord()) {
//            mTttRtcEngine.startAudioRecording(RecordUploader.get().createRecordPath(chatBean.otherId),
//                    Constants.TTT_AUDIO_RECORDING_QUALITY_MEDIUM);
//        }
    }

    /**
     * ????????????????????????
     */
    private void updateState(int state) {
        this.state = state;
        switch (state) {

            //????????? , ??????????????????????????? ??????????????????
            case StateChatting: {

                setSpeaker(true);

                soundRing.stop();
                //????????????
                timeCh.setVisibility(View.VISIBLE);
                timeCh.setFormat("%s");
                timeCh.setBase(SystemClock.elapsedRealtime());
                timeCh.start();

                //??????????????????  ????????????
                mSignTv.setVisibility(View.VISIBLE);
                mMiddleActionLl.setVisibility(View.VISIBLE);
                mMiddleHangUpTv.setVisibility(View.VISIBLE);
                mCallingDesTv.setVisibility(View.GONE);
                mLeftHangUpTv.setVisibility(View.GONE);
                mAnswerTv.setVisibility(View.GONE);
                break;
            }
            //??????????????????
            case StateReceive: {
                //????????????  ??????????????????
                mMiddleHangUpTv.setVisibility(View.GONE);
                mLeftHangUpTv.setVisibility(View.VISIBLE);
                mAnswerTv.setVisibility(View.VISIBLE);

                startTimeCounter(true);
                soundRing.start();
                break;
            }
            //??????????????????
            case StateCall: {
                //??????????????????  ????????????????????????
                joinChannel();
                mCallingDesTv.setVisibility(View.VISIBLE);
                startTimeCounter(false);
                soundRing.start();
                break;
            }
        }
    }

    private void getChatState() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("videoUserId", chatBean.getUserId());
        paramMap.put("videoCoverUserId", chatBean.getActorId());
        paramMap.put("roomId", chatBean.roomId);

        OkHttpUtils.post().url(ChatApi.getVideoStatus())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<Integer>>() {

            @Override
            public void onResponse(BaseResponse<Integer> response, int id) {
                if (isFinishing()) {
                    return;
                }
                //??????????????????
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
     * ??????????????????????????????30s
     */
    private void startTimeCounter(final boolean isReceive) {
        stopCounter();
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                timeFinish(isReceive);
            }
        };
        countDownTimer.start();
    }

    private void stopCounter() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    protected void timeFinish(boolean isReceive) {
        if (!isReceive) {
            ToastUtil.showToast(getApplication(), R.string.no_response);
        }
        finish();
    }

    /**
     * ????????????????????????
     */
    private void startTimer() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("anthorId", chatBean.getActorId());
        paramMap.put("userId", chatBean.getUserId());
        paramMap.put("chatType", 2);//1:?????? 2:??????
        paramMap.put("roomId", chatBean.roomId);
        OkHttpUtils.post().url(ChatApi.VIDEO_CHAT_BIGIN_TIMING())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                if (isFinishing()) {
                    return;
                }
                if (response != null) {
                    if (response.m_istatus == NetCode.SUCCESS) {
                        joinChannel();
                    } else if (response.m_istatus == -7) {
                        new VipDialog(AudioChatActivity.this, "?????????????????????VIP???????????????").show();
                    } else if (response.m_istatus == -1) {
                        ChargeHelper.showSetCoverDialog(AudioChatActivity.this);
                    } else {
                        if (!TextUtils.isEmpty(response.m_strMessage)) {
                            ToastUtil.showToast(getApplicationContext(), response.m_strMessage);
                        } else {
                            ToastUtil.showToast(getApplicationContext(), R.string.please_retry);
                        }
                        finish();
                    }
                } else {
                    ToastUtil.showToast(getApplicationContext(), R.string.please_retry);
                    finish();
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                ToastUtil.showToast(getApplicationContext(), R.string.please_retry);
                finish();
            }
        });
    }

    /**
     * ?????????????????????
     * ++++++++++++++++????????????++++++++++++++
     */
    private void getUserInfo() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        paramMap.put("coverUserId", chatBean.otherId);
        OkHttpUtils.post().url(ChatApi.getUserInfoById())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<AudioUserBean>>() {
            @Override
            public void onResponse(BaseResponse<AudioUserBean> response, int id) {
                if (isFinishing()) {
                    return;
                }
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    AudioUserBean audioUserBean = response.m_object;
                    //??????
                    nameTv.setText(audioUserBean.nickName);
                    //??????
                    if (TextUtils.isEmpty(audioUserBean.handImg)) {
                        headIv.setImageResource(R.drawable.default_head_img);
                    } else {
                        ImageLoadHelper.glideShowCircleImageWithUrl(AudioChatActivity.this,
                                audioUserBean.handImg, headIv);
                        //????????????
                        ImageLoadHelper.glideShowCircleImageWithUrl(AudioChatActivity.this,
                                audioUserBean.handImg, mGiftHeadIv);
                    }
                    //????????????
                    mSignTv.setText(audioUserBean.t_autograph);
                    //??????
                    if (audioUserBean.isFollow == 0) {//?????????
                        mFocusTv.setSelected(false);
                        mFocusTv.setText(getString(R.string.focus));
                    } else {
                        mFocusTv.setSelected(true);
                        mFocusTv.setText(getString(R.string.have_focus));
                    }
                    updateUser(audioUserBean);
                }
            }
        });
    }

    protected void updateUser(AudioUserBean audioUserBean) {

    }

    /**
     * ??????????????????????????????
     */
    private void hangUp() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("roomId", chatBean.roomId);
        OkHttpUtils.post().url(ChatApi.BREAK_LINK())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(
                new AjaxCallback<BaseResponse>() {
                    @Override
                    public void onResponse(BaseResponse response, int id) {
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        super.onError(call, e, id);
                        ToastUtil.showToast(getApplicationContext(), R.string.system_error);
                    }
                });
    }


    /**
     * ????????????
     */
    private void sendMessage(TIMElem timElem) {
        //?????????????????????????????????????????????
        TIMMessage msg = new TIMMessage();
        if (msg.addElement(timElem) != 0) {
            ToastUtil.showToast(getApplicationContext(), R.string.element_send_fail);
            return;
        }
        if (mConversation != null) {
            if (timElem != null) {
                //????????????
                mConversation.sendMessage(msg, new TIMValueCallBack<TIMMessage>() {
                    @Override
                    public void onError(int code, String desc) {//??????????????????
                        //????????? code ??????????????? desc????????????????????????????????????
                        String content = "TIM send message failed. code: " + code + " errmsg: " + desc;
                        LogUtil.i(content);
                        ToastUtil.showToast(getApplicationContext(), content);
                        IMHelper.resLogin(code);
                    }

                    @Override
                    public void onSuccess(TIMMessage msg) {//??????????????????
                        LogUtil.i("TIM SendMsg bitmap");
                        addNewMessage(msg, false);
                    }
                });
            } else {
                ToastUtil.showToast(getApplicationContext(), R.string.element_send_fail);
            }
        } else {
            ToastUtil.showToast(getApplicationContext(), R.string.tim_send_fail);
            initIm();
        }
    }

    /**
     * ?????????
     */
    @Override
    public boolean onNewMessages(List<TIMMessage> list) {
        LogUtil.i("?????????????????????????????????");
        //????????????
        String peer = String.valueOf(10000 + chatBean.otherId);
        for (TIMMessage timMessage : list) {
            TIMConversation conversation = timMessage.getConversation();
            if (conversation != null && conversation.getType() == TIMConversationType.C2C
                    && !TextUtils.isEmpty(conversation.getPeer())
                    && TextUtils.equals(conversation.getPeer(), peer)) {
                addNewMessage(timMessage, true);
            }
        }
        return true;
    }

    /**
     * ??????????????????
     */
    private void addNewMessage(TIMMessage msg, boolean needParse) {
        for (int i = 0; i < msg.getElementCount(); ++i) {
            ChatMessageBean chatMessageBean = new ChatMessageBean();
            chatMessageBean.isSelf = msg.isSelf();
            chatMessageBean.time = msg.timestamp();
            TIMElem elem = msg.getElement(i);
            if (elem.getType() == TIMElemType.Custom) {//??????
                TIMCustomElem customElem = (TIMCustomElem) elem;
                byte[] data = customElem.getData();
                String json = new String(data);
                if (needParse) {
                    parseCustomMessage(json);
                }
            }
        }
    }

    /**
     * ?????????????????????
     */
    private void parseCustomMessage(String json) {
        try {
            CustomMessageBean bean = CustomMessageBean.parseBean(json);
            if (bean != null) {
                if (bean.type.equals("1")) {//??????
                    LogUtil.i("??????????????????: " + bean.gift_name);
                    startGif(bean.gift_gif_url);
                    startGiftInAnim(bean, false, false);
                } else if (bean.type.equals("0")) {//??????
                    bean.gift_name = getResources().getString(R.string.gold);
                    LogUtil.i("??????????????????: " + bean.gift_name);
                    startGiftInAnim(bean, false, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????GIF??????
     */
    private void startGif(String path) {
        if (!TextUtils.isEmpty(path)) {
            SVGAParser parser = new SVGAParser(this);
            try {
                URL url = new URL(path);
                parser.parse(url, new SVGAParser.ParseCompletion() {
                    @Override
                    public void onComplete(@NotNull SVGAVideoEntity videoItem) {

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
     * ????????????????????????
     *
     * @param fromSend ??????????????? ????????????
     */
    private void startGiftInAnim(CustomMessageBean bean, boolean fromSend, boolean isGold) {
        String lastDes = mGiftDesTv.getText().toString().trim();
        //????????????????????????????????????,????????????????????????,???????????????
        if ((!TextUtils.isEmpty(lastDes) && !lastDes.contains(bean.gift_name)) || isGold) {
            mSingleTimeSendGiftCount = 0;
        }
        mSingleTimeSendGiftCount++;
        if (mSingleTimeSendGiftCount == 1) {//??????????????????????????????
            //??????
            String des;
            if (fromSend) {//?????????
                des = getResources().getString(R.string.send_to) + bean.gift_name;
            } else {//?????????
                des = getResources().getString(R.string.send_you) + bean.gift_name;
            }
            mGiftDesTv.setText(des);
            //????????????
            if (isGold) {//?????????
                mGiftIv.setImageResource(R.drawable.ic_gold);
                String goldNumber = getResources().getString(R.string.multi) + bean.gold_number;
                mGiftNumberTv.setText(goldNumber);
            } else {
                ImageLoadHelper.glideShowImageWithUrl(this, bean.gift_still_url, mGiftIv);
                String giftNumber = getResources().getString(R.string.multi) + mSingleTimeSendGiftCount;
                mGiftNumberTv.setText(giftNumber);
            }

            mGiftLl.setVisibility(View.VISIBLE);
            mGiftLl.clearAnimation();
            TranslateAnimation mGiftLayoutInAnim = (TranslateAnimation) AnimationUtils.loadAnimation(getApplicationContext(), R.anim.lp_gift_in);
            mGiftLl.setAnimation(mGiftLayoutInAnim);
            mGiftLayoutInAnim.start();
            mHandler.removeCallbacks(mGiftRunnable);
            mGiftLayoutInAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //2????????????
                    mHandler.postDelayed(mGiftRunnable, 3000);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        } else {
            if (!isGold) {//??????
                String giftNumber = getResources().getString(R.string.multi) + mSingleTimeSendGiftCount;
                mGiftNumberTv.setText(giftNumber);

                mHandler.removeCallbacks(mGiftRunnable);
                mHandler.postDelayed(mGiftRunnable, 3000);
                startComboAnim(mGiftNumberTv);
            }
        }

    }

    private void startGiftOutAnim() {
        mGiftLl.clearAnimation();
        Animation mGiftLayoutInAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.lp_gift_out);
        mGiftLl.setAnimation(mGiftLayoutInAnim);
        mGiftLayoutInAnim.start();
        mGiftLayoutInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (mSingleTimeSendGiftCount != 1) {
                    mGiftLl.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void startComboAnim(final TextView giftNumView) {
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(giftNumView, "scaleX", 1.8f, 1.0f);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(giftNumView, "scaleY", 1.8f, 1.0f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.setDuration(300);
        animSet.setInterpolator(new OvershootInterpolator());
        animSet.playTogether(anim1, anim2);
        animSet.start();
    }

    /**
     * ??????runnable
     */
    private Runnable mGiftRunnable = new Runnable() {
        @Override
        public void run() {
            mSingleTimeSendGiftCount = 0;
            startGiftOutAnim();
        }
    };

    @Override
    protected void onDestroy() {
        destroy();
        super.onDestroy();
    }

    /**
     * ??????Socket Message
     */
    int[] Subscriptions = {
            Mid.onLineToVoiceRes,
            Mid.anchorLinkUserToVoiceRes,
            Mid.brokenVoiceLineRes,
            Mid.MONEY_NOT_ENOUGH,
            Mid.brokenVIPLineRes,
            Mid.brokenUserLineRes,
    };

    OnCommonListener<SocketResponse> subscription = new OnCommonListener<SocketResponse>() {
        @Override
        public void execute(SocketResponse response) {
            switch (response.mid) {
                case Mid.onLineToVoiceRes:
                case Mid.anchorLinkUserToVoiceRes:
                case Mid.brokenVoiceLineRes:
                    onAudio(response.mid);
                    break;
                case Mid.MONEY_NOT_ENOUGH:
                    moneyNotEnough();
                    break;
                case Mid.brokenVIPLineRes:
                case Mid.brokenUserLineRes:
                    brokenVIPLineRes(response.roomId, response.breakUserId, response.mid);
                    break;
            }
        }
    };

}