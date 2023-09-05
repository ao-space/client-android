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

package xyz.eulix.space.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;

import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultWithNullCallback;
import xyz.eulix.space.ui.PermissionActivity;
import xyz.eulix.space.ui.mine.privacy.EulixPermissionManagerActivity;
import xyz.eulix.space.view.dialog.EulixDialogUtil;
import xyz.eulix.space.view.dialog.PermissionAlertDialog;

/**
 * Author:      Zhu Fuyu
 * Description: 动态权限工具类
 * History:     2022/5/19
 */
public class PermissionUtils {
    public static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final String PERMISSION_READ_CONTACTS = Manifest.permission.READ_CONTACTS;
    public static final String PERMISSION_WRITE_CONTACTS = Manifest.permission.WRITE_CONTACTS;
    public static final String PERMISSION_ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final String PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;

    //获取权限成功
    public static final int STATUS_SUCCESS = 0;
    //申请权限拒绝, 但是下次申请权限还会弹窗
    public static final int STATUS_REFUSE = 1;
    //申请权限拒绝，并且是永久，不会再弹窗
    public static final int STATUS_REFUSE_PERMANENT = 2;
    //默认未请求授权状态
    public static final int STATUS_DEFAULT = 3;

    public static final HashMap<String, String> PERMISSION_MAP = new HashMap<>();

    static {
        PERMISSION_MAP.put(PERMISSION_CAMERA, "拍摄照片和录制视频");
        PERMISSION_MAP.put(PERMISSION_WRITE_STORAGE, "访问您设备上的照片、媒体内容和文件");
        PERMISSION_MAP.put(PERMISSION_READ_CONTACTS, "读取联系人");
        PERMISSION_MAP.put(PERMISSION_WRITE_CONTACTS, "读取联系人");
    }

