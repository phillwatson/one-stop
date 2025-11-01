package com.hillayes.integration.api.share;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.PaginatedSharePrices;
import com.hillayes.onestop.api.RegisterShareIndexRequest;
import com.hillayes.onestop.api.ShareIndexResponse;
import io.restassured.common.mapper.TypeRef;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;

public class ShareIndexApi extends ApiBase {
    private static final TypeRef<List<ShareIndexResponse>> SHARE_INDEX_LIST = new TypeRef<>() {};
    public ShareIndexApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public List<ShareIndexResponse>  registerShareIndices(List<RegisterShareIndexRequest> request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .post("/api/v1/shares/indices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(SHARE_INDEX_LIST);
    }

    public PaginatedSharePrices getAllShareIndices(int pageIndex, int pageSize) {
        return givenAuth()
            .queryParam("page", pageIndex)
            .queryParam("page-size", pageSize)
            .get("/api/v1/shares/indices")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(PaginatedSharePrices.class);
    }
}
