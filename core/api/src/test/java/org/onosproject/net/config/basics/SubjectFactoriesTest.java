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

package org.onosproject.net.config.basics;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.config.SubjectFactory;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.model.topo.UiTopoLayoutId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class SubjectFactoriesTest {

    class TestCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String key) {
            return new TestApplicationId(key);
        }

    }

    @Before
    public void setUp() {
        SubjectFactories.setCoreService(new TestCoreService());
    }

    @Test
    public void testAppIdFactory() {
        SubjectFactory<ApplicationId> appIdFactory = SubjectFactories.APP_SUBJECT_FACTORY;
        assertThat(appIdFactory, notNullValue());

        ApplicationId id = NetTestTools.APP_ID;
        ApplicationId createdAppId = appIdFactory.createSubject(id.name());
        assertThat(createdAppId.id(), equalTo(id.id()));
        assertThat(appIdFactory.subjectKey(id), is(id.name()));
    }

    @Test
    public void testDeviceIdFactory() {
        SubjectFactory<DeviceId> deviceIdFactory = SubjectFactories.DEVICE_SUBJECT_FACTORY;
        assertThat(deviceIdFactory, notNullValue());

        String deviceName = "d1";
        String ofDeviceName = "of:" + deviceName;
        DeviceId id = NetTestTools.did(deviceName);

        DeviceId createdDeviceId = deviceIdFactory.createSubject(ofDeviceName);
        assertThat(createdDeviceId, equalTo(id));
        assertThat(deviceIdFactory.subjectKey(id), is(ofDeviceName));
    }

    @Test
    public void testConnectPointFactory() {
        SubjectFactory<ConnectPoint> connectPointFactory = SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
        assertThat(connectPointFactory, notNullValue());

        String deviceName = "d1";
        String ofDeviceName = "of:" + deviceName;
        int devicePort = 2;
        String cpString = ofDeviceName + "/" + Integer.toString(devicePort);
        ConnectPoint cp = NetTestTools.connectPoint(deviceName, devicePort);

        ConnectPoint createdConnectPoint = connectPointFactory.createSubject(cpString);
        assertThat(createdConnectPoint, equalTo(cp));
        assertThat(connectPointFactory.subjectKey(cp), is(cpString));
    }

    @Test
    public void testHostFactory() {
        SubjectFactory<HostId> hostFactory = SubjectFactories.HOST_SUBJECT_FACTORY;
        assertThat(hostFactory, notNullValue());

        String hostName = "11:11:11:11:11:11/3";
        HostId hostId = NetTestTools.hid(hostName);

        HostId createdHostId = hostFactory.createSubject(hostName);
        assertThat(createdHostId, equalTo(hostId));
        assertThat(hostFactory.subjectKey(hostId), is(hostId.toString()));
    }

    @Test
    public void testLinkFactory() {
        SubjectFactory<LinkKey> linkFactory = SubjectFactories.LINK_SUBJECT_FACTORY;
        assertThat(linkFactory, notNullValue());

        String deviceName1 = "d1";
        String deviceName2 = "d2";
        String ofDeviceName1 = "of:" + deviceName1;
        String ofDeviceName2 = "of:" + deviceName2;
        int devicePort1 = 2;
        int devicePort2 = 3;
        String cpString1 = ofDeviceName1 + "/" + Integer.toString(devicePort1);
        String cpString2 = ofDeviceName2 + "/" + Integer.toString(devicePort2);
        ConnectPoint cp1 = NetTestTools.connectPoint(deviceName1, devicePort1);
        ConnectPoint cp2 = NetTestTools.connectPoint(deviceName2, devicePort2);
        String linkString1 = cpString1 + '-' + cpString2;
        LinkKey key1 = LinkKey.linkKey(cp1, cp2);

        LinkKey createdLink1 = linkFactory.createSubject(linkString1);
        assertThat(createdLink1.asId(), is(linkString1));
        assertThat(linkFactory.subjectKey(key1), is(linkString1));
    }

    @Test
    public void testRegionIdFactory() {
        SubjectFactory<RegionId> regionIdFactory = SubjectFactories.REGION_SUBJECT_FACTORY;
        assertThat(regionIdFactory, notNullValue());

        String region1 = "region1";
        RegionId id = RegionId.regionId(region1);

        RegionId createdRegionId = regionIdFactory.createSubject(region1);
        assertThat(createdRegionId.id(), equalTo(region1));
        assertThat(regionIdFactory.subjectKey(id), is(region1));
    }

    @Test
    public void testUITopoLayoutIdFactory() {
        SubjectFactory<UiTopoLayoutId> uiTopoLayoutIdFactory = SubjectFactories.LAYOUT_SUBJECT_FACTORY;
        assertThat(uiTopoLayoutIdFactory, notNullValue());

        String layout1 = "layout1";
        UiTopoLayoutId id = UiTopoLayoutId.layoutId(layout1);

        UiTopoLayoutId createdLayouId = uiTopoLayoutIdFactory.createSubject(layout1);
        assertThat(createdLayouId.id(), equalTo(layout1));
        assertThat(uiTopoLayoutIdFactory.subjectKey(id), is(layout1));
    }
}