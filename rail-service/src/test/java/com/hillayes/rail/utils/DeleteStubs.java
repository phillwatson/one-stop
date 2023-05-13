package com.hillayes.rail.utils;

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import wiremock.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to delete the stubs that are no longer valid. For example;
 * the stubs to retrieve an entity will be invalid after the entity is deleted.
 * <pre>
 *  DeleteStubs.StubbingList stubbings = new DeleteStubs.StubbingList();
 *
 *  // stubbing to retrieve the entity by its id
 *  stubbings.add(wireMockServer.stubFor(get(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
 *      .willReturn(ok()
 *          .withHeader("Content-Type", "application/json")
 *          .withBody(json(response)))
 *  ));
 *
 *  // stubbing to delete the entity by its id - and then delete the get() stubbing
 *  wireMockServer.stubFor(delete(urlEqualTo("/api/v2/requisitions/" + response.id + "/"))
 *      .willReturn(ok()
 *          .withHeader("Content-Type", "application/json")
 *          .withBody(json(response)))
 *      // This is the important part - to delete the get() stubbing
 *      .withPostServeAction("DeleteStubs", stubbings)
 *  );
 * </pre>
 */
public class DeleteStubs extends PostServeAction {
    @Override
    public String getName() {
        return "DeleteStubs";
    }

    @Override
    public void doAction(final ServeEvent serveEvent, final Admin admin, final Parameters parameters) {
        // delete the stubbings that were passed in
        StubbingList mappingsToDelete = parameters.as(StubbingList.class);
        for (StubMapping mapping : mappingsToDelete.mappings) {
            admin.removeStubMapping(mapping);
        }

        // delete the stubbing that called this
        admin.removeStubMapping(serveEvent.getStubMapping());
    }

    /**
     * Used to collect the stubs to be deleted.
     */
    public static class StubbingList {
        @JsonProperty
        List<StubMapping> mappings = new ArrayList<>();

        public StubbingList add(StubMapping mapping) {
            mappings.add(mapping);
            return this;
        }
    }
}
