package com.hillayes.nordigen.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.hillayes.commons.json.MapperFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InstitutionTest {
    private final ObjectReader reader = MapperFactory.readerFor(Institution.class);

    @Test
    public void testSerialisationWithoutConsentRenewal() throws JsonProcessingException {
        String json = """
            {
                "id": "ING_WB_INGBNL2A",
                "name": "ING Wholesale Banking",
                "bic": "INGBNL2AXXX",
                "transaction_total_days": "540",
                "countries": [ "IT", "BE", "SK", "FR", "RO" ],
                "logo": "https://storage.googleapis.com/gc-prd-institution_icons-production/DE/PNG/ing.png",
                "max_access_valid_for_days": "180",
                "supported_features": [
                    "account_selection",
                    "business_accounts",
                    "corporate_accounts",
                    "pending_transactions",
                    "private_accounts"
                ],
                "identification_codes": []
            }
            """;

        Institution institution = reader.readValue(json);

        assertNotNull(institution);
        assertEquals("ING_WB_INGBNL2A", institution.id);
        assertEquals("ING Wholesale Banking", institution.name);
        assertEquals("INGBNL2AXXX", institution.bic);
        assertEquals(540, institution.transactionTotalDays);
        assertEquals(5, institution.countries.size());
        assertEquals("https://storage.googleapis.com/gc-prd-institution_icons-production/DE/PNG/ing.png", institution.logo);
        assertEquals(180, institution.maxAccessValidForDays);
        assertEquals(0, institution.maxAccessValidForDaysReconfirmation);
        assertEquals(5, institution.supportedFeatures.size());
        assertEquals(0, institution.identificationCodes.size());
    }

    @Test
    public void testSerialisationWithConsentRenewal() throws JsonProcessingException {
        String json = """
            {
                "id": "WISE_TRWIGB22",
                "name": "Wise",
                "bic": "TRWIGB22XXX",
                "transaction_total_days": "730",
                "countries": [
                    "GB"
                ],
                "logo": "https://storage.googleapis.com/gc-prd-institution_icons-production/UK/PNG/wise.png",
                "max_access_valid_for_days": "90",
                "max_access_valid_for_days_reconfirmation": "730",
                "supported_features": [
                    "account_selection",
                    "business_accounts",
                    "card_accounts",
                    "funds_confirmation",
                    "pending_transactions",
                    "private_accounts",
                    "reconfirmation_of_consent",
                    "submit_payment"
                ],
                "identification_codes": []
            }
            """;

        Institution institution = reader.readValue(json);

        assertNotNull(institution);
        assertEquals("WISE_TRWIGB22", institution.id);
        assertEquals("Wise", institution.name);
        assertEquals("TRWIGB22XXX", institution.bic);
        assertEquals(730, institution.transactionTotalDays);
        assertEquals(1, institution.countries.size());
        assertTrue(institution.countries.contains("GB"));
        assertEquals("https://storage.googleapis.com/gc-prd-institution_icons-production/UK/PNG/wise.png", institution.logo);
        assertEquals(90, institution.maxAccessValidForDays);
        assertEquals(730, institution.maxAccessValidForDaysReconfirmation);
        assertEquals(8, institution.supportedFeatures.size());
        assertTrue(institution.supportedFeatures.contains(Institution.RENEWAL_SUPPORTED));
        assertEquals(0, institution.identificationCodes.size());
    }
}
