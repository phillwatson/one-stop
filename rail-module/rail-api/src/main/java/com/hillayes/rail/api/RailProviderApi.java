package com.hillayes.rail.api;

import com.hillayes.rail.api.domain.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Represents the interface made available by all Rail Providers that we intend
 * to support. This is intended to be agnostic of the underlying provider, and
 * implementations will use the Adaptor pattern to map the Rail Provider's
 * functionality to this interface.
 */
public interface RailProviderApi {
    /**
     * Used when we have a collection of RailProviderApi implementations, and we
     * want to select the correct instance based on a given RailProvider value.
     * <p>
     * We inject all instances using the class Instance<RailProviderApi>, and use
     * this method to identify the appropriate instance. For example;
     * <pre>
     *     \@Inject \@Any
     *     jakarta.enterprise.inject.Instance<RailProviderApi> railProviderApis;
     * </pre>
     *
     * @param railProvider the RailProvider value that identifies the implementation.
     * @return true if this instance supports the given RailProvider.
     */
    boolean isFor(RailProvider railProvider);

    /**
     * Returns the identified Institution, or empty if not found.
     * @param id the institution id.
     * @return the institution, or empty if not found.
     */
    public Optional<RailInstitution> getInstitution(String id);

    /**
     * Returns a list of Institutions for the given country code.
     * @param countryCode the country code.
     * @param paymentsEnabled true if only institutions that support payments should be returned.
     * @return the list of institutions, possibly empty.
     */
    public List<RailInstitution> listInstitutions(String countryCode,
                                                  boolean paymentsEnabled);

    /**
     * Registers an agreement with the given institution. This will initiate the process of
     * obtaining the user's authorisation to access their accounts held with the institution.
     * It will return a URL to redirect the user to, in order for them to authorise and complete
     * the registration process.
     *
     * The process is a two phase process, where the user will be redirected to the URL returned
     * in the {@link RailAgreement#getAgreementLink()}. That URL will present the user with a form
     * to enter their credentials for the institution, and authorise our access to their accounts.
     *
     * The rail provider will then call the given callback URI to inform us of whether the user has
     * given or denied the authorisation. The given reference will be returned in that callback,
     * and can be used to correlate the agreement.
     *
     * @param institution the institution for which authorisation is required.
     * @param callbackUri the URI that the rail provider will call to confirm the registration.
     * @param reference a unique reference that will correlate the agreement after authorisation.
     * @return the URI to which the user should be redirected to authorise the registration.
     */
    public RailAgreement register(RailInstitution institution, URI callbackUri, String reference);

    /**
     * Returns the agreement with the given id, or empty if not found.
     * @param id the agreement id.
     * @return the agreement, or empty if not found.
     */
    public Optional<RailAgreement> getAgreement(String id);

    /**
     * Deletes the identified agreement, removing the authorisation to access the associated
     * account details.
     * @param id the agreement id.
     * @return true if the agreement was deleted, false if not found.
     */
    public boolean deleteAgreement(String id);

    /**
     * Returns the account with the given id, or empty if not found.
     * @param id the account id.
     * @return the account, or empty if not found.
     */
    public Optional<RailAccount> getAccount(String id);

    /**
     * Returns a list of account balances for the identified account, started from the given
     * date.
     * @param accountId the account id.
     * @param dateFrom the date from which balances should be returned.
     * @return the list of balances, possibly empty.
     */
    public List<RailBalance> listBalances(String accountId, LocalDate dateFrom);

    /**
     * Returns a list of transactions for the identified account, started from the given
     * date.
     * @param accountId the rail account's unique identifier.
     * @param dateFrom the date of the start of the period to be searched, inclusive.
     * @param dateTo the date of the end of the period to be searched, inclusive.
     * @return the list of transactions, possibly empty.
     */
    public List<RailTransaction> listTransactions(String accountId,
                                                  LocalDate dateFrom,
                                                  LocalDate dateTo);
}
