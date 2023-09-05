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

import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.presenter.UnbindVerifyCheckPresenter;

public class UnbindVerifyCheckActivity extends AbsActivity<UnbindVerifyCheckPresenter.IUnbindVerifyCheck, UnbindVerifyCheckPresenter> implements UnbindVerifyCheckPresenter.IUnbindVerifyCheck {
    private VerificationCodeInput verifyInput;
    @Override
    public void initView() {
        setContentView(R.layout.activity_unbind_verify_check);
        verifyInput = findViewById(R.id.verify_input);
    }

    @Override
    public void initData() {

    }

    @Override
    public void initViewData() {

    }

    @Override
    public void initEvent() {
        verifyInput.setOnCompleteListener(new VerificationCodeInput.CompleteListener() {
            @Override
            public void onCompleted(String string) {
                Toast.makeText(getApplicationContext(),string,Toast.LENGTH_SHORT).show();
            }
        });

    }

    @NotNull
    @Override
    public UnbindVerifyCheckPresenter createPresenter() {
        return new UnbindVerifyCheckPresenter();
    }
}