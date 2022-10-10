package com.yiliaodemo.chat.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.PersonInfoActivity;
import com.yiliaodemo.chat.activity.ReportActivity;
import com.yiliaodemo.chat.activity.ShareActivity;
import com.yiliaodemo.chat.activity.SlidePhotoActivity;
import com.yiliaodemo.chat.banner.MZBannerView;
import com.yiliaodemo.chat.banner.MZHolderCreator;
import com.yiliaodemo.chat.banner.MZViewHolder;
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
import com.yiliaodemo.chat.dialog.BottomListDialog;
import com.yiliaodemo.chat.dialog.GiftDialog;
import com.yiliaodemo.chat.dialog.ProtectDialog;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.helper.ImageLoadHelper;
import com.yiliaodemo.chat.helper.ShareUrlHelper;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.AudioVideoRequester;
import com.yiliaodemo.chat.net.BlackRequester;
import com.yiliaodemo.chat.net.FocusRequester;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.DevicesUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.view.tab.FragmentParamBuilder;
import com.yiliaodemo.chat.view.tab.LabelViewHolder;
import com.yiliaodemo.chat.view.tab.TabFragmentAdapter;
import com.yiliaodemo.chat.view.tab.TabPagerLayout;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import okhttp3.Call;

public class PersonInfoFragment extends BaseFragment {

    Unbinder unbinder;

    private int otherId;

    private ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> bean;

    private TabFragmentAdapter tabFragmentAdapter;

    @Override
    protected int initLayout() {
        return R.layout.fragment_person_info;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        otherId = getActivity().getIntent().getIntExtra(Constant.ACTOR_ID, 0);
        getData();
        isSelf();
        ShareUrlHelper.getShareUrl(false);
        initViewPager();
    }

    /**
     * 自己的资料页，隐藏布局
     */
    private void isSelf() {
        if (otherId == AppManager.getInstance().getUserInfo().t_id) {
            findViewById(R.id.bottom_ll).setVisibility(View.GONE);
            findViewById(R.id.follow_btn).setVisibility(View.INVISIBLE);
            View scroll = findViewById(R.id.view_pager);
            ((ViewGroup.MarginLayoutParams) scroll.getLayoutParams()).bottomMargin = 0;
            scroll.requestLayout();
        }
    }

    private void initViewPager() {

        ViewPager viewPager = findViewById(R.id.view_pager);

        TabPagerLayout tabPagerLayout = findViewById(R.id.person_info_tl);

        tabFragmentAdapter = new TabFragmentAdapter(getChildFragmentManager(), viewPager);

        tabFragmentAdapter.init(
                FragmentParamBuilder.create()
                        .withName("资料")
                        .withClazz(PersonDataFragment.class)
                        .withViewHolder(new LabelViewHolder(tabPagerLayout))
                        .build()
        );

        tabPagerLayout.init(viewPager);
    }

    @OnClick({
            R.id.follow_btn,
            R.id.back_iv,
            R.id.chat_im,
            R.id.chat_video,
            R.id.chat_hello,
            R.id.dian_black_iv,
            R.id.chat_gift,
    })
    public void onClick(View view) {

        /*
         * 退出
         */
        if (view.getId() == R.id.back_iv) {
            getActivity().finish();
            return;
        }

        if (getBean() == null)
            return;

        switch (view.getId()) {

            /*
             * 礼物
             */
            case R.id.chat_gift:
                if (AppManager.getInstance().getUserInfo().isSameSexToast(mContext, bean.t_sex))
                    return;
                new GiftDialog(mContext, otherId).show();
                break;

            /*
             * 私信
             */
            case R.id.chat_im:
                IMHelper.toChat(getActivity(), bean.t_nickName, otherId, bean.t_sex);
                break;

            /*
             * 视频
             */
            case R.id.chat_video: {
                if (AppManager.getInstance().getUserInfo().isSameSexToast(mContext, bean.t_sex))
                    return;
                AudioVideoRequester audioVideoRequester = new AudioVideoRequester(
                        getActivity(),
                        getBean().t_role == 1,
                        otherId);
                audioVideoRequester.executeVideo();
                break;
            }

            /*
             * 守护榜
             */
            case R.id.chat_hello:
                new ProtectDialog(mContext, otherId) {
                    @Override
                    protected void update() {
                        PersonDataFragment personDataFragment = (PersonDataFragment) tabFragmentAdapter.getCurrentFragment();
                        if (personDataFragment != null) {
                            personDataFragment.protectRv();
                        }
                    }
                }.show();
                break;

            /*
             * 关注
             */
            case R.id.follow_btn:
                follow(view.isSelected());
                break;

            /*
             * 更多
             */
            case R.id.dian_black_iv:
                new BottomListDialog.Builder(getActivity())
                        .addMenuListItem(new String[]{"分享", "举报", "加入黑名单"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        if (getBean() == null)
                                            return;
                                        String url = ShareUrlHelper.getShareUrl(true);
                                        if (TextUtils.isEmpty(url)) {
                                            return;
                                        }
                                        ShareActivity.start(mContext, new ShareActivity.ShareParams()
                                                .typeTextImage()
                                                .setImageUrl(bean.t_handImg)
                                                .setTitle(String.format(getString(R.string.share_title), bean.t_nickName))
                                                .setSummary(getString(R.string.please_check))
                                                .setContentUrl(url));
                                        break;
                                    case 1:
                                        Intent intent = new Intent(getActivity(), ReportActivity.class);
                                        intent.putExtra(Constant.ACTOR_ID, otherId);
                                        startActivity(intent);
                                        break;
                                    case 2:
                                        new AlertDialog.Builder(getActivity())
                                                .setMessage(String.format(getString(R.string.black_alert), bean.t_nickName))
                                                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(final DialogInterface dialog, int which) {
                                                        new BlackRequester() {
                                                            @Override
                                                            public void onSuccess(BaseResponse response, boolean addToBlack) {
                                                                ToastUtil.showToast(R.string.black_add_ok);
                                                                dialog.dismiss();
                                                            }
                                                        }.post(otherId, true);
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
                            }
                        }).show();
                break;
        }
    }

