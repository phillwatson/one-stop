package com.hillayes.rail.service;

import com.hillayes.rail.api.RailProviderApi;
import com.hillayes.rail.api.domain.RailInstitution;
import com.hillayes.rail.api.domain.RailProvider;
import com.hillayes.rail.config.RailProviderFactory;
import com.hillayes.rail.config.ServiceConfiguration;
import com.hillayes.rail.utils.TestApiData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.hillayes.rail.utils.TestApiData.mockRailProviderApi;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class InstitutionServiceTest {
    @Mock
    RailProviderFactory railProviderFactory;

    @Mock
    ServiceConfiguration config;

    @InjectMocks
    InstitutionService fixture;

    @BeforeEach
    public void init() {
        openMocks(this);

        ServiceConfiguration.Caches caches = mock(ServiceConfiguration.Caches.class);
        when(caches.institutions()).thenReturn(Duration.ofSeconds(30));
        when(config.caches()).thenReturn(caches);

        fixture.init();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testList(Boolean paymentsEnabled) {
        // given: a rail provider API
        RailProvider railProvider = RailProvider.NORDIGEN;
        RailProviderApi railProviderApi = mockRailProviderApi(railProvider);
        when(railProviderFactory.get(railProvider)).thenReturn(railProviderApi);

        // and: a requested country code and payments enabled
        String countryCode = "GB";

        // and: the rail has a list of institutions
        List<RailInstitution> institutions = List.of(
            TestApiData.mockInstitution(),
            TestApiData.mockInstitution(),
            TestApiData.mockInstitution()
        );
        when(railProviderApi.listInstitutions(countryCode, paymentsEnabled))
            .thenReturn(institutions);

        // when: list is called
        List<RailInstitution> result = fixture.list(railProvider, countryCode, paymentsEnabled);

        // then: the list of institutions is returned
        assertNotNull(result);
        assertEquals(institutions.size(), result.size());

        // and: the rail was called
        verify(railProviderApi).listInstitutions(countryCode, paymentsEnabled);

        // when: the service is called a second time
        Mockito.clearInvocations(railProviderApi);
        List<RailInstitution> secondResult = fixture.list(railProvider, countryCode, paymentsEnabled);

        // then: the list of institutions is returned
        assertNotNull(secondResult);
        assertEquals(institutions.size(), secondResult.size());

        // and: the rail was NOT called
        verifyNoMoreInteractions(railProviderApi);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testList_AllRailProviders(Boolean paymentsEnabled) {
        // given: a requested country code and payments enabled
        String countryCode = "GB";

        // and: each Rail API has a collection of rail provider APIs
        List<RailProviderApi> railApis = Arrays.stream(RailProvider.values())
            .map(id -> {
                RailProviderApi api = mockRailProviderApi(id);
                when(api.listInstitutions(countryCode, paymentsEnabled))
                    .thenReturn(List.of(
                        TestApiData.mockInstitution(),
                        TestApiData.mockInstitution(),
                        TestApiData.mockInstitution()
                    ));
                return api;
            })
            .toList();

        // and: the factory returns the collection of rail provider APIs
        when(railProviderFactory.getAll()).then((s) -> railApis.stream() );

        // when: list is called - with no rail provider ID
        List<RailInstitution> result = fixture.list(null, countryCode, paymentsEnabled);

        // then: the list of ALL institutions is returned
        assertNotNull(result);
        assertEquals(railApis.size() * 3, result.size());

        // and: each rail was called
        railApis.forEach(api -> verify(api).listInstitutions(countryCode, paymentsEnabled));

        // when: the service is called a second time
        railApis.forEach(Mockito::clearInvocations);
        List<RailInstitution> secondResult = fixture.list(null, countryCode, paymentsEnabled);

        // then: the list of ALL institutions is returned
        assertNotNull(secondResult);
        assertEquals(railApis.size() * 3, secondResult.size());

        // and: the rails were NOT called
        railApis.forEach(api -> verify(api, never()).listInstitutions(countryCode, paymentsEnabled));
    }

    @Test
    public void testGet() {
        // given: a rail provider API
        RailProvider railProvider = RailProvider.NORDIGEN;
        RailProviderApi railProviderApi = mockRailProviderApi(railProvider);
        when(railProviderFactory.get(railProvider)).thenReturn(railProviderApi);

        // and: the rail has the identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(railProviderApi.getInstitution(institution.getId()))
            .thenReturn(Optional.of(institution));

        // when: the service is called
        Optional<RailInstitution> result = fixture.get(railProvider, institution.getId());

        // then: the institution is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(institution, result.get());

        // when: the service is called a second time
        Mockito.clearInvocations(railProviderApi);
        Optional<RailInstitution> secondResult = fixture.get(railProvider, institution.getId());

        // then: the institution is returned
        assertNotNull(secondResult);
        assertTrue(secondResult.isPresent());
        assertEquals(institution, secondResult.get());

        // and: the rail was NOT called
        verifyNoMoreInteractions(railProviderApi);
    }

    @Test
    public void testGet_NotFound() {
        // given: a rail provider API
        RailProvider railProvider = RailProvider.NORDIGEN;
        RailProviderApi railProviderApi = mockRailProviderApi(railProvider);
        when(railProviderFactory.get(railProvider)).thenReturn(railProviderApi);

        // and: an unknown institution ID
        String institutionId = UUID.randomUUID().toString();
        when(railProviderApi.getInstitution(institutionId))
            .thenReturn(Optional.empty());

        // when: the service is called
        Optional<RailInstitution> result = fixture.get(railProvider, institutionId);

        // then: NO institution is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGet_AllRailProviders() {
        // given: a collection of rail provider APIs
        RailProviderApi nordigenApi = mockRailProviderApi(RailProvider.NORDIGEN);
        RailProviderApi yapilyApi = mockRailProviderApi(RailProvider.YAPILY);

        RailProviderApi railProviderApi = mock(RailProviderApi.class);
        when(railProviderFactory.getAll()).thenReturn(
            Stream.of(nordigenApi, yapilyApi, railProviderApi)
        );

        // and: the rail has the identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(railProviderApi.getInstitution(institution.getId()))
            .thenReturn(Optional.of(institution));

        // when: the service is called
        Optional<RailInstitution> result = fixture.get(institution.getId());

        // then: the institution is returned
        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals(institution, result.get());
    }

    @Test
    public void testGet_AllRailProviders_NotFound() {
        // given: a collection of rail provider APIs
        RailProviderApi nordigenApi = mockRailProviderApi(RailProvider.NORDIGEN);
        RailProviderApi yapilyApi = mockRailProviderApi(RailProvider.YAPILY);

        RailProviderApi railProviderApi = mock(RailProviderApi.class);
        when(railProviderFactory.getAll()).thenReturn(
            Stream.of(nordigenApi, yapilyApi, railProviderApi)
        );

        // and: the rail has the identified institution
        RailInstitution institution = TestApiData.mockInstitution();
        when(railProviderApi.getInstitution(institution.getId()))
            .thenReturn(Optional.empty());

        // when: the service is called
        Optional<RailInstitution> result = fixture.get(institution.getId());

        // then: NO institution is returned
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
