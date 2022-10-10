package com.yiliaodemo.chat.util.upload;


import com.yiliaodemo.chat.base.AppManager;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.yiliaodemo.chat.util.ToastUtil;
import com.yiliaodemo.chat.util.permission.PermissionUtil;

import java.util.List;

/**
 * 文件上传器
 */
public class FileUploadManager {

    private static void upload(final List<UploadTask> uploadTasks, final OnCommonListener<Boolean> onCommonListener) {

        if (uploadTasks != null && uploadTasks.size() > 0) {
            UploadTask task = uploadTasks.get(0);
            OnCommonListener<UploadTask> listener = new OnCommonListener<UploadTask>() {
                @Override
                public void execute(UploadTask task) {
                    if (!task.ok) {
                        ToastUtil.showToast(task.message);
                        if (onCommonListener != null) {
                            onCommonListener.execute(false);
                        }
                    } else {
                        boolean allOk = true;
                        for (UploadTask uploadTask : uploadTasks) {
                            if (!uploadTask.ok) {
                                allOk = false;
                                executeTask(uploadTask, this);
                                break;
                            }
                        }
                        if (allOk) {
                            if (onCommonListener != null)
                                onCommonListener.execute(true);
                        }
                    }
                }
            };
            executeTask(task, listener);
        }
    }

    /**
     * 上传图片/视频文件集合
     */
    public static void permissionUpload(final List<UploadTask> uploadTasks, final OnCommonListener<Boolean> onCommonListener) {
        PermissionUtil.requestPermissions(AppManager.getInstance(), new PermissionUtil.OnPermissionListener() {
            @Override
            public void onPermissionGranted() {
                upload(uploadTasks, onCommonListener);
            }

            @Override
            public void onPermissionDenied() {
                ToastUtil.showToast("没有文件读写权限，无法上传");
            }
        });
    }

    private static void executeTask(UploadTask task, OnCommonListener<UploadTask> listener) {
        if (task.isVideo) {
            new VideoUploader().execute(task, listener);
        } else {
            new PictureUploader().execute(task, listener);
        }
    }
}