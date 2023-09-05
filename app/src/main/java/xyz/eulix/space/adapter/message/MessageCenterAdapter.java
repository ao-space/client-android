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

package xyz.eulix.space.adapter.message;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.MessageCenterBean;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.push.LoginBean;
import xyz.eulix.space.network.push.LoginConfirmBean;
import xyz.eulix.space.network.push.SecurityApplyBean;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.view.rv.FooterView;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/21 11:34
 */
public class MessageCenterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private List<MessageCenterBean> mMessageCenterBeanList;
    private Context mContext;
    private long standardTimestamp;
    private OnItemClickListener mOnItemClickListener;
    private FooterView footerView;
    private String boxUuid;
    private String boxBind;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public MessageCenterAdapter(@NonNull Context context, List<MessageCenterBean> messageCenterBeanList, EulixBoxBaseInfo eulixBoxBaseInfo) {
        mContext = context;
        mMessageCenterBeanList = messageCenterBeanList;
        standardTimestamp = FormatUtil.generateDayZeroTime();
        generateBoxUuidAndBind(eulixBoxBaseInfo);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void generateBoxUuidAndBind(EulixBoxBaseInfo eulixBoxBaseInfo) {
        if (eulixBoxBaseInfo == null) {
            boxUuid = null;
            boxBind = null;
        } else {
            boxUuid = eulixBoxBaseInfo.getBoxUuid();
            boxBind = eulixBoxBaseInfo.getBoxBind();
        }
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (position >= 0 && mMessageCenterBeanList != null && mMessageCenterBeanList.size() > position) {
            MessageCenterBean messageCenterBean = mMessageCenterBeanList.get(position);
            if (messageCenterBean != null) {
                long timestamp = Math.max(messageCenterBean.getMessageTimestamp(), 0L);
                switch (FormatUtil.dayOfDiff(timestamp, standardTimestamp, 7)) {
                    case 0:
                        holder.messageDate.setText(FormatUtil.formatTime(timestamp, ConstantField.TimeStampFormat.TODAY_FORMAT));
                        break;
                    case -1:
                        String date = mContext.getString(R.string.yesterday_head) + " " + FormatUtil.formatTime(timestamp, ConstantField.TimeStampFormat.TODAY_FORMAT);
                        holder.messageDate.setText(date);
                        break;
                    default:
                        holder.messageDate.setText(FormatUtil.formatTime(timestamp, ConstantField.TimeStampFormat.FILE_API_MINUTE_FORMAT));
                        break;
                }
                String messageType = messageCenterBean.getMessageType();
                String messageTitle = mContext.getString(R.string.unknown);
                String messageContent = mContext.getString(R.string.unknown);
                String messageDetail = mContext.getString(R.string.unknown);
                Boolean showDetail = null;
                if (messageType != null) {
                    String messageData = messageCenterBean.getData();
                    switch (messageType) {
                        case ConstantField.PushType.LOGIN:
                            LoginBean loginBean = null;
                            String loginTerminalMode = null;
                            messageTitle = mContext.getString(R.string.login_reminder);
                            if (messageData != null) {
                                try {
                                    loginBean = new Gson().fromJson(messageData, LoginBean.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (loginBean != null) {
                                loginTerminalMode = loginBean.getTerminalMode();
                            }
                            StringBuilder loginContentBuilder = new StringBuilder();
                            loginContentBuilder.append(mContext.getString(R.string.login_notification_part_1));
                            if (loginTerminalMode != null && !TextUtils.isEmpty(loginTerminalMode)) {
                                loginContentBuilder.append(" ");
                                loginContentBuilder.append(loginTerminalMode);
                                loginContentBuilder.append(" ");
                            } else {
                                loginContentBuilder.append(mContext.getString(R.string.a_terminal));
                            }
                            loginContentBuilder.append(mContext.getString(R.string.login_notification_part_2));
                            messageContent = loginContentBuilder.toString();
                            messageDetail = mContext.getString(R.string.login_reminder_detail);
                            showDetail = true;
                            break;
                        case ConstantField.PushType.LOGOUT:
                        case ConstantField.PushType.REVOKE:
                            messageTitle = mContext.getString(R.string.logout_reminder);
                            messageContent = mContext.getString(R.string.logout_reminder_content);
                            messageDetail = mContext.getString(R.string.logout_reminder_detail);
                            showDetail = false;
                            break;
                        case ConstantField.PushType.MEMBER_SELF_DELETE:
                            messageTitle = mContext.getString(R.string.member_delete_reminder);
                            messageContent = mContext.getString(R.string.member_self_delete_reminder_content);
                            break;
                        case ConstantField.PushType.BOX_UPGRADE:
                            break;
                        case ConstantField.PushType.APP_UPGRADE:
                            break;
                        case ConstantField.PushType.LOGIN_CONFIRM:
                            LoginConfirmBean loginConfirmBean = null;
                            String loginConfirmTerminalMode = null;
                            String loginConfirmAoId = null;
                            String userDomain = null;
                            messageTitle = mContext.getString(R.string.login_confirm_reminder);
                            if (messageData != null) {
                                try {
                                    loginConfirmBean = new Gson().fromJson(messageData, LoginConfirmBean.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (loginConfirmBean != null) {
                                loginConfirmTerminalMode = loginConfirmBean.getTerminalMode();
                                loginConfirmAoId = loginConfirmBean.getAoid();
                            }
                            StringBuilder loginConfirmContentBuilder = new StringBuilder();
                            if (loginConfirmTerminalMode != null && !TextUtils.isEmpty(loginConfirmTerminalMode)) {
                                loginConfirmContentBuilder.append(loginConfirmTerminalMode);
                                loginConfirmContentBuilder.append(" ");
                            } else {
                                loginConfirmContentBuilder.append(mContext.getString(R.string.a_terminal_head));
                            }
                            loginConfirmContentBuilder.append(mContext.getString(R.string.login_confirm_reminder_content_part_1));
                            if (loginConfirmAoId != null && boxUuid != null && ("1".equals(boxBind) || "-1".equals(boxBind))) {
                                UserInfo userInfo = getGranterInfo(boxUuid, boxBind, loginConfirmAoId);
                                if (userInfo != null) {
                                    String boxDomain = userInfo.getUserDomain();
                                    if (boxDomain != null) {
                                        userDomain = generateBaseUrl(boxDomain);
                                    }
                                }
                            }
                            if (userDomain != null && !TextUtils.isEmpty(userDomain)) {
                                loginConfirmContentBuilder.append(mContext.getString(R.string.left_bracket));
                                loginConfirmContentBuilder.append(userDomain);
                                loginConfirmContentBuilder.append(mContext.getString(R.string.right_bracket));
                            }
                            loginConfirmContentBuilder.append(mContext.getString(R.string.login_confirm_reminder_content_part_2));
                            messageContent = loginConfirmContentBuilder.toString();
                            messageDetail = mContext.getString(R.string.login_confirm_reminder_detail);
                            showDetail = false;
                            break;
                        case ConstantField.PushType.UPGRADE_SUCCESS:
                            messageTitle = mContext.getString(R.string.box_upgrade_reminder);
                            messageContent = mContext.getString(R.string.upgrade_success_reminder_content);
                            messageDetail = mContext.getString(R.string.reminder_detail);
                            showDetail = true;
                            break;
                        case ConstantField.PushType.BOX_SYSTEM_RESTART:
                            messageTitle = mContext.getString(R.string.box_upgrade_reminder);
                            messageContent = mContext.getString(R.string.box_system_restart_reminder_content);
                            break;
                        case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                            messageTitle = mContext.getString(R.string.box_upgrade_reminder);
                            String versionStr = mContext.getResources().getString(R.string.app_name);
                            Logger.d("zfy", "data:" + messageData);
                            if (!TextUtils.isEmpty(messageData)) {
                                try {
                                    JSONObject jsonObject = new JSONObject(messageData);
                                    versionStr = versionStr + " " + jsonObject.optString("version");
                                    //删除换行符
                                    versionStr.replaceAll("\n", "");
                                } catch (Exception e) {
                                    Logger.d("zfy", "data is not json");
                                }
                            }
                            messageContent = mContext.getString(R.string.box_package_pulled_desc_msg_center).replace("%$", versionStr);
                            messageDetail = mContext.getString(R.string.reminder_detail);
                            showDetail = true;
                            break;
                        case ConstantField.PushType.BOX_START_UPGRADE:
                            messageTitle = mContext.getString(R.string.box_upgrade_reminder);
                            messageContent = mContext.getString(R.string.box_start_upgarde_desc);
                            break;
                        case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                        case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                            SecurityApplyBean securityApplyBean = null;
                            String authDeviceInfo = null;
                            messageTitle = mContext.getString(R.string.security_reminder);
                            if (messageData != null) {
                                try {
                                    securityApplyBean = new Gson().fromJson(messageData, SecurityApplyBean.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (securityApplyBean != null) {
                                authDeviceInfo = securityApplyBean.getAuthDeviceInfo();
                            }
                            StringBuilder securityPasswordApplyBuilder = new StringBuilder();
                            securityPasswordApplyBuilder.append(mContext.getString(R.string.security_password_apply_reminder_content_part_1));
                            if (authDeviceInfo != null && !TextUtils.isEmpty(authDeviceInfo)) {
                                securityPasswordApplyBuilder.append(" ");
                                securityPasswordApplyBuilder.append(authDeviceInfo);
                                securityPasswordApplyBuilder.append(" ");
                            }
                            securityPasswordApplyBuilder.append(mContext.getString(R.string.security_password_apply_reminder_content_part_2));
                            messageContent = securityPasswordApplyBuilder.toString();
                            messageDetail = mContext.getString(R.string.login_confirm_reminder_detail);
                            showDetail = false;
                            break;
                        case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_SUCCESS:
                            messageTitle = mContext.getString(R.string.security_reminder);
                            messageContent = mContext.getString(R.string.security_password_modify_success_reminder_content);
                            messageDetail = mContext.getString(R.string.security_password_success_reminder_detail);
                            showDetail = false;
                            break;
                        case ConstantField.PushType.SECURITY_PASSWORD_RESET_SUCCESS:
                            messageTitle = mContext.getString(R.string.security_reminder);
                            messageContent = mContext.getString(R.string.security_password_reset_success_reminder_content);
                            messageDetail = mContext.getString(R.string.security_password_success_reminder_detail);
                            showDetail = false;
                            break;
                        default:
                            break;
                    }
                }
                holder.messageTitle.setText(messageTitle);
                holder.messageContent.setText(messageContent);
                if (showDetail == null) {
                    holder.viewDetailsContainer.setVisibility(View.GONE);
                    holder.messageDetail.setVisibility(View.GONE);
                } else {
                    if (!TextUtils.isEmpty(messageDetail)) {
                        holder.messageDetail.setVisibility(View.VISIBLE);
                        holder.messageDetail.setText(messageDetail);
                    } else {
                        holder.messageDetail.setVisibility(View.GONE);
                    }
                    holder.viewDetailsContainer.setVisibility(showDetail ? View.VISIBLE : View.GONE);
                }
                holder.messageContainer.setTag(position);
                if (showDetail != null && showDetail) {
                    holder.messageContainer.setOnClickListener(this);
                } else {
                    holder.messageContainer.setClickable(false);
                }
            }
        }
    }

    private UserInfo getGranterInfo(String boxUuid, String boxBind, String aoId) {
        UserInfo userInfo = null;
        String clientUuid = DataUtil.getClientUuid(mContext);
        if (boxUuid != null && aoId != null && clientUuid != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            if (boxBind != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            }
            List<UserInfo> userInfoList = EulixSpaceDBUtil.getGranterUserInfoList(mContext, clientUuid, queryMap);
            if (userInfoList != null) {
                for (UserInfo info : userInfoList) {
                    if (info != null && aoId.equals(info.getUserId())) {
                        userInfo = info;
                        break;
                    }
                }
            }
        }
        return userInfo;
    }

    private String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = "";
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = DebugUtil.getEnvironmentServices();
            } else {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    public void updateData(List<MessageCenterBean> messageCenterBeanList, boolean isTotalUpdate) {
        mMessageCenterBeanList = messageCenterBeanList;
        if (isTotalUpdate) {
            standardTimestamp = FormatUtil.generateDayZeroTime();
        }
        notifyDataSetChanged();
    }

    public void updateData(List<MessageCenterBean> messageCenterBeanList, boolean isTotalUpdate, EulixBoxBaseInfo eulixBoxBaseInfo) {
        generateBoxUuidAndBind(eulixBoxBaseInfo);
        updateData(messageCenterBeanList, isTotalUpdate);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.message_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            holder.itemView.setTag(position);
            generateViewHolderData((ViewHolder) holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return (mMessageCenterBeanList == null ? 0 : mMessageCenterBeanList.size());
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof ViewHolder) {
            Object positionTag = holder.itemView.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                generateViewHolderData((ViewHolder) holder, position);
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    public FooterView getFooterView() {
        return footerView;
    }

    public void setFooterView(FooterView footerView) {
        this.footerView = footerView;
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            Object positionTag = v.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position);
                }
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView messageDate;
        private LinearLayout messageContainer;
        private TextView messageTitle;
        private TextView messageContent;
        private TextView messageDetail;
        private LinearLayout viewDetailsContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageDate = itemView.findViewById(R.id.message_date);
            messageContainer = itemView.findViewById(R.id.message_container);
            messageTitle = itemView.findViewById(R.id.message_title);
            messageContent = itemView.findViewById(R.id.message_content);
            messageDetail = itemView.findViewById(R.id.message_detail);
            viewDetailsContainer = itemView.findViewById(R.id.view_details_container);
        }
    }
}
