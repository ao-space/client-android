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

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.network.agent.InitialCallback;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.network.gateway.VersionCompatibleCallback;
import xyz.eulix.space.network.gateway.VersionCompatibleResponseBody;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description: 引导页presenter
 * History:     2021/7/16
 */
public class LaunchPresenter extends AbsPresenter<LaunchPresenter.ILauncher> {
    private static final String TAG = LaunchPresenter.class.getSimpleName();
    private Long apkSize = null;
    private String downloadUrl = "";
    private String md5 = "";
    private String newestVersion = "";
    private String updateDescription = "";

    private InitialCallback initialCallback = new InitialCallback() {
        @Override
        public void onSuccess(String message, Integer code, Integer result) {
            if (iView != null) {
                iView.eulixSpaceInitialized(code);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.eulixSpaceInitialized(null);
            }
        }

        @Override
        public void onError(String msg, String url, String bleKey, String bleIv) {
            if (iView != null) {
                iView.eulixSpaceInitialized(null);
            }
        }

        @Override
        public void onError(String msg, String url, String password, String bleKey, String bleIv) {
            if (iView != null) {
                iView.eulixSpaceInitialized(null);
            }
        }
    };

    private VersionCompatibleCallback versionCompatibleCallback = new VersionCompatibleCallback() {
        @Override
        public void onSuccess(boolean isAppForceUpdate, boolean isBoxForceUpdate, VersionCompatibleResponseBody.Results.LatestPkg latestAppPkg, VersionCompatibleResponseBody.Results.LatestPkg latestBoxPkg) {
            apkSize = null;
            downloadUrl = null;
            md5 = null;
            newestVersion = null;
            updateDescription = null;
            if (latestAppPkg != null) {
                apkSize = latestAppPkg.pkgSize;
                downloadUrl = latestAppPkg.downloadUrl;
                md5 = latestAppPkg.md5;
                newestVersion = latestAppPkg.pkgVersion;
                updateDescription = latestAppPkg.updateDesc;
            }
            if (iView != null) {
                iView.versionCheckResultCallback(isAppForceUpdate, isBoxForceUpdate, latestAppPkg, latestBoxPkg);
            }
        }

        @Override
        public void onFail() {
            if (iView != null) {
                iView.versionCheckResultCallback(false, false, null, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.versionCheckResultCallback(false, false, null, null);
            }
        }
    };

    public interface ILauncher extends IBaseView{
        void fileInitialized();
        void eulixSpaceInitialized(Integer result);
        void versionCheckResultCallback(boolean isAppForceUpdate, boolean isBoxForceUpdate, VersionCompatibleResponseBody.Results.LatestPkg latestAppPkg, VersionCompatibleResponseBody.Results.LatestPkg latestBoxPkg);
    }

    @Deprecated
    public void initEulixSpace() {
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        String boxDomainValue = null;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                    boxDomainValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                    break;
                }
            }
        }
        if (boxDomainValue != null) {
            String boxDomain = boxDomainValue;
            ThreadPool.getInstance().execute(() -> AgentUtil.initial(boxDomain, null, null, initialCallback));
        } else {
            if (iView != null) {
                iView.eulixSpaceInitialized(null);
            }
        }
    }

    public void checkUpdate() {
        GatewayUtil.checkVersionCompatibleImportant(context, false, versionCompatibleCallback);
    }

    public String getApkDownloadPath() {
        return DataUtil.getApkDownloadPath(context);
    }

    public boolean setApkDownloadPath(String apkDownloadPath) {
        return DataUtil.setApkDownloadPath(context, apkDownloadPath, true);
    }

    public Boolean isAdmin() {
        Boolean isAdmin = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    if ("1".equals(bindValue)) {
                        isAdmin = true;
                    } else if ("-1".equals(bindValue)) {
                        isAdmin = false;
                    }
                }
            }
        }
        return isAdmin;
    }

    public void initFile(boolean externalStorageEnable) {
        if (context != null) {
            String fileUUID = null;
            String fileRSA2048PublicKey = null;
            String fileRSA2048PrivateKey = null;
            String spUUID = null;
            String spRSA2048PublicKey = null;
            String spRSA2048PrivateKey = null;
            EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context.getApplicationContext());
            if (eulixSpaceSharePreferenceHelper != null) {
                if (eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)) {
                    spUUID = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID);
                    if (StringUtil.stringToUUID(spUUID) == null) {
                        spUUID = null;
                    }
                    Logger.d(TAG, "read uuid: " + spUUID);
                }
                if (eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                        && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
                    spRSA2048PublicKey = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY);
                    spRSA2048PrivateKey = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY);
                    Logger.d(TAG, "read rsa 2048 public key: " + spRSA2048PublicKey + ", private key: " + spRSA2048PrivateKey);
                    if (!(spRSA2048PublicKey != null && spRSA2048PrivateKey != null
                            && spRSA2048PublicKey.length() == ConstantField.Algorithm.RSA_2048_PUBLIC_LENGTH
                            && EncryptionUtil.checkKey(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                            , null, spRSA2048PublicKey, spRSA2048PrivateKey, null))) {
                        spRSA2048PublicKey = null;
                        spRSA2048PrivateKey = null;
                    }
                }
            }
            boolean isWrite = false;
            if (fileUUID == null) {
                isWrite = true;
                if (spUUID == null) {
                    String uuid = UUID.randomUUID().toString();
                    if (eulixSpaceSharePreferenceHelper != null) {
                        eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.UUID, uuid, false);
                    }
                    Logger.i(TAG, "write uuid: " + uuid);
                    fileUUID = uuid;
                } else {
                    fileUUID = spUUID;
                }
            } else {
                if (spUUID == null) {
                    if (eulixSpaceSharePreferenceHelper != null) {
                        eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.UUID, fileUUID, false);
                    }
                    Logger.i(TAG, "copy uuid: " + fileUUID);
                } else {
                    if (!spUUID.equals(fileUUID)) {
                        isWrite = true;
                        fileUUID = spUUID;
                    }
                }
            }
            if (fileRSA2048PublicKey != null && fileRSA2048PrivateKey != null) {
                if (spRSA2048PublicKey != null && spRSA2048PrivateKey != null) {
                    if (!spRSA2048PublicKey.equals(fileRSA2048PublicKey) || !spRSA2048PrivateKey.equals(fileRSA2048PrivateKey)) {
                        isWrite = true;
                        fileRSA2048PublicKey = spRSA2048PublicKey;
                        fileRSA2048PrivateKey = spRSA2048PrivateKey;
                    }
                } else {
                    if (eulixSpaceSharePreferenceHelper != null) {
                        eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY, fileRSA2048PublicKey, false);
                        eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY, fileRSA2048PrivateKey, false);
                        Logger.i(TAG, "copy rsa 2048 public key: " + fileRSA2048PublicKey + ", private key: " + fileRSA2048PrivateKey);
                    }
                }
            } else {
                isWrite = true;
                if (spRSA2048PublicKey != null && spRSA2048PrivateKey != null) {
                    fileRSA2048PublicKey = spRSA2048PublicKey;
                    fileRSA2048PrivateKey = spRSA2048PrivateKey;
                } else {
                    KeyPair keyPair = EncryptionUtil.generateKeyPair(ConstantField.Algorithm.RSA, null, 2048);
                    if (keyPair != null) {
                        String publicKey = StringUtil.byteArrayToString(keyPair.getPublic().getEncoded());
                        String privateKey = StringUtil.byteArrayToString(keyPair.getPrivate().getEncoded());
                        if (eulixSpaceSharePreferenceHelper != null) {
                            eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY, publicKey, false);
                            eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY, privateKey, false);
                            Logger.i(TAG, "write rsa 2048 public key: " + publicKey + ", private key: " + privateKey);
                        }
                        fileRSA2048PublicKey = publicKey;
                        fileRSA2048PrivateKey = privateKey;
                    }
                }
            }
        }
        if (iView != null) {
            iView.fileInitialized();
        }
    }

    public Long getApkSize() {
        return apkSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getMd5() {
        return md5;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public String getUpdateDescription() {
        return updateDescription;
    }
}
