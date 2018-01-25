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

package org.onosproject.net.pi.impl;

import org.onosproject.net.Device;
import org.onosproject.net.flow.IndexTableId;
import org.onosproject.net.flow.TableId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.service.PiTranslationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * PI utility class.
 */
final class PiUtils {

    private static final Logger log = LoggerFactory.getLogger(PiUtils.class);

    private PiUtils() {
        // Hides constructor.
    }

    static PiPipelineInterpreter getInterpreterOrNull(Device device, PiPipeconf pipeconf) {
        if (device != null) {
            return device.is(PiPipelineInterpreter.class) ? device.as(PiPipelineInterpreter.class) : null;
        } else {
            // The case of device == null should be admitted only during unit testing.
            // In any other case, the interpreter should be constructed using the device.as() method to make sure that
            // behaviour's handler/data attributes are correctly populated.
            // FIXME: modify test class PiFlowRuleTranslatorTest to avoid passing null device
            // I.e. we need to create a device object that supports is/as method for obtaining the interpreter.
            log.warn("getInterpreterOrNull() called with device == null, is this a unit test?");
            try {
                return (PiPipelineInterpreter) pipeconf.implementation(PiPipelineInterpreter.class)
                        .orElse(null)
                        .newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(format("Unable to instantiate interpreter of pipeconf %s",
                                                          pipeconf.id()));
            }
        }
    }

    static PiTableId translateTableId(TableId tableId, PiPipelineInterpreter interpreter)
            throws PiTranslationException {
        switch (tableId.type()) {
            case PIPELINE_INDEPENDENT:
                return (PiTableId) tableId;
            case INDEX:
                IndexTableId indexId = (IndexTableId) tableId;
                if (interpreter == null) {
                    throw new PiTranslationException(format(
                            "Unable to map table ID '%d' from index to PI: missing interpreter", indexId.id()));
                } else if (!interpreter.mapFlowRuleTableId(indexId.id()).isPresent()) {
                    throw new PiTranslationException(format(
                            "Unable to map table ID '%d' from index to PI: missing ID in interpreter", indexId.id()));
                } else {
                    return interpreter.mapFlowRuleTableId(indexId.id()).get();
                }
            default:
                throw new PiTranslationException(format(
                        "Unrecognized table ID type %s", tableId.type().name()));
        }
    }
}
