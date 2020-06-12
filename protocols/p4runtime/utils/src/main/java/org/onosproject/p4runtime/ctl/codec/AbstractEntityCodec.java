/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.codec;

import com.google.protobuf.Message;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract implementation of a specialized codec that translates PI runtime
 * entities and their handles into P4Runtime protobuf messages and vice versa.
 * Supports also encoding to "key" P4Runtime Entity messages used in read and
 * delete operations.
 *
 * @param <P> PI runtime class
 * @param <H> PI handle class
 * @param <M> P4Runtime protobuf message class
 * @param <X> metadata class
 */
public abstract class AbstractEntityCodec
        <P extends PiEntity, H extends PiHandle, M extends Message, X>
        extends AbstractCodec<P, M, X> {

    protected abstract M encodeKey(H handle, X metadata, PiPipeconf pipeconf,
                                   P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException;

    protected abstract M encodeKey(P piEntity, X metadata, PiPipeconf pipeconf,
                                   P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException;

    /**
     * Returns a P4Runtime protobuf message representing the P4Runtime.Entity
     * "key" for the given PI handle, metadata and pipeconf.
     *
     * @param handle   PI handle instance
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return P4Runtime protobuf message
     * @throws CodecException if the given PI entity cannot be encoded (see
     *                        exception message)
     */
    public M encodeKey(H handle, X metadata, PiPipeconf pipeconf)
            throws CodecException {
        checkNotNull(handle);
        try {
            return encodeKey(handle, metadata, pipeconf, browserOrFail(pipeconf));
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new CodecException(e.getMessage());
        }
    }

    /**
     * Returns a P4Runtime protobuf message representing the P4Runtime.Entity
     * "key" for the given PI entity, metadata and pipeconf.
     *
     * @param piEntity PI entity instance
     * @param metadata metadata
     * @param pipeconf pipeconf
     * @return P4Runtime protobuf message
     * @throws CodecException if the given PI entity cannot be encoded (see
     *                        exception message)
     */
    public M encodeKey(P piEntity, X metadata, PiPipeconf pipeconf)
            throws CodecException {
        checkNotNull(piEntity);
        try {
            return encodeKey(piEntity, metadata, pipeconf, browserOrFail(pipeconf));
        } catch (P4InfoBrowser.NotFoundException e) {
            throw new CodecException(e.getMessage());
        }
    }
}
