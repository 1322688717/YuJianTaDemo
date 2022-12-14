package com.yiliaodemo.chat.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.adapter.CoverRecyclerAdapter;
import com.yiliaodemo.chat.adapter.SetChargeRecyclerAdapter;
import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.base.BaseResponse;
import com.yiliaodemo.chat.bean.CoverUrlBean;
import com.yiliaodemo.chat.bean.LabelBean;
import com.yiliaodemo.chat.bean.PersonBean;
import com.yiliaodemo.chat.constant.ChatApi;
import com.yiliaodemo.chat.constant.Constant;
import com.yiliaodemo.chat.dialog.CityPickerDialog;
import com.yiliaodemo.chat.fragment.HomeCityFragment;
import com.yiliaodemo.chat.helper.ImageHelper;
import com.yiliaodemo.chat.helper.LocationHelper;
import com.yiliaodemo.chat.layoutmanager.PickerLayoutManager;
import com.yiliaodemo.chat.net.AjaxCallback;
import com.yiliaodemo.chat.net.NetCode;
import com.yiliaodemo.chat.oss.QServiceCfg;
import com.yiliaodemo.chat.util.DevicesUtil;
import com.yiliaodemo.chat.util.FileUtil;
import com.yiliaodemo.chat.util.LogUtil;
import com.yiliaodemo.chat.util.ParamUtil;
import com.yiliaodemo.chat.util.ToastUtil;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.yalantis.ucrop.UCrop;
import com.zhihu.matisse.Matisse;
import com.zhy.http.okhttp.OkHttpUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Request;

/*
 * Copyright (C) 2018
 * ????????????
 *
 * ???????????????????????????????????????
 * ?????????
 * ???????????????2018/6/14
 *
 * ????????????
 * ???????????????
 * ????????????
 */
public class ModifyUserInfoActivity extends BaseActivity {

    @BindView(R.id.nick_tv)
    TextView mNickTv;

    @BindView(R.id.job_tv)
    TextView mJobTv;

    @BindView(R.id.high_tv)
    TextView mHighTv;

    @BindView(R.id.age_tv)
    TextView mAgeTv;

    @BindView(R.id.body_tv)
    TextView mBodyTv;

    @BindView(R.id.marriage_tv)
    TextView mMarriageTv;

    @BindView(R.id.city_tv)
    TextView mCityTv;

    @BindView(R.id.sign_tv)
    EditText mSignEt;

    @BindView(R.id.submit_tv)
    TextView mSubmitTv;

    @BindView(R.id.evidence_rv)
    RecyclerView mEvidenceRv;

    @BindView(R.id.upload_iv)
    ImageView mUploadIv;

    @BindView(R.id.scrollView)
    LinearLayout mScrollView;

    //?????????
    private QServiceCfg mQServiceCfg;

    //????????????
    private List<CoverUrlBean> mCoverUrlBeans = new ArrayList<>();
    private CoverRecyclerAdapter mCoverAdapter;

    //option?????????
    private String mOptionSelectStr = "";

    //????????????
    private final int SET_NICK = 0x09;

    private PersonBean<LabelBean, CoverUrlBean> personBean;

    private boolean isVerify;

    public static void verifyStart(Context context) {
        Intent starter = new Intent(context, ModifyUserInfoActivity.class);
        starter.putExtra("verify", true);
        context.startActivity(starter);
    }

    @Override
    protected View getContentView() {
        return inflate(R.layout.activity_modify_user_info_layout);
    }

    @Override
    protected void onContentAdded() {
        setTitle(R.string.edit_verify_info);
        isVerify = getIntent().getBooleanExtra("verify", false);
        mQServiceCfg = QServiceCfg.instance(ModifyUserInfoActivity.this);
        setListener();
        controlKeyboardLayout();
        getUserInfo();
    }

    private void setListener() {
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mCoverAdapter = new CoverRecyclerAdapter(this) {
            @Override
            protected void deleted() {
                mUploadIv.setVisibility(View.VISIBLE);
            }
        };
        mEvidenceRv.setLayoutManager(layoutManager);
        mEvidenceRv.setAdapter(mCoverAdapter);

        int size = (DevicesUtil.getScreenW(mContext) - DevicesUtil.dp2px(mContext, 40)) / 4;
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(size, size);
        params1.leftMargin = DevicesUtil.dp2px(mContext, 5);
        params1.rightMargin = DevicesUtil.dp2px(mContext, 5);
        mUploadIv.setLayoutParams(params1);
    }

