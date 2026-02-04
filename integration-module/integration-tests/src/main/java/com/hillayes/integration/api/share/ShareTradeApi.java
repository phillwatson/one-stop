package com.hillayes.integration.api.share;

import com.hillayes.integration.api.ApiBase;
import com.hillayes.onestop.api.ShareTradeRequest;
import com.hillayes.onestop.api.ShareTradeResponse;

import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;

public class ShareTradeApi extends ApiBase {
    public ShareTradeApi(Map<String, String> authCookies) {
        super(authCookies);
    }

    public ShareTradeResponse getShareTrade(UUID shareTradeId) {
        return givenAuth()
            .get("/api/v1/shares/trades/{shareTradeId}", shareTradeId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(ShareTradeResponse.class);
    }

    public ShareTradeResponse updateShareTrade(UUID shareTradeId,
                                               ShareTradeRequest request) {
        return givenAuth()
            .contentType(JSON)
            .body(request)
            .put("/api/v1/shares/trades/{shareTradeId}", shareTradeId)
            .then()
            .statusCode(200)
            .contentType(JSON)
            .extract().as(ShareTradeResponse.class);
    }

    public void deleteShareTrade(UUID shareTradeId) {
        givenAuth()
            .delete("/api/v1/shares/trades/{shareTradeId}", shareTradeId)
            .then()
            .statusCode(204);
    }
}
