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

package org.onosproject.odtn.internal;

import java.util.NoSuchElementException;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;

import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class DefaultTapiResolverTest {

    private DefaultTapiResolver tapiResolver;

    private TapiNodeRef nodeRef;
    private TapiNepRef nepRef;
    private DeviceId deviceId;
    private ConnectPoint cp;
    private String sipId;

    private DeviceId dummyDeviceId;
    private Integer dummyPort;
    private ConnectPoint dummyCp;
    private String dummySipId;

    private TapiDataProducer mockTapiDataProducer;

    @Before
    public void setUp() {
        nodeRef = TapiNodeRef.create(
                "49e2ac46-3975-44b4-b84f-8fab28222a39",
                "5638e8e6-ac17-40d9-86e4-7c1febab6f1a");
        nepRef = TapiNepRef.create(
                "59e2ac46-3975-44b4-b84f-8fab28222a39",
                "6638e8e6-ac17-40d9-86e4-7c1febab6f1a",
                "cd673055-e2b2-4f67-88c8-adfae96385bc");
        deviceId = DeviceId.deviceId("netconf:172.24.3.5:11011");
        cp = new ConnectPoint(deviceId, PortNumber.portNumber("42"));
        sipId = "01c39723-7c0d-4754-8d64-fd9ff412404c";
        nodeRef.setDeviceId(deviceId);
        nepRef.setConnectPoint(cp).setSipId(sipId);

        dummyDeviceId = DeviceId.deviceId("dummy");
        dummyPort = 4;
        dummyCp = new ConnectPoint(dummyDeviceId, PortNumber.portNumber(dummyPort));
        dummySipId = "00000000-0000-0000-0000-000000000000";

        tapiResolver = new DefaultTapiResolver();

        mockTapiDataProducer = EasyMock.createMock(TapiDataProducer.class);
        mockTapiDataProducer.updateCacheRequest(tapiResolver);
        replay(mockTapiDataProducer);

        tapiResolver.dataProvider = mockTapiDataProducer;
    }

    @Test
    public void testGetNodeRef() {
        tapiResolver.addNodeRef(nodeRef);
        assertThat(nodeRef, is(tapiResolver.getNodeRef(deviceId)));
    }

    @Test
    public void testGetNepRefWithConnectPoint() {
        tapiResolver.addNepRef(nepRef);
        assertThat(nepRef, is(tapiResolver.getNepRef(cp)));
    }

    @Test
    public void testGetNepRefWithSipId() {
        tapiResolver.addNepRef(nepRef);
        assertThat(nepRef, is(tapiResolver.getNepRef(sipId)));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNodeRefWhenEmpty() {
        tapiResolver.getNodeRef(deviceId);
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNepRefWithConnectPointWhenEmpty() {
        tapiResolver.getNepRef(cp);
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNepRefWithSipIdWhenEmpty() {
        tapiResolver.getNepRef(cp);
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNodeRefNotExist() {
        tapiResolver.addNodeRef(nodeRef);
        tapiResolver.getNodeRef(dummyDeviceId);
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNepRefWithConnectPointNotExist() {
        tapiResolver.addNepRef(nepRef);
        tapiResolver.getNepRef(dummyCp);
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetNepRefWithSipIdNotExist() {
        tapiResolver.addNepRef(nepRef);
        tapiResolver.getNepRef(dummySipId);
    }

}