package com.yiliaodemo.chat.activity;

import android.graphics.Bitmap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.bean.ErWeiBean;
import com.yiliaodemo.chat.bean.PosterBean;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.helper.ImageLoadHelper;
import com.yiliaodemo.chat.helper.ShareUrlHelper;
import com.yiliaodemo.chat.helper.SharedPreferenceHelper;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.util.BitmapUtil;
import com.yiliaodemo.chat.util.DensityUtil;
import com.yiliaodemo.chat.util.FileUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.util.ViewShotUtil;
import com.yiliaodemo.chat.util.ZXingUtils;
import com.yiliaodemo.chat.util.permission.PermissionUtil;
import com.yiliaodemo.chat.view.Xcircleindicator;
import com.yiliaodemo.chat.view.gallery.CardAdapter;
import com.yiliaodemo.chat.view.gallery.CardScaleHelper;
import com.yiliaodemo.chat.view.recycle.AbsRecycleAdapter;
import com.yiliaodemo.chat.view.recycle.ViewHolder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class PromotionPosterActivity extends BaseActivity {

    @BindView(R.id.content_rv)
    RecyclerView mContentRv;

    @BindView(R.id.empty_tv)
    TextView emptyTv;

    @BindView(R.id.indicator)
    Xcircleindicator indicator;

    private CardAdapter cardAdapter;
    private CardScaleHelper mCardScaleHelper = null;

    private int mLastPos = -1;
    private ImageView mBlurView;
    private List<PosterBean> mList = new ArrayList<>();
    private Bitmap codeBitmap;
    private String shareUrl;
    private boolean gettedData;

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_promotion_poster);
    }

    @Override
    protected void onContentAdded() {
        setTitle(R.string.promotion_poster);
        initRecycler();
        initShare();
    }

    /**
     * 初始化分享
     */
    private void initShare() {
        String saveUrl = SharedPreferenceHelper.getShareUrl(getApplicationContext());
        if (!TextUtils.isEmpty(saveUrl)) {
            shareUrl = saveUrl;
            //生成bitmap
            codeBitmap = ZXingUtils.createQRImage(shareUrl, DensityUtil.dip2px(getApplicationContext(), 100)
                    , DensityUtil.dip2px(getApplicationContext(), 100));
        }
        getShareUrl();
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecycler() {
        LinearLayoutManager gridLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mContentRv.setLayoutManager(gridLayoutManager);
        RecyclerView.ItemAnimator itemAnimator = mContentRv.getItemAnimator();
        if (itemAnimator != null) {
            itemAnimator.setChangeDuration(0);
        }

        cardAdapter = new CardAdapter(new AbsRecycleAdapter.Type(R.layout.item_gallery, PosterBean.class)) {
            @Override
            public void convert(ViewHolder holder, Object t) {
                PosterBean bean = (PosterBean) t;
                ImageLoadHelper.glideShowCornerImageWithUrl(mContext, bean.t_img_path,
                        (ImageView) holder.getView(R.id.imageView));
                if (codeBitmap != null) {
                    holder.<TextView>getView(R.id.invite_code_tv).setVisibility(View.VISIBLE);
                    holder.<TextView>getView(R.id.invite_id_tv).setVisibility(View.VISIBLE);
                    holder.<TextView>getView(R.id.invite_id_tv).setText(String.valueOf(AppManager.getInstance().getUserInfo().getIdCard()));
                    holder.<ImageView>getView(R.id.code_iv).setImageBitmap(codeBitmap);
                }
            }
        };
        cardAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                emptyTv.setVisibility(mList == null || mList.size() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        mContentRv.setAdapter(cardAdapter);
        mCardScaleHelper = new CardScaleHelper();
        initBlurBackground();
    }

    private void initBlurBackground() {
        mBlurView = findViewById(R.id.blurView);
        mContentRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NotNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    notifyBackgroundChange();
                }
            }
        });
        notifyBackgroundChange();
    }

    private void notifyBackgroundChange() {
        if (mList == null || mList.size() <= 0 || mLastPos == mCardScaleHelper.getCurrentItemPos()) {
            return;
        }
        mLastPos = mCardScaleHelper.getCurrentItemPos();
        indicator.setCurrentPage(mLastPos);
        String url = mList.get(mCardScaleHelper.getCurrentItemPos()).t_img_path;
        ImageLoadHelper.glideShowCornerImageWithFade(mContext, url, mBlurView);
    }

    /**
     * 获取分享数据
     */
    private void getShareUrl() {
        ShareUrlHelper.getShareUrl(new OnCommonListener<ErWeiBean<PosterBean>>() {
            @Override
            public void execute(ErWeiBean<PosterBean> bean) {
                if (!isFinishing() && bean != null) {
                    gettedData = true;
                    shareUrl = bean.shareUrl;
                    SharedPreferenceHelper.saveShareUrl(getApplicationContext(), bean.shareUrl);
                    codeBitmap = ZXingUtils.createQRImage(shareUrl, DensityUtil.dip2px(getApplicationContext(), 100)
                            , DensityUtil.dip2px(getApplicationContext(), 100));
                    List<PosterBean> beans = bean.backgroundPath;
                    if (beans != null && beans.size() > 0) {
                        mList = beans;
                        cardAdapter.setData(mList, true);
                        indicator.initData(mList.size(), 0);
                        indicator.setCurrentPage(0);
                        mCardScaleHelper.setCurrentItemPos(0);
                        mCardScaleHelper.attachToRecyclerView(mContentRv);
                        notifyBackgroundChange();
                    }
                }
            }
        });
    }

    @OnClick(R.id.share_tv)
    public void onClick(View view) {
        if (TextUtils.isEmpty(shareUrl)) {
            getShareUrl();
            ToastUtil.showToast(gettedData ? "分享链接错误，请联系客服" : "正在获取数据中...");
            return;
        }
        if (mList == null || mList.size() == 0) {
            getShareUrl();
            return;
        }

        PermissionUtil.requestPermissions(PromotionPosterActivity.this, new PermissionUtil.OnPermissionListener() {
            @Override
            public void onPermissionGranted() {

                showLoadingDialog();
                RecyclerView.ViewHolder viewHolder = mContentRv.findViewHolderForAdapterPosition(mCardScaleHelper.getCurrentItemPos());
                Bitmap bitmap = ViewShotUtil.viewConversionBitmap(viewHolder.itemView.findViewById(R.id.gallery_fl));
                if (bitmap == null) {
                    ToastUtil.showToast(R.string.picture_save_error);
                    return;
                }
                // 首先保存图片
                File pFile = new File(FileUtil.YCHAT_DIR);
                if (!pFile.exists()) {
                    boolean res = pFile.mkdir();
                    if (!res) {
                        ToastUtil.showToast(R.string.picture_save_error);
                        return;
                    }
                }
                File dFile = new File(Constant.SHARE_PATH);
                if (!dFile.exists()) {
                    boolean res = dFile.mkdir();
                    if (!res) {
                        ToastUtil.showToast(R.string.picture_save_error);
                        return;
                    }
                } else {
                    FileUtil.deleteFiles(dFile.getPath());
                }
                File file = BitmapUtil.saveBitmapAsJpg(bitmap, Constant.SHARE_PATH + "poster.png");
                bitmap.recycle();
                if (file == null) {
                    dismissLoadingDialog();
                    ToastUtil.showToast(R.string.picture_save_error);
                    return;
                }
                dismissLoadingDialog();
                ShareActivity.start(PromotionPosterActivity.this,
                        new ShareActivity.ShareParams()
                                .typeImage()
                                .setSavePic(true)
                                .setImageUrl(file.getPath())
                                .setContentUrl(shareUrl));

            }

            @Override
            public void onPermissionDenied() {
                ToastUtil.showToast("没有文件读写权限，无法保存");
            }
        }, PermissionUtil.filePermission);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (codeBitmap != null) {
            codeBitmap.recycle();
        }
    }

}