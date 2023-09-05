/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.ui.mine;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.message.MessageCenterAdapter;
import xyz.eulix.space.bean.MessageCenterBean;
import xyz.eulix.space.network.notification.PageInfo;
import xyz.eulix.space.presenter.MessageCenterPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.rv.FooterView;
import xyz.eulix.space.view.rv.HeaderFooterWrapper;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/20 15:41
 */
public class MessageCenterActivity extends AbsActivity<MessageCenterPresenter.IMessageCenter, MessageCenterPresenter> implements MessageCenterPresenter.IMessageCenter, View.OnClickListener, MessageCenterAdapter.OnItemClickListener {
    private static final int SECOND_UNIT = 1000;
    private static final int DELETE_PUSH_MESSAGE = 1;
    private ImageButton back;
    private TextView title;
    private Button cleanUp;
    private SwipeRefreshLayout swipeRefreshContainer;
    private RelativeLayout exceptionContainer;
    private LinearLayout networkExceptionContainer;
    private Button refreshNow;
    private LinearLayout status404Container;
    private LinearLayout emptyMessageContainer;
    private RecyclerView messageList;
    private MessageCenterAdapter mAdapter;
    private HeaderFooterWrapper headerFooterWrapper;
    private FooterView footer;
    private List<MessageCenterBean> messageCenterBeans;
    private List<String> messageIds = new ArrayList<>();
    private MessageCenterHandler mHandler;
    private int mPage = 1;
    private int mTotalPage = 1;
    private int maxChildCount = 7;
    // 上拉加载使能，本地加载时失效，网络加载到来之后生效
    private boolean isLoadingEnable = false;
    // 本地加载是否存在，不存在则展示网络错误，刷新时强制为true
    private boolean isLocalEmpty = true;
    private Boolean isReadAll = null;

    private HeaderFooterWrapper.ILoadMore loadMore = new HeaderFooterWrapper.ILoadMore() {
        @Override
        public void loadMore() {
            if (isLoadingEnable && presenter != null && mPage < mTotalPage) {
                presenter.getAllNotification((mPage + 1));
            }
        }
    };

    static class MessageCenterHandler extends Handler {
        private WeakReference<MessageCenterActivity> messageCenterActivityWeakReference;

        public MessageCenterHandler(MessageCenterActivity activity) {
            messageCenterActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MessageCenterActivity activity = messageCenterActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case DELETE_PUSH_MESSAGE:
                        if (activity.isReadAll != null) {
                            if (!activity.isReadAll) {
                                sendEmptyMessageDelayed(DELETE_PUSH_MESSAGE, SECOND_UNIT);
                            } else if (activity.presenter != null) {
                                activity.presenter.deleteAllMessage();
                            }
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_message_center);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        cleanUp = findViewById(R.id.function_text);
        swipeRefreshContainer = findViewById(R.id.swipe_refresh_container);
        exceptionContainer = findViewById(R.id.exception_container);
        networkExceptionContainer = findViewById(R.id.network_exception_container);
        refreshNow = findViewById(R.id.refresh_now);
        status404Container = findViewById(R.id.status_404_container);
        emptyMessageContainer = findViewById(R.id.empty_message_container);
        messageList = findViewById(R.id.message_list);
    }

    @Override
    public void initData() {
        mHandler = new MessageCenterHandler(this);
        messageCenterBeans = new ArrayList<>();
    }

    @Override
    public void initViewData() {
        title.setText(R.string.message_center);
        cleanUp.setText(R.string.clean_up);
        updateFunctionText();
        maxChildCount = Math.max((int) (Math.ceil((ViewUtils.getScreenHeight(this) - ViewUtils.getStatusBarHeight(this))
                * 1.0 / getResources().getDimensionPixelSize(R.dimen.dp_75))), maxChildCount);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        cleanUp.setOnClickListener(this);
        refreshNow.setOnClickListener(this);
        initAdapter();
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setOnRefreshListener(() -> {
                isLocalEmpty = true;
                refreshEulixMessage();
            });
        }
        getLocalEulixMessage();
        refreshEulixMessage();
        if (presenter != null) {
            isReadAll = false;
            presenter.readAllMessage();
        }
    }

    private void initAdapter() {
        mAdapter = new MessageCenterAdapter(this, messageCenterBeans, (presenter == null ? null : presenter.getEulixBoxBaseInfo()));
        mAdapter.setOnItemClickListener(this);
        messageList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        headerFooterWrapper = new HeaderFooterWrapper(mAdapter);
        messageList.setAdapter(headerFooterWrapper);
        footer = new FooterView(this);
        mAdapter.setFooterView(footer);
    }

