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

package xyz.eulix.space.network.register;

import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * date: 2021/6/17 9:25
 */
public class RegisterDeviceManager {
    private static final String TAG = RegisterDeviceManager.class.getSimpleName();
    private static Retrofit retrofit;

    public static void registerDevice(RegisterDeviceRequestBody requestBody, final IRegisterDeviceCallback callback) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(ConstantField.URL.BASE_SERVER_URL_RELEASE)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new Gson().toJson(requestBody));

        RegisterDeviceService service = retrofit.create(RegisterDeviceService.class);
        Observable<RegisterDeviceResponseBody> observable = service.registerDevice(body);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RegisterDeviceResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(RegisterDeviceResponseBody registerDeviceResponseBody) {
                        Logger.i(TAG, "on next: " + (registerDeviceResponseBody == null ? "null" : registerDeviceResponseBody.toString()));
                        if (callback != null) {
                            callback.onResult(registerDeviceResponseBody);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e(TAG, "on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "on complete");
                    }
                });
    }
}
