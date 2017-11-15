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
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.group.Group;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiTableEntry;

/**
 * A service to translate protocol-dependent entities to protocol-independent ones.
 */
@Beta
public interface PiTranslationService {

    /**
     * Returns a PI table entry equivalent to the given flow rule for the given protocol-independent pipeline
     * configuration.
     *
     * @param rule     a flow rule
     * @param pipeconf a pipeline configuration
     * @return a table entry
     * @throws PiTranslationException if the flow rule cannot be translated
     */
    PiTableEntry translateFlowRule(FlowRule rule, PiPipeconf pipeconf)
            throws PiTranslationException;

    /**
     * Returns a PI action group equivalent to the given group for the given protocol-independent pipeline
     * configuration.
     *
     * @param group    a group
     * @param pipeconf a pipeline configuration
     * @return a PI action group
     * @throws PiTranslationException if the group cannot be translated
     */
    PiActionGroup translateGroup(Group group, PiPipeconf pipeconf)
            throws PiTranslationException;

    /**
     * Signals that an error was encountered while translating an entity.
     */
    class PiTranslationException extends Exception {

        /**
         * Creates a new exception with the given message.
         *
         * @param message a message
         */
        public PiTranslationException(String message) {
            super(message);
        }
    }
}
