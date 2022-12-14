package com.yiliaodemo.chat.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.bean.ChatUserInfo;
import com.yiliaodemo.chat.bean.ServeBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.im.ChatActivity;
import com.yiliaodemo.chat.im.ChatGroupActivity;
import com.yiliaodemo.chat.im.ImConstants;
import com.yiliaodemo.chat.im.ImNotifyManager;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.net.PageRequester;
import com.yiliaodemo.chat.util.LogUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.tencent.imsdk.TIMCallBack;
import com.tencent.imsdk.TIMConnListener;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.TIMConversationType;
import com.tencent.imsdk.TIMElem;
import com.tencent.imsdk.TIMElemType;
import com.tencent.imsdk.TIMFriendshipManager;
import com.tencent.imsdk.TIMGroupEventListener;
import com.tencent.imsdk.TIMGroupManager;
import com.tencent.imsdk.TIMGroupTipsElem;
import com.tencent.imsdk.TIMGroupTipsType;
import com.tencent.imsdk.TIMManager;
import com.tencent.imsdk.TIMMessage;
import com.tencent.imsdk.TIMRefreshListener;
import com.tencent.imsdk.TIMTextElem;
import com.tencent.imsdk.TIMUserConfig;
import com.tencent.imsdk.TIMUserProfile;
import com.tencent.imsdk.TIMUserStatusListener;
import com.tencent.imsdk.TIMValueCallBack;
import com.tencent.imsdk.conversation.ConversationManager;
import com.tencent.imsdk.ext.group.TIMGroupBaseInfo;
import com.tencent.imsdk.ext.group.TIMGroupDetailInfo;
import com.tencent.qcloud.tim.uikit.modules.chat.base.ChatInfo;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/*
 * Copyright (C) 2018
 * ????????????
 *
 * ????????????:  ???????????????
 * ?????????
 * ???????????????2019/5/31
 *
 * ????????????
 * ???????????????
 * ????????????
 */
public class IMHelper {


    private static OnCommonListener<Boolean> loginResult;

    /**
     * 6014	IM SDK ??????????????????????????????????????????????????????????????????????????????????????? TIMManager getLoginUser ????????????????????????
     * 6026	?????????????????????????????????????????????????????????????????? login ?????????????????????
     * 6206	UserSig ????????????????????????????????? UserSig ?????????????????????
     * 6004	???????????????getConversation ????????????????????????????????????????????????????????????????????????????????????
     * 6226	?????????????????????????????????????????? UserSig ???????????????
     * 70101 ??????????????????????????????
     *
     * @param code ????????????
     */
    public static void resLogin(int code) {
        List<Integer> filter = Arrays.asList(6014, 6206, 6206, 6004, 6226, 70101);
        if (filter.contains(code)) {
            ChatUserInfo chatUserInfo = SharedPreferenceHelper.getAccountInfo(AppManager.getInstance());
            getSign(chatUserInfo, AppManager.getInstance());
        }
    }

    public static void setLoginResult(OnCommonListener<Boolean> callback) {
        loginResult = callback;
    }

    public static boolean isLogined() {
        String loginUser = TIMManager.getInstance().getLoginUser();
        LogUtil.i("isLoginIM: " + loginUser);
        return !TextUtils.isEmpty(loginUser);
    }

    public static void checkLogin() {
        if (!isLogined()) {
            login();
        }
    }

    public static void login() {
        ChatUserInfo chatUserInfo = AppManager.getInstance().getUserInfo();
        if (chatUserInfo.t_id != 0) {
            LogUtil.i("login IM");
            getSign(chatUserInfo, AppManager.getInstance());
        }
    }

    /**
     * ????????????
     */
    private static void getSign(final ChatUserInfo chatUserInfo, final Context context) {
        final Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", chatUserInfo.t_id);
        OkHttpUtils.post().url(ChatApi.GET_IM_USER_SIG())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                LogUtil.i("??????IM????????????==--", call.request().url().toString() + "   Exception:  "
                        + e.getMessage());
            }