    private void getLocalEulixMessage() {
        isLoadingEnable = false;
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setRefreshing(false);
        }
        if (presenter != null) {
            List<MessageCenterBean> messageCenterBeanList = presenter.getLocalNotification();
            isLocalEmpty = (messageCenterBeanList == null || messageCenterBeanList.size() <= 0);
            openMessageDirectory(messageCenterBeanList);
        }
        cleanUp.setVisibility(isLocalEmpty ? View.GONE : View.VISIBLE);
        if (messageList != null && (!messageList.canScrollVertically(-1) && !messageList.canScrollVertically(1))
                && messageCenterBeans != null && messageCenterBeans.size() < maxChildCount) {
            setFooter(true, false);
            setFooterVisible(false);
        } else if (footer != null) {
            setFooter(true, true);
            setFooterVisible(true);
            footer.showBottom(getString(R.string.home_bottom_flag));
        }
    }

    private void refreshEulixMessage() {
        if (presenter != null) {
            presenter.getAllNotification(1);
        }
    }

    private void updateFunctionText() {
        if (messageCenterBeans != null && cleanUp != null) {
            cleanUp.setVisibility(messageCenterBeans.size() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void setFooter(boolean isAdd, boolean isForce) {
        if (isAdd) {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() <= 0 && messageList != null && footer != null
                    && (isForce || (messageList.canScrollVertically(-1) || messageList.canScrollVertically(1)))) {
                // 只添加一次footer
                headerFooterWrapper.addFooterView(footer, loadMore);
            }
        } else {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() > 0) {
                headerFooterWrapper.removeAllFooters();
            }
        }
    }

    private void setFooterVisible(boolean isVisible) {
        if (footer != null) {
            ViewGroup.LayoutParams param = footer.getLayoutParams();

            if (isVisible) {
                param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                param.height = getResources().getDimensionPixelSize(R.dimen.dp_33);
            } else {
                param.width = 0;
                param.height = 0;
            }
            footer.setLayoutParams(param);
        }
    }

    private void openMessageDirectory(List<MessageCenterBean> messageCenterBeanList) {
        setFooter(false, true);
        if (messageCenterBeanList != null) {
            messageIds.clear();
            mPage = 1;
            messageCenterBeans = messageCenterBeanList;
            if (mAdapter != null && headerFooterWrapper != null) {
                mAdapter.updateData(messageCenterBeanList, true);
                headerFooterWrapper.notifyDataSetChanged();
            }
            for (MessageCenterBean messageCenterBean : messageCenterBeanList) {
                if (messageCenterBean != null) {
                    String messageId = messageCenterBean.getMessageId();
                    if (messageId != null) {
                        messageIds.add(messageId);
                    }
                }
            }
            handleDataResult(messageCenterBeanList.size() <= 0 ? -2: -3, true);
        } else {
            handleDataResult(-5, true);
        }
    }

    private void addMessageDirectory(List<MessageCenterBean> messageCenterBeanList, PageInfo pageInfo) {
        if (pageInfo != null && pageInfo.getPage() != null) {
            mPage = Math.max(mPage, pageInfo.getPage());
            if (messageCenterBeans != null) {
                if (messageCenterBeanList != null) {
                    for (MessageCenterBean messageCenterBean : messageCenterBeanList) {
                        if (messageCenterBean != null) {
                            String messageId = messageCenterBean.getMessageId();
                            if (messageId != null && !messageIds.contains(messageId)) {
                                messageIds.add(messageId);
                                messageCenterBeans.add(messageCenterBean);
                            }
                        }
                    }
                }
                if (mAdapter != null && headerFooterWrapper != null) {
                    mAdapter.updateData(messageCenterBeans, false);
                    headerFooterWrapper.notifyDataSetChanged();
                }
            }
        }
    }

    private void handleMessageCenterInfo(PageInfo pageInfo, List<MessageCenterBean> messageCenterBeanList) {
        Integer pageValue = pageInfo.getPage();
        Integer totalPageValue = pageInfo.getTotal();
        if (totalPageValue != null) {
            mTotalPage = totalPageValue;
        }
        if (pageValue != null && pageValue > 1) {
            addMessageDirectory(messageCenterBeanList, pageInfo);
        } else {
            openMessageDirectory(messageCenterBeanList);
        }
        if (mTotalPage > 1) {
            Integer pageSizeValue = pageInfo.getPageSize();
            if (pageSizeValue != null) {
                int pageSize = pageSizeValue;
                if (pageSize <= 0) {
                    pageSize = messageCenterBeanList.size();
                }
                pageSize = Math.max(pageSize, 1);
                if (presenter != null && (pageSize * mPage) <= maxChildCount && mPage < mTotalPage) {
                    presenter.getAllNotification((mPage + 1));
                }
            }
        }
        if (mPage == mTotalPage) {
            if (messageList != null && (!messageList.canScrollVertically(-1) && !messageList.canScrollVertically(1))) {
                setFooterVisible(false);
            } else if (footer != null) {
                setFooterVisible(true);
                footer.showBottom(getString(R.string.home_bottom_flag));
            }
        } else {
            setFooterVisible(true);
            if (footer != null) {
                footer.showLoading();
            }
        }
    }

    private void handleDataResult(int statusCode, boolean isTotalUpdate) {
        switch (statusCode) {
            case -3:
                cleanUp.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyMessageContainer.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.GONE);
                messageList.setVisibility(View.VISIBLE);
                break;
            case -2:
                cleanUp.setVisibility(View.GONE);
                messageList.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyMessageContainer.setVisibility(View.VISIBLE);
                break;
            case ConstantField.OBTAIN_ACCESS_TOKEN_CODE:
                obtainAccessToken();
//                showImageTextToast(R.drawable.toast_refuse, R.string.active_device_offline_hint);
                break;
            case ConstantField.FILE_DISCONNECT_CODE:
                if (messageCenterBeans == null || messageCenterBeans.size() <= 0) {
                    handleDataResult(-2, isTotalUpdate);
                }
                showImageTextToast(R.drawable.toast_refuse, R.string.active_device_offline_hint);
                break;
            case ConstantField.SERVER_EXCEPTION_CODE:
                if (messageCenterBeans == null || messageCenterBeans.size() <= 0) {
                    handleDataResult(-2, isTotalUpdate);
                }
                showServerExceptionToast();
                break;
            case ConstantField.NETWORK_ERROR_CODE:
                messageList.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.VISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyMessageContainer.setVisibility(View.INVISIBLE);
                break;
            default:
                if (messageCenterBeans != null) {
                    messageCenterBeans.clear();
                }
                if (mAdapter != null && headerFooterWrapper != null) {
                    mAdapter.updateData(messageCenterBeans, isTotalUpdate);
                    headerFooterWrapper.notifyDataSetChanged();
                }
                cleanUp.setVisibility(View.GONE);
                messageList.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.VISIBLE);
                emptyMessageContainer.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void obtainAccessToken() {
        Intent serviceIntent = new Intent(MessageCenterActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @NotNull
    @Override
    public MessageCenterPresenter createPresenter() {
        return new MessageCenterPresenter();
    }

    @Override
    public void allNotificationResult(Integer code, List<MessageCenterBean> messageCenterBeanList, PageInfo pageInfo) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (swipeRefreshContainer != null) {
                    swipeRefreshContainer.setRefreshing(false);
                }
                isLoadingEnable = true;
                if (messageCenterBeanList == null) {
                    if (isLocalEmpty) {
                        handleDataResult(code, false);
                    }
                    getLocalEulixMessage();
                    if (messageList != null && (!messageList.canScrollVertically(-1) && !messageList.canScrollVertically(1))) {
                        setFooterVisible(false);
                    } else if (footer != null) {
                        setFooterVisible(true);
                        footer.showBottom(getString(R.string.home_bottom_flag));
                    }
                } else {
                    if (pageInfo != null) {
                        handleMessageCenterInfo(pageInfo, messageCenterBeanList);
                    } else {
                        mTotalPage = 1;
                        closeLoading();
                        openMessageDirectory(messageCenterBeanList);
                    }
                }
                setFooter(true, false);
            });
        }
    }

    @Override
    public void deleteNotificationResult(Integer code, Integer result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if ((code >= 200 && code < 400) || code == ConstantField.KnownError.NotificationError.MESSAGE_NOT_EXIST) {
                    showImageTextToast(R.drawable.toast_right, R.string.message_center_clean_up_success);
                    refreshEulixMessage();
                    if (isReadAll != null) {
                        mHandler.sendEmptyMessage(DELETE_PUSH_MESSAGE);
                    }
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.message_center_clean_up_failed);
                }
            });
        }
    }

    @Override
    public void handleReadAll() {
        if (mHandler != null) {
            mHandler.post(() -> isReadAll = true);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.function_text:
                    if (presenter != null) {
                        showLoading("");
                        presenter.deleteAllNotification();
                    }
                    break;
                case R.id.refresh_now:
                    isLocalEmpty = true;
                    refreshEulixMessage();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        if (mAdapter != null && messageCenterBeans != null && position >= 0 && messageCenterBeans.size() > position) {
            MessageCenterBean messageCenterBean = messageCenterBeans.get(position);
            if (messageCenterBean != null) {
                Intent intent = null;
                String messageType = messageCenterBean.getMessageType();
                if (messageType != null) {
                    switch (messageType) {
                        case ConstantField.PushType.LOGIN:
                            intent = new Intent(MessageCenterActivity.this, DeviceManageActivity.class);
                            break;
                        case ConstantField.PushType.UPGRADE_SUCCESS:
                        case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                            intent = new Intent(MessageCenterActivity.this, SystemUpdateActivity.class);
                            break;
                        default:
                            break;
                    }
                }
                if (intent != null) {
                    startActivity(intent);
                }
            }
        }
    }
}
