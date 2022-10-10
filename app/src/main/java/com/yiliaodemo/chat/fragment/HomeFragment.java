package com.yiliaodemo.chat.fragment;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.SearchActivity;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseFragment;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.AdBean;
import com.yiliaodemo.chat.bean.VerifyBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.dialog.FirstChargeDialog;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.view.AdView;
import com.yiliaodemo.chat.view.tab.FragmentParam;
import com.yiliaodemo.chat.view.tab.FragmentParamBuilder;
import com.yiliaodemo.chat.view.tab.LabelCityViewHolder;
import com.yiliaodemo.chat.view.tab.LabelViewHolder;
import com.yiliaodemo.chat.view.tab.TabFragmentAdapter;
import com.yiliaodemo.chat.view.tab.TabPagerLayout;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;

/**
 * 首页
 */
public class HomeFragment extends BaseFragment {

    private TabPagerLayout tabPagerLayout;
    private ViewPager mContentVp;
    private TextView retryTv;
    private boolean isGetData;
    private LabelCityViewHolder cityViewHolder;
    private FirstChargeDialog firstChargeDialog;
    private View firstChargeBtn;
    private int actorVerifyState = -2;

    @Override
    protected int initLayout() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView(View view, Bundle savedInstanceState) {
        mContentVp = view.findViewById(R.id.content_vp);
        tabPagerLayout = view.findViewById(R.id.home_one_labels_ll);

        retryTv = view.findViewById(R.id.retry_tv);

        //重新加载数据
        retryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isGetData) {
                    loadAd();
                }
            }
        });

        //搜索
        view.findViewById(R.id.category_iv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SearchActivity.class);
                startActivity(intent);
            }
        });

        //首充
        firstChargeBtn = view.findViewById(R.id.first_charge_iv);
        firstChargeDialog = new FirstChargeDialog(getActivity()) {
            @Override
            protected void ok(int visible) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                firstChargeBtn.setVisibility(visible);
            }
        };

        firstChargeBtn.setVisibility(View.GONE);
        firstChargeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstChargeDialog.show();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        firstChargeDialog.check();
        setOperateTop();
    }

    /**
     * 认证状态
     */
    private void getVerifyStatus() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", mContext.getUserId());
        OkHttpUtils.post().url(ChatApi.GET_VERIFY_STATUS())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<VerifyBean>>() {
            @Override
            public void onResponse(BaseResponse<VerifyBean> response, int id) {

                if (getActivity() == null || getActivity().isFinishing())
                    return;
                int lastState = actorVerifyState;
                boolean init = actorVerifyState == -2;
                // （bean == null 未申请) ->state=-1  0.审核中  1.审核成功 2.审核失败
                if (response != null && response.m_object != null) {
                    actorVerifyState = response.m_object.t_certification_type;
                } else {
                    actorVerifyState = -1;
                }
                if (!init && lastState != actorVerifyState) {
                    if (actorVerifyState == 1) {
                        ToastUtil.showToast("主播审核通过");
                    }
                    isGetData = false;
                    loadAd();
                }
            }
        });
    }

    /**
     * 我要置顶
     * 限制女主播 & VIP/SVIP男用户
     */
    private void setOperateTop() {

        if (getView() == null)
            return;

        boolean enable = AppManager.getInstance().getUserInfo().isWomenActor()
                || AppManager.getInstance().getUserInfo().isVipMan();
        final View operateTopIv = getView().findViewById(R.id.operate_top_iv);

        operateTopIv.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (operateTopIv.getVisibility() == View.VISIBLE && operateTopIv.getTag() == null) {
            operateTopIv.setTag("");
            ObjectAnimator animator = ObjectAnimator.ofFloat(operateTopIv, "translationY", 0, 15);
            animator.setInterpolator(new LinearInterpolator());
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.setRepeatMode(ObjectAnimator.REVERSE);
            animator.setDuration(1000);
            animator.start();
        }

        if (enable) {
            operateTopIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setClickable(false);
                    translateTop((ImageView) v);
                }
            });
        }
    }

    private void translateTop(final ImageView view) {
        view.setImageResource(R.drawable.actor_to_top);
        final int offY = view.getTop() + view.getHeight();
        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, -offY);
        animation.setDuration(1500);
        animation.setInterpolator(new AccelerateInterpolator());
        animation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
                toTop(view);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

        });
        view.startAnimation(animation);
    }

    TranslateAnimation reverse1, reverse2;

    private void translateReverse(final ImageView view, boolean ok) {
        view.setVisibility(View.VISIBLE);
        if (ok) {
            view.setClickable(true);
            view.setImageResource(R.drawable.actor_to_top);
            int offY = view.getHeight();
            if (reverse1 == null) {
                reverse1 = new TranslateAnimation(0, 0, offY, 0);
                reverse1.setDuration(500);
                reverse1.setInterpolator(new DecelerateInterpolator());
            }
            view.startAnimation(reverse1);
        } else {
            view.setImageResource(R.drawable.actor_to_bottom);
            int offY = view.getTop() + view.getHeight();
            if (reverse2 == null) {
                reverse2 = new TranslateAnimation(0, 0, -offY, 0);
                reverse2.setDuration(1500);
                reverse2.setInterpolator(new AccelerateInterpolator());
            }
            reverse2.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setClickable(true);
                    view.setImageResource(R.drawable.actor_to_top);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            view.startAnimation(reverse2);
        }
    }

    private void toTop(final ImageView view) {
        if (getActivity() == null || getActivity().isFinishing())
            return;
        String method = AppManager.getInstance().getUserInfo().isWomenActor() ?
                ChatApi.OPERATION_TOPPING() : ChatApi.OPERATION_TOPPING_MAN();
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", mContext.getUserId());
        OkHttpUtils.post().url(method)
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                if (getActivity() == null || getActivity().isFinishing())
                    return;
                boolean ok = false;
                if (response != null) {
                    if (response.m_istatus == NetCode.SUCCESS) {
                        ToastUtil.showToast(mContext, R.string.operate_top_success);
                        ok = true;
                    } else {
                        if (!TextUtils.isEmpty(response.m_strMessage)) {
                            ToastUtil.showToast(mContext, response.m_strMessage);
                        } else {
                            ToastUtil.showToast(mContext, R.string.operate_top_fail);
                        }
                    }
                } else {
                    ToastUtil.showToast(mContext, R.string.operate_top_fail);
                }
                translateReverse(view, ok);
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                super.onError(call, e, id);
                ToastUtil.showToast(mContext, R.string.operate_top_fail);
                translateReverse(view, false);
            }

        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AdView adView = findViewById(R.id.ad_view);
        adView.request(getActivity());

        loadAd();
    }

    /**
     * 主页tabls
     */
    private void loadAd() {
        retryTv.setVisibility(View.GONE);
        OkHttpUtils
                .get()
                .url(ChatApi.getAdTable())
                .addParams("userId", AppManager.getInstance().getUserInfo().t_id + "")
                .addParams("type", 2 + "")
                .addParams("page", 1 + "")
                .addParams("size", 100 + "")
                .build().execute(new AjaxCallback<BaseResponse<List<AdBean>>>() {
            @Override
            public void onResponse(BaseResponse<List<AdBean>> response, int id) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (response != null && response.m_istatus == NetCode.SUCCESS
                        && response.m_object != null && response.m_object.size() > 0) {
                    if (isGetData) {
                        return;
                    }
                    isGetData = true;
                    initLabels(response.m_object);
                } else {
                    retryTv.setVisibility(View.VISIBLE);
                    mContentVp.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                retryTv.setVisibility(View.VISIBLE);
                mContentVp.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 添加相应用户的fragment，女主or用户
     * 主页导航栏:
     * 新人 0;
     * 推荐 1;
     * 活跃 2;
     * 女神 3;
     * 粉丝 4;
     * 关注 5;
     * 视频 6;
     * 附近 7;
     * 聊场 9;
     * 动态 10;
     * 府邸 11;
     * H5广告:url地址
     */
    private void initLabels(List<AdBean> beans) {

        int defaultSelected = 0;
        int index = 0;

        List<FragmentParam> fragmentParams = new ArrayList<>();
        for (AdBean bean : beans) {
            FragmentParamBuilder builder = FragmentParamBuilder.create();
            if ("0".equals(bean.t_ad_table_target)) {
                Bundle args = new Bundle();
                args.putString("queryType", "0");
                builder.withClazz(HomeContentFragment.class).withBundle(args);
            } else if ("1".equals(bean.t_ad_table_target)) {
                Bundle args = new Bundle();
                args.putString("queryType", "1");
                builder.withClazz(HomeContentFragment.class).withBundle(args);
            } else if ("2".equals(bean.t_ad_table_target)) {
                Bundle args = new Bundle();
                args.putString("queryType", "2");
                builder.withClazz(HomeContentFragment.class).withBundle(args);
            } else if ("3".equals(bean.t_ad_table_target)) {
                Bundle args = new Bundle();
                args.putString("queryType", "3");
                builder.withClazz(HomeBannerFragment.class).withBundle(args);
            } else if ("4".equals(bean.t_ad_table_target)) {
                builder.withClazz(FansFragment.class);
            } else if ("5".equals(bean.t_ad_table_target)) {
                builder.withClazz(FocusFragment.class);
            } else if ("6".equals(bean.t_ad_table_target)) {
                builder.withClazz(VideoFragment.class);
            }
//            else if ("7".equals(bean.t_ad_table_target)) {
//                Bundle args = new Bundle();
//                args.putString("queryType", "5");
//                builder.withClazz(HomeCityFragment.class).withBundle(args);
//            }
            else if ("8".equals(bean.t_ad_table_target)) {
                builder.withClazz(RandomChatFragment.class);
            } else if ("9".equals(bean.t_ad_table_target)) {
                /*
                 *
                 *
                 * 默认选中聊场
                 *
                 */
                builder.withClazz(HomeLabelFragment.class);
                defaultSelected = index;
            } else if ("10".equals(bean.t_ad_table_target)) {
                builder.withClazz(ActiveFragment.class);
            } else if ("11".equals(bean.t_ad_table_target)) {
                builder.withClazz(MansionManFragment.class);
            } else if (!TextUtils.isEmpty(bean.t_ad_table_target) && bean.t_ad_table_target.startsWith("http")) {
                Bundle args = new Bundle();
                args.putString(Constant.URL, bean.t_ad_table_target);
                builder.withClazz(WebFragment.class).withBundle(args);
            }
            if (builder.isAvailable()) {
                LabelViewHolder labelViewHolder;
                if ("7".equals(bean.t_ad_table_target)) {
                    cityViewHolder = new LabelCityViewHolder(tabPagerLayout);
                    labelViewHolder = cityViewHolder;
                } else {
                    labelViewHolder = new LabelViewHolder(tabPagerLayout);
                }
                builder.withName(bean.t_ad_table_name);
                builder.withViewHolder(labelViewHolder);
                fragmentParams.add(builder.build());
                index++;
            }
        }
        mContentVp.clearOnPageChangeListeners();
        TabFragmentAdapter adapter = new TabFragmentAdapter(getChildFragmentManager(), mContentVp);
        adapter.init(defaultSelected, fragmentParams);
        tabPagerLayout.init(mContentVp);
        mContentVp.setVisibility(View.VISIBLE);
    }

    @Override
    protected void showChanged(boolean b) {
        if (b && cityViewHolder != null) {
            cityViewHolder.showChanged(true);
        }
        if (b) {
            getVerifyStatus();
        }
    }
}