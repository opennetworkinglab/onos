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

import com.google.common.primitives.UnsignedInteger;
import org.onosproject.net.PortNumber;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPreReplica;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

import static java.lang.String.format;

/**
 * Codec for P4Runtime PRE Replica.
 */
public class PreReplicaCodec extends AbstractCodec<PiPreReplica,
        P4RuntimeOuterClass.Replica, Object> {

    @Override
    protected P4RuntimeOuterClass.Replica encode(
            PiPreReplica replica, Object ignore,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        final int p4PortId;
        try {
            UnsignedInteger egressPort = UnsignedInteger.valueOf(replica.egressPort().toLong());
            p4PortId = egressPort.intValue();
        } catch (IllegalArgumentException e) {
            throw new CodecException(format(
                    "Cannot cast 64 bit port value '%s' to 32 bit",
                    replica.egressPort()));
        }
        return P4RuntimeOuterClass.Replica.newBuilder()
                .setEgressPort(p4PortId)
                .setInstance(replica.instanceId())
                .build();
    }

    @Override
    protected PiPreReplica decode(
            P4RuntimeOuterClass.Replica message, Object ignore,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException, P4InfoBrowser.NotFoundException {
        return new PiPreReplica(
                PortNumber.portNumber(message.getEgressPort()),
                message.getInstance());
    }
}
