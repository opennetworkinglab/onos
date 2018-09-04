/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.openstacknode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacknode.api.NeutronConfig;

/**
 * Hamcrest matcher for neutron config.
 */
public final class NeutronConfigJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final NeutronConfig neutronConfig;

    private static final String USE_METADATA_PROXY = "useMetadataProxy";
    private static final String METADATA_PROXY_SECRET = "metadataProxySecret";

    private NeutronConfigJsonMatcher(NeutronConfig neutronConfig) {
        this.neutronConfig = neutronConfig;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check useMetaDataProxy
        JsonNode jsonUseMetadataProxy = jsonNode.get(USE_METADATA_PROXY);
        if (jsonUseMetadataProxy != null) {
            boolean useMetadataProxy = neutronConfig.useMetadataProxy();
            if (jsonUseMetadataProxy.asBoolean() != useMetadataProxy) {
                description.appendText("useMetadataProxy was " + jsonUseMetadataProxy);
                return false;
            }
        }

        // check metadataProxySecret
        JsonNode jsonMetadataProxySecret = jsonNode.get(METADATA_PROXY_SECRET);
        if (jsonMetadataProxySecret != null) {
            String metadataProxySecret = neutronConfig.metadataProxySecret();
            if (!jsonMetadataProxySecret.asText().equals(metadataProxySecret)) {
                description.appendText("metadataProxySecret was " + jsonUseMetadataProxy);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(neutronConfig.toString());
    }

    /**
     * Factory to allocate neutron config matcher.
     *
     * @param config neutron config object we are looking for
     * @return matcher
     */
    public static NeutronConfigJsonMatcher matchNeutronConfig(NeutronConfig config) {
        return new NeutronConfigJsonMatcher(config);
    }
}
