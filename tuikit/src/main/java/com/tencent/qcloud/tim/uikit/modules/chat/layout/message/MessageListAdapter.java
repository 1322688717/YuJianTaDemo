package com.tencent.qcloud.tim.uikit.modules.chat.layout.message;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.tencent.qcloud.tim.uikit.modules.chat.interfaces.IChatProvider;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.holder.IOnCustomMessageDrawListener;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.holder.MessageBaseHolder;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.holder.MessageContentHolder;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.holder.MessageCustomHolder;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.holder.MessageEmptyHolder;
import com.tencent.qcloud.tim.uikit.modules.chat.layout.message.holder.MessageHeaderHolder;
import com.tencent.qcloud.tim.uikit.modules.message.MessageInfo;
import com.tencent.qcloud.tim.uikit.utils.BackgroundTasks;

import java.util.ArrayList;
import java.util.List;


public class MessageListAdapter extends RecyclerView.Adapter {

    public static final int MSG_TYPE_HEADER_VIEW = -99;
    private static final String TAG = MessageListAdapter.class.getSimpleName();
    private boolean mLoading = true;
    private MessageLayout mRecycleView;
    private List<MessageInfo> mDataSource = new ArrayList<>();
    private MessageLayout.OnItemClickListener mOnItemClickListener;
    private IOnCustomMessageDrawListener mOnCustomMessageDrawListener;

    public void setOnCustomMessageDrawListener(IOnCustomMessageDrawListener listener) {
        mOnCustomMessageDrawListener = listener;
    }

    public MessageLayout.OnItemClickListener getOnItemClickListener() {
        return this.mOnItemClickListener;
    }

    public void setOnItemClickListener(MessageLayout.OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = MessageEmptyHolder.Factory.getInstance(parent, this, viewType);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
        final MessageInfo msg = getItem(position);
        MessageBaseHolder baseHolder = (MessageBaseHolder) holder;
        baseHolder.setOnItemClickListener(mOnItemClickListener);
        switch (getItemViewType(position)) {
            case MSG_TYPE_HEADER_VIEW:
                ((MessageHeaderHolder) baseHolder).setLoadingStatus(mLoading);
                break;
            case MessageInfo.MSG_TYPE_TEXT:
            case MessageInfo.MSG_TYPE_IMAGE:
            case MessageInfo.MSG_TYPE_VIDEO:
            case MessageInfo.MSG_TYPE_CUSTOM_FACE:
            case MessageInfo.MSG_TYPE_AUDIO:
            case MessageInfo.MSG_TYPE_FILE:
                break;
            default:
                break;
        }
        baseHolder.layoutViews(msg, position);
        // ?????????????????????????????????????????????????????????????????????????????????????????????
        if (getItemViewType(position) == MessageInfo.MSG_TYPE_CUSTOM) {
            if (holder.getClass() == MessageCustomHolder.class) {
                MessageCustomHolder customHolder = (MessageCustomHolder) holder;
                if (mOnCustomMessageDrawListener != null) {
                    mOnCustomMessageDrawListener.onDraw(customHolder, msg);
                }
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecycleView = (MessageLayout) recyclerView;
    }

    public void showLoading() {
        if (mLoading) {
            return;
        }
        mLoading = true;
        notifyItemChanged(0);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof MessageContentHolder) {
            ((MessageContentHolder) holder).msgContentFrame.setBackground(null);
        }
    }

    public void notifyDataSourceChanged(final int type, final int value) {
        BackgroundTasks.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                mLoading = false;
                if (type == MessageLayout.DATA_CHANGE_TYPE_REFRESH) {
                    notifyDataSetChanged();
                    mRecycleView.scrollToEnd();
                } else if (type == MessageLayout.DATA_CHANGE_TYPE_ADD_BACK) {
                    notifyItemRangeInserted(mDataSource.size() + 1, value);
                    notifyDataSetChanged();
                    mRecycleView.scrollToEnd();
                } else if (type == MessageLayout.DATA_CHANGE_TYPE_UPDATE) {
                    notifyItemChanged(value + 1);
                } else if (type == MessageLayout.DATA_CHANGE_TYPE_LOAD || type == MessageLayout.DATA_CHANGE_TYPE_ADD_FRONT) {
                    //??????????????????0??????????????????
                    if (value == 0) {
                        notifyItemChanged(0);
                    } else {
                        //??????????????????????????????????????????????????????????????????????????????????????????5????????????????????????????????????????????????????????????????????????
                        if (getItemCount() > value) {
                            notifyItemRangeInserted(0, value);
                        } else {
                            notifyItemRangeInserted(0, value);
                        }
                    }
                } else if (type == MessageLayout.DATA_CHANGE_TYPE_DELETE) {
                    notifyItemRemoved(value + 1);
                    notifyDataSetChanged();
                    mRecycleView.scrollToEnd();
                }
            }
        }, 100);
    }

    @Override
    public int getItemCount() {
        return mDataSource.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return MSG_TYPE_HEADER_VIEW;
        }
        MessageInfo msg = getItem(position);
        return msg.getMsgType();
    }

    public void setDataSource(IChatProvider provider) {
        if (provider == null) {
            mDataSource.clear();
        } else {
            mDataSource = provider.getDataSource();
            provider.setAdapter(this);
        }
        notifyDataSourceChanged(MessageLayout.DATA_CHANGE_TYPE_REFRESH, getItemCount());
    }

    public MessageInfo getItem(int position) {
        if (position == 0 || mDataSource.size() == 0) {
            return null;
        }
        MessageInfo info = mDataSource.get(position - 1);
        return info;
    }
}