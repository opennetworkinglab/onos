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

package org.onosproject.segmentrouting;

import com.google.common.collect.Lists;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.ProbeMode;

import java.util.List;
import java.util.Objects;

public class MockHostProbingService implements HostProbingService {
    List<Probe> probes;

    private class Probe {
        private Host host;
        private ConnectPoint connectPoint;
        private ProbeMode probeMode;

        Probe(Host host, ConnectPoint connectPoint, ProbeMode probeMode) {
            this.host = host;
            this.connectPoint = connectPoint;
            this.probeMode = probeMode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Probe)) {
                return false;
            }
            Probe that = (Probe) o;
            return (Objects.equals(this.host, that.host) &&
                    Objects.equals(this.connectPoint, that.connectPoint) &&
                    Objects.equals(this.probeMode, that.probeMode));
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, connectPoint, probeMode);
        }
    }

    MockHostProbingService() {
        probes = Lists.newArrayList();
    }

    boolean verifyProbe(Host host, ConnectPoint connectPoint, ProbeMode probeMode) {
        Probe probe = new Probe(host, connectPoint, probeMode);
        return probes.contains(probe);
    }

    @Override
    public void probeHost(Host host, ConnectPoint connectPoint, ProbeMode probeMode) {
        probes.add(new Probe(host, connectPoint, probeMode));
    }
}
