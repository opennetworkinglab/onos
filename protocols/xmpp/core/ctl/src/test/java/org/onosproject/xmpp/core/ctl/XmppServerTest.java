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

package org.onosproject.xmpp.core.ctl;

import org.easymock.EasyMock;
import org.junit.Test;
import org.onosproject.xmpp.core.XmppDeviceFactory;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Test class for XmppServer class.
 */
public class XmppServerTest {

    XmppServer server = new XmppServer();

    @Test
    public void testStart() {
        XmppDeviceFactory mockXmppDeviceFactory = EasyMock.createMock(XmppDeviceFactory.class);
        server.start(mockXmppDeviceFactory);
        assertNotNull(server.channel);
        assertNotNull(server.channelClass);
        assertNotNull(server.eventLoopGroup);
    }

    @Test
    public void testSetConfiguration() {
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("xmppPort", "5222");
        server.setConfiguration(properties);
        assertThat(server.port, is(5222));
    }

}
