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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.pi.model.PiRegisterId;
import org.onosproject.net.pi.runtime.PiRegisterCell;
import org.onosproject.net.pi.runtime.PiRegisterCellId;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Protocol-independent register programmable device behaviour.
 */
@Beta
public interface PiRegisterProgrammable extends HandlerBehaviour {

    /**
     * Write a register cell of this device.
     *
     * @param entry register entry
     * @return Completable future with result of the operation
     */
    CompletableFuture<Boolean> writeRegisterCell(PiRegisterCell entry);

    /**
     * Gets the register cell given the register cell ID from the device.
     *
     * @param cellId the register cell Id
     * @return completable future with the register cell
     */
    CompletableFuture<PiRegisterCell> readRegisterCell(PiRegisterCellId cellId);

    /**
     * Gets the register cells given the collection of cellIds from the device.
     *
     * @param cellIds the collection of register cell Id
     * @return completable future with the collection of register cells
     */
    CompletableFuture<Collection<PiRegisterCell>> readRegisterCells(Set<PiRegisterCellId> cellIds);

    /**
     * Gets all of the register cells given register ID from the device.
     * @param registerId the identifier of register
     * @return completable future with the collection of register cells
     */
    CompletableFuture<Collection<PiRegisterCell>> readRegisterCells(PiRegisterId registerId);

    /**
     * Gets all of the register cells from the device.
     *
     * @return completable future with the collection of register cells
     */
    CompletableFuture<Collection<PiRegisterCell>> readAllRegisterCells();
}