            @Override
            public void onResponse(String response, int id) {
                LogUtil.i("??????IM??????: " + response);
                boolean ok = false;
                if (!TextUtils.isEmpty(response)) {
                    JSONObject object = null;
                    try {
                        object = JSON.parseObject(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (object != null && object.containsKey("m_istatus") && object.getIntValue("m_istatus") == 1) {
                        String sign = object.getString("m_object");
                        if (!TextUtils.isEmpty(sign)) {
                            ok = true;
                            if (!isGetServeInfo) {
                                getSeverList(null);
                            }
                            loginTIM(chatUserInfo, sign, context);
                        } else {
                            ToastUtil.showToast(context, "??????????????????");
                        }
                    }
                } else {
                    ToastUtil.showToast(context, "?????????????????? response??????");
                }
                if (!ok) {
                    if (loginResult != null) {
                        loginResult.execute(false);
                    }
                }
            }
        });
    }

    /**
     * ????????????Im
     */
    private static void loginTIM(final ChatUserInfo chatUserInfo, String userSig, final Context context) {
        setUserConfig(chatUserInfo, context);
        // identifier???????????????userSig ?????????????????????
        String userName = String.valueOf(10000 + chatUserInfo.t_id);
        TIMManager.getInstance().login(userName, userSig, new TIMCallBack() {
            @Override
            public void onError(int code, String desc) {
                //????????? code ??????????????? desc????????????????????????????????????
                //????????? code ???????????????????????????
                LogUtil.i("TIM login failed. code: " + code + " errmsg: " + desc);
//                ToastUtil.showToastLong(context, "????????????????????????,???????????????");
//                exit(context);
                if (loginResult != null) {
                    loginResult.execute(false);
                }
            }

            @Override
            public void onSuccess() {
                LogUtil.i("TIM login succ");
                TIMManager.getInstance().addMessageListener(ImNotifyManager.get());
                syncGroup(null);
                syncUser();
                checkTIMInfo(chatUserInfo.t_nickName, chatUserInfo.headUrl);
                if (loginResult != null) {
                    loginResult.execute(true);
                }
            }
        });
    }

    /**
     * ????????????????????????
     */
    private static void syncUser() {
        List<String> list = new ArrayList<>();
        List<TIMConversation> conversationList = ConversationManager.getInstance().getConversationList();
        if (conversationList != null && conversationList.size() > 0) {
            for (TIMConversation timConversation : conversationList) {
                if (!TextUtils.isEmpty(timConversation.getPeer()) && timConversation.getType() == TIMConversationType.C2C) {
                    list.add(timConversation.getPeer());
                }
            }
        }
        if (list.size() > 0) {
            TIMFriendshipManager.getInstance().getUsersProfile(list,
                    true,
                    new TIMValueCallBack<List<TIMUserProfile>>() {

                        @Override
                        public void onError(int i, String s) {

                        }

                        @Override
                        public void onSuccess(List<TIMUserProfile> timUserProfiles) {

                        }

                    });
        }
    }

    /**
     * ??????????????????
     */
    private static void setUserConfig(final ChatUserInfo chatUserInfo, final Context activity) {
        //??????????????????
        TIMUserConfig userConfig = new TIMUserConfig()
                //???????????????????????????????????????
                .setUserStatusListener(new TIMUserStatusListener() {
                    @Override
                    public void onForceOffline() {
                        //????????????????????????
                        LogUtil.i("??????TIM onForceOffline");
                        exit(activity);
                    }

                    @Override
                    public void onUserSigExpired() {
                        //???????????????????????????????????? userSig ???????????? IM SDK
                        LogUtil.i("??????TIM  onUserSigExpired");
                        getSign(chatUserInfo, activity);
                    }
                })
                //?????????????????????????????????
                .setConnectionListener(new TIMConnListener() {
                    @Override
                    public void onConnected() {
                        LogUtil.i("??????TIM  onConnected");
                    }

                    @Override
                    public void onDisconnected(int code, String desc) {
                        LogUtil.i("??????TIM onDisconnected");
                        getSign(chatUserInfo, activity);
                    }

                    @Override
                    public void onWifiNeedAuth(String name) {
                        LogUtil.i("??????TIM  onWifiNeedAuth");
                    }
                })
                //???????????????????????????
                .setGroupEventListener(new TIMGroupEventListener() {
                    @Override
                    public void onGroupTipsEvent(TIMGroupTipsElem elem) {
                        LogUtil.i("??????TIM  onGroupTipsEvent, type: " + elem.getTipsType());
                    }
                })
                //???????????????????????????
                .setRefreshListener(new TIMRefreshListener() {
                    @Override
                    public void onRefresh() {
                        LogUtil.i("??????TIM  onRefresh");
                    }

                    @Override
                    public void onRefreshConversation(List<TIMConversation> conversations) {
                        LogUtil.i("??????TIM  onRefreshConversation, conversation size: " + conversations.size());
                    }
                });
        //?????????????????????????????????????????????
        TIMManager.getInstance().setUserConfig(userConfig);
    }

    /**
     * ??????TIM??????
     */
    public static void checkTIMInfo(final String nick, final String faceUrl) {
        //???????????????????????????????????????
        TIMFriendshipManager.getInstance().getSelfProfile(new TIMValueCallBack<TIMUserProfile>() {
            @Override
            public void onError(int i, String s) {
                LogUtil.i("TIM??????????????????: " + i + "  des: " + s);
            }

            @Override
            public void onSuccess(TIMUserProfile timUserProfile) {
                if (timUserProfile != null) {
                    //?????????????????????  ??????????????????
                    String timNick = timUserProfile.getNickName();
                    String timFaceUrl = timUserProfile.getFaceUrl();
                    if (!timNick.equals(nick) || !timFaceUrl.equals(faceUrl) || !isSameSex(timUserProfile.getGender())) {
                        updateTIMInfo(nick, faceUrl);
                    }
                }
            }
        });
    }

    /**
     * ??????TIM??????
     */
    private static void updateTIMInfo(String nick, String faceUrl) {
        HashMap<String, Object> profileMap = new HashMap<>();
        if (!TextUtils.isEmpty(nick)) {
            profileMap.put(TIMUserProfile.TIM_PROFILE_TYPE_KEY_NICK, nick);
        }
        if (!TextUtils.isEmpty(faceUrl)) {
            profileMap.put(TIMUserProfile.TIM_PROFILE_TYPE_KEY_FACEURL, faceUrl);
        }

        profileMap.put(TIMUserProfile.TIM_PROFILE_TYPE_KEY_GENDER, getImSex());

        TIMFriendshipManager.getInstance().modifySelfProfile(profileMap, new TIMCallBack() {
            @Override
            public void onError(int code, String desc) {
                LogUtil.i("TIM????????????  failed: " + code + " desc" + desc);
            }

            @Override
            public void onSuccess() {
                LogUtil.i("TIM???????????? success");
            }
        });

    }

    /**
     * ???userId??????Im??????
     */
    public static void sendMessage(int userId, String text, final TIMValueCallBack<TIMMessage> callBack) {
        String mTargetId = String.valueOf(10000 + userId);
        TIMTextElem textElem = new TIMTextElem();
        textElem.setText(text);
        TIMMessage msg = new TIMMessage();
        if (msg.addElement(textElem) != 0) {
            return;
        }
        TIMConversation mConversation = TIMManager.getInstance()
                .getConversation(TIMConversationType.C2C, mTargetId);
        if (mConversation != null) {
            mConversation.sendMessage(msg, new TIMValueCallBack<TIMMessage>() {
                @Override
                public void onError(int i, String s) {
                    if (callBack != null) {
                        callBack.onError(i, s);
                    }
                }

                @Override
                public void onSuccess(TIMMessage timMessage) {
                    if (callBack != null) {
                        callBack.onSuccess(timMessage);
                    }
                }
            });
        }
    }

    /**
     * ??????Im?????????
     */
    public static void toChat(Context context, String chatName, int otherId, int sex) {
        if (AppManager.getInstance().getUserInfo().t_id == otherId) {
            return;
        }
        checkServeIm(otherId, new OnCommonListener<Boolean>() {
            @Override
            public void execute(Boolean aBoolean) {
                if (aBoolean) {
                    toChatServe(context, chatName, otherId);
                } else {
                    if (sex != -1 && AppManager.getInstance().getUserInfo().isSameSexToast(context, sex))
                        return;
                    ChatInfo chatInfo = new ChatInfo();
                    chatInfo.setType(TIMConversationType.C2C);
                    chatInfo.setId(String.valueOf(otherId + 10000));
                    chatInfo.setChatName(chatName);
                    Intent intent = new Intent(context, ChatActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ImConstants.CHAT_INFO, chatInfo);
                    context.startActivity(intent);
                }
            }
        });
    }

    /**
     * ??????Im??????
     */
    public static void toGroup(Context context, String chatName, String peer) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setType(TIMConversationType.Group);
        chatInfo.setId(peer);
        chatInfo.setChatName(chatName);
        Intent intent = new Intent(context, ChatGroupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImConstants.CHAT_INFO, chatInfo);
        context.startActivity(intent);
    }

    /**
     * ??????
     */
    private static void exit(Context mContext) {
        AppManager.getInstance().exit(mContext.getString(R.string.login_other), true);
    }

    private static boolean isGetServeInfo;
    private static final Map<String, Boolean> serveBeanMap = new HashMap<>();

    /**
     * ??????
     * ChatFragment??????????????????
     * MessageUtil????????????????????????
     * toChat ??????sex??????
     */
    public static void toChatServe(Context context, String chatName, int actorId) {
        if (AppManager.getInstance().getUserInfo().t_id == actorId) {
            return;
        }
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setType(TIMConversationType.C2C);
        chatInfo.setId(String.valueOf(actorId + 10000));
        chatInfo.setChatName(chatName);
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("serve", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ImConstants.CHAT_INFO, chatInfo);
        context.startActivity(intent);
    }

    private static void getSeverList(OnCommonListener<Object> onCommonListener) {

        PageRequester<ServeBean> pageRequester = new PageRequester<ServeBean>() {

            @Override
            public void onSuccess(List<ServeBean> list, boolean isRefresh) {
                isGetServeInfo = true;
                serveBeanMap.clear();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(AppManager.getInstance());
                if (list != null && list.size() > 0) {
                    sp.edit().putString("serveListStr", JSON.toJSONString(list)).apply();
                    for (ServeBean serveBean : list) {
                        serveBeanMap.put(String.valueOf(serveBean.t_idcard), null);
                    }
                } else {
                    sp.edit().putString("serveListStr", null).apply();
                }
            }

            @Override
            public void onAfter(int id) {
                if (onCommonListener != null) {
                    onCommonListener.execute(null);
                }
            }
        };
        pageRequester.setApi(ChatApi.getServiceId());
        pageRequester.onRefresh();

    }

    public static void checkServeIm(final int otherId, final OnCommonListener<Boolean> onCommonListener) {
        if (!isGetServeInfo) {
            getSeverList(new OnCommonListener<Object>() {
                @Override
                public void execute(Object o) {
                    serveCallback(otherId, onCommonListener);
                }
            });
        } else {
            serveCallback(otherId, onCommonListener);
        }
    }

    private static boolean isSelfServe() {
        return serveBeanMap.containsKey(String.valueOf(AppManager.getInstance().getUserInfo().t_id + 10000));
    }

    private static void serveCallback(int otherId, OnCommonListener<Boolean> onCommonListener) {
        boolean serve = serveBeanMap.containsKey(String.valueOf(otherId + 10000)) || isSelfServe();
        if (onCommonListener != null) {
            onCommonListener.execute(serve);
        }
    }

    /**
     * ???????????????
     */
    public static boolean isPublicGroup(String groupId) {
        TIMGroupDetailInfo timGroupDetailInfo = TIMGroupManager.getInstance().queryGroupInfo(groupId);
        return timGroupDetailInfo != null && "Public".equals(timGroupDetailInfo.getGroupType());
    }

    /**
     * ???????????????
     */
    public static void syncGroup(OnCommonListener<Object> onCommonListener) {
        TIMGroupManager.getInstance().getGroupList(new TIMValueCallBack<List<TIMGroupBaseInfo>>() {

            @Override
            public void onError(int i, String s) {

            }

            @Override
            public void onSuccess(List<TIMGroupBaseInfo> timGroupBaseInfo) {
                LogUtil.i(JSON.toJSONString(timGroupBaseInfo));
                List<TIMConversation> conversationList = ConversationManager.getInstance().getConversationList();
                boolean clearAll = timGroupBaseInfo == null || timGroupBaseInfo.size() == 0;
                boolean haveChange = false;
                Map<String, Object> map = new HashMap<>();
                if (!clearAll) {
                    for (TIMGroupBaseInfo groupBaseInfo : timGroupBaseInfo) {
                        if ("Public".equals(groupBaseInfo.getGroupType())) {
                            map.put(groupBaseInfo.getGroupId(), null);
                        } else {
                            ConversationManager.getInstance().deleteConversationAndLocalMsgs(
                                    TIMConversationType.Group,
                                    groupBaseInfo.getGroupId());
                            TIMGroupManager.getInstance().quitGroup(groupBaseInfo.getGroupId(), new TIMCallBack() {

                                @Override
                                public void onError(int i, String s) {
                                }

                                @Override
                                public void onSuccess() {
                                }

                            });
                        }
                    }
                }
                if (conversationList != null && conversationList.size() > 0) {
                    for (TIMConversation timConversation : conversationList) {
                        if (timConversation.getType() != TIMConversationType.Group) {
                            continue;
                        }
                        if (clearAll || !map.containsKey(timConversation.getPeer())) {
                            ConversationManager.getInstance().deleteConversationAndLocalMsgs(
                                    timConversation.getType(),
                                    timConversation.getPeer());
                            haveChange = true;
                        }
                    }
                }
                if (haveChange && onCommonListener != null) {
                    onCommonListener.execute(null);
                }
            }

        });
    }


    /**
     * ???????????????/??????/??????/???????????????????????????
     */
    public static boolean isGroupDismiss(TIMMessage timMessage) {
        if (timMessage.getConversation().getType() == TIMConversationType.Group
                && timMessage.getElementCount() > 0
                && timMessage.getElement(0).getType() == TIMElemType.GroupTips) {
            TIMElem timElem = timMessage.getElement(0);
            TIMGroupTipsElem groupTips = (TIMGroupTipsElem) timElem;
            TIMGroupTipsType tipsType = groupTips.getTipsType();
            if (tipsType == TIMGroupTipsType.Quit
                    || tipsType == TIMGroupTipsType.DelGroup
                    || tipsType == TIMGroupTipsType.Kick) {
                TIMConversation timConversation = timMessage.getConversation();
                ConversationManager.getInstance().deleteConversationAndLocalMsgs(
                        timConversation.getType(),
                        timConversation.getPeer());
                return true;
            }
        }
        return false;
    }

    /**
     * ????????????
     */
    public static boolean filterC2CSex(TIMConversation timConversation) {

        if (timConversation == null) {
            return true;
        }

        if (timConversation.getType() == TIMConversationType.Group) {
            return false;
        }

        //????????????
        if (isSelfServe() || serveBeanMap.containsKey(timConversation.getPeer())) {
            return false;
        }

        String peer = timConversation.getPeer();
        TIMUserProfile timUserProfile = TIMFriendshipManager.getInstance().queryUserProfile(peer);
        if (timUserProfile == null) {
            TIMFriendshipManager.getInstance().getUsersProfile(Collections.singletonList(peer),
                    true,
                    new TIMValueCallBack<List<TIMUserProfile>>() {

                        @Override
                        public void onError(int i, String s) {

                        }

                        @Override
                        public void onSuccess(List<TIMUserProfile> timUserProfiles) {
                            filterC2CSex(timConversation);
                        }

                    });
            return false;
        }

        int sex = timUserProfile.getGender();
        if (sex == 0) {
            asyncCheckSex(timConversation, peer);
            return true;
        }
        boolean isSame = isSameSex(sex);
        if (isSame) {
            asyncCheckSex(timConversation, peer);
        }
        return isSame;

    }

    /**
     * ??????????????????,????????????????????????
     */
    private static void asyncCheckSex(TIMConversation timConversation, String peer) {
        TIMFriendshipManager.getInstance().getUsersProfile(Collections.singletonList(peer),
                true,
                new TIMValueCallBack<List<TIMUserProfile>>() {

                    @Override
                    public void onError(int i, String s) {

                    }

                    @Override
                    public void onSuccess(List<TIMUserProfile> timUserProfiles) {
                        TIMUserProfile timUserProfile = timUserProfiles.get(0);
                        if (timUserProfile.getGender() != 0 && isSameSex(timUserProfile.getGender())) {
                            ConversationManager.getInstance().deleteConversationAndLocalMsgs(
                                    timConversation.getType(),
                                    timConversation.getPeer());
                        }
                    }

                });
    }

    /**
     * @param imSex //imSex: 1???2??? self: 1???0???
     */
    private static boolean isSameSex(int imSex) {
        return AppManager.getInstance().getUserInfo().t_sex == imSex
                || imSex == AppManager.getInstance().getUserInfo().t_sex + 2;
    }

    /**
     * @return 1???2???
     */
    private static int getImSex() {
        return AppManager.getInstance().getUserInfo().t_sex == 1 ? 1 : 2;
    }

    public static void init() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(AppManager.getInstance());
        String serveListStr = sp.getString("serveListStr", null);
        if (TextUtils.isEmpty(serveListStr)) {
            return;
        }
        try {
            List<ServeBean> list = JSON.parseObject(serveListStr, new TypeReference<List<ServeBean>>() {
            });
            if (list != null && list.size() > 0) {
                for (ServeBean serveBean : list) {
                    serveBeanMap.put(String.valueOf(serveBean.t_idcard), null);
                }
            }
        } catch (Exception e) {
            sp.edit().putString("serveListStr", null).apply();
            e.printStackTrace();
        }

    }

}