    @OnClick({
            R.id.submit_tv,
            R.id.upload_iv,
            R.id.job_ll,
            R.id.age_rl,
            R.id.high_rl,
            R.id.body_rl,
            R.id.marriage_rl,
            R.id.city_rl,
            R.id.nick_rl,
    })
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.submit_tv: {

                //????????????
                if (!submitCheckInput()) {
                    return;
                }

                //??????????????????
                showLoadingDialog();
                uploadInfo();
                break;
            }

            case R.id.job_ll: {//??????
                showOptionDialog(JOB);
                break;
            }

            case R.id.age_rl: {//??????
                showOptionDialog(AGE);
                break;
            }

            case R.id.high_rl: {//??????
                showOptionDialog(HIGH);
                break;
            }

            case R.id.body_rl: {//??????
                showOptionDialog(BODY);
                break;
            }

            case R.id.marriage_rl: {//????????????
                showOptionDialog(MARRIAGE);
                break;
            }

            case R.id.city_rl: {//??????
                new CityPickerDialog(mContext) {
                    @Override
                    public void onSelected(String city, String city2) {
                        mCityTv.setText(city2);
                        checkInput();
                    }
                }.show();
                break;
            }

            case R.id.upload_iv: {//????????????

                if (personBean == null) {
                    getUserInfo();
                    ToastUtil.showToast("???????????????");
                    return;
                }

                //??????????????????6???
                if (mEvidenceRv.getChildCount() >= 6 || mCoverUrlBeans.size() >= 6) {
                    ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.four_most);
                    return;
                }

