/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.yms.app.yob;

import org.onosproject.yms.app.ysr.TestYangSchemaNodeProvider;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;

/**
 * YOB test Utility.
 */
final class YobTestUtils {

    /**
     * Schema nodes.
     */
    static final String ROOT_DATA_RESOURCE = "/restconf/data";
    static final String TOPOLOGY = "yms-topology";
    static final String NODE = "node";
    static final String LEAF_1A1 = "leaf1a1";
    static final String LEAF_1A2 = "leaf1a2";
    static final String LEAF_1BIA = "leaf1bia";
    static final String LEAF_1BIB = "leaf1bib";
    static final String ROUTER_ID = "router-id";
    static final String ROUTER_IP = "router-ip";
    static final String STR_LEAF_VALUE = "leaf value";
    static final String YMS_IETF_NETWORK = "yms-ietf-network";
    static final String NETWORKS_STATE = "networks-state";
    static final String NETWORKS = "networks";
    static final String NETWORK = "network";
    static final String NETWORK_REF = "network-ref";
    static final String IETF_TE_TOPOLOGY = "yms-ietf-te-topology";
    static final String TE_NODE_EVENT = "te-node-event";
    static final String TE_NODE_ATTRIBUTES = "te-node-attributes";
    static final String ADMIN_STATUS = "admin-status";
    static final String UP = "up";
    static final String LINK = "link";
    static final String YMS_NETWORK_TOPOLOGY = "yms-network-topology";
    static final String TE = "te";
    static final String CONFIG = "config";
    static final String BUNDLED_LINKS = "bundled-links";
    static final String BUNDLED_LINK = "bundled-link";
    static final String SEQUENCE = "sequence";
    static final String TE_LINK_TEMPLATE = "te-link-template";

    private YobTestUtils() {
        TEST_SCHEMA_PROVIDER.processSchemaRegistry(null);
    }

    private static final TestYangSchemaNodeProvider
            TEST_SCHEMA_PROVIDER = new TestYangSchemaNodeProvider();

    YangSchemaRegistry schemaRegistry() {
        return TEST_SCHEMA_PROVIDER.getDefaultYangSchemaRegistry();
    }

    /**
     * Returns the YANG object builder factory instance.
     *
     * @return YANG object builder factory instance
     */
    static YobTestUtils instance() {
        return LazyHolder.INSTANCE;
    }

    /*
     * Bill Pugh Singleton pattern. INSTANCE won't be instantiated until the
     * LazyHolder class is loaded via a call to the instance() method below.
     */
    private static class LazyHolder {
        private static final YobTestUtils INSTANCE = new YobTestUtils();
    }
}
