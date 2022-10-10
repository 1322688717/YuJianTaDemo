package com.yiliaodemo.chat.im;

import android.app.Fragment;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;

import com.yiliaodemo.chat.R;
import com.yiliaodemo.chat.base.BaseActivity;
import com.yiliaodemo.chat.helper.IMHelper;
import com.yiliaodemo.chat.listener.OnCommonListener;
import com.tencent.qcloud.tim.uikit.modules.chat.C2CChatManagerKit;
import com.tencent.qcloud.tim.uikit.modules.chat.base.ChatInfo;

/**
 * Im聊天室
 */
public class ChatActivity extends BaseActivity {

    private Fragment mChatFragment;

    private boolean isOnStart;

    @Override
    protected void onNewIntent(Intent intent) {
        if (!isOnStart)
            return;
        super.onNewIntent(intent);
        setIntent(intent);
        toChat();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isOnStart = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        C2CChatManagerKit.getInstance().setChatting(false);
    }

    @Override
    protected View getContentView() {
        return inflate(R.layout.chat_activity);
    }

    @Override
    protected void onContentAdded() {
        needHeader(false);
        toChat();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (mChatFragment != null) {
            mChatFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void toChat() {
        ChatInfo mChatInfo = (ChatInfo) getIntent().getSerializableExtra(ImConstants.CHAT_INFO);
        int otherId = Integer.parseInt(mChatInfo.getId()) - 10000;
        IMHelper.checkServeIm(otherId, new OnCommonListener<Boolean>() {
            @Override
            public void execute(Boolean aBoolean) {
                if (isFinishing()) {
                    return;
                }
                C2CChatManagerKit.getInstance().setChatting(true);
                if (aBoolean) {
                    mChatFragment = new ChatServeFragment();
                } else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    mChatFragment = new ChatFragment();
                }
                getFragmentManager().beginTransaction().replace(R.id.empty_view, mChatFragment).commitAllowingStateLoss();
            }
        });
    }
}