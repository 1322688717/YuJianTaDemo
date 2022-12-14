package com.yiliaodemo.chat.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.adapter.PostActiveRecyclerAdapter;
import com.yiliaodemo.chat.adapter.SetChargeRecyclerAdapter;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.ActiveLocalBean;
import com.yiliaodemo.chat.bean.PostFileBean;
import com.yiliaodemo.chat.bean.VideoRetrieverBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.helper.ImageHelper;
import com.yiliaodemo.chat.layoutmanager.PickerLayoutManager;
import com.yiliaodemo.chat.listener.OnFileUploadListener;
import com.yiliaodemo.chat.listener.OnLuCompressListener;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.oss.QServiceCfg;
import com.yiliaodemo.chat.util.BitmapUtil;
import com.yiliaodemo.chat.util.DevicesUtil;
import com.yiliaodemo.chat.util.FileUtil;
import com.yiliaodemo.chat.util.LogUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.util.VideoFileUtils;
import com.yiliaodemo.chat.videoupload.TXUGCPublish;
import com.yiliaodemo.chat.videoupload.TXUGCPublishTypeDef;
import com.yiliaodemo.chat.view.MyProcessView;
import com.zhihu.matisse.Matisse;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Call;

/*
 * Copyright (C) 2018
 * ????????????
 *
 * ?????????????????????????????????
 * ?????????
 * ???????????????2018/12/19
 *
 * ????????????
 * ???????????????
 * ????????????
 */
public class PostActiveActivity extends BaseActivity {

    @BindView(R.id.content_rv)
    RecyclerView mContentRv;

    @BindView(R.id.video_fl)
    FrameLayout mVideoFl;

    @BindView(R.id.video_iv)
    ImageView mVideoIv;

    @BindView(R.id.video_charge_tv)
    TextView mVideoChargeTv;

    @BindView(R.id.content_et)
    EditText mContentEt;

    @BindView(R.id.upload_fl)
    FrameLayout mUploadFl;

    @BindView(R.id.process_pv)
    MyProcessView mProcessPv;

    @BindView(R.id.video_done_tv)
    TextView mVideoDoneTv;

    @BindView(R.id.where_tv)
    TextView mWhereTv;

    @BindView(R.id.where_iv)
    ImageView mWhereIv;

    private PostActiveRecyclerAdapter mAdapter;

    //???????????????
    private List<ActiveLocalBean> mLocalBeans = new ArrayList<>();

    private final int REQUEST_ALBUM_IMAGE_VIDEO = 0x100;//???????????????????????????
    private final int REQUEST_ALBUM_IMAGE = 0x111;//??????????????????

    //????????????
    //?????????????????????????????????
    private String mSelectedLocalVideoThumbPath = "";
    private String mVideoFileId = "";
    private String mVideoFileUrl = "";
    private String mVideoTime = "";

    //??????
    private final int VIDEO = 0;
    private final int PICTURE = 1;
    private String[] mVideoStrs = new String[]{};
    private String[] mPictureStrs = new String[]{};
    private String mSelectContent = "";
    private int mPostType = PICTURE;//????????????????????????????????????,????????????
    private final int CAMERA_REQUEST_CODE = 0x116;
    private final int REQUEST_SELECT_POSITION = 0x180;

