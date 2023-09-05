package xyz.eulix.space.network.developer;

import android.text.TextUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:38
 */
public class DevelopOptionsManager {
    private static final String TAG = DevelopOptionsManager.class.getSimpleName();

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String ivParams, String apiVersion, long timeout, TimeUnit unit) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .connectTimeout(timeout, unit)
                .readTimeout(timeout, unit)
                .writeTimeout(timeout, unit)
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("application/json; charset=utf-8"), apiVersion))
                .build();
    }

    private static String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
            } else {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    private static Retrofit generateRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static Retrofit generateRetrofit(String baseUrl, OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    public static void getDevelopOptionsSwitch(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IGetDevelopOptionsSwitchCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.ADMINISTRATOR_GET_DEVELOP_OPTIONS_SWITCH, accessToken, secret, ivParams, apiVersion, 2, TimeUnit.MINUTES);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DevelopOptionsService service = retrofit.create(DevelopOptionsService.class);
        Observable<GetDevelopOptionsSwitchResponse> observable = service.getDevelopOptionsSwitch(uuid.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GetDevelopOptionsSwitchResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(GetDevelopOptionsSwitchResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResponse(response);
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

    public static void postDevelopOptionsSwitch(DevelopOptionsSwitchInfo request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IPostDevelopOptionsSwitchCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.ADMINISTRATOR_UPDATE_DEVELOP_OPTIONS_SWITCH, accessToken, secret, ivParams, apiVersion, 2, TimeUnit.MINUTES);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DevelopOptionsService service = retrofit.create(DevelopOptionsService.class);
        Observable<PostDevelopOptionsSwitchResponse> observable = service.postDevelopOptionsSwitch(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<PostDevelopOptionsSwitchResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(PostDevelopOptionsSwitchResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResponse(response);
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
