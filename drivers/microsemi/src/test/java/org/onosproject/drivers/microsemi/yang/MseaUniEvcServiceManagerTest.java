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
package org.onosproject.drivers.microsemi.yang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onosproject.drivers.microsemi.yang.impl.MseaUniEvcServiceManager;
import org.onosproject.drivers.microsemi.yang.utils.CeVlanMapUtils;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.Identifier45;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.ServiceListType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.VlanIdType;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcService;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcServiceOpParam;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.DefaultMefServices;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.MefServices;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.EvcPerUniServiceTypeEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.TagPush;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.tagpush.tagpush.PushTagTypeEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.DefaultTagPush;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.Profiles;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.DefaultProfiles;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.DefaultUni;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.Uni;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.BwpGroup;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.Cos;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.DefaultBwpGroup;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.DefaultCos;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.Bwp;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.DefaultBwp;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.bwp.ColorModeEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.DefaultEvc;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.Evc;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.UniSideInterfaceAssignmentEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.DefaultEvcPerUni;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.EvcPerUni;
import org.onosproject.drivers.microsemi.yang.custom.CustomEvcPerUnic;
import org.onosproject.drivers.microsemi.yang.custom.CustomEvcPerUnin;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnic;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnin;

public class MseaUniEvcServiceManagerTest {

    MseaUniEvcServiceManager mseaUniEvcServiceSvc;
    NetconfSession session;

