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

package xyz.eulix.space.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import xyz.eulix.space.util.PreferenceUtil;

public class PermissionActivity extends AppCompatActivity {
    private static PermissionCallback mCallback;
    private static final String KEY_PERMISSION = "permission";
    private static final int RC_REQUEST_PERMISSION = 101;

    public static void request(Context context, String[] permissions, PermissionCallback callback) {
        mCallback = callback;
        Intent intent = new Intent(context, PermissionActivity.class);
        intent.putExtra(KEY_PERMISSION, permissions);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        if (!i.hasExtra(KEY_PERMISSION)) {
            finish();
        } else {
            String[] permissions = i.getStringArrayExtra(KEY_PERMISSION);
            for (int j = 0; j < permissions.length; j++) {
                PreferenceUtil.saveBaseKeyBoolean(PermissionActivity.this, permissions[j], false);
            }
            ActivityCompat.requestPermissions(PermissionActivity.this, permissions, RC_REQUEST_PERMISSION);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_REQUEST_PERMISSION) {
            return;
        }
        // 处理申请结果
        boolean[] shouldShowRequestPermissionRationale = new boolean[permissions.length];
        for (int i = 0; i < shouldShowRequestPermissionRationale.length; i++) {
            shouldShowRequestPermissionRationale[0] = false;
        }

        for (int i = 0; i < permissions.length; i++) {
            shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
        }
        onRequestPermission(permissions, grantResults, shouldShowRequestPermissionRationale);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onRequestPermission(String[] permissions, int[] grantResults, boolean[] shouldShowRequestPermissionRationale) {
        int length = permissions.length;
        int granted = 0;
        for (int i = 0; i < length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                PreferenceUtil.saveBaseKeyBoolean(PermissionActivity.this, permissions[i], false);
                if (shouldShowRequestPermissionRationale[i]) {
                    PreferenceUtil.savePermissionDeny(this, permissions[i], true);
                    if (mCallback != null) {
                        mCallback.shouldShowRational(permissions[i]);
                    }
                } else {
                    if (mCallback != null) {
                        mCallback.onPermissionReject(permissions[i]);
                    }
                }
            } else {
                PreferenceUtil.saveBaseKeyBoolean(PermissionActivity.this, permissions[i], true);
                granted++;
            }
        }
        if (granted == length) {
            if (mCallback != null) {
                mCallback.onPermissionGranted();
            }
        }
        finish();
    }

    public interface PermissionCallback {
        // 同意授权
        void onPermissionGranted();

        // 拒绝：未选择不再提示
        void shouldShowRational(String permission);

        // 拒绝：选择不再提示
        void onPermissionReject(String permission);
    }
}