    //???????????? ????????????
    private TXUGCPublish mVideoPublish = null;
    private QServiceCfg mQServiceCfg;//??????

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_post_active_layout);
    }

    @Override
    protected void onContentAdded() {
        initTitle();
        initRecycler();
        getAnchorVideoCost();
        getPrivatePhotoMoney();
    }

    /**
     * ???????????????
     */
    private void initTitle() {
        setTitle(R.string.post_active);
        setRightText(R.string.post);
        mRightTv.setTextColor(getResources().getColor(R.color.white));
        mRightTv.setBackgroundResource(R.drawable.shape_post_text_back);
        mRightTv.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mRightTv.getLayoutParams();
        params.rightMargin = DevicesUtil.dp2px(getApplicationContext(), 15);
        mQServiceCfg = QServiceCfg.instance(getApplicationContext());
    }

    /**
     * ?????????recycler
     */
    private void initRecycler() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
        mContentRv.setNestedScrollingEnabled(false);
        mContentRv.setLayoutManager(gridLayoutManager);
        mAdapter = new PostActiveRecyclerAdapter(PostActiveActivity.this);
        mContentRv.setAdapter(mAdapter);
        mAdapter.setOnPostItemClickListener(new PostActiveRecyclerAdapter.OnPostItemClickListener() {
            @Override
            public void onAddClick(int position) {
                showChooseDialog();
            }

            @Override
            public void onDeleteClick(ActiveLocalBean bean, int position) {
                try {
                    if (bean != null && !TextUtils.isEmpty(bean.localPath)) {
                        File file = new File(bean.localPath);
                        boolean result = file.delete();
                        if (result) {//???????????????
                            //?????????9?????????,????????????????????????
                            int size = mLocalBeans.size();
                            if (size >= 9 && !TextUtils.isEmpty(mLocalBeans.get(size - 1).localPath)) {
                                mLocalBeans.remove(position);
                                mLocalBeans.add(new ActiveLocalBean());
                            } else {
                                mLocalBeans.remove(position);
                            }
                            mAdapter.loadData(mLocalBeans);
                        } else {
                            ToastUtil.showToast(getApplicationContext(), R.string.delete_fail);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtil.showToast(getApplicationContext(), R.string.delete_fail);
                }
            }

            @Override
            public void onFreeClick(ActiveLocalBean bean, int position) {
                showChargeOptionDialog(PICTURE, position);
            }
        });

        //?????????????????????  ??????????????????
        mLocalBeans.add(new ActiveLocalBean());
        mAdapter.loadData(mLocalBeans);
    }

    @OnClick({
            R.id.video_delete_iv,
            R.id.video_charge_tv,
            R.id.right_text,
            R.id.where_ll
    })
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_delete_iv: {//????????????
                mContentRv.setVisibility(View.VISIBLE);
                mVideoFl.setVisibility(View.GONE);
                mSelectedLocalVideoThumbPath = "";
                mVideoChargeTv.setText(getResources().getString(R.string.free_one));
                mVideoFileId = "";
                mVideoFileUrl = "";
                mVideoTime = "";
                break;
            }
            case R.id.video_charge_tv: {//????????????
                showChargeOptionDialog(VIDEO, 0);
                break;
            }
            case R.id.right_text: {//??????
                postActive();
                break;
            }
            case R.id.where_ll: {//????????????
                Intent intent = new Intent(getApplicationContext(), SelectPositionActivity.class);
                startActivityForResult(intent, REQUEST_SELECT_POSITION);
                break;
            }
        }
    }

    /**
     * ????????????
     */
    private void postActive() {
        //??????
        String textContent = mContentEt.getText().toString().trim();
        //??????
        //??????????????????????????????
        if (mLocalBeans.size() == 1 && TextUtils.isEmpty(mSelectedLocalVideoThumbPath)
                && TextUtils.isEmpty(textContent)) {
            ToastUtil.showToast(getApplicationContext(), R.string.set_content);
            return;
        }

        showLoadingDialog();
        //????????????
        if (mPostType == VIDEO) {
            if (!TextUtils.isEmpty(mSelectedLocalVideoThumbPath)) {
                uploadVideoCoverFileWithQQ(mSelectedLocalVideoThumbPath);
            }
        } else {
            //??????,??????????????????,????????????????????????????????????
            //???????????????
            if (mLocalBeans != null && mLocalBeans.size() > 1) {
                uploadImageFileWithQQ(0, new OnFileUploadListener() {
                    @Override
                    public void onFileUploadSuccess() {
                        List<PostFileBean> postFileBeans = new ArrayList<>();
                        for (ActiveLocalBean bean : mLocalBeans) {
                            if (!TextUtils.isEmpty(bean.imageUrl)) {
                                int gold = bean.gold;
                                PostFileBean fileBean = new PostFileBean();
                                fileBean.fileType = 0;
                                fileBean.fileUrl = bean.imageUrl;
                                fileBean.gold = gold;
                                if (gold > 0) {
                                    fileBean.t_is_private = 1;
                                }
                                postFileBeans.add(fileBean);
                            }
                        }
                        addToOurActive(postFileBeans);
                    }
                });
            } else {
                addToOurActive(null);
            }
        }
    }

    /**
     * ?????????????????????????????????
     */
    private void uploadImageFileWithQQ(final int index, final OnFileUploadListener listener) {
        String filePath = mLocalBeans.get(index).localPath;
        if (TextUtils.isEmpty(filePath)) {//????????????,????????????????????????
            listener.onFileUploadSuccess();
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            uploadImageFileWithQQ(index + 1, listener);
            return;
        }
        //?????????
        String fileName;
        if (filePath.length() < 50) {
            fileName = filePath.substring(filePath.length() - 17, filePath.length());
        } else {
            String last = filePath.substring(filePath.length() - 4, filePath.length());
            if (last.contains("png")) {
                fileName = System.currentTimeMillis() + ".png";
            } else {
                fileName = System.currentTimeMillis() + ".jpg";
            }
        }

        String cosPath = "/active/" + fileName;
        long signDuration = 600; //?????????????????????????????????
        PutObjectRequest putObjectRequest = new PutObjectRequest(Constant.TENCENT_CLOUD_BUCKET, cosPath, filePath);
        putObjectRequest.setSign(signDuration, null, null);
        mQServiceCfg.getCosCxmService().putObjectAsync(putObjectRequest, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                LogUtil.i("???????????????success =  " + result.accessUrl);
                String resultUrl = result.accessUrl;
                if (!resultUrl.contains("http") || !resultUrl.contains("https")) {
                    resultUrl = "https://" + resultUrl;
                }
                if (mLocalBeans != null) {
                    //???ActiveLocalBean????????????
                    if (mLocalBeans.size() >= index + 1) {
                        ActiveLocalBean localBean = mLocalBeans.get(index);
                        localBean.imageUrl = resultUrl;
                    }
                    //?????????????????????,???????????????,????????????????????????
                    if (mLocalBeans.size() > index + 1
                            && !TextUtils.isEmpty(mLocalBeans.get(index + 1).localPath)) {
                        uploadImageFileWithQQ(index + 1, listener);
                    } else {
                        listener.onFileUploadSuccess();
                    }
                }
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, CosXmlClientException clientException, CosXmlServiceException serviceException) {
                String errorMsg = clientException != null ? clientException.toString() : serviceException.toString();
                LogUtil.i("?????????fail: " + errorMsg);
            }
        });
    }

    /**
     * ?????????????????????????????????
     */
    private void uploadVideoCoverFileWithQQ(String filePath) {
        String fileName;
        if (filePath.length() < 50) {
            fileName = filePath.substring(filePath.length() - 17, filePath.length());
        } else {
            String last = filePath.substring(filePath.length() - 4, filePath.length());
            if (last.contains("png")) {
                fileName = System.currentTimeMillis() + ".png";
            } else {
                fileName = System.currentTimeMillis() + ".jpg";
            }
        }

        String cosPath = "/active/" + fileName;
        long signDuration = 600; //?????????????????????????????????
        PutObjectRequest putObjectRequest = new PutObjectRequest(Constant.TENCENT_CLOUD_BUCKET, cosPath, filePath);
        putObjectRequest.setSign(signDuration, null, null);
        putObjectRequest.setProgressListener(new CosXmlProgressListener() {
            @Override
            public void onProgress(long progress, long max) {
            }
        });
        mQServiceCfg.getCosCxmService().putObjectAsync(putObjectRequest, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                LogUtil.i("?????????success =  " + result.accessUrl);
                String resultUrl = result.accessUrl;
                if (!resultUrl.contains("http") || !resultUrl.contains("https")) {
                    resultUrl = "https://" + resultUrl;
                }

                int gold = 0;
                if (!mVideoChargeTv.getText().toString().trim().equals(getResources().getString(R.string.free_one))) {
                    gold = Integer.parseInt(mVideoChargeTv.getText().toString());
                }
                List<PostFileBean> fileBeans = new ArrayList<>();
                PostFileBean bean = new PostFileBean();
                bean.fileId = mVideoFileId;
                bean.fileType = 1;
                bean.fileUrl = mVideoFileUrl;
                bean.gold = gold;
                bean.t_video_time = mVideoTime;
                if (gold > 0) {
                    bean.t_is_private = 1;
                }
                bean.t_cover_img_url = resultUrl;
                fileBeans.add(bean);
                addToOurActive(fileBeans);
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, CosXmlClientException clientException, CosXmlServiceException serviceException) {
                String errorMsg = clientException != null ? clientException.toString() : serviceException.toString();
                LogUtil.i("?????????fail: " + errorMsg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingDialog();
                        ToastUtil.showToast(getApplicationContext(), R.string.post_fail);
                    }
                });
            }
        });
    }

    /**
     * ????????????????????????
     */
    private void addToOurActive(List<PostFileBean> fileBeans) {
        //??????
        String textContent = mContentEt.getText().toString().trim();

        //??????
        Object fileJson = "";
        if (fileBeans != null && fileBeans.size() > 0) {
            fileJson = JSON.toJSON(fileBeans);
        }

        //??????
        String position = mWhereTv.getText().toString().trim();
        if (!TextUtils.isEmpty(position) && position.equals(getResources().getString(R.string.where))) {
            position = "";
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("content", textContent);
        paramMap.put("isVisible", 0);
        paramMap.put("address", position);//??????
        paramMap.put("files", fileJson);//??????
        OkHttpUtils.post().url(ChatApi.RELEASE_DYNAMIC())
                .addParams("param", ParamUtil.getParam(paramMap, false))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                dismissLoadingDialog();
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    ToastUtil.showToast(getApplicationContext(), R.string.post_success);
                    finish();
                } else {
                    ToastUtil.showToast(getApplicationContext(), R.string.post_fail);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                dismissLoadingDialog();
                ToastUtil.showToast(getApplicationContext(), R.string.post_fail);
            }
        });
    }

    /**
     * ????????????dialog
     */
    private void showChooseDialog() {
        final Dialog mDialog = new Dialog(PostActiveActivity.this, R.style.DialogStyle_Dark_Background);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialog_choose_image_from_layout, null);
        setDialogView(view, mDialog);
        mDialog.setContentView(view);
        Point outSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(outSize);
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = outSize.x;
            window.setGravity(Gravity.CENTER); // ??????????????????dialog???????????????
        }
        mDialog.setCanceledOnTouchOutside(true);
        if (!isFinishing()) {
            mDialog.show();
        }
    }

    /**
     * ??????????????????dialog???view
     */
    private void setDialogView(View view, final Dialog mDialog) {
        //??????
        TextView shoot_tv = view.findViewById(R.id.shoot_tv);
        shoot_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpToCamera();
                mDialog.dismiss();
            }
        });
        //??????
        TextView album_tv = view.findViewById(R.id.album_tv);
        album_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //?????????????????????
                //?????????????????????
                if (mLocalBeans != null && mLocalBeans.size() == 1) {
                    ImageHelper.openPictureVideoChoosePage(PostActiveActivity.this, REQUEST_ALBUM_IMAGE_VIDEO);
                } else {
                    //????????????
                    ImageHelper.openPictureChoosePage(PostActiveActivity.this, REQUEST_ALBUM_IMAGE);
                }
                mDialog.dismiss();
            }
        });
    }

    /**
     * ???????????????
     */
    private void jumpToCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager
                    .PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager
                            .PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager
                            .PERMISSION_GRANTED) {
                Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            } else {
                //????????????????????????????????????????????????
                ActivityCompat.requestPermissions(PostActiveActivity.this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA}, 100);
            }
        } else {
            Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //????????????    ???????????????????????????????????????, ????????????????????????
            if (requestCode == REQUEST_ALBUM_IMAGE_VIDEO || requestCode == REQUEST_ALBUM_IMAGE) {
                List<Uri> mSelectedUris = Matisse.obtainResult(data);
                List<String> stringList = Matisse.obtainPathResult(data);
                if (mSelectedUris != null && mSelectedUris.size() > 0 && stringList != null && stringList.size() > 0) {
                    String path = stringList.get(0);
                    Uri fileUri = mSelectedUris.get(0);
                    if (!TextUtils.isEmpty(path) && fileUri != null) {
                        //???????????????
                        if (requestCode == REQUEST_ALBUM_IMAGE || !fileUri.toString().contains("video")) {
                            mPostType = PICTURE;
                            dealImageFile(path);
                        } else {
                            //???????????????
                            mPostType = VIDEO;
                            dealVideoFile(path);
                        }
                    }
                }
            } else if (requestCode == REQUEST_SELECT_POSITION) {//????????????
                if (data != null) {
                    String selectedPosition = data.getStringExtra(Constant.CHOOSED_POSITION);
                    if (!TextUtils.isEmpty(selectedPosition) &&
                            !selectedPosition.equals(getResources().getString(R.string.not_show))) {
                        mWhereTv.setText(selectedPosition);
                        mWhereTv.setTextColor(getResources().getColor(R.color.main));
                        mWhereIv.setSelected(true);
                    }
                }
            }
        } else if (requestCode == CAMERA_REQUEST_CODE && data != null) {
            if (resultCode == 101) {//??????
                String imagePath = data.getStringExtra("imagePath");
                LogUtil.i("??????????????????:  " + imagePath);
                if (!TextUtils.isEmpty(imagePath)) {
                    mPostType = PICTURE;
                    compressImageWithLuBan(imagePath);
                } else {
                    ToastUtil.showToast(mContext.getApplicationContext(), R.string.file_invalidate);
                }
            } else if (resultCode == 102) {//??????
                String videoUrl = data.getStringExtra("videoUrl");
                LogUtil.i("???????????????Url:  " + videoUrl);
                if (!TextUtils.isEmpty(videoUrl)) {
                    mPostType = VIDEO;
                    dealShootVideoFile(videoUrl);
                } else {
                    ToastUtil.showToast(mContext.getApplicationContext(), R.string.file_invalidate);
                }
            }
        }
    }


    //-----------------------------????????????----------------------------------------

    /**
     * ?????????????????????,????????????????????????
     */
    private void dealShootVideoFile(String videoUrl) {
        File file = new File(videoUrl);
        if (!file.exists()) {
            ToastUtil.showToast(getApplicationContext(), R.string.file_not_exist);
            return;
        }
        LogUtil.i("????????????: " + file.length() / 1024 / 1024);
        double fileSize = (double) file.length() / 1024 / 1024;
        if (fileSize > 50) {
            ToastUtil.showToast(getApplicationContext(), R.string.file_too_big);
            return;
        }
        //??????????????????????????????
        VideoThumbTask task = new VideoThumbTask(PostActiveActivity.this, videoUrl);
        task.execute();
        //????????????
        getSign(videoUrl);
    }

    /**
     * ---------------------------??????????????????-------------------------------------
     */
    private void dealVideoFile(String filePath) {
        try {
            File file = new File(filePath);
            LogUtil.i("????????????: " + file.length() / 1024 / 1024);
            double fileSize = (double) file.length() / 1024 / 1024;
            if (fileSize > 50) {
                ToastUtil.showToast(getApplicationContext(), R.string.file_too_big);
                return;
            }
            showLoadingDialog();

            //??????????????????????????????
            VideoThumbTask task = new VideoThumbTask(PostActiveActivity.this, filePath);
            task.execute();
            //????????????
            getSign(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            dismissLoadingDialog();
        }
    }

    static class VideoThumbTask extends AsyncTask<Integer, Void, Bitmap> {

        private WeakReference<PostActiveActivity> mWeakAty;
        private String mPath;

        VideoThumbTask(PostActiveActivity activity, String path) {
            mWeakAty = new WeakReference<>(activity);
            mPath = path;
        }

        @Override
        protected Bitmap doInBackground(Integer... integers) {
            final PostActiveActivity activity = mWeakAty.get();
            if (activity != null) {
                try {
                    final int overWidth = (int) (DevicesUtil.getScreenW(activity) * 0.75);
                    final int overHeight = (int) (DevicesUtil.getScreenH(activity) * 0.75);
                    VideoRetrieverBean retrieverBean = VideoFileUtils.getVideoInfo(mPath);
                    Bitmap bitmap = retrieverBean.bitmap;
                    Bitmap showBmp = VideoFileUtils.getVideoThumbnail(mPath, DevicesUtil.dp2px(activity, 100),
                            DevicesUtil.dp2px(activity, 100), MediaStore.Video.Thumbnails.MICRO_KIND);
                    //???????????????
                    File pFile = new File(FileUtil.YCHAT_DIR);
                    if (!pFile.exists()) {
                        boolean res = pFile.mkdir();
                        if (!res) {
                            return null;
                        }
                    }
                    File dir = new File(Constant.ACTIVE_VIDEO_DIR);
                    if (!dir.exists()) {
                        boolean res = dir.mkdir();
                        if (!res) {
                            return null;
                        }
                    } else {
                        FileUtil.deleteFiles(Constant.ACTIVE_VIDEO_DIR);
                    }

                    activity.mVideoTime = retrieverBean.videoDuration;
                    activity.mSelectedLocalVideoThumbPath = Constant.ACTIVE_VIDEO_DIR + System.currentTimeMillis() + ".png";
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    final byte[] bytes = baos.toByteArray();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Glide.with(activity).asBitmap().load(bytes).override(overWidth, overHeight)
                                    .centerCrop().into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    BitmapUtil.saveBitmapAsPng(resource, activity.mSelectedLocalVideoThumbPath);
                                }
                            });
                        }
                    });
                    return showBmp;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            PostActiveActivity activity = mWeakAty.get();
            if (activity != null && bitmap != null) {//??????adapter
                activity.mVideoFl.setVisibility(View.VISIBLE);
                activity.mContentRv.setVisibility(View.GONE);
                activity.mVideoIv.setImageBitmap(bitmap);
                activity.dismissLoadingDialog();
            }
        }
    }

    /**
     * ????????????????????????
     * fromCheck  ????????????????????? ??????????????????
     */
    private void getSign(final String filePath) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        OkHttpUtils.post().url(ChatApi.GET_VIDEO_SIGN())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                ToastUtil.showToast(getApplicationContext(), R.string.upload_fail);
            }

            @Override
            public void onResponse(String response, int id) {
                if (!TextUtils.isEmpty(response)) {
                    JSONObject jsonObject = JSON.parseObject(response);
                    int m_istatus = jsonObject.getInteger("m_istatus");
                    if (m_istatus == NetCode.SUCCESS) {
                        String m_object = jsonObject.getString("m_object");
                        if (!TextUtils.isEmpty(m_object)) {
                            //????????????
                            beginUpload(m_object, filePath);
                        } else {
                            ToastUtil.showToast(getApplicationContext(), R.string.upload_fail);
                        }
                    } else {
                        ToastUtil.showToast(getApplicationContext(), R.string.upload_fail);
                    }
                } else {
                    ToastUtil.showToast(getApplicationContext(), R.string.upload_fail);
                }
            }
        });
    }

    /**
     * ????????????
     */
    private void beginUpload(final String sign, String filePath) {
        mUploadFl.setVisibility(View.VISIBLE);
        mProcessPv.setVisibility(View.VISIBLE);
        if (mVideoPublish == null) {
            mVideoPublish = new TXUGCPublish(this.getApplicationContext(), "carol_android");
            mVideoPublish.setListener(new TXUGCPublishTypeDef.ITXVideoPublishListener() {
                @Override
                public void onPublishProgress(long uploadBytes, long totalBytes) {
                    mProcessPv.setProcess((int) (100 * uploadBytes / totalBytes));
                }

                @Override
                public void onPublishComplete(TXUGCPublishTypeDef.TXPublishResult result) {
                    if (result.retCode == 0) {//????????????
                        mVideoDoneTv.setVisibility(View.VISIBLE);
                        mProcessPv.setVisibility(View.INVISIBLE);
                        LogUtil.i("????????????id: " + result.videoId);
                        LogUtil.i("????????????url: " + result.videoURL);
                        mVideoFileId = result.videoId;
                        mVideoFileUrl = result.videoURL;
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ToastUtil.showToast(getApplicationContext(), R.string.upload_fail);
                            }
                        });
                    }
                }
            });
        }

        TXUGCPublishTypeDef.TXPublishParam param = new TXUGCPublishTypeDef.TXPublishParam();
        // signature????????????????????? https://www.qcloud.com/document/product/266/9221
        param.signature = sign;
        param.videoPath = filePath;
        int publishCode = mVideoPublish.publishVideo(param);
        if (publishCode != 0) {
            LogUtil.i("???????????????????????????" + publishCode);
        }
    }


    //----------------------------????????????-------------------------------------------

    /**
     * ?????????????????????,???????????? ?????????
     * ???????????????????????????,????????????????????????
     */
    private void dealImageFile(String filePath) {
        compressImageWithLuBan(filePath);
    }

    /**
     * ??????LuBan????????????
     */
    private void compressImageWithLuBan(String filePath) {
        ImageHelper.compressImageWithLuBanNotDelete(getApplicationContext(), filePath, Constant.ACTIVE_IMAGE_DIR,
                new OnLuCompressListener() {
                    @Override
                    public void onStart() {
                        showLoadingDialog();
                    }

                    @Override
                    public void onSuccess(File file) {
                        if (!TextUtils.isEmpty(file.getAbsolutePath())) {
                            ActiveLocalBean localBean = new ActiveLocalBean();
                            localBean.type = 0;//??????
                            localBean.localPath = file.getAbsolutePath();

                            if (mLocalBeans != null && mLocalBeans.size() > 0) {
                                int size = mLocalBeans.size();
                                if (size >= 9) {
                                    mLocalBeans.remove(size - 1);//?????????????????????
                                }
                                mLocalBeans.add(size - 1, localBean);
                                mAdapter.loadData(mLocalBeans);
                            }
                        }
                        dismissLoadingDialog();
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtil.showToast(getApplicationContext(), R.string.choose_picture_failed);
                        dismissLoadingDialog();
                    }
                });
    }

    /**
     * ??????????????????
     */
    private void getAnchorVideoCost() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        OkHttpUtils.post().url(ChatApi.GET_PRIVATE_VIDEO_MONEY())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<String>>() {
            @Override
            public void onResponse(BaseResponse<String> response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    String m_object = response.m_object;
                    if (!TextUtils.isEmpty(m_object)) {
                        mVideoStrs = m_object.split(",");
                    }
                }
            }
        });
    }

    /**
     * ??????????????????
     */
    private void getPrivatePhotoMoney() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        OkHttpUtils.post().url(ChatApi.GET_PRIVATE_PHOTO_MONEY())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<String>>() {
            @Override
            public void onResponse(BaseResponse<String> response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    String m_object = response.m_object;
                    if (!TextUtils.isEmpty(m_object)) {
                        mPictureStrs = m_object.split(",");
                    }
                }
            }
        });
    }

    /**
     * ??????????????????dialog
     */
    private void showChargeOptionDialog(int passPosition, int beanPosition) {
        final Dialog mDialog = new Dialog(this, R.style.DialogStyle_Dark_Background);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_charge_layout, null);
        setDialogView(view, mDialog, passPosition, beanPosition);
        mDialog.setContentView(view);
        Point outSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(outSize);
        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = outSize.x;
            window.setGravity(Gravity.BOTTOM); // ??????????????????dialog???????????????
            window.setWindowAnimations(R.style.BottomPopupAnimation); // ????????????
        }
        mDialog.setCanceledOnTouchOutside(false);
        if (!isFinishing()) {
            mDialog.show();
        }
    }

    /**
     * ?????? dialog view
     */
    private void setDialogView(View view, final Dialog mDialog, final int passPosition, final int beanPosition) {
        TextView cancel_tv = view.findViewById(R.id.cancel_tv);
        cancel_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        TextView title_tv = view.findViewById(R.id.title_tv);

        final List<String> beans = new ArrayList<>();
        switch (passPosition) {
            case VIDEO: {
                String content = getResources().getString(R.string.private_video) + getResources().getString(R.string.gold_des);
                title_tv.setText(content);
                beans.addAll(Arrays.asList(mVideoStrs));
                break;
            }
            case PICTURE: {
                String content = getResources().getString(R.string.private_image) + getResources().getString(R.string.gold_des);
                title_tv.setText(content);
                beans.addAll(Arrays.asList(mPictureStrs));
                break;
            }
        }

        SetChargeRecyclerAdapter adapter = new SetChargeRecyclerAdapter(this);
        final RecyclerView content_rv = view.findViewById(R.id.content_rv);
        final PickerLayoutManager pickerLayoutManager = new PickerLayoutManager(getApplicationContext(),
                content_rv, PickerLayoutManager.VERTICAL, false, 5, 0.4f, true);
        content_rv.setLayoutManager(pickerLayoutManager);
        content_rv.setAdapter(adapter);
        adapter.loadData(beans);
        pickerLayoutManager.setOnSelectedViewListener(new PickerLayoutManager.OnSelectedViewListener() {
            @Override
            public void onSelectedView(View view, int position) {
                LogUtil.i("??????: " + position);
                if (position < beans.size()) {
                    mSelectContent = beans.get(position);
                }
            }
        });

        TextView confirm_tv = view.findViewById(R.id.confirm_tv);
        confirm_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (passPosition) {
                    case VIDEO: {
                        if (TextUtils.isEmpty(mSelectContent)) {
                            mSelectContent = mVideoStrs[0];
                        }
                        mVideoChargeTv.setText(mSelectContent);
                        mSelectContent = "";
                        break;
                    }
                    case PICTURE: {//??????
                        if (TextUtils.isEmpty(mSelectContent)) {
                            mSelectContent = mPictureStrs[0];
                        }

                        if (mLocalBeans != null && mLocalBeans.size() > beanPosition + 1) {
                            ActiveLocalBean localBean = mLocalBeans.get(beanPosition);
                            localBean.gold = Integer.parseInt(mSelectContent);
                            mAdapter.loadData(mLocalBeans);
                        }

                        mSelectContent = "";
                        break;
                    }
                }
                mDialog.dismiss();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //??????post??????????????????
        try {
            FileUtil.deleteFiles(Constant.ACTIVE_VIDEO_DIR);
            FileUtil.deleteFiles(Constant.ACTIVE_IMAGE_DIR);
            String videoPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "JCamera";
            FileUtil.deleteFiles(videoPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
