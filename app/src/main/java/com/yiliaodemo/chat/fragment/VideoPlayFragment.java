package com.yiliaodemo.chat.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.PersonInfoActivity;
import com.yiliaodemo.chat.activity.ReportActivity;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseFragment;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.ActorInfoBean;
import com.yiliaodemo.chat.bean.ChargeBean;
import com.yiliaodemo.chat.bean.CoverUrlBean;
import com.yiliaodemo.chat.bean.InfoRoomBean;
import com.yiliaodemo.chat.bean.LabelBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.dialog.GiftDialog;
import com.yiliaodemo.chat.glide.GlideCircleTransform;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.AudioVideoRequester;
import com.yiliaodemo.chat.net.FocusRequester;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.DensityUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.view.viewpager.YViewPager;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * 动态视频播放页 & 主播视频封面播放页（上下滑动布局） & 个人相册视频播放页
 */
public class VideoPlayFragment extends BaseFragment {

    @BindView(R.id.plv)
    PLVideoTextureView plv;

    @BindView(R.id.nick_tv)
    TextView nickTv;

    @BindView(R.id.age_tv)
    TextView ageTv;

    @BindView(R.id.online_tv)
    TextView onlineTv;

    @BindView(R.id.follow_tv)
    TextView followTv;

    @BindView(R.id.head_iv)
    ImageView headImg;

    @BindView(R.id.up_iv)
    View upView;

    @BindView(R.id.bottom_ll)
    View infoView;

    Unbinder unbinder;

    private int mActorId;

    private ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> mActorInfoBean;

    private String videoUrl;

    private boolean canPlay = true;

