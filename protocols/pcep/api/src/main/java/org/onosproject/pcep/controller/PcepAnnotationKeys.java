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
package org.onosproject.pcep.controller;

/**
 * Collection of keys for annotation for PCEP tunnels.
 */
public final class PcepAnnotationKeys {

    /**
     *  Prohibits instantiation.
     */
    private PcepAnnotationKeys() {
    }

    /**
     * Annotation key for bandwidth.
     * The value for this key is interpreted as Mbps.
     */
    public static final String BANDWIDTH = "bandwidth";

    /**
     * Annotation key for the LSP signaling type.
     */
    public static final String LSP_SIG_TYPE = "lspSigType";

    /**
     * Annotation key for the PCC tunnel id.
     */
    public static final String PCC_TUNNEL_ID = "PccTunnelId";

    /**
     * Annotation key for the LSP id assigned per tunnel per session.
     */
    public static final String PLSP_ID = "PLspId";

    /**
     * Annotation key for the LSP id assigned per tunnel.
     */
    public static final String LOCAL_LSP_ID = "localLspId";

    /**
     * Annotation key for the identification of initiated LSP.
     */
    public static final String PCE_INIT = "pceInit";

    /**
     * Annotation key for the cost type.
     */
    public static final String COST_TYPE = "costType";

    /**
     * Annotation key for the Delegation.
     * Whether LSPs are delegated or not
     */
    public static final String DELEGATE = "delegate";
}
