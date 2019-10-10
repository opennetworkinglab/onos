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

package org.onosproject.drivers.p4runtime;

import org.onosproject.drivers.p4runtime.mirror.P4RuntimeDefaultEntryMirror;
import org.onosproject.net.behaviour.PiPipelineProgrammable;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.p4runtime.api.P4RuntimeReadClient;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.SUPPORT_DEFAULT_TABLE_ENTRY;
import static org.onosproject.drivers.p4runtime.P4RuntimeDriverProperties.DEFAULT_SUPPORT_DEFAULT_TABLE_ENTRY;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstract implementation of the PiPipelineProgrammable behaviours for a P4Runtime device.
 */
public abstract class AbstractP4RuntimePipelineProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements PiPipelineProgrammable {

    protected final Logger log = getLogger(getClass());

    /**
     * Returns a byte buffer representing the target-specific device data to be used in the SetPipelineConfig message.
     *
     * @param pipeconf pipeconf
     * @return byte buffer
     */
    public abstract ByteBuffer createDeviceDataBuffer(PiPipeconf pipeconf);

    @Override
    public CompletableFuture<Boolean> setPipeconf(PiPipeconf pipeconf) {
        if (!setupBehaviour("setPipeconf()")) {
            return completedFuture(false);
        }

        final ByteBuffer deviceDataBuffer = createDeviceDataBuffer(pipeconf);
        if (deviceDataBuffer == null) {
            // Hopefully the child class logged the problem.
            return completedFuture(false);
        }
        CompletableFuture<Boolean> pipeconfSet = client.setPipelineConfig(
                p4DeviceId, pipeconf, deviceDataBuffer);
        return getDefaultEntries(pipeconfSet, pipeconf);
    }

    @Override
    public CompletableFuture<Boolean> isPipeconfSet(PiPipeconf pipeconf) {
        if (!setupBehaviour("isPipeconfSet()")) {
            return completedFuture(false);
        }

        return client.isPipelineConfigSet(p4DeviceId, pipeconf);
    }

    @Override
    public abstract Optional<PiPipeconf> getDefaultPipeconf();

    /**
     * Once the pipeconf is set successfully, we should store all the default entries
     * before notify other service to prevent overwriting the default entries.
     * Default entries may be used in P4RuntimeFlowRuleProgrammable.
     * <p>
     * This method returns a completable future with the result of the pipeconf set
     * operation (which might not be true).
     *
     * @param pipeconfSet completable future for setting pipeconf
     * @param pipeconf pipeconf
     * @return completable future eventually true if the pipeconf set successfully
     */
    private CompletableFuture<Boolean> getDefaultEntries(CompletableFuture<Boolean> pipeconfSet, PiPipeconf pipeconf) {
        if (!driverBoolProperty(
                SUPPORT_DEFAULT_TABLE_ENTRY,
                DEFAULT_SUPPORT_DEFAULT_TABLE_ENTRY)) {
            return pipeconfSet;
        }
        return pipeconfSet.thenApply(setSuccess -> {
            if (!setSuccess) {
                return setSuccess;
            }
            final P4RuntimeDefaultEntryMirror mirror = handler()
                    .get(P4RuntimeDefaultEntryMirror.class);

            final PiPipelineModel pipelineModel = pipeconf.pipelineModel();
            final P4RuntimeReadClient.ReadRequest request = client.read(
                    p4DeviceId, pipeconf);
            // Read default entries from all non-constant tables.
            // Ignore constant default entries.
            pipelineModel.tables().stream()
                    .filter(t -> !t.isConstantTable())
                    .forEach(t -> {
                        if (!t.constDefaultAction().isPresent()) {
                            request.defaultTableEntry(t.id());
                        }
                    });
            final P4RuntimeReadClient.ReadResponse response = request.submitSync();
            mirror.sync(deviceId, response.all(PiTableEntry.class));
            return true;
        });
    }
}
