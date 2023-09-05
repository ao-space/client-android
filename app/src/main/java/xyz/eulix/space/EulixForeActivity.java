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

package xyz.eulix.space;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.SystemUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/6 13:58
 */
public class EulixForeActivity extends AppCompatActivity {
    private static final String TAG = EulixForeActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_transparent);
        if (!handleIntent(getIntent()) && !SystemUtil.isApplicationLaunch(TAG)) {
            Intent launcherIntent = null;
            PackageManager packageManager = getPackageManager();
            if (packageManager != null) {
                launcherIntent = packageManager.getLaunchIntentForPackage(getPackageName());
            }
            if (launcherIntent == null) {
                launcherIntent = new Intent();
                launcherIntent.setComponent(new ComponentName(getPackageName(), ConstantField.LAUNCHER_CLASS_NAME));
            }
            startActivity(launcherIntent);
        }
        finish();
    }

    private boolean handleIntent(Intent intent) {
        boolean isHandle = false;
        Uri data = intent.getData();
        if (data != null) {
            
        }
        return isHandle;
    }
}
