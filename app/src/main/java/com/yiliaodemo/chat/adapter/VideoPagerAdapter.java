package com.yiliaodemo.chat.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.PersonInfoActivity;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.ActorPlayBean;
import com.yiliaodemo.chat.bean.InfoRoomBean;
import com.yiliaodemo.chat.bean.LabelBean;
import com.yiliaodemo.chat.bean.AlbumBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.dialog.GiftDialog;
import com.yiliaodemo.chat.dialog.LookResourceDialog;
import com.yiliaodemo.chat.glide.GlideCircleTransform;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.AudioVideoRequester;
import com.yiliaodemo.chat.net.FocusRequester;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.DensityUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class VideoPagerAdapter extends RecyclerView.Adapter<VideoPagerAdapter.VideoPagerHolder> {

    private List<AlbumBean> videoBeans;
    private Activity activity;

    public VideoPagerAdapter(Activity activity) {
        videoBeans = new ArrayList<>();
        this.activity = activity;
    }

    public void setBeans(List<AlbumBean> videoBeans, boolean isRefresh) {
        if (isRefresh)
            this.videoBeans.clear();
        if (videoBeans != null)
            this.videoBeans.addAll(videoBeans);
    }

    @NonNull
    @Override
    public VideoPagerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VideoPagerHolder videoPagerHolder = new VideoPagerHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_video_pager,
                        parent,
                        false));
        return videoPagerHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull VideoPagerHolder holder, int position) {
        VideoPagerHolder viewHolder = holder;
        viewHolder.position = position;
        Context mContext = holder.itemView.getContext();
        AlbumBean bean = videoBeans.get(position);

        //????????????
        if (bean.canSee()) {
            Glide.with(mContext)
                    .load(bean.t_video_img)
                    .skipMemoryCache(true)
                    .dontAnimate()
                    .override(720, 1280)
                    .into(viewHolder.coverImage);
            viewHolder.lockView.setVisibility(View.GONE);
        } else {
            Glide.with(mContext)
                    .load(bean.t_video_img)
                    .skipMemoryCache(true)
                    .dontAnimate()
                    .override(720, 1280)
                    .transform(new BlurTransformation())
                    .into(viewHolder.coverImage);
            viewHolder.lockView.setVisibility(View.VISIBLE);
        }

        //??????
        Glide.with(mContext)
                .load(bean.t_handImg)
                .dontAnimate()
                .transform(new GlideCircleTransform(mContext))
                .error(R.drawable.default_head_img)
                .into(viewHolder.headImage);

        viewHolder.getActorInfo(bean);
    }

    @Override
    public int getItemCount() {
        return videoBeans.size();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoPagerHolder holder) {
        resetViewHolder(holder);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoPagerHolder holder) {
        resetViewHolder(holder);
    }

    @Override
    public void onViewRecycled(@NonNull VideoPagerHolder holder) {
        resetViewHolder(holder);
    }

    public final void resetViewHolder(VideoPagerHolder holder) {
        if (holder.videoView.isPlaying())
            holder.videoView.stopPlayback();
        holder.videoView.setTag(null);
        holder.clickView.setOnClickListener(null);
        holder.mPauseIv.setVisibility(View.GONE);
        holder.coverImage.setVisibility(View.VISIBLE);
    }

    public AlbumBean getItem(int position) {
        return videoBeans.get(position);
    }

    public void play() {

    }

    public void showLoadingDialog() {

    }

    public void dismissLoadingDialog() {

    }

    public class VideoPagerHolder extends RecyclerView.ViewHolder {

        ImageView headImage;
        View lockView;
        View giftView;
        TextView mFocusTv;
        TextView mVideoChatTv;
        TextView onLineTv;
        View infoView;
        TextView ageTv;

        public PLVideoTextureView videoView;
        public ImageView coverImage;
        public TextView mNameTv;
        public View clickView;
        public ImageView mPauseIv;

        public int position;

        private boolean haveGetData;

        int[] stateIcons = {
                R.drawable.shape_free_indicator,
                R.drawable.shape_busy_indicator,
                R.drawable.shape_offline_indicator};

        int[] stateTexts = {R.string.free, R.string.busy, R.string.offline};

        VideoPagerHolder(View itemView) {

            super(itemView);

            ageTv = itemView.findViewById(R.id.age_tv);
            infoView = itemView.findViewById(R.id.bottom_ll);
            onLineTv = itemView.findViewById(R.id.online_tv);
            clickView = itemView.findViewById(R.id.click_view);
            videoView = itemView.findViewById(R.id.video_view);
            coverImage = itemView.findViewById(R.id.cover_iv);
            headImage = itemView.findViewById(R.id.head_iv);
            lockView = itemView.findViewById(R.id.lock_fl);
            giftView = itemView.findViewById(R.id.send_gift_btn);
            mNameTv = itemView.findViewById(R.id.nick_tv);
            mFocusTv = itemView.findViewById(R.id.follow_tv);
            mVideoChatTv = itemView.findViewById(R.id.video_chat_tv);
            mPauseIv = itemView.findViewById(R.id.pause_iv);

            /**
             * ??????
             */
            mFocusTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    follow(v.isSelected());
                }
            });

            /**
             * ??????
             */
            lockView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlbumBean bean = videoBeans.get(position);
                    bean.t_file_type = 1;
                    LookResourceDialog.showAlbum(activity, bean, getActorId(), new OnCommonListener<Boolean>() {
                        @Override
                        public void execute(Boolean aBoolean) {
                            if (aBoolean) {
                                bean.is_see = 1;
                                notifyDataSetChanged();
                                play();
                            }
                        }
                    });
                }
            });

            /**
             * ????????????
             */
            giftView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new GiftDialog(activity, getActorId()).show();
                }
            });

            /**
             * ????????????
             */
            headImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PersonInfoActivity.start(activity, getActorId());
                }
            });

            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!haveGetData) {
                        ToastUtil.showToast(activity, R.string.data_getting);
                        getActorInfo(getItem(position));
                        return;
                    }
                    if (AppManager.getInstance().getUserInfo().t_sex == 0) {
                        ToastUtil.showToast(activity, R.string.sex_can_not_communicate);
                        return;
                    }
                    //?????????????????????,???????????????????????????,????????????????????????????????????
                    AudioVideoRequester audioVideoRequester = new AudioVideoRequester(activity,
                            true,
                            getActorId());
