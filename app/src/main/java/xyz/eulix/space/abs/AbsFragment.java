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

package xyz.eulix.space.abs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.util.ToastManager;

/**
 * Author:      Zhu Fuyu
 * Description: 基础Fragment类
 * History:     2021/7/16
 */
public abstract class AbsFragment<V extends IBaseView, P extends AbsPresenter<V>> extends Fragment implements IBaseView {
    @Nullable
    public View root;
    public P presenter;
    private ToastManager toastManager;

    @androidx.annotation.Nullable
    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initRootView(inflater, container, savedInstanceState);
        this.presenter = createPresenter();
        presenter.attachView((V) this);
        initData();
        return this.root;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @androidx.annotation.Nullable @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(this.root);
        initViewData();
        initEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public abstract void initRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @NotNull
    public abstract P createPresenter();

    public abstract void initData();

    public abstract void initView(@Nullable View root);

    public abstract void initViewData();

    public abstract void initEvent();

    public void showServerExceptionToast() {
        showImageTextToast(R.drawable.toast_refuse, R.string.service_exception_hint);
    }

    public void showPureTextToast(@StringRes int resId) {
        if (toastManager == null) {
            Context context = getActivity();
            toastManager = new ToastManager(context == null ? EulixSpaceApplication.getResumeActivityContext() : context);
        }
        toastManager.showPureTextToast(resId);
    }

    public void showImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId) {
        if (toastManager == null) {
            Context context = getActivity();
            toastManager = new ToastManager(context == null ? EulixSpaceApplication.getResumeActivityContext() : context);
        }
        toastManager.showImageTextToast(drawableResId, stringResId);
    }

    @Override
    public void authenticateResult(boolean isSuccess, String responseId, String requestId) {
        ;
    }

    @Override
    public void authenticateError(int code, CharSequence errMsg, String responseId, String requestId) {

    }
}
