package xyz.eulix.space.did.network;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import xyz.eulix.space.util.ConstantField;

public interface DIDService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DID_DOCUMENT_API)
    Observable<DIDDocumentResponse> getDIDDocument(@Header("Request-Id") String requestId, @Query("aoId") String aoId, @Query("did") String did);
}
