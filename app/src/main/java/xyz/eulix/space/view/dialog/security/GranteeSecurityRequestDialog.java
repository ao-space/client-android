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

package xyz.eulix.space.view.dialog.security;

import android.app.Dialog;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.eulix.space.R;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/8/15 16:36
 */
public class GranteeSecurityRequestDialog {
    public static final int REQUEST_ACCEPT = 0;
    public static final int REQUEST_DENY = REQUEST_ACCEPT + 1;
    public static final int REQUEST_EXPIRE = REQUEST_DENY + 1;
    private static final int REQUEST_CANCEL = REQUEST_EXPIRE + 1;
    public static final int REQUEST_TOO_MANY = REQUEST_CANCEL + 1;
    public static final int NEW_DEVICE_REQUEST_CANCEL = REQUEST_TOO_MANY + 1;
    private Context mContext;
    private boolean isNewDevice;
    private Dialog granteeRequestDialog;
    private Dialog requestFailDialog;
    private TextView requestFailContent;
    private TextView requestFailHint;
    private Dialog newDeviceRequestCancelDialog;
    private GranteeSecurityRequestCallback mCallback;
    private int mRequestCode;

    public GranteeSecurityRequestDialog(@NonNull Context context, @Nullable GranteeSecurityRequestCallback callback, boolean isNewDevice) {
        mContext = context;
        granteeRequestDialog = new Dialog(context, R.style.EulixDialog);
        requestFailDialog = new Dialog(context, R.style.EulixDialog);
        mCallback = callback;
        this.isNewDevice = isNewDevice;
        init(context);
    }

    public interface GranteeSecurityRequestCallback {
        void cancelRequest();
        void handleRequestCode(int requestCode);
    }

