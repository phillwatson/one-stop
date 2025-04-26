package com.hillayes.nordigen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillayes.commons.json.MapperFactory;
import com.hillayes.nordigen.model.*;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextFloat;
import static org.apache.commons.lang3.RandomUtils.nextInt;

public class TestData {
    /**
     * A shared object mapper for when a test needs to serialize/deserialize objects.
     */
    private final static ObjectMapper objectMapper = MapperFactory.defaultMapper();

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static InstitutionDetail mockInstitution() {
        InstitutionDetail institution = new InstitutionDetail();
        institution.id = UUID.randomUUID().toString();
        institution.name = randomAlphanumeric(30);
        institution.bic = randomAlphanumeric(10);
        institution.countries = List.of("GB", "FR");
        institution.logo = "https://sandboxfinance.com/logo.png";
        return institution;
    }

    public static AccountSummary mockAccountSummary(String institutionId) {
        return AccountSummary.builder()
            .id(UUID.randomUUID().toString())
            .institutionId(institutionId)
            .iban(randomAlphanumeric(12))
            .ownerName(randomAlphanumeric(22))
            .status(AccountStatus.READY)
            .created(OffsetDateTime.now().minusDays(10))
            .lastAccessed(OffsetDateTime.now().minusDays(2))
            .build();
    }

    public static Requisition mockRequisition(EndUserAgreement agreement, AccountSummary ... accountSummaries) {
        return Requisition.builder()
            .id(UUID.randomUUID().toString())
            .institutionId(agreement.institutionId)
            .agreement(agreement.id)
            .accounts(Arrays.stream(accountSummaries).map(a -> a.id).toList())
            .build();
    }

    public static Balance mockBalance() {
        return Balance.builder()
            .balanceAmount(CurrencyAmount.builder().amount(nextFloat()).currency("GBP").build())
            .referenceDate(LocalDate.now().minusDays(nextInt()))
            .balanceType(randomAlphanumeric(5))
            .build();
    }

    public static TransactionList mockTransactionList(int bookedCount, int pendingCount) {
        return TransactionList.builder()
            .booked(transactionDetailList(bookedCount))
            .pending(transactionDetailList(pendingCount))
            .build();
    }

    private static List<TransactionDetail> transactionDetailList(int count) {
        List<TransactionDetail> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(mockTransactionDetail());
        }
        return result;
    }

    public static TransactionDetail mockTransactionDetail() {
        return TransactionDetail.builder()
            .bookingDate(LocalDate.now().minusDays(nextInt()))
            .internalTransactionId(UUID.randomUUID().toString())
            .transactionId(UUID.randomUUID().toString())
            .transactionAmount(CurrencyAmount.builder().amount(nextFloat()).currency("GBP").build())
            .build();
    }
}
