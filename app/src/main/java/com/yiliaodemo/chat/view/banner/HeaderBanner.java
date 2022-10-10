package com.yiliaodemo.chat.view.banner;

import android.app.Activity;
import android.view.ViewGroup;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.view.recycle.BannerHolder;
import com.yiliaodemo.chat.view.recycle.ListTypeAdapter;
import com.yiliaodemo.chat.view.recycle.ViewHolder;

public class HeaderBanner extends ListTypeAdapter.BindViewHolder {

    private BannerHolder bannerHolder;
    private Activity activity;

    public HeaderBanner(Activity activity) {
        super(R.layout.header_banner);
        this.activity = activity;
    }

    @Override
    public ViewHolder createViewHolder(ViewGroup parent, int layoutId) {
        ViewHolder viewHolder = super.createViewHolder(parent, layoutId);
        bannerHolder = new BannerHolder(activity, viewHolder.itemView);
        bannerHolder.loop(activity, true);
        return viewHolder;
    }

    public void loop(Activity activity, boolean b) {
        if (bannerHolder != null) {
            bannerHolder.loop(activity, b);
        }
    }
}