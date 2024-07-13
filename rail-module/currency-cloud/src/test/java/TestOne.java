import com.currencycloud.client.CurrencyCloudClient;
import com.currencycloud.client.backoff.BackOff;
import com.currencycloud.client.backoff.BackOffResult;
import com.currencycloud.client.exception.AuthenticationException;
import com.currencycloud.client.model.Currency;

import java.util.List;

public class TestOne {
    public static CurrencyCloudClient getClient() {
        CurrencyCloudClient client = new CurrencyCloudClient(
            CurrencyCloudClient.Environment.demo,
            "",
            "",
            CurrencyCloudClient.HttpClientConfiguration.builder()
                .httpConnTimeout(3000)
                .httpReadTimeout(45000)
                .build()
        );

        return client;
    }

    public static void main(String[] args) {
        CurrencyCloudClient client = getClient();
        try {
            client.authenticate();
            System.out.println(client);

            try {
                client.authenticate();
                System.out.println("Authenticated");
            } catch (Exception e) {
                System.out.println("Failed to authenticate: " + e.getMessage());
            }

            BackOffResult<List<Currency>> result = BackOff.<List<Currency>>builder()
                .withTask(() -> client.currencies())
                .withExceptionHandler(exception -> {
                    if (exception instanceof AuthenticationException) {
                        client.authenticate();
                    }
                })
                .execute();

        } finally {
            client.endSession();
        }
    }
}
