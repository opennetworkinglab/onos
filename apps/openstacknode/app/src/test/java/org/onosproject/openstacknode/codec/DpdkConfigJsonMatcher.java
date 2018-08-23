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
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.DpdkConfig.DatapathType;
import org.onosproject.openstacknode.api.DpdkInterface;

/**
 * Hamcrest matcher for dpdk config.
 */
public final class DpdkConfigJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {
    private final DpdkConfig dpdkConfig;

    private static final String DATA_PATH_TYPE = "datapathType";
    private static final String SOCKET_DIR = "socketDir";
    private static final String DPDK_INTFS = "dpdkIntfs";

    private DpdkConfigJsonMatcher(DpdkConfig dpdkConfig) {
        this.dpdkConfig = dpdkConfig;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check datapath type
        DatapathType jsonDatapathType = DatapathType.valueOf(
                jsonNode.get(DATA_PATH_TYPE).asText().toUpperCase());
        DatapathType datapathType = dpdkConfig.datapathType();

        if (!jsonDatapathType.equals(datapathType)) {
            description.appendText("datapath type was " + jsonDatapathType.name());
            return false;
        }

        // check socket directory
        JsonNode jsonSocketDir = jsonNode.get(SOCKET_DIR);
        if (jsonSocketDir != null) {
            String socketDir = dpdkConfig.socketDir();

            if (!jsonSocketDir.asText().equals(socketDir)) {
                description.appendText("socketDir was " + jsonSocketDir);
                return false;
            }
        }

        // check dpdk interfaces
        JsonNode jsonDpdkintfs = jsonNode.get(DPDK_INTFS);
        if (jsonDpdkintfs != null) {
            if (jsonDpdkintfs.size() != dpdkConfig.dpdkIntfs().size()) {
                description.appendText("dpdk interface size was " + jsonDpdkintfs.size());
                return false;
            }

            for (DpdkInterface dpdkIntf : dpdkConfig.dpdkIntfs()) {
                boolean intfFound = false;
                for (int intfIndex = 0; intfIndex < jsonDpdkintfs.size(); intfIndex++) {
                    DpdkInterfaceJsonMatcher intfMatcher =
                            DpdkInterfaceJsonMatcher.matchesDpdkInterface(dpdkIntf);
                    if (intfMatcher.matches(jsonDpdkintfs.get(intfIndex))) {
                        intfFound = true;
                        break;
                    }
                }

                if (!intfFound) {
                    description.appendText("DpdkIntf not found " + dpdkIntf.toString());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(dpdkConfig.toString());
    }

    /**
     * Factory to allocate and dpdk config matcher.
     *
     * @param dpdkConfig dpdk config object we are looking for
     * @return matcher
     */
    public static DpdkConfigJsonMatcher matchDpdkConfig(DpdkConfig dpdkConfig) {
        return new DpdkConfigJsonMatcher(dpdkConfig);
    }
}
