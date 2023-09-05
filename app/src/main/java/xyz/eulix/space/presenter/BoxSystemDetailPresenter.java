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

package xyz.eulix.space.presenter;

import android.text.TextUtils;

import com.google.gson.Gson;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxVersionCheckEvent;
import xyz.eulix.space.event.BoxVersionDetailInfoEvent;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.network.gateway.IVersionCheckCallback;
import xyz.eulix.space.network.gateway.VersionCheckResponseBody;
import xyz.eulix.space.network.upgrade.UpgradeUtils;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 盒子系统规格详情页Presenter
 * History:     2022/7/19
 */
public class BoxSystemDetailPresenter extends AbsPresenter<BoxSystemDetailPresenter.IBoxSystemDetail> {
    public interface IBoxSystemDetail extends IBaseView {
        void onCheckCallback(boolean hasUpdate);

        void onCheckError(String msg);

        void onGetDeviceVersionInfo(boolean result, DeviceVersionInfoBean deviceVersionInfoBean);
    }

    public boolean isActiveUserAdmin() {
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == EulixSpaceDBUtil.getActiveDeviceUserIdentity(context));
    }


    //检查盒子系统版本更新
    public void checkBoxVersion() {
        GatewayUtil.checkVersionBoxOrApp(context, true, new IVersionCheckCallback() {
            @Override
            public void onResult(VersionCheckResponseBody responseBody) {
                if (responseBody != null && responseBody.results != null) {
                    VersionCheckResponseBody.Results results = responseBody.results;
                    Logger.d("zfy", "result newVersionExist:" + results.newVersionExist);

                    if (results.newVersionExist && results.latestBoxPkg != null) {
                        ConstantField.boxVersionCheckBody = results;
                        EventBusUtil.post(new BoxVersionCheckEvent());
                        iView.onCheckCallback(true);
                        return;
                    } else {
                        iView.onCheckCallback(false);
                    }
                } else if (responseBody != null && responseBody.getCodeInt() == ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR){
                    iView.onCheckError(String.valueOf(ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR));
                }
                ConstantField.boxVersionCheckBody = null;
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "checkVersion error:" + msg);
                iView.onCheckError("");
            }
        });
    }

    //获取系统信息详情
    public void getDeviceVersionDetailInfo() {
        UpgradeUtils.getDeviceVersionDetailInfo(context, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                Logger.d("zfy", "getDeviceVersionDetailInfo onResult:" + result + ",extraObj:" + extraObj.toString());
                if (result) {
                    DeviceVersionInfoBean deviceVersionInfoBean = (DeviceVersionInfoBean) extraObj;
                    iView.onGetDeviceVersionInfo(true, deviceVersionInfoBean);
                    Gson gson = new Gson();
                    String jsonStr = gson.toJson(deviceVersionInfoBean);
                    Logger.d("jsonStr=" + jsonStr);
                    if (!TextUtils.isEmpty(jsonStr)) {
                        PreferenceUtil.saveDeviceVersionDetailInfo(context, jsonStr);
                        EventBusUtil.post(new BoxVersionDetailInfoEvent());
                    }
                } else {
                    iView.onGetDeviceVersionInfo(false, null);
                }
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "getDeviceVersionDetailInfo onError " + msg);
                iView.onGetDeviceVersionInfo(false, null);
            }
        });
    }
}
