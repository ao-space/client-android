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

package xyz.eulix.space.view.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.util.ClipboardUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.ToastManager;
import xyz.eulix.space.view.BottomDialog;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/14 10:08
 */
public class InviteLinkDialog extends BottomDialog {
    private Context mContext;
    private LinearLayout linkContainer;
    private LinearLayout.LayoutParams linkLayoutParams;
    private LinearLayout.LayoutParams splitLayoutParams;
    private View weChatContainer;
    private View copyLinkContainer;
    private String baseLink;
    private String inviteLink;
    private ToastManager toastManager;

    public InviteLinkDialog(@NonNull Context context) {
        super(context);
        init(context);
    }

    public InviteLinkDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    public InviteLinkDialog(@NonNull Context context, int themeResId, boolean isTransparent) {
        super(context, themeResId, isTransparent);
        init(context);
    }

    private void init(@NonNull Context context) {
        mContext = context;
        baseLink = (DebugUtil.getOfficialEnvironmentWeb() + (FormatUtil.isChinese(FormatUtil.getLocale(context)
                , false) ? ConstantField.URL.SERVER_INVITE_MEMBERS_API : ConstantField.URL.EN_SERVER_INVITE_MEMBERS_API));
        toastManager = new ToastManager(context);
        View view = LayoutInflater.from(context).inflate(R.layout.invite_link_dialog, null);
        setCancelable(true);
        setContentView(view);
        ImageButton exit = view.findViewById(R.id.dialog_exit);
        linkContainer = view.findViewById(R.id.link_container);
        linkLayoutParams = new LinearLayout.LayoutParams(mContext.getResources().getDimensionPixelSize(R.dimen.dp_106)
                , ViewGroup.LayoutParams.MATCH_PARENT);
        splitLayoutParams = new LinearLayout.LayoutParams(mContext.getResources().getDimensionPixelSize(R.dimen.dp_28)
                , ViewGroup.LayoutParams.MATCH_PARENT);
        generateWeChat();
        generateCopyLink();
        exit.setOnClickListener(v -> dismissDialog());
    }

    private void generateWeChat() {
        weChatContainer = LayoutInflater.from(mContext).inflate(R.layout.invite_link_item, null);
        ImageView linkImage = weChatContainer.findViewById(R.id.link_image);
        TextView linkText = weChatContainer.findViewById(R.id.link_text);
        linkImage.setImageResource(R.drawable.we_chat_2x);
        linkText.setText(R.string.we_chat);
        weChatContainer.setOnClickListener(v -> {
            if (inviteLink != null) {

            }
            dismissDialog();
        });
    }

    private void generateCopyLink() {
        copyLinkContainer = LayoutInflater.from(mContext).inflate(R.layout.invite_link_item, null);
        ImageView linkImage = copyLinkContainer.findViewById(R.id.link_image);
        TextView linkText = copyLinkContainer.findViewById(R.id.link_text);
        linkImage.setImageResource(R.drawable.copy_link_2x);
        linkText.setText(R.string.copy_link);
        copyLinkContainer.setOnClickListener(v -> {
            if (inviteLink != null) {
                if (ClipboardUtil.setClipData(mContext, inviteLink)) {
                    if (toastManager != null) {
                        toastManager.showPureTextToast(R.string.copy_to_clipboard_success);
                    }
                } else {
                    if (toastManager != null) {
                        toastManager.showPureTextToast(R.string.copy_to_clipboard_failed);
                    }
                }
            }
            dismissDialog();
        });
    }

    private void initLinkContainer() {
        if (linkContainer != null) {
            linkContainer.removeAllViews();
            linkContainer.addView(copyLinkContainer, linkLayoutParams);
        }
    }

    public String getAccountName() {
        String accountName = null;
        String clientUuid = DataUtil.getClientUuid(mContext);
        UserInfo userInfo = EulixSpaceDBUtil.getActiveUserInfo(mContext, clientUuid);
        if (userInfo != null && userInfo.isAdmin()) {
            accountName = userInfo.getNickName();
        }
        return accountName;
    }

    public void showDialog(String query) {
        initLinkContainer();
        if (query != null && !query.startsWith("?")) {
            query = "?" + query;
        }
        if (query == null) {
            inviteLink = baseLink;
        } else {
            inviteLink = baseLink + query;
        }
        if (!isShowing()) {
            show();
        }
    }

    public void dismissDialog() {
        if (isShowing()) {
            dismiss();
        }
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        inviteLink = null;
        super.dismiss();
    }
}