    private void init(Context context) {
        View granteeRequestDialogView = LayoutInflater.from(context).inflate(R.layout.grantee_security_request_dialog, null);
        TextView granterMobilePhoneInaccessible = granteeRequestDialogView.findViewById(R.id.granter_mobile_phone_inaccessible);
        Button granteeCancelRequest = granteeRequestDialogView.findViewById(R.id.grantee_cancel_request);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if (mCallback != null) {
                    mCallback.cancelRequest();
                }
                handleGranteeRequestResult(isNewDevice ? NEW_DEVICE_REQUEST_CANCEL : REQUEST_CANCEL);
            }
        };
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(mContext.getResources().getColor(R.color.blue_ff337aff));
        String granterMobilePhoneInaccessibleContent = context.getString(R.string.granter_mobile_phone_inaccessible);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(granterMobilePhoneInaccessibleContent);
        int contentLength = granterMobilePhoneInaccessibleContent.length();
        spannableStringBuilder.setSpan(clickableSpan, 0, contentLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableStringBuilder.setSpan(foregroundColorSpan, 0, contentLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        granterMobilePhoneInaccessible.setMovementMethod(LinkMovementMethod.getInstance());
        granterMobilePhoneInaccessible.setText(spannableStringBuilder);
        granteeCancelRequest.setOnClickListener(v -> {
            if (mCallback != null) {
                mCallback.cancelRequest();
            }
            dismissGranteeRequestDialog();
        });

        View requestFailDialogView = LayoutInflater.from(context).inflate(R.layout.eulix_space_error_one_button_two_content_dialog_style_2, null);
        ImageView requestFailImage = requestFailDialogView.findViewById(R.id.dialog_image);
        TextView requestFailTitle = requestFailDialogView.findViewById(R.id.dialog_title);
        requestFailContent = requestFailDialogView.findViewById(R.id.dialog_content);
        requestFailHint = requestFailDialogView.findViewById(R.id.dialog_hint);
        Button requestFailButton = requestFailDialogView.findViewById(R.id.dialog_button);
        requestFailImage.setImageResource(R.drawable.operate_fail_2x);
        requestFailTitle.setText(R.string.operate_fail);
        requestFailButton.setText(R.string.ok);
        requestFailButton.setOnClickListener(v -> {
            requestFailContent.setText("");
            requestFailHint.setText("");
            if (mCallback != null) {
                mCallback.handleRequestCode(mRequestCode);
            }
            mRequestCode = 0;
            dismissRequestFailDialog();
        });

        granteeRequestDialog.setCancelable(false);
        granteeRequestDialog.setContentView(granteeRequestDialogView);
        requestFailDialog.setCancelable(false);
        requestFailDialog.setContentView(requestFailDialogView);

        if (isNewDevice) {
            View newDeviceRequestCancelDialogView = LayoutInflater.from(context).inflate(R.layout.eulix_space_error_two_button_dialog_style_2, null);
            ImageView newDeviceRequestCancelImage = newDeviceRequestCancelDialogView.findViewById(R.id.dialog_image);
            TextView newDeviceRequestCancelTitle = newDeviceRequestCancelDialogView.findViewById(R.id.dialog_title);
            TextView newDeviceRequestCancelContent = newDeviceRequestCancelDialogView.findViewById(R.id.dialog_content);
            Button newDeviceRequestCancelConfirm = newDeviceRequestCancelDialogView.findViewById(R.id.dialog_button_1);
            Button newDeviceRequestCancelCancel = newDeviceRequestCancelDialogView.findViewById(R.id.dialog_button_2);
            newDeviceRequestCancelImage.setImageResource(R.drawable.login_expired_2x);
            newDeviceRequestCancelTitle.setText(R.string.notice_hint);
            SpannableStringBuilder newDeviceSpannableStringBuilder = new SpannableStringBuilder();
            String newDeviceGranterMobilePhoneInaccessibleContent = context.getString(R.string.new_device_no_bind_security_mailbox_hint_part_1);
            String twentyFourHours = context.getString(R.string.twenty_four_hours);
            newDeviceSpannableStringBuilder.append(newDeviceGranterMobilePhoneInaccessibleContent);
            newDeviceSpannableStringBuilder.append(twentyFourHours);
            newDeviceSpannableStringBuilder.append(context.getString(R.string.new_device_no_bind_security_mailbox_hint_part_2));
            int highlightStart = newDeviceGranterMobilePhoneInaccessibleContent.length();
            int highlightEnd = highlightStart + twentyFourHours.length();
            newDeviceSpannableStringBuilder.setSpan(foregroundColorSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            newDeviceRequestCancelContent.setText(newDeviceSpannableStringBuilder);
            newDeviceRequestCancelConfirm.setText(R.string.confirm_ok);
            newDeviceRequestCancelCancel.setText(R.string.cancel);
            newDeviceRequestCancelConfirm.setOnClickListener(v -> {
                if (mCallback != null) {
                    mCallback.handleRequestCode(mRequestCode);
                }
                mRequestCode = 0;
                dismissNewDeviceRequestCancelDialog();
            });
            newDeviceRequestCancelCancel.setOnClickListener(v -> {
                mRequestCode = 0;
                dismissNewDeviceRequestCancelDialog();
            });

            newDeviceRequestCancelDialog = new Dialog(context, R.style.EulixDialog);
            newDeviceRequestCancelDialog.setCancelable(false);
            newDeviceRequestCancelDialog.setContentView(newDeviceRequestCancelDialogView);
        }
    }


    /**
     * 处理请求结果
     * @param requestCode 见本类的REQUEST_XX
     */
    public void handleGranteeRequestResult(int requestCode) {
        dismissGranteeRequestDialog();
        switch (requestCode) {
            case REQUEST_DENY:
                requestFailContent.setText(R.string.granter_deny_reason);
                requestFailHint.setText("");
                mRequestCode = requestCode;
                showRequestFailDialog();
                break;
            case REQUEST_EXPIRE:
                requestFailContent.setText(R.string.granter_expire_reason);
                requestFailHint.setText("");
                mRequestCode = requestCode;
                showRequestFailDialog();
                break;
            case REQUEST_CANCEL:
                requestFailContent.setText(R.string.granter_inaccessible_reason);
                requestFailHint.setText(R.string.granter_inaccessible_hint);
                mRequestCode = requestCode;
                showRequestFailDialog();
                break;
            case REQUEST_TOO_MANY:
                requestFailContent.setText(R.string.granter_request_too_many_reason);
                requestFailHint.setText("");
                mRequestCode = requestCode;
                showRequestFailDialog();
                break;
            case NEW_DEVICE_REQUEST_CANCEL:
                mRequestCode = requestCode;
                showNewDeviceRequestCancelDialog();
                break;
            default:
                break;
        }
    }

    public void showGranteeRequestDialog() {
        if (granteeRequestDialog != null && !granteeRequestDialog.isShowing()) {
            granteeRequestDialog.show();
            Window window = granteeRequestDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(mContext.getResources().getDimensionPixelSize(R.dimen.dp_307)
                        , ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    public void dismissGranteeRequestDialog() {
        if (granteeRequestDialog != null && granteeRequestDialog.isShowing()) {
            granteeRequestDialog.dismiss();
        }
    }

    private void showRequestFailDialog() {
        if (requestFailDialog != null && !requestFailDialog.isShowing()) {
            requestFailDialog.show();
            Window window = requestFailDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(mContext.getResources().getDimensionPixelSize(R.dimen.dp_259)
                        , ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissRequestFailDialog() {
        if (requestFailDialog != null && requestFailDialog.isShowing()) {
            requestFailDialog.dismiss();
        }
    }

    private void showNewDeviceRequestCancelDialog() {
        if (newDeviceRequestCancelDialog != null && !newDeviceRequestCancelDialog.isShowing()) {
            newDeviceRequestCancelDialog.show();
            Window window = newDeviceRequestCancelDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(mContext.getResources().getDimensionPixelSize(R.dimen.dp_259)
                        , ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissNewDeviceRequestCancelDialog() {
        if (newDeviceRequestCancelDialog != null && newDeviceRequestCancelDialog.isShowing()) {
            newDeviceRequestCancelDialog.dismiss();
        }
    }
}