    public static String[] unGetPermissions(Context context, String... permissions) {
        int size = permissions.length;
        ArrayList<String> list = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                list.add(permission);
            }
        }
        String[] listArray = new String[list.size()];
        list.toArray(listArray);
        return listArray;
    }

    /**
     * 获取权限状态
     */
    public static int getAuthorizeStatus(Activity activity, String authorize) {
        int flag = ActivityCompat.checkSelfPermission(activity, authorize);
        if (flag == PackageManager.PERMISSION_GRANTED) {
            return STATUS_SUCCESS;
        }
        boolean isShould = ActivityCompat.shouldShowRequestPermissionRationale(activity, authorize);
        if (isShould) {
            return STATUS_REFUSE;
        }

        if (!PreferenceUtil.checkBaseContains(activity, authorize)) {
            return STATUS_DEFAULT;
        }
        return STATUS_REFUSE_PERMANENT;
    }

    //查询单个权限是否获取到
    public static boolean isPermissionGranted(Context context, String permission) {
        if (TextUtils.isEmpty(permission)) {
            return false;
        }
        int flag = ActivityCompat.checkSelfPermission(context, permission);
        return flag == PackageManager.PERMISSION_GRANTED;
    }

    //请求获取单个权限 - 带自定义提示框
    public static void requestPermissionWithNotice(Activity activity, String permission, ResultCallback callback) {
        if (activity == null || TextUtils.isEmpty(permission)) {
            if (callback != null) {
                callback.onResult(false, null);
            }
            return;
        }

        requestPermissionGroupWithNotice(activity, new String[]{permission}, callback);
    }

    //批量申请权限组 - 带自定义提示框
    public static void requestPermissionGroupWithNotice(Activity activity, String[] permissions, ResultCallback callback) {
        if (activity == null || permissions == null || permissions.length == 0) {
            if (callback != null) {
                callback.onResult(false, null);
            }
            return;
        }

        int permissionStatus = getAuthorizeStatus(activity, permissions[0]);
        switch (permissionStatus) {
            case STATUS_SUCCESS:
                if (callback != null) {
                    callback.onResult(true, null);
                }
                break;
            case STATUS_DEFAULT:
                PermissionActivity.request(activity, permissions, new PermissionActivity.PermissionCallback() {
                    @Override
                    public void onPermissionGranted() {
                        if (callback != null) {
                            callback.onResult(true, null);
                        }
                    }

                    @Override
                    public void shouldShowRational(String permission) {
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    }

                    @Override
                    public void onPermissionReject(String permission) {
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    }
                });
                break;
            case STATUS_REFUSE:
            case STATUS_REFUSE_PERMANENT:
                EulixDialogUtil.showPermissionAlertDialog(activity, permissions[0], new PermissionAlertDialog.OnClickListener() {
                    @Override
                    public void onClickToSet() {
                        if (permissionStatus == STATUS_REFUSE) {
                            PermissionActivity.request(activity, permissions, new PermissionActivity.PermissionCallback() {
                                @Override
                                public void onPermissionGranted() {
                                    if (callback != null) {
                                        callback.onResult(true, null);
                                    }
                                }

                                @Override
                                public void shouldShowRational(String permission) {
                                    if (callback != null) {
                                        callback.onResult(false, null);
                                    }
                                }

                                @Override
                                public void onPermissionReject(String permission) {
                                    if (callback != null) {
                                        callback.onResult(false, null);
                                    }
                                }
                            });
                        } else {
                            //跳转权限管理页
                            Intent permissionManageIntent = new Intent(activity, EulixPermissionManagerActivity.class);
                            activity.startActivity(permissionManageIntent);
                            if (callback != null) {
                                callback.onResult(false, null);
                            }
                        }
                    }

                    @Override
                    public void onClickExit() {
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    }
                });
                break;
            default:
                if (callback != null) {
                    callback.onResult(false, null);
                }

        }
    }

    //批量申请权限组 - 带自定义提示框
    public static void requestPermissionGroupWithNotice(Activity activity, int requestCode, String[] permissions, ResultWithNullCallback callback) {
        if (activity == null || permissions == null || permissions.length == 0) {
            if (callback != null) {
                callback.onResult(false, null);
            }
            return;
        }

        int permissionStatus = getAuthorizeStatus(activity, permissions[0]);
        switch (permissionStatus) {
            case STATUS_SUCCESS:
                if (callback != null) {
                    callback.onResult(true, null);
                }
                break;
            case STATUS_DEFAULT:
                PermissionActivity.request(activity, permissions, new PermissionActivity.PermissionCallback() {
                    @Override
                    public void onPermissionGranted() {
                        if (callback != null) {
                            callback.onResult(true, null);
                        }
                    }

                    @Override
                    public void shouldShowRational(String permission) {
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    }

                    @Override
                    public void onPermissionReject(String permission) {
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    }
                });
                break;
            case STATUS_REFUSE:
            case STATUS_REFUSE_PERMANENT:
                EulixDialogUtil.showPermissionAlertDialog(activity, permissions[0], new PermissionAlertDialog.OnClickListener() {
                    @Override
                    public void onClickToSet() {
                        if (permissionStatus == STATUS_REFUSE) {
                            PermissionActivity.request(activity, permissions, new PermissionActivity.PermissionCallback() {
                                @Override
                                public void onPermissionGranted() {
                                    if (callback != null) {
                                        callback.onResult(true, null);
                                    }
                                }

                                @Override
                                public void shouldShowRational(String permission) {
                                    if (callback != null) {
                                        callback.onResult(false, null);
                                    }
                                }

                                @Override
                                public void onPermissionReject(String permission) {
                                    if (callback != null) {
                                        callback.onResult(false, null);
                                    }
                                }
                            });
                        } else {
                            //跳转权限管理页
                            Intent permissionManageIntent = new Intent(activity, EulixPermissionManagerActivity.class);
                            permissionManageIntent.putExtra(ConstantField.FOR_RESULT, true);
                            activity.startActivityForResult(permissionManageIntent, requestCode);
                            if (callback != null) {
                                callback.onResult(null, null);
                            }
                        }
                    }

                    @Override
                    public void onClickExit() {
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    }
                });
                break;
            default:
                if (callback != null) {
                    callback.onResult(false, null);
                }

        }
    }
}