//                    if (v.getTag() != null) {
                    audioVideoRequester.executeVideo();
//                    } else {
//                        audioVideoRequester.executeAudio();
//                    }
                }
            };

            /**
             * ???TA??????
             */
            itemView.findViewById(R.id.video_chat_btn).setTag("");
            itemView.findViewById(R.id.video_chat_btn).setOnClickListener(onClickListener);
            itemView.findViewById(R.id.audio_chat_btn).setOnClickListener(onClickListener);

            /**
             * IM??????
             */
            itemView.findViewById(R.id.text_chat_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!haveGetData) {
                        ToastUtil.showToast(activity, R.string.data_getting);
                        getActorInfo(getItem(position));
                        return;
                    }
                    AlbumBean bean = videoBeans.get(position);
                    IMHelper.toChat(activity, bean.t_nickName, bean.t_user_id, -1);
                }
            });
        }

        /**
         * ????????????id
         */
        public int getActorId() {
            return videoBeans.get(position).t_user_id;
        }

        /**
         * ??????????????????
         */
        public void getActorInfo(final AlbumBean bean) {
            resetActorView();
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
            paramMap.put("coverConsumeUserId", String.valueOf(getActorId()));
            paramMap.put("albumId", String.valueOf(bean.t_id));
            paramMap.put("queryType", String.valueOf(0));
            OkHttpUtils.post().url(ChatApi.GET_ACTOR_PLAY_PAGE())
                    .addParams("param", ParamUtil.getParam(paramMap))
                    .build().execute(new AjaxCallback<BaseResponse<ActorPlayBean<LabelBean, InfoRoomBean>>>() {
                @Override
                public void onResponse(BaseResponse<ActorPlayBean<LabelBean, InfoRoomBean>> response, int id) {

                    if (activity.isFinishing()) {
                        return;
                    }

                    //??????????????????
                    if (bean != getItem(position)) {
                        getActorInfo(getItem(position));
                        return;
                    }

                    if (response != null && response.m_istatus == NetCode.SUCCESS) {
                        ActorPlayBean<LabelBean, InfoRoomBean> playBean = response.m_object;
                        if (playBean != null) {

                            if (AppManager.getInstance().getUserInfo().t_id != getActorId()) {
                                infoView.setVisibility(View.VISIBLE);
                            }

                            //??????
                            Glide.with(activity)
                                    .load(bean.t_handImg)
                                    .error(R.drawable.default_head_img)
                                    .override(DensityUtil.dip2px(activity, 50))
                                    .transform(new GlideCircleTransform(activity))
                                    .into(headImage);

                            //??????
                            mNameTv.setText(playBean.t_nickName);

                            //??????
                            ageTv.setText(String.format("%s???", playBean.t_age));

                            //????????????  0:????????? 1????????????
                            refreshFollow(playBean.isFollow == 1);

                            //????????????(0.??????1.??????2.??????)
                            onLineTv.setCompoundDrawablesRelativeWithIntrinsicBounds(stateIcons[playBean.t_onLine], 0, 0, 0);
                            onLineTv.setText(stateTexts[playBean.t_onLine]);
                            haveGetData = true;
                        }
                    }
                }
            });
        }

        /**
         * ????????????
         */
        private void follow(boolean isFollow) {
            final boolean setFollow = !isFollow;
            new FocusRequester() {
                @Override
                public void onSuccess(BaseResponse response, boolean focus) {
                    if (activity == null || activity.isFinishing())
                        return;
                    refreshFollow(setFollow);
                }
            }.focus(getActorId(), setFollow);
        }

        /**
         * ??????
         */
        private void refreshFollow(boolean isFollow) {
            mFocusTv.setSelected(isFollow);
            mFocusTv.setText(isFollow ? "?????????" : "??????");
            int drawId = isFollow ? R.drawable.video_follow_actor_selected : R.drawable.video_follow_actor_unselected;
            mFocusTv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, drawId, 0, 0);
        }

        /**
         * ????????????
         */
        private void resetActorView() {
            refreshFollow(false);
            infoView.setVisibility(View.GONE);
            haveGetData = false;
            mNameTv.setText(null);
            coverImage.setVisibility(View.VISIBLE);
        }
    }
}