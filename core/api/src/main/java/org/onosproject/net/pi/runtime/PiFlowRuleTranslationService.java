/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.pi.model.PiPipeconf;

/**
 * A service to translate ONOS flow rules to table entries of a protocol-independent pipeline.
 */
@Beta
public interface PiFlowRuleTranslationService {

    /**
     * Returns a table entry equivalent to the given flow rule for the given protocol-independent
     * pipeline configuration.
     *
     * @param rule     a flow rule
     * @param pipeconf a pipeline configuration
     * @return a table entry
     * @throws PiFlowRuleTranslationException if the flow rule cannot be translated
     */
    PiTableEntry translate(FlowRule rule, PiPipeconf pipeconf)
            throws PiFlowRuleTranslationException;

    /**
     * Signals that an error was encountered while translating flow rule.
     */
    class PiFlowRuleTranslationException extends Exception {

        /**
         * Creates a new exception with the given message.
         *
         * @param message a message
         */
        public PiFlowRuleTranslationException(String message) {
            super(message);
        }
    }
}
