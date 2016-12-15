/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types.rev20160229.mseatypes.Identifier45;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types.rev20160229.mseatypes.ServiceListType;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.types.rev20160229.mseatypes.VlanIdType;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.MseaUniEvcService;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.MseaUniEvcServiceOpParam;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.DefaultMefServices;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.MefServices;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.evcperuniextensionattributes.EvcPerUniServiceTypeEnum;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.TagPush;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.tagpush.tagpush.PushTagTypeEnum;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.DefaultTagPush;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.Profiles;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.DefaultProfiles;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.DefaultUni;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.Uni;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.profiles.BwpGroup;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.profiles.Cos;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.profiles.DefaultBwpGroup;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.profiles.DefaultCos;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.Bwp;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.DefaultBwp;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.profiles.bwpgroup.bwp.ColorModeEnum;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.CustomEvc;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.DefaultEvc;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.Evc;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.UniSideInterfaceAssignmentEnum;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.DefaultEvcPerUni;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.EvcPerUni;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.CustomEvcPerUnic;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.CustomEvcPerUnin;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.DefaultEvcPerUnic;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.DefaultEvcPerUnin;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnic;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc.service.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnin;
import org.onosproject.yms.ymsm.YmsService;

public class MseaUniEvcServiceManagerTest {

    MseaUniEvcServiceManager mseaUniEvcServiceSvc;
    YmsService ymsService;
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
        Uni.UniBuilder uniBuilder = new DefaultUni.UniBuilder();

        MefServices.MefServicesBuilder mefBuilder = new DefaultMefServices.MefServicesBuilder();
        MefServices mefServices = mefBuilder.uni(uniBuilder.build()).build();

        MseaUniEvcService.MseaUniEvcServiceBuilder evcUniBuilder =
                new MseaUniEvcServiceOpParam.MseaUniEvcServiceBuilder();

        MseaUniEvcServiceOpParam mseaUniEvcServiceFilter =
                (MseaUniEvcServiceOpParam) evcUniBuilder.mefServices(mefServices).build();
        MseaUniEvcService result = null;
        try {
            result =
                    mseaUniEvcServiceSvc.getConfigMseaUniEvcService(
                            mseaUniEvcServiceFilter, session, TargetConfig.RUNNING);
        } catch (NetconfException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("Error: " + e.getMessage());
        }

