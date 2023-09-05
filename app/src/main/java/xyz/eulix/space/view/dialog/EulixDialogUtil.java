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
import android.content.DialogInterface;
import android.view.View;

import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 对话框工具类
 * History:     2021/8/25
 */
public class EulixDialogUtil {

    //显示底部删除提示对话框
    public static void showBottomDeleteDialog(Context context, String content,
                                              DialogInterface.OnClickListener ok, DialogInterface.OnClickListener cancel) {
        new EulixBottomSelectDialog.Builder(context)
                .setContent(content)
                .setNegButton(context.getString(R.string.cancel), cancel)
                .setPosButton(context.getString(R.string.confirm_delete), ok)
                .create()
                .show();
    }

    //显示底部删除提示对话框
    public static void showBottomDeleteDialog(Context context, String content,
                                              String okText, DialogInterface.OnClickListener ok, DialogInterface.OnClickListener cancel) {
        new EulixBottomSelectDialog.Builder(context)
                .setContent(content)
                .setNegButton(context.getString(R.string.cancel), cancel)
                .setPosButton(okText, ok)
                .create()
                .show();
    }

    //显示loading对话框
    public static EulixLoadingDialog createLoadingDialog(Context context, String text, boolean cancelable) {
        EulixLoadingDialog dialog = new EulixLoadingDialog.Builder(context)
                .setText(text)
                .setCancelable(cancelable)
                .createDialog();
        return dialog;
    }

    //显示选择确认对话框
    public static void showChooseAlertDialog(Context context, String titleText, String contentText, String confirmText,
                                             DialogInterface.OnClickListener confirmListener, DialogInterface.OnClickListener cancelListener) {
        new EulixAlertDialog.Builder(context)
                .setTitle(titleText)
                .setContent(contentText)
                .setConfirmButton(confirmText, confirmListener)
                .setCancelButton(null, cancelListener)
                .create()
                .show();
    }

    //显示选择确认对话框
    public static void showNoticeAlertDialog(Context context, String titleText, String contentText, String confirmText,
                                             DialogInterface.OnClickListener confirmListener) {
        new EulixNoticeAlertDialog.Builder(context)
                .setTitle(titleText)
                .setContent(contentText)
                .setConfirmButton(confirmText, confirmListener)
                .create()
                .show();
    }

    //获取安全密码校验对话框
    public static SecurityPwdVerifyDialog getSecurityPwdVerifyDialog(Context context, SecurityPwdVerifyDialog.InputListener listener) {
        SecurityPwdVerifyDialog dialog = new SecurityPwdVerifyDialog.Builder(context)
                .setVerifyListener(listener)
                .create();
        return dialog;
    }

    //显示隐私协议对话框
    public static void showPrivacyAgreementDialog(Context context, DialogInterface.OnClickListener ok,
                                                  DialogInterface.OnClickListener cancel) {
        new PrivacyAgreementDialog.Builder(context)
                .setPosButton(context.getResources().getString(R.string.agree), ok)
                .setNegButton(context.getResources().getString(R.string.disagree_and_quit), cancel)
                .create()
                .show();
    }


    //显示系统升级提示对话框
    public static SystemUpgradeNoticeDialog showSystemUpgradeNoticeDialog(Context context, String versionName, DialogInterface.OnClickListener listener) {
        SystemUpgradeNoticeDialog dialog = new SystemUpgradeNoticeDialog.Builder(context)
                .setVersionName(versionName)
                .setOnClickListener(listener)
                .create();
        dialog.show();
        return dialog;
    }

    //获取任务进度展示对话框
    public static TaskProgressDialog getTaskProgressDialog(Context context, String taskName) {
        return new TaskProgressDialog.Builder(context).setTaskName(taskName).create();
    }

    //显示权限提示对话框
    public static void showPermissionAlertDialog(Context context, String permission, PermissionAlertDialog.OnClickListener onClickListener) {
        new PermissionAlertDialog.Builder(context)
                .setPermission(permission)
                .setOnClickListener(onClickListener)
                .create()
                .show();
    }

    //显示平台地址输入对话框
    public static void showPlatformInputDialog(Context context, String address, PlatformAddressInputDialog.OnConfirmListener listener){
        new PlatformAddressInputDialog.Builder(context)
                .setAddress(address)
                .setListener(listener)
                .create()
                .show();
    }
}