                //????????????
                ImageHelper.openPictureChoosePage(ModifyUserInfoActivity.this, Constant.REQUEST_CODE_CHOOSE);
                break;
            }

            case R.id.nick_rl: {//??????
                String content = mNickTv.getText().toString().trim();
                Intent intent = new Intent(ModifyUserInfoActivity.this, ModifyTwoActivity.class);
                intent.putExtra(Constant.MODIFY_TWO, 0);
                intent.putExtra(Constant.MODIFY_CONTENT, content);
                startActivityForResult(intent, SET_NICK);
                break;
            }

        }
    }

    /**
     * ??????????????????dialog
     */
    private final int JOB = 0;
    private final int AGE = 1;
    private final int MARRIAGE = 2;
    private final int HIGH = 3;
    private final int BODY = 4;

    private void showOptionDialog(int index) {
        final Dialog mDialog = new Dialog(this, R.style.DialogStyle_Dark_Background);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_charge_layout, null);
        setDialogView(view, mDialog, index);
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
    private void setDialogView(View view, final Dialog mDialog, final int index) {
        TextView cancel_tv = view.findViewById(R.id.cancel_tv);
        cancel_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        TextView title_tv = view.findViewById(R.id.title_tv);

        final List<String> beans = new ArrayList<>();
        switch (index) {
            case JOB: {
                title_tv.setText(R.string.job);
                beans.add("??????");
                beans.add("??????");
                beans.add("??????");
                beans.add("??????");
                beans.add("??????");
                beans.add("??????");
                beans.add("????????????");
                beans.add("??????");
                beans.add("??????");
                beans.add("??????");
                break;
            }
            case AGE: {
                title_tv.setText(R.string.age_title);
                for (int i = 18; i < 100; i++) {
                    beans.add(String.valueOf(i));
                }
                break;
            }
            case MARRIAGE: {
                title_tv.setText(R.string.marriage);
                beans.add("??????");
                beans.add("??????");
                beans.add("??????");
                break;
            }
            case HIGH: {
                title_tv.setText(R.string.high_title_des);
                for (int i = 160; i < 200; i++) {
                    beans.add(String.valueOf(i));
                }
                break;
            }
            case BODY: {
                title_tv.setText(R.string.body_title_des);
                for (int i = 30; i < 81; i++) {
                    beans.add(String.valueOf(i));
                }
                break;
            }
        }

        SetChargeRecyclerAdapter adapter = new SetChargeRecyclerAdapter(this);
        RecyclerView content_rv = view.findViewById(R.id.content_rv);
        PickerLayoutManager pickerLayoutManager = new PickerLayoutManager(ModifyUserInfoActivity.this,
                content_rv, PickerLayoutManager.VERTICAL, false, 5, 0.3f, true);
        content_rv.setLayoutManager(pickerLayoutManager);
        content_rv.setAdapter(adapter);
        adapter.loadData(beans);
        pickerLayoutManager.setOnSelectedViewListener(new PickerLayoutManager.OnSelectedViewListener() {
            @Override
            public void onSelectedView(View view, int position) {
                mOptionSelectStr = beans.get(position);
                LogUtil.i("??????: " + position + " " + mOptionSelectStr);
            }
        });
        mOptionSelectStr = beans.get(0);
        //??????
        TextView confirm_tv = view.findViewById(R.id.confirm_tv);
        confirm_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (index) {
                    case JOB: {
                        mJobTv.setText(mOptionSelectStr);
                        break;
                    }
                    case AGE: {
                        mAgeTv.setText(mOptionSelectStr);
                        break;
                    }
                    case MARRIAGE: {
                        mMarriageTv.setText(mOptionSelectStr);
                        break;
                    }
                    case HIGH: {
                        mHighTv.setText(mOptionSelectStr);
                        break;
                    }
                    case BODY: {
                        mBodyTv.setText(mOptionSelectStr);
                        break;
                    }
                }
                mDialog.dismiss();
                checkInput();
            }
        });
    }

    /**
     * ??????????????????
     */
    private void getUserInfo() {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        OkHttpUtils.post().url(ChatApi.GET_PERSONAL_DATA())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<PersonBean<LabelBean, CoverUrlBean>>>() {
            @Override
            public void onResponse(BaseResponse<PersonBean<LabelBean, CoverUrlBean>> response, int id) {

                if (isFinishing()) {
                    return;
                }

                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    personBean = response.m_object;

                    if (personBean != null) {

                        //??????
                        mNickTv.setText(personBean.t_nickName);

                        //??????
                        String job = personBean.t_vocation;
                        if (!TextUtils.isEmpty(job)) {
                            mJobTv.setText(job);
                        }

                        //??????
                        if (personBean.t_height > 0) {
                            mHighTv.setText(String.valueOf(personBean.t_height));
                        }

                        //??????
                        if (personBean.t_age > 0) {
                            mAgeTv.setText(String.valueOf(personBean.t_age));
                        }

                        //??????
                        if (personBean.t_weight > 0) {
                            mBodyTv.setText(String.valueOf(personBean.t_weight));
                        }

                        //????????????
                        if (!TextUtils.isEmpty(personBean.t_marriage)) {
                            mMarriageTv.setText(personBean.t_marriage);
                        }

                        //??????
                        if (!TextUtils.isEmpty(personBean.t_city)) {
                            if (LocationHelper.get().getLocation() != null) {
                                mCityTv.setText(LocationHelper.get().getLocation().getCity());
                            } else {
                                mCityTv.setText(personBean.t_city);
                            }
                        }

                        //????????????
                        if (!TextUtils.isEmpty(personBean.t_autograph)) {
                            mSignEt.setText(personBean.t_autograph);
                        }

                        //?????????
                        if (personBean.coverList != null && personBean.coverList.size() > 0) {
                            mCoverUrlBeans = personBean.coverList;
                            setThumbImage(mCoverUrlBeans);
                        }

                        //??????????????????
                        checkInput();
                    }
                }
            }

            @Override
            public void onBefore(Request request, int id) {
                super.onBefore(request, id);
                showLoadingDialog();
            }

            @Override
            public void onAfter(int id) {
                super.onAfter(id);
                dismissLoadingDialog();
            }

        });
    }

    /**
     * ????????????
     */
    private void uploadInfo() {

        //??????
        String nick = mNickTv.getText().toString().trim();

        //??????
        String job = mJobTv.getText().toString().trim();

        //??????
        String high = mHighTv.getText().toString().trim();

        //??????
        String age = mAgeTv.getText().toString().trim();

        //??????
        String body = mBodyTv.getText().toString().trim();

        //????????????
        String marriage = mMarriageTv.getText().toString().trim();

        //??????
        String city = mCityTv.getText().toString().trim();

        //????????????
        String sign = mSignEt.getText().toString().trim();
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("t_nickName", nick);
//        paramMap.put("t_phone", phone);
        paramMap.put("t_height", high);
        paramMap.put("t_weight", body);
        paramMap.put("t_marriage", marriage);
        paramMap.put("t_city", city);
        paramMap.put("t_autograph", sign);
        paramMap.put("t_vocation", job);
        paramMap.put("t_age", age);
//        paramMap.put("t_weixin", weChat);
//        paramMap.put("t_qq", qqChat);
//        paramMap.put("t_handImg", TextUtils.isEmpty(mHeadImageHttpUrl) ? "" : mHeadImageHttpUrl);
//        paramMap.put("lables", labels);
        OkHttpUtils.post().url(ChatApi.UPDATE_PERSON_DATA())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse>() {
            @Override
            public void onResponse(BaseResponse response, int id) {
                dismissLoadingDialog();
                if (response != null) {
                    String message = response.m_strMessage;
                    if (response.m_istatus == NetCode.SUCCESS) {
                        if (!TextUtils.isEmpty(message)) {
                            ToastUtil.showToast(ModifyUserInfoActivity.this, message);
                        }
                        HomeCityFragment.city = city;
                        finish();
                    } else {
                        if (!TextUtils.isEmpty(message)) {
                            ToastUtil.showToast(ModifyUserInfoActivity.this, message);
                        } else {
                            ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.edit_fail);
                        }
                    }
                } else {
                    ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.edit_fail);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                dismissLoadingDialog();
                ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.edit_fail);
            }
        });
    }

    private final List<Integer> labelBg = new ArrayList<>();

    /**
     * ??????????????????????????????
     */
    private boolean submitCheckInput() {
        if (personBean == null) {
            ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.data_getting);
            getUserInfo();
            return false;
        }
        return checkInput(true);
    }

    private boolean checkInput() {
        return checkInput(false);
    }

    /**
     * ?????????????????????
     */
    private boolean checkInput(boolean toast) {

        //??????
        String nick = mNickTv.getText().toString().trim();
        if (TextUtils.isEmpty(nick)) {
            if (toast)
                ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.please_input_nick);
            return false;
        }

        //??????
        String city = mCityTv.getText().toString().trim();
        if (TextUtils.isEmpty(city)) {
            if (toast)
                ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.please_input_city);
            return false;
        }

        //???????????????????????????????????????????????????
        if (AppManager.getInstance().getUserInfo().t_role == 1 || isVerify) {

            //??????
            if (mCoverUrlBeans == null || mCoverUrlBeans.size() == 0) {
                if (toast)
                    ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.actor_at_least_upload_one);
                return false;
            }
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == Constant.REQUEST_CODE_CHOOSE || requestCode == Constant.REQUEST_CODE_CHOOSE_HEAD_IMG)
                && resultCode == RESULT_OK) {//????????????
            List<String> pathResult = Matisse.obtainPathResult(data);
            if (pathResult != null && pathResult.size() > 0) {
                try {
                    String filePath = pathResult.get(0);
                    if (!TextUtils.isEmpty(filePath)) {
                        File file = new File(filePath);
                        if (!file.exists()) {
                            LogUtil.i("??????????????? ");
                        } else {
                            LogUtil.i("????????????: " + file.length() / 1024);
                        }
                        //????????????
                        if (requestCode == Constant.REQUEST_CODE_CHOOSE) {
                            cutWithUCrop(filePath, true);
                        } else {
                            cutWithUCrop(filePath, false);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (resultCode == RESULT_OK && (requestCode == Constant.UCROP_REQUEST_CODE_COVER || requestCode == Constant.UCROP_REQUEST_CODE_HEAD)) {
            Uri resultUri = UCrop.getOutput(data);
            String filePath = FileUtil.getPathAbove19(this, resultUri);
            if (requestCode == Constant.UCROP_REQUEST_CODE_COVER) {
                //???????????????????????????
                uploadCoverFileWithQQ(filePath);
            }
        } else if (requestCode == SET_NICK && resultCode == RESULT_OK) {//????????????
            if (data != null) {
                String phone = data.getStringExtra(Constant.MODIFY_CONTENT);
                if (!TextUtils.isEmpty(phone)) {
                    mNickTv.setText(phone);
                }
            }
        }
        checkInput();
    }

    /**
     * ??????u crop??????
     */
    private void cutWithUCrop(String sourceFilePath, boolean fromCover) {
        //?????? ??????resize?????????
        int overWidth;
        int overHeight;
        if (fromCover) {
            overWidth = DevicesUtil.getScreenW(mContext);
            overHeight = DevicesUtil.dp2px(mContext, 435);
        } else {
            overWidth = DevicesUtil.getScreenW(ModifyUserInfoActivity.this);
            overHeight = DevicesUtil.getScreenW(ModifyUserInfoActivity.this);
        }
        //????????????
        String dirPath;
        if (fromCover) {
            dirPath = Constant.COVER_AFTER_SHEAR_DIR;
        } else {
            dirPath = Constant.HEAD_AFTER_SHEAR_DIR;
        }
        File pFile = new File(FileUtil.YCHAT_DIR);
        if (!pFile.exists()) {
            boolean res = pFile.mkdir();
            LogUtil.i("res: " + res);
        }
        File file = new File(dirPath);
        if (!file.exists()) {
            boolean res = file.mkdir();
            LogUtil.i("res: " + res);
        }
        if (!fromCover) {
            FileUtil.deleteFiles(dirPath);
        }
        //?????????
        String filePath = file.getPath() + File.separator + System.currentTimeMillis() + ".png";
        //?????????
        int requestCode;
        if (fromCover) {
            requestCode = Constant.UCROP_REQUEST_CODE_COVER;
        } else {
            requestCode = Constant.UCROP_REQUEST_CODE_HEAD;
        }
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.file_not_exist);
            return;
        }
        UCrop.of(Uri.fromFile(new File(sourceFilePath)), Uri.fromFile(new File(filePath)))
                .withAspectRatio(overWidth, overHeight)
                .withMaxResultSize(overWidth, overHeight)
                .start(this, requestCode);
    }

    /**
     * ?????????????????????????????????
     */
    private void uploadCoverFileWithQQ(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.file_invalidate);
            return;
        }
        //?????????
        String fileName;
        if (filePath.length() < 50) {
            fileName = filePath.substring(filePath.length() - 17);
        } else {
            String last = filePath.substring(filePath.length() - 4);
            if (last.contains("png")) {
                fileName = System.currentTimeMillis() + ".png";
            } else {
                fileName = System.currentTimeMillis() + ".jpg";
            }
        }

        String cosPath = "/cover/" + fileName;
        long signDuration = 600; //?????????????????????????????????
        PutObjectRequest putObjectRequest = new PutObjectRequest(Constant.TENCENT_CLOUD_BUCKET, cosPath, filePath);
        putObjectRequest.setSign(signDuration, null, null);
        mQServiceCfg.getCosCxmService().putObjectAsync(putObjectRequest, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                LogUtil.i("?????????success =  " + result.accessUrl);
                String resultUrl = result.accessUrl;
                if (!resultUrl.contains("http") || !resultUrl.contains("https")) {
                    resultUrl = "https://" + resultUrl;
                }
                //????????????????????????
                //????????????
                uploadToSelfServer(resultUrl);
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, CosXmlClientException clientException, CosXmlServiceException serviceException) {
                String errorMsg = clientException != null ? clientException.toString() : serviceException.toString();
                LogUtil.i("?????????fail: " + errorMsg);
                ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.upload_fail);
            }
        });
    }

    /**
     * ??????????????????????????????
     */
    private void uploadToSelfServer(final String imgUrl) {
        String t_first = "1";//???????????????
        if (mCoverUrlBeans != null && mCoverUrlBeans.size() == 0) {
            t_first = "0";//?????????
        }
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("userId", getUserId());
        paramMap.put("coverImg", imgUrl);
        paramMap.put("t_first", t_first);//0 ????????????  1 ?????????
        OkHttpUtils.post().url(ChatApi.REPLACE_COVER_IMG())
                .addParams("param", ParamUtil.getParam(paramMap))
                .build().execute(new AjaxCallback<BaseResponse<Integer>>() {
            @Override
            public void onResponse(BaseResponse<Integer> response, int id) {
                if (response != null && response.m_istatus == NetCode.SUCCESS) {
                    Integer t_id = response.m_object;
                    CoverUrlBean bean = new CoverUrlBean();
                    bean.t_img_url = imgUrl;
                    bean.t_id = t_id;
                    mCoverUrlBeans.add(bean);
                    setThumbImage(mCoverUrlBeans);
                } else {
                    ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.upload_fail);
                }
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                super.onError(call, e, id);
                ToastUtil.showToast(ModifyUserInfoActivity.this, R.string.upload_fail);
            }
        });
    }

    /**
     * ????????????????????????
     */
    private void setThumbImage(List<CoverUrlBean> coverUrlBeans) {
        if (coverUrlBeans != null && coverUrlBeans.size() >= 6) {
            mUploadIv.setVisibility(View.GONE);
        }
        mCoverAdapter.loadData(coverUrlBeans);
        mEvidenceRv.setVisibility(View.VISIBLE);
    }

    /**
     * ??????
     */
    private void controlKeyboardLayout() {
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                mScrollView.getWindowVisibleDisplayFrame(r);
                //r.top ??????????????????
                int screenHeight = mScrollView.getRootView().getHeight();
                int softHeight = screenHeight - r.bottom;
                if (softHeight > 200) {//????????????????????????100???????????????????????????
                    mScrollView.scrollTo(0, 150);
                } else {//?????????????????????????????????
                    mScrollView.scrollTo(0, 0);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //??????cover??????????????????
        FileUtil.deleteFiles(Constant.COVER_AFTER_SHEAR_DIR);
        FileUtil.deleteFiles(Constant.HEAD_AFTER_SHEAR_DIR);

    }
}