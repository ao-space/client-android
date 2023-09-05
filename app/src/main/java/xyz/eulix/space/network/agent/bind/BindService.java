package xyz.eulix.space.network.agent.bind;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.network.EulixBaseRequest;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.AgentBaseResponse;
import xyz.eulix.space.util.ConstantField;

public interface BindService {
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BIND_COMMUNICATION_START_API)
    Observable<EulixBaseResponse> bindCommunicationStart();

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.BIND_COMMUNICATION_PROGRESS_API)
    Observable<AgentBaseResponse> getBindCommunicationProgress();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BIND_SPACE_CREATE_API)
    Observable<AgentBaseResponse> bindSpaceCreate(@Body EulixBaseRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BIND_REVOKE_API)
    Observable<AgentBaseResponse> bindRevoke(@Body EulixBaseRequest body);
}
