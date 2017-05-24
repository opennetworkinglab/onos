/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.ui.topo;

import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.ui.AbstractUiTest;
import org.onosproject.ui.model.topo.UiLinkId;

/**
 * Base class for unit tests of {@link BiLink} and {@link BiLinkMap}.
 */
public abstract class BiLinkTestBase extends AbstractUiTest {

    protected static class FakeLink implements Link {
        private final ConnectPoint src;
        private final ConnectPoint dst;

        FakeLink(ConnectPoint src, ConnectPoint dst) {
            this.src = src;
            this.dst = dst;
        }

        @Override
        public ConnectPoint src() {
            return src;
        }

        @Override
        public ConnectPoint dst() {
            return dst;
        }

        @Override
        public Type type() {
            return null;
        }

        @Override
        public State state() {
            return null;
        }

        @Override
        public boolean isExpected() {
            return false;
        }

        @Override
        public Annotations annotations() {
            return null;
        }

        @Override
        public ProviderId providerId() {
            return null;
        }

        @Override
        public <B extends Behaviour> B as(Class<B> projectionClass) {
            return null;
        }

        @Override
        public <B extends Behaviour> boolean is(Class<B> projectionClass) {
            return false;
        }
    }

    protected static final DeviceId DEV_A_ID = DeviceId.deviceId("device-A");
    protected static final DeviceId DEV_B_ID = DeviceId.deviceId("device-B");
    protected static final PortNumber PORT_1 = PortNumber.portNumber(1);
    protected static final PortNumber PORT_2 = PortNumber.portNumber(2);

    protected static final ConnectPoint CP_A1 = new ConnectPoint(DEV_A_ID, PORT_1);
    protected static final ConnectPoint CP_B2 = new ConnectPoint(DEV_B_ID, PORT_2);

    protected static final LinkKey KEY_AB = LinkKey.linkKey(CP_A1, CP_B2);
    protected static final LinkKey KEY_BA = LinkKey.linkKey(CP_B2, CP_A1);

    protected static final Link LINK_AB = new FakeLink(CP_A1, CP_B2);
    protected static final Link LINK_BA = new FakeLink(CP_B2, CP_A1);

    protected static class ConcreteLink extends BiLink {
        public ConcreteLink(LinkKey key, Link link) {
            super(key, link);
        }

        public ConcreteLink(UiLinkId uiLinkId) {
            super(uiLinkId);
        }

        @Override
        public LinkHighlight highlight(Enum<?> type) {
            return null;
        }
    }

    protected static class ConcreteLinkMap extends BiLinkMap<ConcreteLink> {
        @Override
        public ConcreteLink create(LinkKey key, Link link) {
            return new ConcreteLink(key, link);
        }
    }

}
