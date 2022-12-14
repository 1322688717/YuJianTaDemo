package com.yiliaodemo.chat.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseFragment;
import com.yiliaodemo.chat.base.BaseListResponse;
import com.yiliaodemo.chat.bean.VipBean;
import com.yiliaodemo.chat.bean.VipInfoBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.activity.PayChooserActivity;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.view.recycle.AbsRecycleAdapter;
import com.yiliaodemo.chat.view.recycle.OnItemClickListener;
import com.yiliaodemo.chat.view.recycle.ViewHolder;
import com.zhy.http.okhttp.OkHttpUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * vip
 */
public class VipFragment extends BaseFragment {

    @BindView(R.id.vip_tv)
    TextView vipTv;

    @BindView(R.id.vip_btn)
    TextView vipBtn;

    @BindView(R.id.rights_interests_rv)
    RecyclerView rightsInterestsRv;

    @BindView(R.id.package_rv)
    RecyclerView packageRv;

    @BindView(R.id.vip_pay)
    TextView vipPay;

    @BindView(R.id.rights_interests_iv)
    ImageView rightsInterestsIv;

    @BindView(R.id.open_iv)
    ImageView openIv;

    @BindView(R.id.bg_ll)
    View bgView;

    AbsRecycleAdapter adapter;

    Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRightsInterestsRv();
        getVipList();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshVip();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    protected int initLayout() {
        return R.layout.fragment_vip;
    }

    @OnClick({R.id.vip_pay})
    public void onClick(View view) {

        /**
         * ????????????
         */
        if (adapter != null && adapter.getData() != null) {
            VipBean selectedBean = null;
            for (Object datum : adapter.getData()) {
                VipBean bean = (VipBean) datum;
                if (bean.isSelected) {
                    selectedBean = bean;
                    break;
                }
            }
            if (selectedBean == null) {
                ToastUtil.showToast("?????????VIP");
                return;
            }
            PayChooserActivity.start(getActivity(), selectedBean.t_id);
        }
    }

    /**
     * vip????????????
     */
    protected void setVipTv(VipInfoBean bean) {
        vipTv.setText("?????????");
        if (bean.t_is_vip == 0) {
            vipTv.setText(String.format("%s??????", bean.vipTime.t_end_time));
        }
    }

    /**
     * ??????VIP??????UI
     */
    private void refreshVip() {
        AppManager.getInstance().refreshVip(new OnCommonListener<VipInfoBean>() {
            @Override
            public void execute(VipInfoBean bean) {
                if (getActivity() == null || getActivity().isFinishing())
                    return;
                setVipTv(bean);
            }
        });
    }

    /**
     * ????????????RecycleView
     */
    private void setRightsInterestsRv() {
        AbsRecycleAdapter adapter = new AbsRecycleAdapter(
                new AbsRecycleAdapter.Type(R.layout.item_vip_rights_interests, RightsInterestsBean.class)) {
            @Override
            public void convert(ViewHolder holder, Object t) {
                RightsInterestsBean bean = (RightsInterestsBean) t;
                holder.<TextView>getView(R.id.title_tv)
                        .setCompoundDrawablesRelativeWithIntrinsicBounds(0, bean.drawId, 0, 0);
                holder.<TextView>getView(R.id.title_tv).setText(bean.title);
                holder.<TextView>getView(R.id.sub_title_tv).setText(bean.subtitle);
            }
        };
        rightsInterestsRv.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        rightsInterestsRv.setAdapter(adapter);
        adapter.setDatas(getRightsInterests());
    }

    /**
     * ??????????????????
     */
    protected List<RightsInterestsBean> getRightsInterests() {
        return Arrays.asList(

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests1,
                        "???????????????????????????",
                        "?????????????????????10??????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests2,
                        "??????????????????",
                        "????????????????????????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests3,
                        "??????????????????",
                        "????????????????????????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests4,
                        "??????????????????",
                        "????????????????????????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests5,
                        "??????????????????",
                        "??????????????????????????????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests6,
                        "??????????????????",
                        "????????????????????????????????????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests6,
                        "????????????",
                        "??????????????????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests6,
                        "????????????",
                        "??????????????????"),

                new RightsInterestsBean(
                        R.drawable.vip_rights_interests6,
                        "??????????????????",
                        "????????????????????????????????????")

        );
    }

    /**
     * ??????VIP??????
     */
    private void getVipList() {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", AppManager.getInstance().getUserInfo().t_id);
        paramMap.put("t_vip_type", getVipType());
        OkHttpUtils.post().url(ChatApi.GET_VIP_SET_MEAL_LIST())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseListResponse<VipBean>>() {
            @Override
            public void onResponse(BaseListResponse<VipBean> response, int id) {
                if (getActivity() == null || getActivity().isFinishing()) {
                    return;
                }
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    List<VipBean> vipBeans = response.m_object;
                    if (vipBeans != null && vipBeans.size() > 0) {
                        vipBeans.get(0).isSelected = true;
                        setVipList(vipBeans);
                    }
                }
            }
        });
    }

    /**
     * 0:??????vip  2:??????svip
     */
    protected int getVipType() {
        return 0;
    }

    /**
     * vip?????????
     */
    private void setVipList(List<VipBean> beans) {
        if (adapter == null) {
            final DecimalFormat decimalFormat = new DecimalFormat("#.##");
            adapter = new AbsRecycleAdapter(new AbsRecycleAdapter.Type(R.layout.item_vip_package, VipBean.class)) {
                @Override
                public void convert(ViewHolder holder, Object t) {
                    VipBean bean = (VipBean) t;
                    if (bean.isSelected) {
                        vipPay.setText(String.format("????????????%s???", decimalFormat.format(bean.t_money)));
                    }
                    holder.itemView.setSelected(bean.isSelected);
                    holder.<TextView>getView(R.id.month_tv).setText(bean.t_setmeal_name);
                    holder.<TextView>getView(R.id.price_tv).setText(String.format("???%s", decimalFormat.format(bean.t_money)));
                    holder.<TextView>getView(R.id.day_price_tv).setText(bean.t_remarks);
                    holder.<TextView>getView(R.id.day_price_tv).setVisibility(TextUtils.isEmpty(bean.t_remarks) ? View.INVISIBLE : View.VISIBLE);
                }
            };
            adapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view, Object object, int position) {
                    VipBean selectedBean = (VipBean) adapter.getData().get(position);
                    for (Object datum : adapter.getData()) {
                        VipBean bean = (VipBean) datum;
                        bean.isSelected = bean == selectedBean;
                    }
                    adapter.notifyDataSetChanged();
                }
            });
            adapter.setDatas(beans);
            packageRv.setLayoutManager(new GridLayoutManager(getActivity(), 3));
            packageRv.setAdapter(adapter);
        }
    }

    protected static class RightsInterestsBean {

        public RightsInterestsBean(int drawId, String title, String subtitle) {
            this.drawId = drawId;
            this.title = title;
            this.subtitle = subtitle;
        }

        public int drawId;
        public String title;
        public String subtitle;
    }
}