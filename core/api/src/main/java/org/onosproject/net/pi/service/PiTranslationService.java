/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.service;

import com.google.common.annotations.Beta;

/**
 * A service to translate protocol-dependent (PD) entities to
 * protocol-independent (PI) ones.
 */
@Beta
public interface PiTranslationService {

    /**
     * Returns a flow rule translator.
     *
     * @return flow rule translator
     */
    PiFlowRuleTranslator flowRuleTranslator();

    /**
     * Returns a group translator.
     *
     * @return group translator
     */
    PiGroupTranslator groupTranslator();

    /**
     * Returns a meter translator.
     *
     * @return meter translator
     */
    PiMeterTranslator meterTranslator();

    /**
     * Returns a group translator for packet replication engine (PRE)
     * entries.
     *
     * @return replication group translator
     */
    PiReplicationGroupTranslator replicationGroupTranslator();
}