        assertNotNull(result);
    }

    @Test
    public void testSetMseaUniEvcServiceMseaUniEvcServiceOpParamEvcs() {
      TagPush.TagPushBuilder tpBuilder1 = new DefaultTagPush.TagPushBuilder();
      org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.uni.evc
          .service.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation
          .tagpush.TagPush.TagPushBuilder tpInnerBuilder1 =
              new org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea
                  .uni.evc.service.rev20160317.mseaunievcservice.evcperuniextensionattributes
                  .tagmanipulation.tagpush.DefaultTagPush.TagPushBuilder();
      TagPush tp1 = tpBuilder1
          .tagPush(tpInnerBuilder1
                  .outerTagVlan(new VlanIdType(3))
                  .pushTagType(PushTagTypeEnum.PUSHSTAG)
                  .build())
          .build();

      EvcPerUnin.EvcPerUninBuilder epunBuilder1 = new DefaultEvcPerUnin.EvcPerUninBuilder();
      EvcPerUnin epun1 = epunBuilder1
              .evcPerUniServiceType(EvcPerUniServiceTypeEnum.EVPL)
              .ceVlanMap(ServiceListType.fromString("10"))
              .ingressBwpGroupIndex("0")
              .tagManipulation(tp1)
              .build();

      EvcPerUnic.EvcPerUnicBuilder epucBuilder1 = new DefaultEvcPerUnic.EvcPerUnicBuilder();
      EvcPerUnic epuc1 = epucBuilder1
              .ceVlanMap(new ServiceListType("11"))
              .ingressBwpGroupIndex("0")
              .build();

      EvcPerUni.EvcPerUniBuilder epuBuilder = new DefaultEvcPerUni.EvcPerUniBuilder();

      List<Evc> evcList = new ArrayList<Evc>();
      Evc.EvcBuilder evcBuilder1 = new DefaultEvc.EvcBuilder();
      evcList.add(evcBuilder1
              .evcIndex(1)
              .name(new Identifier45("evc-1"))
              .evcPerUni(epuBuilder.evcPerUnin(epun1).evcPerUnic(epuc1).build())
              .build());


      EvcPerUnin.EvcPerUninBuilder epunBuilder2 = new DefaultEvcPerUnin.EvcPerUninBuilder();
      EvcPerUnin epun2 = epunBuilder2
              .ceVlanMap(ServiceListType.fromString("13"))
              .ingressBwpGroupIndex("0")
              .build();

      EvcPerUnic.EvcPerUnicBuilder epucBuilder2 = new DefaultEvcPerUnic.EvcPerUnicBuilder();
      EvcPerUnic epuc2 = epucBuilder2
              .ceVlanMap(new ServiceListType("12"))
              .ingressBwpGroupIndex("0")
              .build();

      Evc.EvcBuilder evcBuilder2 = new DefaultEvc.EvcBuilder();
      evcList.add(evcBuilder2
              .evcIndex(2)
              .name(new Identifier45("evc-2"))
              .evcPerUni(epuBuilder.evcPerUnin(epun2).evcPerUnic(epuc2).build())
              .build());

      Uni.UniBuilder uniBuilder = new DefaultUni.UniBuilder();
      Uni uni = uniBuilder.name(new Identifier45("testUni")).evc(evcList).build();

      MefServices.MefServicesBuilder mefBuilder = new DefaultMefServices.MefServicesBuilder();
      MefServices mefServices = mefBuilder.uni(uni).build();

      MseaUniEvcService.MseaUniEvcServiceBuilder evcUniBuilder =
              new MseaUniEvcServiceOpParam.MseaUniEvcServiceBuilder();

      MseaUniEvcServiceOpParam mseaUniEvcServiceFilter =
              (MseaUniEvcServiceOpParam) evcUniBuilder.mefServices(mefServices).build();
      try {
          mseaUniEvcServiceSvc.setMseaUniEvcService(mseaUniEvcServiceFilter, session, TargetConfig.RUNNING);
      } catch (NetconfException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          fail("Error: " + e.getMessage());
      }
    }

    @Test
    public void testSetMseaUniEvcServiceMseaUniEvcServiceOpParamProfiles() {
      List<Cos> cosList = new ArrayList<Cos>();
      Cos.CosBuilder cosBuilder0 = new DefaultCos.CosBuilder();
      cosList.add(cosBuilder0.cosIndex(0).name("cos0").build());
      Cos.CosBuilder cosBuilder1 = new DefaultCos.CosBuilder();
      cosList.add(cosBuilder1.cosIndex(1).name("cos1").build());

      List<BwpGroup> bwpGroupList = new ArrayList<BwpGroup>();
      BwpGroup.BwpGroupBuilder bwpGrpBuilder = new DefaultBwpGroup.BwpGroupBuilder();
      bwpGroupList.add(bwpGrpBuilder.groupIndex((short) 0).build());

      List<Bwp> bwpList = new ArrayList<Bwp>();
      Bwp.BwpBuilder bwpBuilder1 = new DefaultBwp.BwpBuilder();
      bwpList.add(bwpBuilder1.cosIndex(0).colorMode(ColorModeEnum.COLORAWARE).build());
      Bwp.BwpBuilder bwpBuilder2 = new DefaultBwp.BwpBuilder();
      bwpList.add(bwpBuilder2.cosIndex(1).colorMode(ColorModeEnum.COLORBLIND).build());

      BwpGroup.BwpGroupBuilder bwpGrpBuilder1 = new DefaultBwpGroup.BwpGroupBuilder();
      bwpGroupList.add(bwpGrpBuilder1.groupIndex((short) 1).bwp(bwpList).build());

      Profiles.ProfilesBuilder profilesBuilder = new DefaultProfiles.ProfilesBuilder();
      Profiles profiles = profilesBuilder.bwpGroup(bwpGroupList).build();

      MefServices.MefServicesBuilder mefBuilder = new DefaultMefServices.MefServicesBuilder();
      MefServices mefServices = mefBuilder.profiles(profiles).build();

      MseaUniEvcService.MseaUniEvcServiceBuilder evcUniBuilder =
              new MseaUniEvcServiceOpParam.MseaUniEvcServiceBuilder();

      MseaUniEvcServiceOpParam mseaUniEvcServiceFilter =
              (MseaUniEvcServiceOpParam) evcUniBuilder.mefServices(mefServices).build();
      try {
          mseaUniEvcServiceSvc.setMseaUniEvcService(mseaUniEvcServiceFilter, session, TargetConfig.RUNNING);
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
                    mseaUniEvcServiceSvc.getmseaUniEvcCeVlanMaps(session, TargetConfig.RUNNING);

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
        EvcPerUnin.EvcPerUninBuilder epunBuilder1 = new DefaultEvcPerUnin.EvcPerUninBuilder();
        EvcPerUnin epun1 = epunBuilder1
                .evcPerUniServiceType(EvcPerUniServiceTypeEnum.EVPL)
                .ceVlanMap(ServiceListType.fromString("10"))
                .ingressBwpGroupIndex("0")
                .build();

        EvcPerUnic.EvcPerUnicBuilder epucBuilder1 = new DefaultEvcPerUnic.EvcPerUnicBuilder();
        EvcPerUnic epuc1 = epucBuilder1
                .ceVlanMap(new ServiceListType("11"))
                .ingressBwpGroupIndex("0")
                .build();

        EvcPerUni.EvcPerUniBuilder epuBuilder = DefaultEvcPerUni.builder().evcPerUnic(epuc1).evcPerUnin(epun1);

        Evc evc = CustomEvc.builder().evcPerUni(epuBuilder.build()).build();

        assertEquals("10", evc.evcPerUni().evcPerUnin().ceVlanMap().string());
        assertEquals("11", evc.evcPerUni().evcPerUnic().ceVlanMap().string());

        evc = CustomEvc.builder(evc).addToCeVlanMap(new ServiceListType("12,13"), UniSide.NETWORK).build();

        assertEquals("10,12:13", evc.evcPerUni().evcPerUnin().ceVlanMap().string());
        assertEquals("11", evc.evcPerUni().evcPerUnic().ceVlanMap().string());

    }

    @Test
    public void testChangeEvcCeVlanMapNoValues() {
        EvcPerUnin.EvcPerUninBuilder epunBuilder1 = CustomEvcPerUnin.builder();
        EvcPerUnin epun1 = epunBuilder1
                .evcPerUniServiceType(EvcPerUniServiceTypeEnum.EVPL)
                .ingressBwpGroupIndex("0")
                .build();

        EvcPerUnic.EvcPerUnicBuilder epucBuilder1 = CustomEvcPerUnic.builder();
        EvcPerUnic epuc1 = epucBuilder1
                .ingressBwpGroupIndex("0")
                .build();

        EvcPerUni.EvcPerUniBuilder epuBuilder = DefaultEvcPerUni.builder().evcPerUnic(epuc1).evcPerUnin(epun1);

        Evc evc = CustomEvc.builder().evcPerUni(epuBuilder.build()).build();

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
                    ceVlanUpdates, flowVlanIdMap, session, TargetConfig.RUNNING,
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
