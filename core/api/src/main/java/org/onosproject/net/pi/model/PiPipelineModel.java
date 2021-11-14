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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;

import java.util.Collection;
import java.util.Optional;

/**
 * Model of a protocol-independent pipeline.
 */
@Beta
public interface PiPipelineModel {

    /**
     * Returns the data plane target architecture supported by this pipeline model.
     *
     * @return optional architecture string.
     */
    Optional<String> architecture();

    /**
     * Returns the table model associated with the given ID, if present.
     *
     * @param tableId table ID
     * @return optional table model
     */
    Optional<PiTableModel> table(PiTableId tableId);

    /**
     * Returns the collection of all table models defined by this pipeline model.
     *
     * @return collection of actions
     */
    Collection<PiTableModel> tables();

    /**
     * Returns the counter model associated with the given ID, if present.
     *
     * @param counterId counter ID
     * @return optional counter model
     */
    Optional<PiCounterModel> counter(PiCounterId counterId);

    /**
     * Returns all counter models defined by this pipeline model.
     *
     * @return collection of counter models
     */
    Collection<PiCounterModel> counters();

    /**
     * Returns the meter model associated with the given ID, if present.
     *
     * @param meterId meter ID
     * @return optional meter model
     */
    Optional<PiMeterModel> meter(PiMeterId meterId);

    /**
     * Returns all meter models defined by this pipeline model.
     *
     * @return collection of meter models
     */
    Collection<PiMeterModel> meters();

    /**
     * Returns the register model associated with the given ID, if present.
     *
     * @param registerId register ID
     * @return optional register model
     */
    Optional<PiRegisterModel> register(PiRegisterId registerId);

    /**
     * Returns all register models defined by this pipeline model.
     *
     * @return collection of register models
     */
    Collection<PiRegisterModel> registers();

    /**
     * Returns the action profile model associated with the given ID, if present.
     *
     * @param actionProfileId action profile ID
     * @return optional action profile model
     */
    Optional<PiActionProfileModel> actionProfiles(PiActionProfileId actionProfileId);

    /**
     * Returns all action profile models defined by this pipeline model.
     *
     * @return collection of action profile models
     */
    Collection<PiActionProfileModel> actionProfiles();

    /**
     * Returns the packet operation model of the given type, if present.
     *
     * @param type packet operation type
     * @return packet operation model
     */
    Optional<PiPacketOperationModel> packetOperationModel(PiPacketOperationType type);
}