    @Before
    public void setUp() throws Exception {
        try {
            mseaUniEvcServiceSvc = new MockMseaUniEvcServiceManager();
            mseaUniEvcServiceSvc.activate();
        } catch (UncheckedIOException e) {
            fail(e.getMessage());
        }
        NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo("netconf", "netconf", Ip4Address.valueOf("1.2.3.4"), 830);
        session = new MockNetconfSessionEa1000(deviceInfo);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetMseaUniEvcServiceMseaUniEvcServiceOpParamNetconfSession() {
        MefServices mefServices = new DefaultMefServices();
        mefServices.uni(new DefaultUni());

        MseaUniEvcServiceOpParam evcUni = new MseaUniEvcServiceOpParam();
        evcUni.mefServices(mefServices);

        MseaUniEvcService result = null;
        try {
            result =
                    mseaUniEvcServiceSvc.getConfigMseaUniEvcService(
                            evcUni, session, DatastoreId.RUNNING);
        } catch (NetconfException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("Error: " + e.getMessage());
        }

        assertNotNull(result);
    }

    @Test
    public void testSetMseaUniEvcServiceMseaUniEvcServiceOpParamEvcs() {
      TagPush tp1 = new DefaultTagPush();
      org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice
              .evcperuniextensionattributes.tagmanipulation
          .tagpush.TagPush tpInner1 =
              new org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317
                      .mseaunievcservice.evcperuniextensionattributes
                  .tagmanipulation.tagpush.DefaultTagPush();
      tpInner1.outerTagVlan(new VlanIdType(3));
      tpInner1.pushTagType(PushTagTypeEnum.PUSHSTAG);
      tp1.tagPush(tpInner1);

      EvcPerUnin epun1 = new CustomEvcPerUnin();
      epun1.evcPerUniServiceType(EvcPerUniServiceTypeEnum.EVPL);
      epun1.ceVlanMap(ServiceListType.fromString("10"));
      epun1.ingressBwpGroupIndex("0");
      epun1.tagManipulation(tp1);

      EvcPerUnic epuc1 = new CustomEvcPerUnic();
      epuc1.ceVlanMap(new ServiceListType("11"));
      epuc1.ingressBwpGroupIndex("0");

      EvcPerUni epu1 = new DefaultEvcPerUni();
      epu1.evcPerUnic(epuc1);
      epu1.evcPerUnin(epun1);

      List<Evc> evcList = new ArrayList<Evc>();
      Evc evc1 = new DefaultEvc();
      evc1.evcIndex(1);
      evc1.name(new Identifier45("evc-1"));
      evc1.evcPerUni(epu1);

      evcList.add(evc1);


      EvcPerUnin epun2 = new CustomEvcPerUnin();
      epun2.ceVlanMap(ServiceListType.fromString("13"));
      epun2.ingressBwpGroupIndex("0");

      EvcPerUnic epuc2 = new CustomEvcPerUnic();
      epuc2.ceVlanMap(new ServiceListType("12"));
      epuc2.ingressBwpGroupIndex("0");

      EvcPerUni epu2 = new DefaultEvcPerUni();
      epu2.evcPerUnic(epuc2);
      epu2.evcPerUnin(epun2);

      Evc evc2 = new DefaultEvc();
      evc2.evcIndex(2);
      evc2.name(new Identifier45("evc-2"));
      evc2.evcPerUni(epu2);

      evcList.add(evc2);

      Uni uni = new DefaultUni();
      uni.name(new Identifier45("testUni"));
      uni.evc(evcList);

      MefServices mefServices = new DefaultMefServices();
      mefServices.uni(uni);

      MseaUniEvcServiceOpParam evcUni = new MseaUniEvcServiceOpParam();
      evcUni.mefServices(mefServices);
      try {
          mseaUniEvcServiceSvc.setMseaUniEvcService(evcUni, session, DatastoreId.RUNNING);
      } catch (NetconfException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          fail("Error: " + e.getMessage());
      }
    }

    @Test
    public void testSetMseaUniEvcServiceMseaUniEvcServiceOpParamProfiles() {
      List<Cos> cosList = new ArrayList<Cos>();
      Cos cos0 = new DefaultCos();
      cos0.cosIndex(0);
      cos0.name("cos0");
      cosList.add(cos0);

      Cos cos1 = new DefaultCos();
      cos1.cosIndex(1);
      cos1.name("cos1");
      cosList.add(cos1);

      List<BwpGroup> bwpGroupList = new ArrayList<BwpGroup>();
      BwpGroup bwpGrp = new DefaultBwpGroup();
      bwpGrp.groupIndex((short) 0);
      bwpGroupList.add(bwpGrp);

      List<Bwp> bwpList = new ArrayList<Bwp>();
      Bwp bwp1 = new DefaultBwp();
      bwp1.cosIndex(0);
      bwp1.colorMode(ColorModeEnum.COLORAWARE);
      bwpList.add(bwp1);

      Bwp bwp2 = new DefaultBwp();
      bwp2.cosIndex(1);
      bwp2.colorMode(ColorModeEnum.COLORBLIND);
      bwpList.add(bwp2);

      BwpGroup bwpGrp1 = new DefaultBwpGroup();
      bwpGrp1.groupIndex((short) 1);
      bwpGrp1.bwp(bwpList);
      bwpGroupList.add(bwpGrp1);

      Profiles profiles = new DefaultProfiles();
      profiles.bwpGroup(bwpGroupList);

      MefServices mefServices = new DefaultMefServices();
      mefServices.profiles(profiles);

      MseaUniEvcServiceOpParam evcUni = new MseaUniEvcServiceOpParam();
      evcUni.mefServices(mefServices);
      try {
          mseaUniEvcServiceSvc.setMseaUniEvcService(evcUni, session, DatastoreId.RUNNING);
      } catch (NetconfException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          fail("Error: " + e.getMessage());
      }
    }

    @Test
    public void testDeleteMseaUniEvcServiceMseaUniEvcServiceOpParamProfiles() {
        List<Cos> cosList = new ArrayList<Cos>();
        Cos cos0 = new DefaultCos();
        cos0.cosIndex(0);
        cos0.name("cos0");
        cosList.add(cos0);

        Cos cos1 = new DefaultCos();
        cos1.cosIndex(1);
        cos1.name("cos1");
        cosList.add(cos1);

        List<BwpGroup> bwpGroupList = new ArrayList<BwpGroup>();
        BwpGroup bwpGrp = new DefaultBwpGroup();
        bwpGrp.groupIndex((short) 0);
        bwpGroupList.add(bwpGrp);

        List<Bwp> bwpList = new ArrayList<Bwp>();
        Bwp bwp1 = new DefaultBwp();
        bwp1.cosIndex(0);
        bwp1.colorMode(ColorModeEnum.COLORAWARE);
        bwpList.add(bwp1);

        Bwp bwp2 = new DefaultBwp();
        bwp2.cosIndex(1);
        bwp2.colorMode(ColorModeEnum.COLORBLIND);
        bwpList.add(bwp2);

        BwpGroup bwpGrp1 = new DefaultBwpGroup();
        bwpGrp1.groupIndex((short) 1);
        bwpGrp1.bwp(bwpList);
        bwpGroupList.add(bwpGrp1);

        Profiles profiles = new DefaultProfiles();
        profiles.bwpGroup(bwpGroupList);

        MefServices mefServices = new DefaultMefServices();
        mefServices.profiles(profiles);

        MseaUniEvcServiceOpParam evcUni = new MseaUniEvcServiceOpParam();
        evcUni.mefServices(mefServices);
        try {
            mseaUniEvcServiceSvc.deleteMseaUniEvcService(evcUni, session, DatastoreId.RUNNING);
        } catch (NetconfException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("Error: " + e.getMessage());
        }
    }

    @Test
    public void testGetMseaUniEvcCeVlanMaps() {

        try {
            MseaUniEvcService ceVlanMapsResult7 =
                    mseaUniEvcServiceSvc.getmseaUniEvcCeVlanMaps(session, DatastoreId.RUNNING);

            assertNotNull(ceVlanMapsResult7.mefServices().uni().evc());

            List<Evc> evcList = ceVlanMapsResult7.mefServices().uni().evc();
            assertEquals(3, evcList.size());
            for (Evc evc : evcList) {
               assertNotNull(evc.evcPerUni().evcPerUnic().ceVlanMap());
               assertNotNull(evc.evcPerUni().evcPerUnin().ceVlanMap());

               if (evc.evcIndex() == 7) {
                   assertEquals("700,710,720", evc.evcPerUni().evcPerUnic().ceVlanMap().string());
                   assertEquals("701:703", evc.evcPerUni().evcPerUnin().ceVlanMap().string());
               }
            }

        } catch (NetconfException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("Error: " + e.getMessage());
        }
    }

    @Test
    public void testChangeEvcCeVlanMap() {
        EvcPerUnin epun1 = new CustomEvcPerUnin();
        epun1.evcPerUniServiceType(EvcPerUniServiceTypeEnum.EVPL);
        epun1.ceVlanMap(ServiceListType.fromString("10"));
        epun1.ingressBwpGroupIndex("0");

        EvcPerUnic epuc1 = new CustomEvcPerUnic();
        epuc1.ceVlanMap(new ServiceListType("11"));
        epuc1.ingressBwpGroupIndex("0");

        EvcPerUni epu = new DefaultEvcPerUni();
        epu.evcPerUnic(epuc1);
        epu.evcPerUnin(epun1);

        Evc evc = new DefaultEvc();
        evc.evcPerUni(epu);

        assertEquals("10", evc.evcPerUni().evcPerUnin().ceVlanMap().string());
        assertEquals("11", evc.evcPerUni().evcPerUnic().ceVlanMap().string());

        assertEquals("11", evc.evcPerUni().evcPerUnic().ceVlanMap().string());
    }

    @Test
    public void testChangeEvcCeVlanMapNoValues() {
        EvcPerUnin epun1 = new CustomEvcPerUnin();
        epun1.evcPerUniServiceType(EvcPerUniServiceTypeEnum.EVPL);
        epun1.ingressBwpGroupIndex("0");

        EvcPerUnic epuc1 = new CustomEvcPerUnic();
        epuc1.ingressBwpGroupIndex("0");

        EvcPerUni epu = new DefaultEvcPerUni();
        epu.evcPerUnic(epuc1);
        epu.evcPerUnin(epun1);

        Evc evc = new DefaultEvc();
        evc.evcIndex(1);
        evc.evcPerUni(epu);

        assertEquals("0", evc.evcPerUni().evcPerUnin().ceVlanMap().string());
        assertEquals("0", evc.evcPerUni().evcPerUnic().ceVlanMap().string());
    }

    @Test
    public void testRemoveEvcUniFlowEntries() {

        Map<Integer, String> ceVlanUpdates = new TreeMap<>();
        ceVlanUpdates.put((1 << 2), "");
        ceVlanUpdates.put((1 << 2) + 1, "");
        ceVlanUpdates.put((2 << 2), "");
        ceVlanUpdates.put((2 << 2) + 1, "");

        ceVlanUpdates.put((7 << 2), "700,710,720");
        ceVlanUpdates.put((7 << 2) + 1, "701:703");
        ceVlanUpdates.put((8 << 2), "800,810,820");
        ceVlanUpdates.put((8 << 2) + 1, "801,802,803");

        Map<Integer, List<Short>> flowVlanIdMap = new HashMap<>();
        //These should get ignored because whole EVC is being deleted
        flowVlanIdMap.put(1 << 2, new ArrayList<Short>());
        flowVlanIdMap.get(1 << 2).add((short) 11);

        flowVlanIdMap.put((1 << 2) + 1, new ArrayList<Short>());
        flowVlanIdMap.get((1 << 2) + 1).add((short) 12L);

        //These are the EVCs being removed
        flowVlanIdMap.put(7 << 2, new ArrayList<Short>());
        flowVlanIdMap.get(7 << 2).add((short) 730L);
        flowVlanIdMap.get(7 << 2).add((short) 740L);

        flowVlanIdMap.put((7 << 2) + 1, new ArrayList<Short>());
        flowVlanIdMap.get((7 << 2) + 1).add((short) 705L);
        flowVlanIdMap.get((7 << 2) + 1).add((short) 706L);

        flowVlanIdMap.put(8 << 2, new ArrayList<Short>());
        flowVlanIdMap.get(8 << 2).add((short) 830L);
        flowVlanIdMap.get(8 << 2).add((short) 840L);

        flowVlanIdMap.put((8 << 2) + 1, new ArrayList<Short>());
        flowVlanIdMap.get((8 << 2) + 1).add((short) 805L);
        flowVlanIdMap.get((8 << 2) + 1).add((short) 806L);

        try {
            mseaUniEvcServiceSvc.removeEvcUniFlowEntries(
                    ceVlanUpdates, flowVlanIdMap, session, DatastoreId.RUNNING,
                    UniSideInterfaceAssignmentEnum.UNI_C_ON_OPTICS);
        } catch (NetconfException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("Error: " + e.getMessage());
        }
    }

    @Test
    public void testGetVlanSet1() {
        Short[] vlanIds = CeVlanMapUtils.getVlanSet("10");
        assertEquals(1, vlanIds.length);
    }

    @Test
    public void testGetVlanSet2() {
        Short[] vlanIds = CeVlanMapUtils.getVlanSet("10:20");
        assertEquals(11, vlanIds.length);
        assertEquals(10, vlanIds[0].shortValue());
        assertEquals(20, vlanIds[10].shortValue());
    }

    @Test
    public void testGetVlanSet3() {
        Short[] vlanIds = CeVlanMapUtils.getVlanSet("10:20,30:40");
        assertEquals(22, vlanIds.length);
        assertEquals(10, vlanIds[0].shortValue());
        assertEquals(40, vlanIds[21].shortValue());
    }

    @Test
    public void testGetVlanSet4() {
        Short[] vlanIds = CeVlanMapUtils.getVlanSet("10,20,30");
        assertEquals(3, vlanIds.length);
        assertEquals(10, vlanIds[0].shortValue());
        assertEquals(30, vlanIds[2].shortValue());
    }

    @Test
    public void testVlanListAsString() {
        assertEquals("20:22", CeVlanMapUtils.vlanListAsString(new Short[]{20, 21, 22}));

        assertEquals("20:22,24:25",
                CeVlanMapUtils.vlanListAsString(new Short[]{20, 21, 22, 24, 25}));

        assertEquals("30,33,36:40",
                CeVlanMapUtils.vlanListAsString(new Short[]{30, 33, 36, 37, 38, 39, 40}));

        assertEquals("20", CeVlanMapUtils.vlanListAsString(new Short[]{20}));

        assertEquals("20,22,24,26,28",
                CeVlanMapUtils.vlanListAsString(new Short[]{20, 22, 24, 26, 28}));
    }

    @Test
    public void testAddtoCeVlanMap() {
        assertEquals("20,22:24,26,28",
                CeVlanMapUtils.addtoCeVlanMap("20,22,24,26,28", (short) 23));

        assertEquals("20:26,28",
                CeVlanMapUtils.addtoCeVlanMap("20,21,22,24,25,26,28", (short) 23));

        assertEquals("20,23",
                CeVlanMapUtils.addtoCeVlanMap("20", (short) 23));

        assertEquals("20,22:23",
                CeVlanMapUtils.addtoCeVlanMap("20,22", (short) 23));
    }

    @Test
    public void testCombineVlanSets() {
        assertEquals("10:11,13:14", CeVlanMapUtils.combineVlanSets("10:11", "13:14"));

        assertEquals("10:14", CeVlanMapUtils.combineVlanSets("10:11", "12:14"));

        assertEquals("10:11,14", CeVlanMapUtils.combineVlanSets("10:11", "14"));

        assertEquals("10:12", CeVlanMapUtils.combineVlanSets("10:11", "11:12"));
    }

    @Test
    public void testRemoveZeroIfPossible() {
        assertEquals("0", CeVlanMapUtils.removeZeroIfPossible(""));

        assertEquals("0", CeVlanMapUtils.removeZeroIfPossible("0"));

        assertEquals("1,3", CeVlanMapUtils.removeZeroIfPossible("0:1,3"));

        assertEquals("1:2", CeVlanMapUtils.removeZeroIfPossible("0:2"));

        assertEquals("10:12", CeVlanMapUtils.removeZeroIfPossible("0,10:12"));
    }
}
