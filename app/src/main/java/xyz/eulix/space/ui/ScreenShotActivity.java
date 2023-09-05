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

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Locale;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.share.ShareUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/26 10:47
 */
public class ScreenShotActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout screenShotContainer;
    private ImageView screenShotPreview;
    private ImageButton share;
    private String screenShotPath;

    public void initView() {
        setContentView(R.layout.activity_screen_shot);
        screenShotContainer = findViewById(R.id.screen_shot_container);
        screenShotPreview = findViewById(R.id.screen_shot_preview);
        share = findViewById(R.id.share);
    }

    public void initData() {
        LocaleBean localeBean = null;
        String localeValue = DataUtil.getApplicationLocale(this);
        if (localeValue != null) {
            try {
                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        if (localeBean != null) {
            Locale locale = localeBean.parseLocale();
            Resources resources = getResources();
            if (resources != null && locale != null) {
                Configuration configuration = resources.getConfiguration();
                if (configuration != null) {
                    configuration.setLocale(locale);
                    resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                }
            }
        }
        handleIntent(getIntent());
    }

    public void initViewData() {
        showPreview();
    }

    public void initEvent() {
        screenShotContainer.setOnClickListener(this);
        share.setOnClickListener(this);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null && intent.hasExtra(ConstantField.SCREEN_SHOT_PATH)) {
            screenShotPath = intent.getStringExtra(ConstantField.SCREEN_SHOT_PATH);
            DataUtil.setScreenShotPath(screenShotPath);
        }
    }

    private void showPreview() {
        if (screenShotPreview != null && screenShotPath != null && FileUtil.existFile(screenShotPath)) {
            GlideUtil.load(screenShotPath, screenShotPreview);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        initView();
        initViewData();
        initEvent();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.screen_shot_container:
                    finish();
                    break;
                case R.id.share:
                    ShareUtil.shareFile(this, screenShotPath);
                    finish();
                    break;
                default:
                    break;
            }
        }
    }
}
