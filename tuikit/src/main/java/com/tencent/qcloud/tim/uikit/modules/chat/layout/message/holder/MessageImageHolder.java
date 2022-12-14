package com.tencent.qcloud.tim.uikit.modules.chat.layout.message.holder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.tencent.imsdk.*;
import com.tencent.qcloud.tim.uikit.R;
import com.tencent.qcloud.tim.uikit.TUIKit;
import com.tencent.qcloud.tim.uikit.component.face.FaceManager;
import com.tencent.qcloud.tim.uikit.component.photoview.PhotoViewActivity;
import com.tencent.qcloud.tim.uikit.component.picture.imageEngine.impl.GlideEngine;
import com.tencent.qcloud.tim.uikit.component.video.VideoViewActivity;
import com.tencent.qcloud.tim.uikit.modules.message.MessageInfo;
import com.tencent.qcloud.tim.uikit.utils.TUIKitConstants;
import com.tencent.qcloud.tim.uikit.utils.TUIKitLog;
import com.tencent.qcloud.tim.uikit.utils.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MessageImageHolder extends MessageContentHolder {

    private static final int DEFAULT_MAX_SIZE = 540;
    private static final int DEFAULT_RADIUS = 10;
    private final List<String> downloadEles = new ArrayList<>();
    private ImageView contentImage;
    private ImageView videoPlayBtn;
    private TextView videoDurationText;
    private boolean mClicking;

    public MessageImageHolder(View itemView) {
        super(itemView);
    }

    @Override
    public int getVariableLayout() {
        return R.layout.message_adapter_content_image;
    }

    @Override
    public void initVariableViews() {
        contentImage = rootView.findViewById(R.id.content_image_iv);
        videoPlayBtn = rootView.findViewById(R.id.video_play_btn);
        videoDurationText = rootView.findViewById(R.id.video_duration_tv);
    }

    @Override
    public void layoutVariableViews(MessageInfo msg, int position) {
        msgContentFrame.setBackground(null);
        if (msg.isSelf()) {
            contentImage.setScaleType(ImageView.ScaleType.FIT_END);
        } else {
            contentImage.setScaleType(ImageView.ScaleType.FIT_START);
        }
        switch (msg.getMsgType()) {
            case MessageInfo.MSG_TYPE_CUSTOM_FACE:
            case MessageInfo.MSG_TYPE_CUSTOM_FACE + 1:
                performCustomFace(msg, position);
                break;
            case MessageInfo.MSG_TYPE_IMAGE:
            case MessageInfo.MSG_TYPE_IMAGE + 1:
                performImage(msg, position);
                break;
            case MessageInfo.MSG_TYPE_VIDEO:
            case MessageInfo.MSG_TYPE_VIDEO + 1:
                performVideo(msg, position);
                break;
        }
    }

    private void performCustomFace(final MessageInfo msg, final int position) {
        videoPlayBtn.setVisibility(View.GONE);
        videoDurationText.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        contentImage.setLayoutParams(params);
        TIMElem elem = msg.getElement();
        if (!(elem instanceof TIMFaceElem)) {
            return;
        }
        TIMFaceElem faceEle = (TIMFaceElem) elem;
        String filter = new String(faceEle.getData());
        if (!filter.contains("@2x")) {
            filter += "@2x";
        }
        Bitmap bitmap = FaceManager.getCustomBitmap(faceEle.getIndex(), filter);
        if (bitmap == null) {
            // ?????????????????????????????????emoji????????????
            bitmap = FaceManager.getEmoji(new String(faceEle.getData()));
            if (bitmap == null) {
                // TODO ???????????????????????????????????????????????????????????????
                contentImage.setImageDrawable(rootView.getContext().getResources().getDrawable(R.drawable.ic_input_face_pressed));
            } else {
                contentImage.setImageBitmap(bitmap);
            }
        } else {
            contentImage.setImageBitmap(bitmap);
        }
    }

    private ViewGroup.LayoutParams getImageParams(ViewGroup.LayoutParams params, final MessageInfo msg) {
        if (msg.getImgWidth() == 0 || msg.getImgHeight() == 0) {
            return params;
        }
        if (msg.getImgWidth() > msg.getImgHeight()) {
            params.width = DEFAULT_MAX_SIZE;
            params.height = DEFAULT_MAX_SIZE * msg.getImgHeight() / msg.getImgWidth();
        } else {
            params.width = DEFAULT_MAX_SIZE * msg.getImgWidth() / msg.getImgHeight();
            params.height = DEFAULT_MAX_SIZE;
        }
        return params;
    }

    private void resetParentLayout() {
        ((FrameLayout) contentImage.getParent().getParent()).setPadding(17, 0, 13, 0);
    }

    private void performImage(final MessageInfo msg, final int position) {
        contentImage.setLayoutParams(getImageParams(contentImage.getLayoutParams(), msg));
        resetParentLayout();
        videoPlayBtn.setVisibility(View.GONE);
        videoDurationText.setVisibility(View.GONE);
        TIMElem elem = msg.getElement();
        if (!(elem instanceof TIMImageElem)) {
            performCustomImage(msg, position);
            return;
        }
        final TIMImageElem imageEle = (TIMImageElem) elem;
        final List<TIMImage> imgs = imageEle.getImageList();
        if (!TextUtils.isEmpty(msg.getDataPath())) {
            GlideEngine.loadCornerImage(contentImage, msg.getDataPath(), null, DEFAULT_RADIUS);
        } else {
            for (int i = 0; i < imgs.size(); i++) {
                final TIMImage img = imgs.get(i);
                if (img.getType() == TIMImageType.Thumb) {
                    synchronized (downloadEles) {
                        if (downloadEles.contains(img.getUuid())) {
                            break;
                        }
                        downloadEles.add(img.getUuid());
                    }
                    final String path = TUIKitConstants.IMAGE_DOWNLOAD_DIR + img.getUuid();
                    img.getImage(path, new TIMCallBack() {
                        @Override
                        public void onError(int code, String desc) {
                            downloadEles.remove(img.getUuid());
                            TUIKitLog.e("MessageListAdapter img getImage", code + ":" + desc);
                        }

                        @Override
                        public void onSuccess() {
                            downloadEles.remove(img.getUuid());
                            msg.setDataPath(path);
                            GlideEngine.loadCornerImage(contentImage, msg.getDataPath(), null, DEFAULT_RADIUS);
                        }
                    });
                    break;
                }
            }
        }
        contentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < imgs.size(); i++) {
                    TIMImage img = imgs.get(i);
                    if (img.getType() == TIMImageType.Original) {
                        PhotoViewActivity.mCurrentOriginalImage = img;
                        break;
                    }
                }
                Intent intent = new Intent(TUIKit.getAppContext(), PhotoViewActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(TUIKitConstants.IMAGE_DATA, msg.getDataPath());
                intent.putExtra(TUIKitConstants.SELF_MESSAGE, msg.isSelf());
                TUIKit.getAppContext().startActivity(intent);
            }
        });
        contentImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onMessageLongClick(view, position, msg);
                }
                return true;
            }
        });
    }

    /**
     * ???????????????????????????
     *
     * @param msg
     * @param position
     */
    private void performCustomImage(final MessageInfo msg, final int position) {
        TIMElem elem = msg.getElement();
        if (!(elem instanceof TIMCustomElem)) {
            return;
        }
        if (!TextUtils.isEmpty(msg.getDataPath())) {
            GlideEngine.loadCornerImage(contentImage, msg.getDataPath(), null, DEFAULT_RADIUS);
        }
        contentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GlideEngine.loadImage((String) null, msg.getDataPath(), new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        PhotoViewActivity.mCurrentOriginalImage = null;
                        Intent intent = new Intent(TUIKit.getAppContext(), PhotoViewActivity.class);
                        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(TUIKitConstants.IMAGE_DATA, resource.getAbsolutePath());
                        intent.putExtra(TUIKitConstants.SELF_MESSAGE, msg.isSelf());
                        TUIKit.getAppContext().startActivity(intent);
                        return false;
                    }
                });
            }
        });
        contentImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onMessageLongClick(view, position, msg);
                }
                return true;
            }
        });
    }

    private void performVideo(final MessageInfo msg, final int position) {
        contentImage.setLayoutParams(getImageParams(contentImage.getLayoutParams(), msg));
        resetParentLayout();

        videoPlayBtn.setVisibility(View.VISIBLE);
        videoDurationText.setVisibility(View.VISIBLE);
        TIMElem elem = msg.getElement();
        if (!(elem instanceof TIMVideoElem)) {
            return;
        }
        final TIMVideoElem videoEle = (TIMVideoElem) elem;
        final TIMVideo video = videoEle.getVideoInfo();

        if (!TextUtils.isEmpty(msg.getDataPath())) {
            GlideEngine.loadCornerImage(contentImage, msg.getDataPath(), null, DEFAULT_RADIUS);
        } else {
            final TIMSnapshot shotInfo = videoEle.getSnapshotInfo();
            synchronized (downloadEles) {
                if (!downloadEles.contains(shotInfo.getUuid())) {
                    downloadEles.add(shotInfo.getUuid());
                }
            }

            final String path = TUIKitConstants.IMAGE_DOWNLOAD_DIR + videoEle.getSnapshotInfo().getUuid();
            videoEle.getSnapshotInfo().getImage(path, new TIMCallBack() {
                @Override
                public void onError(int code, String desc) {
                    downloadEles.remove(shotInfo.getUuid());
                    TUIKitLog.e("MessageListAdapter video getImage", code + ":" + desc);
                }

                @Override
                public void onSuccess() {
                    downloadEles.remove(shotInfo.getUuid());
                    msg.setDataPath(path);
                    GlideEngine.loadCornerImage(contentImage, msg.getDataPath(), null, DEFAULT_RADIUS);
                }
            });
        }

        String durations = "00:" + video.getDuaration();
        if (video.getDuaration() < 10) {
            durations = "00:0" + video.getDuaration();
        }
        videoDurationText.setText(durations);

        final String videoPath = TUIKitConstants.VIDEO_DOWNLOAD_DIR + video.getUuid();
        final File videoFile = new File(videoPath);
        //???????????????zanhanding???????????????fix????????????????????????????????????????????????????????????
        if (msg.getStatus() == MessageInfo.MSG_STATUS_SEND_SUCCESS) {
            //???????????????????????????????????????????????????????????????
            statusImage.setVisibility(View.GONE);
            sendingProgress.setVisibility(View.GONE);
        } else if (videoFile.exists() && msg.getStatus() == MessageInfo.MSG_STATUS_SENDING) {
            //?????????????????????????????????????????????????????????????????????????????????????????????????????????
            statusImage.setVisibility(View.GONE);
            sendingProgress.setVisibility(View.VISIBLE);
        } else if (msg.getStatus() == MessageInfo.MSG_STATUS_SEND_FAIL) {
            //????????????????????????????????????????????????????????????????????????
            statusImage.setVisibility(View.VISIBLE);
            sendingProgress.setVisibility(View.GONE);

        }
        //???????????????zanhanding???????????????fix????????????????????????????????????????????????????????????
        msgContentFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClicking) {
                    return;
                }
                sendingProgress.setVisibility(View.VISIBLE);
                mClicking = true;
                //???????????????zanhanding???????????????fix??????????????????????????????????????????????????????????????????????????????
                final File videoFile = new File(videoPath);
                if (videoFile.exists()) {//????????????????????????????????????????????????
                    mAdapter.notifyItemChanged(position);
                    mClicking = false;
                    play(msg);
                    // ??????????????????Activity??????????????????????????????200ms????????????????????????????????????
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mClicking = false;
                        }
                    }, 200);
                } else {
                    getVideo(video, videoPath, msg, true, position);
                }
                //???????????????zanhanding???????????????fix??????????????????????????????????????????????????????????????????????????????
            }
        });
    }

    private void getVideo(TIMVideo video, String videoPath, final MessageInfo msg, final boolean autoPlay, final int position) {
        video.getVideo(videoPath, new TIMCallBack() {
            @Override
            public void onError(int code, String desc) {
                ToastUtil.toastLongMessage("??????????????????:" + code + "=" + desc);
                msg.setStatus(MessageInfo.MSG_STATUS_DOWNLOADED);
                sendingProgress.setVisibility(View.GONE);
                statusImage.setVisibility(View.VISIBLE);
                mAdapter.notifyItemChanged(position);
                mClicking = false;
            }

            @Override
            public void onSuccess() {
                mAdapter.notifyItemChanged(position);
                if (autoPlay) {
                    play(msg);
                }
                // ??????????????????Activity??????????????????????????????200ms????????????????????????????????????
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mClicking = false;
                    }
                }, 200);
            }
        });
    }

    private void play(final MessageInfo msg) {
        statusImage.setVisibility(View.GONE);
        sendingProgress.setVisibility(View.GONE);
        Intent intent = new Intent(TUIKit.getAppContext(), VideoViewActivity.class);
        intent.putExtra(TUIKitConstants.CAMERA_IMAGE_PATH, msg.getDataPath());
        intent.putExtra(TUIKitConstants.CAMERA_VIDEO_PATH, msg.getDataUri());
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        TUIKit.getAppContext().startActivity(intent);
    }

}
