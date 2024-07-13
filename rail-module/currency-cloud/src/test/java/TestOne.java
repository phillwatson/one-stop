import com.currencycloud.client.CurrencyCloudClient;
import com.currencycloud.client.exception.AuthenticationException;
import com.currencycloud.client.exception.BadRequestException;
import com.currencycloud.client.exception.TooManyRequestsException;
import com.currencycloud.client.model.*;
import com.hillayes.commons.backoff.BackoffStrategy;
import com.hillayes.commons.backoff.ExponentialBackoffStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.apache.commons.lang3.RandomStringUtils.*;
import static org.awaitility.Awaitility.await;

public class TestOne {
    private static CurrencyCloudClient client;
    private static BackoffStrategy<CurrencyCloudClient> withAuthBackoff;

    @BeforeAll
    public static void beforeAll() {
        client = getClient();
        client.authenticate();

        withAuthBackoff = getBackoffStrategy(client);
    }

    @AfterAll
    public static void afterAll() {
        client.endSession();
    }

    @Test
    public void testCurrencies() {
        withAuthBackoff.accept(client -> {
            Date tomorrow = Date.from(Instant.now().plus(Duration.ofDays(1)));
            client.currencies().forEach(currency -> {
                try {
                    DetailedRate detailedRate = client.detailedRates(
                        currency.getCode(),
                        "GBP",
                        "buy", BigDecimal.valueOf(10.00),
                        tomorrow, null);

                    System.out.println(detailedRate);
                } catch (BadRequestException e) {
                    System.out.println("Error: " + currency + ": " + e.getMessage());
                }
            });
        });
    }

    @Test
    public void testConvert() {
        withAuthBackoff.accept(client -> {
            DetailedRate detailedRate = client.detailedRates(
                "EUR", "GBP",
                "buy", BigDecimal.valueOf(10.00),
                null, null);

            Conversion conversion = client.createConversion(Conversion.create(
                detailedRate.getClientBuyCurrency(),
                detailedRate.getClientSellCurrency(),
                detailedRate.getFixedSide(),
                detailedRate.getClientBuyAmount(),
                true)
            );
            System.out.println(conversion);

            await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(200)).until(() ->
                client.retrieveConversion(conversion.getId()) != null
            );

            ConversionCancellation cancellation = client.cancelConversion(
                ConversionCancellation.create(conversion.getId()));
            System.out.println(cancellation);
        });
    }

    @Test
    public void testContact() {
        withAuthBackoff.accept(client -> {
            // create a sub-account
            Account account = client.createAccount(Account.create(
                randomAlphanumeric(20),
                "company",
                randomAlphanumeric(20),
                randomAlphanumeric(20),
                randomAlphanumeric(8),
                "GB"));
            System.out.println(account);

            // add a contact to the sub-account
            Contact contact = client.createContact(Contact.create(
                account.getId(),
                randomAlphanumeric(20),
                randomAlphanumeric(20),
                randomAlphanumeric(20) + "@gmail.com",
                randomNumeric(20)));
            System.out.println(contact);

            // list all accounts - one page only
            Accounts accounts = client.findAccounts(null, Pagination.builder().limit(10).build());
            accounts.getAccounts().forEach(acc -> {
                System.out.println(acc);

                // and their contacts - one page only
                Contact criteria = Contact.create();
                criteria.setAccountId(acc.getId());
                client.findContacts(criteria, Pagination.builder().limit(10).build())
                    .getContacts().forEach(System.out::println);
            });
        });
    }

    private static CurrencyCloudClient getClient() {
        return new CurrencyCloudClient(
            CurrencyCloudClient.Environment.demo,
            "",
            "",
            CurrencyCloudClient.HttpClientConfiguration.builder()
                .httpConnTimeout(3000)
                .httpReadTimeout(45000)
                .build()
        );
    }

    private static BackoffStrategy<CurrencyCloudClient> getBackoffStrategy(final CurrencyCloudClient client) {
        return new ExponentialBackoffStrategy(client, 3)
            .setExceptionHandler((exception, retryCount) -> {
                // if authentication has expired
                if (exception instanceof AuthenticationException) {
                    // re-authenticate and try again without delay
                    client.authenticate();
                    return true;
                }

                // if too many requests
                if (exception instanceof TooManyRequestsException) {
                    // retry again with delay
                    return false;
                }

                // throw any other exception to stop retries
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException) exception;
                }
                throw new RuntimeException(exception);
            });
    }
}
