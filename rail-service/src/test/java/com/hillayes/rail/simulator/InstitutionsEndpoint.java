package com.hillayes.rail.simulator;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Singleton;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.hillayes.rail.utils.TestData.toJson;

@Singleton
@Slf4j
public class InstitutionsEndpoint extends AbstractResponseTransformer {
    public InstitutionsEndpoint() {
        super(null);
    }

    public void register(WireMockServer wireMockServer) {
        // mock list endpoint
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v2/institutions/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(InstitutionsEndpoint.class.getSimpleName())
                    .withTransformerParameter("list", true)
            )
        );

        // mock get endpoint
        wireMockServer.stubFor(get(urlPathMatching("/api/v2/institutions/(.*)/"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withTransformers(InstitutionsEndpoint.class.getSimpleName())
            )
        );
    }

    @Override
    public ResponseDefinition transform(Request request,
                                        ResponseDefinition responseDefinition,
                                        FileSource files,
                                        Parameters parameters) {
        RequestMethod method = request.getMethod();

        if (method.equals(RequestMethod.GET)) {
            if (parameters.getBoolean("list", false)) {
                return list(request, responseDefinition);
            }
            return getById(request, responseDefinition);
        }

        return new ResponseDefinitionBuilder()
            .withStatus(400)
            .withBody(toJson(Map.of(
                "status_code", 400,
                "summary", "Unsupported method",
                "detail", "This endpoint does not support the " + method + " method."
            )))
            .build();
    }

    private ResponseDefinition list(Request request,
                                    ResponseDefinition responseDefinition) {
        String filename = getQueryBoolean(request, "payments_enabled")
            .filter(b -> b)
            .map(b -> "institutions-payments-enabled.json")
            .orElse("institutions-payments-disabled.json");

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBodyFile(filename)
            .build();
    }

    private ResponseDefinition getById(Request request,
                                       ResponseDefinition responseDefinition) {
        String id = getIdFromPath(request.getUrl(), 4);
        String entity = DEFINITIONS.get(id);
        if (entity == null) {
            return ResponseDefinitionBuilder.like(responseDefinition)
                .withBody(toJson(Map.of(
                    "summary", "Not found",
                    "detail", "End User Agreement " + id + " not found"
                )))
                .build();
        }

        return ResponseDefinitionBuilder.like(responseDefinition)
            .withStatus(200)
            .withBody(entity)
            .build();
    }
    public final static Map<String,String> DEFINITIONS = Map.of(
        "SANDBOXFINANCE_SFIN0000", """
        {
            "id": "SANDBOXFINANCE_SFIN0000",
            "name": "Sandbox Finance",
            "bic": "SFIN0000",
            "transaction_total_days": "90",
            "countries": [
                "XX"
            ],
            "logo": "https://cdn.nordigen.com/ais/SANDBOXFINANCE_SFIN0000.png",
            "supported_payments": {},
            "supported_features": []
        }
        """,

        "FIRST_DIRECT_MIDLGB22", """
        {
            "id": "FIRST_DIRECT_MIDLGB22",
            "name": "First Direct",
            "bic": "MIDLGB22",
            "transaction_total_days": "730",
            "countries": [
                "GB"
            ],
            "logo": "https://cdn.nordigen.com/ais/FIRST_DIRECT_MIDLGB22.png",
            "supported_payments": {
                "single-payment": [
                    "FPS"
                ]
            },
            "supported_features": [
                "access_scopes",
                "business_accounts",
                "card_accounts",
                "corporate_accounts",
                "payments",
                "pending_transactions",
                "private_accounts",
                "submit_payment"
            ]
        }"""
    );
}
