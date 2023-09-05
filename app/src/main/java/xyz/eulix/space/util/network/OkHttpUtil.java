package xyz.eulix.space.util.network;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;

public class OkHttpUtil {
    private static ConnectionPool mShortConnectionPool;
    private static ConnectionPool mLongConnectionPool;
    private static String clientUUID;

    static {
        generateShortConnectionPool();
        generateLongConnectionPool();
    }

    private static void getClientUUID() {
        if (clientUUID == null) {
            clientUUID = DataUtil.getClientUuid(EulixSpaceApplication.getContext());
        }
    }

    @NonNull
    private static ConnectionPool generateShortConnectionPool() {
        if (mShortConnectionPool == null) {
            mShortConnectionPool = new ConnectionPool(2, 10, TimeUnit.SECONDS);
        }
        return mShortConnectionPool;
    }

    @NonNull
    private static ConnectionPool generateLongConnectionPool() {
        if (mLongConnectionPool == null) {
            mLongConnectionPool = new ConnectionPool(5, 5, TimeUnit.MINUTES);
        }
        return mLongConnectionPool;
    }

    /**
     * 生成自带ConnectionPool的OKHttpClient
     * @param isShortConnection client所用参数易变用TRUE（短连接），否则FALSE（长连接）
     * @return
     */
    @NonNull
    public static OkHttpClient generateOkHttpClient(boolean isShortConnection) {
        return new OkHttpClient.Builder()
                .connectionPool((isShortConnection ? generateShortConnectionPool() : generateLongConnectionPool()))
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
                        // Do nothing
                    }

                    @NotNull
                    @Override
                    public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
                        List<Cookie> cookies = new ArrayList<>();
                        getClientUUID();
                        if (clientUUID != null) {
                            Cookie cookie = new Cookie.Builder()
                                    .name(ConstantField.CookieHeader.CookieName.CLIENT_UUID)
                                    .value(clientUUID)
                                    .domain(httpUrl.host())
                                    .build();
                            cookies.add(cookie);
                        }
                        return cookies;
                    }
                })
                .build();
    }
}
