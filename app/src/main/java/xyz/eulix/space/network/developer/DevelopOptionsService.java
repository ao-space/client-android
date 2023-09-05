package xyz.eulix.space.network.developer;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 15:19
 */
public interface DevelopOptionsService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.ADMINISTRATOR_DEVELOP_OPTIONS_SWITCH_API)
    Observable<GetDevelopOptionsSwitchResponse> getDevelopOptionsSwitch(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.ADMINISTRATOR_DEVELOP_OPTIONS_SWITCH_API)
    Observable<PostDevelopOptionsSwitchResponse> postDevelopOptionsSwitch(@Header("Request-Id") String requestId, @Body DevelopOptionsSwitchInfo request);
}
