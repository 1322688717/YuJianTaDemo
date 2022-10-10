package com.yiliaodemo.chat.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.activity.PostActiveActivity;
import com.yiliaodemo.chat.base.BaseFragment;
import com.yiliaodemo.chat.view.tab.FragmentParamBuilder;
import com.yiliaodemo.chat.view.tab.LabelViewHolder;
import com.yiliaodemo.chat.view.tab.TabFragmentAdapter;
import com.yiliaodemo.chat.view.tab.TabPagerLayout;

/**
 * 发现
 */
public class FindFragment extends BaseFragment {

    @Override
    protected int initLayout() {
        return R.layout.fragment_find_layout;
    }

    @Override
    protected void initView(View view, Bundle savedInstanceState) {

        ViewPager mContentVp = view.findViewById(R.id.content_vp);
        TabPagerLayout tabPagerLayout = view.findViewById(R.id.category_rg);

        TabFragmentAdapter adapter = new TabFragmentAdapter(getChildFragmentManager(), mContentVp);

        //0 全部 1 关注 2 同城
        adapter.init(

                FragmentParamBuilder.create()
                        .withName("全部")
                        .withClazz(ActiveFragment.class)
                        .withArgument("reqType", 0)
                        .withViewHolder(new LabelViewHolder(tabPagerLayout))
                        .build(),

                FragmentParamBuilder.create()
                        .withName("同城")
                        .withClazz(ActiveFragment.class)
                        .withArgument("reqType", 2)
                        .withViewHolder(new LabelViewHolder(tabPagerLayout))
                        .build(),

                FragmentParamBuilder.create()
                        .withName("关注")
                        .withClazz(ActiveFragment.class)
                        .withArgument("reqType", 1)
                        .withViewHolder(new LabelViewHolder(tabPagerLayout))
                        .build()
        );

        tabPagerLayout.init(mContentVp);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View postBtn = findViewById(R.id.post_btn);
        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), PostActiveActivity.class);
                startActivity(intent);
            }
        });
    }
}