    @Override
    protected int initLayout() {
        return R.layout.fragment_actor_pager_video;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        infoView.setVisibility(View.GONE);

        mActorId = getActivity().getIntent().getIntExtra(Constant.ACTOR_ID, 0);
        videoUrl = getActivity().getIntent().getStringExtra(Constant.VIDEO_URL);

        getActorInfo();

        playVideoWithUrl(videoUrl);

        final YViewPager pagerVv = getActivity().findViewById(R.id.pager_vv);
        if (pagerVv != null) {
            /*
             * 上下滑动视频播放页，需要监听page选中
             */
            pagerVv.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    if (plv != null) {
                        if (position == 0) {
                            plv.start();
                            canPlay = true;
                        } else {
                            plv.pause();
                            canPlay = false;
                        }
                    }
                }
            });
        } else {
            /*
             * 单独视频播放页
             */
            upView.setVisibility(View.GONE);
            headImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PersonInfoActivity.start(getActivity(), mActorId);
                }
            });
        }

        plv.setOnErrorListener(new PLOnErrorListener() {
            @Override
            public boolean onError(int i) {
                ToastUtil.showToast("Sorry,播放失败" + i);
                return false;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        if (plv != null)
            plv.stopPlayback();
        super.onDestroyView();
        unbinder.unbind();
    }

    int[] stateIcons = {
            R.drawable.shape_free_indicator,
            R.drawable.shape_busy_indicator,
            R.drawable.shape_offline_indicator};

    int[] stateTexts = {
            R.string.free,
            R.string.busy,
            R.string.offline};

    private void loadData(ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> mActorInfoBean) {
        this.mActorInfoBean = mActorInfoBean;
        ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> bean = mActorInfoBean;
        if (bean != null) {

            //显示非个人信息
            if (mActorId != AppManager.getInstance().getUserInfo().t_id) {
                infoView.setVisibility(View.VISIBLE);
            }

            //头像
            Glide.with(getActivity())
                    .load(bean.t_handImg)
                    .error(R.drawable.default_head_img)
                    .override(DensityUtil.dip2px(getActivity(), 50))
                    .transform(new GlideCircleTransform(getActivity()))
                    .into(headImg);

            //昵称
            nickTv.setText(bean.t_nickName);

            //岁数
            ageTv.setText(String.format("%s岁", bean.t_age));

            //关注  是否关注 0.未关注 1.已关注
            refreshFollow(bean.isFollow == 1);

            //在线状态(0.在线1.忙碌2.离线)
            onlineTv.setCompoundDrawablesRelativeWithIntrinsicBounds(stateIcons[bean.t_onLine], 0, 0, 0);
            onlineTv.setText(stateTexts[bean.t_onLine]);

            if (bean.t_is_not_disturb == 0) {
                onlineTv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.shape_busy_indicator, 0, 0, 0);
                onlineTv.setText("勿扰");
                return;
            }

            if (TextUtils.isEmpty(videoUrl) && !TextUtils.isEmpty(bean.t_addres_url)) {
                videoUrl = bean.t_addres_url;
                playVideoWithUrl(bean.t_addres_url);
            }
        }
    }

    private void playVideoWithUrl(String url) {
        if (plv != null && !TextUtils.isEmpty(url)) {
            videoUrl = url;
            if (plv.isPlaying())
                return;
            plv.setVideoPath(url);
            plv.setLooping(true);
            plv.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (plv != null) {
            plv.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (plv != null && canPlay) {
            plv.start();
        }
    }

    @OnClick({
            R.id.text_chat_btn,
            R.id.video_chat_btn,
            R.id.audio_chat_btn,
            R.id.send_gift_btn,
            R.id.follow_tv,
            R.id.back_iv,
            R.id.complain_iv
    })
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.text_chat_btn:
                if (mActorInfoBean == null) {
                    getActorInfo();
                    return;
                }
                IMHelper.toChat(getActivity(), mActorInfoBean.t_nickName, mActorId, mActorInfoBean.t_sex);
                break;
            case R.id.send_gift_btn:
                new GiftDialog(mContext, mActorId).show();
                break;
            case R.id.back_iv:
                getActivity().finish();
                break;
            case R.id.follow_tv:
                follow(view.isSelected());
                break;
            case R.id.complain_iv:
                Intent intent = new Intent(getActivity(), ReportActivity.class);
                intent.putExtra(Constant.ACTOR_ID, mActorId);
                startActivity(intent);
                break;
            case R.id.video_chat_btn:
            case R.id.audio_chat_btn:
                if (mActorInfoBean == null) {
                    getActorInfo();
                    return;
                }
                if (AppManager.getInstance().getUserInfo().t_sex == mActorInfoBean.t_sex) {
                    ToastUtil.showToast(getActivity(), R.string.sex_can_not_communicate);
                    return;
                }
                AudioVideoRequester audioVideoRequester = new AudioVideoRequester(
                        getActivity(),
                        true,
                        mActorId);
//                if (view.getId() == R.id.video_chat_btn) {
                audioVideoRequester.executeVideo();
//                } else {
//                    audioVideoRequester.executeAudio();
//                }
                break;
        }
    }

    /**
     * 设置关注
     */
    private void follow(boolean isFollow) {
        final boolean setFollow = !isFollow;
        new FocusRequester() {
            @Override
            public void onSuccess(BaseResponse response, boolean focus) {
                if (getActivity() == null || getActivity().isFinishing())
                    return;
                refreshFollow(setFollow);
            }
        }.focus(mActorId, setFollow);
    }

    /**
     * 关注
     */
    private void refreshFollow(boolean isFollow) {
        followTv.setSelected(isFollow);
        followTv.setText(isFollow ? "已关注" : "关注");
        int drawId = isFollow ? R.drawable.video_follow_actor_selected : R.drawable.video_follow_actor_unselected;
        followTv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, drawId, 0, 0);
    }

    /**
     * 获取主播资料
     */
    private void getActorInfo() {

        //FragmentActivity属于ActorUserInfoActivity则无需请求数据
        if (getActivity() instanceof PersonInfoActivity) {
            PersonInfoActivity actorInfoActivity = (PersonInfoActivity) getActivity();
            mActorInfoBean = actorInfoActivity.getBean();
        }
        if (mActorInfoBean == null) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
            paramMap.put("coverUserId", mActorId);
            OkHttpUtils.post().url(ChatApi.GET_ACTOR_INFO())
                    .addParams("param", ParamUtil.getParam(paramMap))
                    .build().execute(new AjaxCallback<BaseResponse<ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean>>>() {
                @Override
                public void onResponse(BaseResponse<ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean>> response, int id) {
                    if (getActivity() == null || getActivity().isFinishing()) {
                        return;
                    }
                    if (response != null && response.m_istatus == NetCode.SUCCESS) {
                        loadData(response.m_object);
                    }
                }
            });
        } else {
            loadData(mActorInfoBean);
        }
    }
}