    /**
     * 打招呼
     */
    private void greet() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        paramMap.put("anchorUserId", otherId);
        OkHttpUtils.post().url(ChatApi.greet())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<String>>() {
            @Override
            public void onResponse(BaseResponse<String> response, int id) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (response != null) {
                    if (response.m_istatus == NetCode.SUCCESS) {
                        IMHelper.sendMessage(otherId, "你好，看了你的资料，我很喜欢，方便聊一聊吗？", null);
                        bean.isGreet = 1;
                    }
                    ToastUtil.showToast(response.m_strMessage);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                ToastUtil.showToast("打招呼失败");
            }
        });
    }

    private void loadData(ActorInfoBean<CoverUrlBean, LabelBean, ChargeBean, InfoRoomBean> bean) {
        if (bean != null) {

            this.bean = bean;

            //Title
            ((TextView) findViewById(R.id.title_nick_tv)).setText(bean.t_nickName);

            TextView ageTv = findViewById(R.id.age_tv);

            //性别
            ageTv.setSelected(bean.t_sex == 1);

            //年龄
            ageTv.setText(String.valueOf(bean.t_age));

            //守护
            if (bean.t_role != 1 || !AppManager.getInstance().getUserInfo().isSexMan()) {
                findViewById(R.id.chat_hello).setVisibility(View.INVISIBLE);
            }

            //轮播图，没有则显示头像
            if (bean.lunbotu == null || bean.lunbotu.size() == 0) {
                bean.lunbotu = new ArrayList<>();
                CoverUrlBean coverUrlBean = new CoverUrlBean();
                coverUrlBean.t_img_url = bean.t_handImg;
                bean.lunbotu.add(coverUrlBean);
            }
            setBanner(bean.lunbotu);

            //价格
            if (bean.t_role == 1) {
                TextView priceTv = findViewById(R.id.price_tv);
                SpannableStringBuilder span = new SpannableStringBuilder();
                span.append("视频:");
                span.append(String.valueOf(bean.anchorSetup.get(0).t_video_gold),
                        new ForegroundColorSpan(0xffFB3B96),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                span.append("金币/分钟");
                priceTv.setText(span);
            }

            //昵称
            TextView nickTv = findViewById(R.id.nick_tv);
            nickTv.setText(bean.t_nickName);

            //vip
            nickTv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            if (bean.t_is_vip == 0) {
                nickTv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.vip_icon, 0);
            }

            //接单量&接听率
            TextView orderCountTv = findViewById(R.id.count_tv);
            orderCountTv.setText(null);
            if (bean.t_role == 1) {
                orderCountTv.setText(String.format("接单量: %s          接听率: %s", bean.t_called_video, bean.t_reception));
            }

            //ID号
            TextView idTv = findViewById(R.id.id_tv);
            idTv.setText(String.format("1v1Demo号: %s   ", bean.t_idcard));
            if (!TextUtils.isEmpty(bean.t_city)) {
                idTv.append(String.format("|   %s", bean.t_city));
            }

            //在线状态 0.空闲1.忙碌2.离线
            setOnLineState(bean.t_onLine);

            //关注
            refreshFollow(bean.isFollow == 1);

            //个性签名
            TextView signTv = findViewById(R.id.sign_tv);
            signTv.setText("个性签名: ");
            signTv.append(!TextUtils.isEmpty(bean.t_autograph) ?
                    bean.t_autograph : getString(R.string.lazy));
        }
    }

    /**
     * 获取主播资料
     */
    private void getData() {

        if (getActivity() instanceof PersonInfoActivity) {
            PersonInfoActivity actorInfoActivity = (PersonInfoActivity) getActivity();
            bean = actorInfoActivity.getBean();
        }

        if (bean == null) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
            paramMap.put("coverUserId", otherId);
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
            loadData(bean);
        }
    }

    /**
     * 设置在线状态
     */
    private void setOnLineState(int state) {
        TextView textView = findViewById(R.id.status_tv);
        if (bean != null) {
            if (bean.t_is_not_disturb == 0) {
                textView.setVisibility(View.VISIBLE);
                textView.setText("勿扰");
                textView.setTextColor(0xfffe2947);
                textView.setBackgroundResource(R.drawable.state_offline);
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.state_point_busy, 0, 0, 0);
                return;
            }
        }
        if (state < 0 || state > 2)
            return;
        int[] points = {
                R.drawable.state_point_online,
                R.drawable.state_point_busy,
                R.drawable.state_point_offline
        };
        int[] bgs = {
                R.drawable.state_online,
                R.drawable.state_busy,
                R.drawable.state_offline
        };
        String[] strings = {
                "在线",
                "忙碌",
                "离线"
        };
        int[] colors = {
                Color.parseColor("#ffffff"),
                Color.parseColor("#fe2947"),
                Color.parseColor("#868686")
        };
        textView.setVisibility(View.VISIBLE);
        textView.setText(strings[state]);
        textView.setTextColor(colors[state]);
        textView.setBackgroundResource(bgs[state]);
        textView.setCompoundDrawablesWithIntrinsicBounds(points[state], 0, 0, 0);
    }

    /**
     * 轮播图
     */
    private void setBanner(final List<CoverUrlBean> coverUrlBeanList) {
        if (coverUrlBeanList == null || coverUrlBeanList.size() == 0) {
            return;
        }
        MZBannerView<CoverUrlBean> mMZBannerView = findViewById(R.id.my_banner);
        if (mMZBannerView.getTag() != null)
            return;

        mMZBannerView.setIndicatorRes(
                R.drawable.banner_indicator_point_unselected,
                R.drawable.banner_indicator_point_selected);

        mMZBannerView.setBannerPageClickListener(new MZBannerView.BannerPageClickListener() {
            @Override
            public void onPageClick(View view, int position) {
                ArrayList<String> list = new ArrayList<>();
                for (CoverUrlBean coverUrlBean : coverUrlBeanList) {
                    list.add(coverUrlBean.t_img_url);
                }
                SlidePhotoActivity.start(getActivity(), list, position);
            }
        });
        mMZBannerView.setIndicatorVisible(false);
        final TextView textView = findViewById(R.id.banner_index_tv);
        final int size = coverUrlBeanList.size();
        textView.setText(String.format("%1$s/%2$s", 1, size));
        mMZBannerView.addPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                textView.setText(String.format("%1$s/%2$s", i + 1, size));
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        mMZBannerView.setIndicatorAlign(MZBannerView.IndicatorAlign.CENTER);
        mMZBannerView.setTag("");
        mMZBannerView.setPages(coverUrlBeanList, new MZHolderCreator<BannerViewHolder>() {
            @Override
            public BannerViewHolder createViewHolder() {
                return new BannerViewHolder();
            }
        });
        mMZBannerView.setCanLoop(true);
        mMZBannerView.start();
    }

    /**
     * 判断是否获取到资料
     */
    private ActorInfoBean getBean() {
        if (bean == null) {
            getData();
            ToastUtil.showToast(mContext, "获取数据中");
        }
        return bean;
    }

    /**
     * 关注
     */
    private void refreshFollow(boolean isFollow) {
        TextView followTv = findViewById(R.id.follow_btn);
        followTv.setSelected(isFollow);
        followTv.setText(isFollow ? "已关注" : "关注");
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
        }.focus(otherId, setFollow);
    }

    @Override
    public void onDestroyView() {
        MZBannerView<CoverUrlBean> bannerView = findViewById(R.id.my_banner);
        if (bannerView != null && bannerView.getTag() != null) {
            bannerView.pause();
        }
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    static class BannerViewHolder implements MZViewHolder<CoverUrlBean> {

        private ImageView mImageView;

        @Override
        public View createView(Context context) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_info_image_vp_layout, null);
            mImageView = view.findViewById(R.id.content_iv);
            return view;
        }

        @Override
        public void onBind(Context context, int i, CoverUrlBean bannerBean) {
            if (bannerBean != null) {
                if (!TextUtils.isEmpty(bannerBean.t_img_url)) {
                    ImageLoadHelper.glideShowImageWithUrl(context, bannerBean.t_img_url, mImageView,
                            DevicesUtil.getScreenW(AppManager.getInstance()),
                            DevicesUtil.dp2px(AppManager.getInstance(), 360));
                }
            }
        }
    }
}