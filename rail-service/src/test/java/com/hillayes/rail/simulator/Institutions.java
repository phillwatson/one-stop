package com.hillayes.rail.simulator;

import java.util.Map;

/**
 * Some selected Nordigen API institutions data for testing.
 */
final class Institutions {
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
