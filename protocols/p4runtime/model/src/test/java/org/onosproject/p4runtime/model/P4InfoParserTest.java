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
package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Assert;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionParamModel;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchFieldModel;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiPacketOperationModel;
import org.onosproject.net.pi.model.PiPacketOperationType;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import p4.config.v1.P4InfoOuterClass.ActionRef;
import p4.config.v1.P4InfoOuterClass.MatchField;
import p4.config.v1.P4InfoOuterClass.P4Info;
import p4.config.v1.P4InfoOuterClass.Table;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * Test for P4Info Parser.
 */
public class P4InfoParserTest {
    private static final String PATH = "basic.p4info";
    private static final String PATH2 = "test_p4runtime_translation_p4info.txt";

    private final URL p4InfoUrl = P4InfoParserTest.class.getResource(PATH);
    private final URL p4InfoUrl2 = P4InfoParserTest.class.getResource(PATH2);

    private static final Long DEFAULT_MAX_TABLE_SIZE = 1024L;
    private static final Long DEFAULT_MAX_ACTION_PROFILE_SIZE = 64L;
    private static final int DEFAULT_MAX_GROUP_SIZE = 16;

    /**
     * Tests the parsing of the architecture field.
     * @throws Exception if the equality group objects does not match expected
     */
    @Test
    public void testParseArchitecture() throws Exception {
        // Generate two PiPipelineModels from the same p4Info file
        PiPipelineModel model = P4InfoParser.parse(p4InfoUrl);
        PiPipelineModel sameAsModel = P4InfoParser.parse(p4InfoUrl);

        PiPipelineModel model3 = P4InfoParser.parse(p4InfoUrl2);

        String architecture1 = model.architecture().orElse(null);
        String architecture2 = sameAsModel.architecture().orElse(null);

        assertThat("null value is returned if `arch` not present in P4Info",
                   architecture1,
                   is(nullValue()));
        assertThat("null value is returned if `arch` not present in P4Info",
                   architecture2,
                   is(nullValue()));

        String architecture3 = model3.architecture().orElse(null);
        assertThat("test that `arch` field is correctly parsed",
                   architecture3,
                   is("v1model"));
    }

    /**
     * Tests parse method.
     * @throws Exception if equality group objects dose not match as expected
     */
    @Test
    public void testParse() throws Exception {
        // Generate two PiPipelineModels from the same p4Info file
        PiPipelineModel model = P4InfoParser.parse(p4InfoUrl);
        PiPipelineModel sameAsModel = P4InfoParser.parse(p4InfoUrl);

        // Check equality
        new EqualsTester().addEqualityGroup(model, sameAsModel).testEquals();

        // Generate a P4Info object from the file
        final P4Info p4info;
        try {
            p4info = getP4InfoMessage(p4InfoUrl);
        } catch (IOException e) {
            throw new P4InfoParserException("Unable to parse protobuf " + p4InfoUrl.toString(), e);
        }
        List<Table> tableMsgs =  p4info.getTablesList();
        PiTableId table0Id = PiTableId.of(tableMsgs.get(0).getPreamble().getName());
        PiTableId wcmpTableId = PiTableId.of(tableMsgs.get(1).getPreamble().getName());
        PiTableId wcmpTableOneShotId = PiTableId.of(tableMsgs.get(2).getPreamble().getName());
        //parse tables
        PiTableModel table0Model = model.table(table0Id).orElse(null);
        PiTableModel wcmpTableModel = model.table(wcmpTableId).orElse(null);
        PiTableModel wcmpTableOneShotModel = model.table(wcmpTableOneShotId).orElse(null);
        PiTableModel table0Model2 = sameAsModel.table(table0Id).orElse(null);
        PiTableModel wcmpTableModel2 = sameAsModel.table(wcmpTableId).orElse(null);

        new EqualsTester().addEqualityGroup(table0Model, table0Model2)
                .addEqualityGroup(wcmpTableModel, wcmpTableModel2).testEquals();
        // Check existence
        assertThat("model parsed value is null", table0Model, notNullValue());
        assertThat("model parsed value is null", wcmpTableModel, notNullValue());
        assertThat("model parsed value is null", wcmpTableOneShotModel, notNullValue());
        assertThat("Incorrect size for table0 size", table0Model.maxSize(), is(equalTo(DEFAULT_MAX_TABLE_SIZE)));
        assertThat("Incorrect size for wcmp_table size", wcmpTableModel.maxSize(), is(equalTo(DEFAULT_MAX_TABLE_SIZE)));
        assertThat("Incorrect size for wcmp_table_one_shot size", wcmpTableOneShotModel.maxSize(),
                   is(equalTo(DEFAULT_MAX_TABLE_SIZE)));
        // Check one-shot annotation
        assertThat("error parsing one-shot annotation", wcmpTableModel.oneShotOnly(), is(false));
        assertThat("error parsing one-shot annotation", wcmpTableOneShotModel.oneShotOnly(), is(true));

        // Check matchFields
        List<MatchField> matchFieldList = tableMsgs.get(0).getMatchFieldsList();
        List<PiMatchFieldModel> piMatchFieldList = new ArrayList<>();
        for (MatchField matchFieldIter : matchFieldList) {
            MatchField.MatchType matchType = matchFieldIter.getMatchType();
            PiMatchType piMatchType;
            switch (matchType) {
                case EXACT: piMatchType = PiMatchType.EXACT; break;
                case LPM: piMatchType = PiMatchType.LPM; break;
                case TERNARY: piMatchType = PiMatchType.TERNARY; break;
                case RANGE: piMatchType = PiMatchType.RANGE; break;
                default: Assert.fail(); return;
            }
            piMatchFieldList.add(new P4MatchFieldModel(PiMatchFieldId.of(matchFieldIter.getName()),
                                                       matchFieldIter.getBitwidth(), piMatchType));
        }
        // Check MatchFields size
        assertThat("Incorrect size for matchFields", table0Model.matchFields().size(), is(equalTo(9)));
        // Check if matchFields are in order
        assertThat("Incorrect order for matchFields", table0Model.matchFields(), IsIterableContainingInOrder.contains(
                piMatchFieldList.get(0), piMatchFieldList.get(1),
                piMatchFieldList.get(2), piMatchFieldList.get(3),
                piMatchFieldList.get(4), piMatchFieldList.get(5),
                piMatchFieldList.get(6), piMatchFieldList.get(7),
                piMatchFieldList.get(8)));
        assertThat("Incorrect size for matchFields", wcmpTableModel.matchFields().size(), is(equalTo(1)));
        // check if matchFields are in order
        matchFieldList = tableMsgs.get(1).getMatchFieldsList();
        assertThat("Incorrect order for matchFields",
                   wcmpTableModel.matchFields(), IsIterableContainingInOrder.contains(
                        new P4MatchFieldModel(PiMatchFieldId.of(matchFieldList.get(0).getName()),
                                              matchFieldList.get(0).getBitwidth(), PiMatchType.EXACT)));
        //check table0 actionsRefs
        List<ActionRef> actionRefList = tableMsgs.get(0).getActionRefsList();
        assertThat("Incorrect size for actionRefs", actionRefList.size(), is(equalTo(4)));
        //create action instances
        PiActionId actionId = PiActionId.of("set_egress_port");
        PiActionParamId piActionParamId = PiActionParamId.of("port");
        int bitWitdth = 9;
        PiActionParamModel actionParamModel = new P4ActionParamModel(piActionParamId, bitWitdth);
        ImmutableMap<PiActionParamId, PiActionParamModel> params = new
                ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                .put(piActionParamId, actionParamModel).build();

        PiActionModel setEgressPortAction = new P4ActionModel(actionId, params);

        actionId = PiActionId.of("send_to_cpu");
        PiActionModel sendToCpuAction =
                new P4ActionModel(actionId, new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>().build());
        actionId = PiActionId.of("_drop");
        PiActionModel dropAction =
                new P4ActionModel(actionId, new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>().build());
        actionId = PiActionId.of("NoAction");
        PiActionModel noAction =
                new P4ActionModel(actionId, new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>().build());
        actionId = PiActionId.of("table0_control.set_next_hop_id");
        piActionParamId = PiActionParamId.of("next_hop_id");
        bitWitdth = 16;
        actionParamModel = new P4ActionParamModel(piActionParamId, bitWitdth);
        params = new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                .put(piActionParamId, actionParamModel).build();

        PiActionModel setNextHopIdAction = new P4ActionModel(actionId, params);

        //check table0 actions
        assertThat("action dose not match",
                   table0Model.actions(), IsIterableContainingInAnyOrder.containsInAnyOrder(
                        setEgressPortAction, sendToCpuAction, setNextHopIdAction, dropAction));
        //check wcmp_table actions
        assertThat("actions dose not match",
                   wcmpTableModel.actions(), IsIterableContainingInAnyOrder.containsInAnyOrder(
                        setEgressPortAction, noAction));

        PiActionModel table0DefaultAction = table0Model.constDefaultAction().orElse(null);

        new EqualsTester().addEqualityGroup(table0DefaultAction, dropAction).testEquals();

        // Check existence
        assertThat("model parsed value is null", table0DefaultAction, notNullValue());

        //parse action profiles
        PiTableId tableId = PiTableId.of("wcmp_control.wcmp_table");
        ImmutableSet<PiTableId> tableIds = new ImmutableSet.Builder<PiTableId>().add(tableId).build();
        PiActionProfileId actionProfileId = PiActionProfileId.of("wcmp_control.wcmp_selector");
        PiActionProfileModel wcmpSelector3 = new P4ActionProfileModel(actionProfileId, tableIds,
                                                                      true, DEFAULT_MAX_ACTION_PROFILE_SIZE,
                                                                      DEFAULT_MAX_GROUP_SIZE);
        PiActionProfileModel wcmpSelector = model.actionProfiles(actionProfileId).orElse(null);
        PiActionProfileModel wcmpSelector2 = sameAsModel.actionProfiles(actionProfileId).orElse(null);

        new EqualsTester().addEqualityGroup(wcmpSelector, wcmpSelector2, wcmpSelector3).testEquals();

        // Check existence
        assertThat("model parsed value is null", wcmpSelector, notNullValue());
        assertThat("Incorrect value for actions profiles", model.actionProfiles(), containsInAnyOrder(wcmpSelector));
        // ActionProfiles size
        assertThat("Incorrect size for action profiles", model.actionProfiles().size(), is(equalTo(1)));

        //parse counters
        PiCounterModel ingressPortCounterModel =
                model.counter(PiCounterId.of("port_counters_ingress.ingress_port_counter")).orElse(null);
        PiCounterModel egressPortCounterModel =
                model.counter(PiCounterId.of("port_counters_egress.egress_port_counter")).orElse(null);
        PiCounterModel table0CounterModel =
                model.counter(PiCounterId.of("table0_control.table0_counter")).orElse(null);
        PiCounterModel wcmpTableCounterModel =
                model.counter(PiCounterId.of("wcmp_control.wcmp_table_counter")).orElse(null);

        PiCounterModel ingressPortCounterModel2 =
                sameAsModel.counter(PiCounterId.of("port_counters_ingress.ingress_port_counter")).orElse(null);
        PiCounterModel egressPortCounterModel2 =
                sameAsModel.counter(PiCounterId.of("port_counters_egress.egress_port_counter")).orElse(null);
        PiCounterModel table0CounterModel2 =
                sameAsModel.counter(PiCounterId.of("table0_control.table0_counter")).orElse(null);
        PiCounterModel wcmpTableCounterModel2 =
                sameAsModel.counter(PiCounterId.of("wcmp_control.wcmp_table_counter")).orElse(null);

        new EqualsTester()
                .addEqualityGroup(ingressPortCounterModel, ingressPortCounterModel2)
                .addEqualityGroup(egressPortCounterModel, egressPortCounterModel2)
                .addEqualityGroup(table0CounterModel, table0CounterModel2)
                .addEqualityGroup(wcmpTableCounterModel, wcmpTableCounterModel2)
                .testEquals();

        assertThat("model parsed value is null", ingressPortCounterModel, notNullValue());
        assertThat("model parsed value is null", egressPortCounterModel, notNullValue());
        assertThat("model parsed value is null", table0CounterModel, notNullValue());
        assertThat("model parsed value is null", wcmpTableCounterModel, notNullValue());

        //Parse meters
        Collection<PiMeterModel> meterModel = model.meters();
        Collection<PiMeterModel> meterModel2 = sameAsModel.meters();

        assertThat("model parsed meter collection should be empty", meterModel.isEmpty(), is(true));
        assertThat("model parsed meter collection should be empty", meterModel2.isEmpty(), is(true));

        //parse packet operations
        PiPacketOperationModel packetInOperationalModel =
                model.packetOperationModel(PiPacketOperationType.PACKET_IN).orElse(null);
        PiPacketOperationModel packetOutOperationalModel =
                model.packetOperationModel(PiPacketOperationType.PACKET_OUT).orElse(null);

        PiPacketOperationModel packetInOperationalModel2 =
                sameAsModel.packetOperationModel(PiPacketOperationType.PACKET_IN).orElse(null);
        PiPacketOperationModel packetOutOperationalModel2 =
                sameAsModel.packetOperationModel(PiPacketOperationType.PACKET_OUT).orElse(null);

        new EqualsTester()
                .addEqualityGroup(packetInOperationalModel, packetInOperationalModel2)
                .addEqualityGroup(packetOutOperationalModel, packetOutOperationalModel2)
                .testEquals();

        // Check existence
        assertThat("model parsed value is null", packetInOperationalModel, notNullValue());
        assertThat("model parsed value is null", packetOutOperationalModel, notNullValue());
    }

    /**
     * Tests parse method with P4Runtime translation fields and optional fields.
     * @throws Exception if equality group objects dose not match as expected
     */
    @Test
    public void testParseP4RuntimeTranslationAndOptional() throws Exception {
        PiPipelineModel model = P4InfoParser.parse(p4InfoUrl2);
        // Generate a P4Info object from the file
        final P4Info p4info;
        try {
            p4info = getP4InfoMessage(p4InfoUrl2);
        } catch (IOException e) {
            throw new P4InfoParserException("Unable to parse protobuf " + p4InfoUrl.toString(), e);
        }
        List<Table> tableMsgs =  p4info.getTablesList();
        PiTableId table0Id = PiTableId.of(tableMsgs.get(0).getPreamble().getName());

        //parse tables
        PiTableModel table0Model = model.table(table0Id).orElse(null);

        // Check matchFields
        List<MatchField> matchFieldList = tableMsgs.get(0).getMatchFieldsList();
        List<PiMatchFieldModel> piMatchFieldList = new ArrayList<>();

        for (MatchField matchFieldIter : matchFieldList) {
            MatchField.MatchType matchType = matchFieldIter.getMatchType();
            PiMatchType piMatchType;
            switch (matchType) {
                case EXACT: piMatchType = PiMatchType.EXACT; break;
                case LPM: piMatchType = PiMatchType.LPM; break;
                case TERNARY: piMatchType = PiMatchType.TERNARY; break;
                case RANGE: piMatchType = PiMatchType.RANGE; break;
                case OPTIONAL: piMatchType = PiMatchType.OPTIONAL; break;
                default: Assert.fail(); return;
            }
            if (matchFieldIter.getTypeName().getName().equals("mac_addr_t")) {
                piMatchFieldList.add(new P4MatchFieldModel(PiMatchFieldId.of(matchFieldIter.getName()),
                                                           P4MatchFieldModel.BIT_WIDTH_UNDEFINED, piMatchType));
            } else {
                piMatchFieldList.add(new P4MatchFieldModel(PiMatchFieldId.of(matchFieldIter.getName()),
                                                           matchFieldIter.getBitwidth(), piMatchType));
            }
        }
        assertThat("Incorrect order for matchFields",
                   table0Model.matchFields(),
                   IsIterableContainingInOrder.contains(
                           piMatchFieldList.get(0), piMatchFieldList.get(1),
                           piMatchFieldList.get(2), piMatchFieldList.get(3)));

        //check table0 actionsRefs
        List<ActionRef> actionRefList = tableMsgs.get(0).getActionRefsList();
        assertThat("Incorrect size for actionRefs",
                   actionRefList.size(),
                   is(equalTo(4)));

        //create action instances
        // Set egress with string as parameter
        PiActionId actionId = PiActionId.of("set_egress_port");
        PiActionParamId piActionParamId = PiActionParamId.of("port");
        PiActionParamModel actionParamModel = new P4ActionParamModel(
                piActionParamId, P4ActionParamModel.BIT_WIDTH_UNDEFINED);
        ImmutableMap<PiActionParamId, PiActionParamModel> params = new
                ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                .put(piActionParamId, actionParamModel).build();
        PiActionModel setEgressPortAction = new P4ActionModel(actionId, params);

        // Set egress with 32 bit as parameter
        actionId = PiActionId.of("set_egress_port2");
        piActionParamId = PiActionParamId.of("port");
        int bitWitdth = 32;
        actionParamModel = new P4ActionParamModel(
                piActionParamId, bitWitdth);
        params = new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                .put(piActionParamId, actionParamModel).build();

        PiActionModel setEgressPortAction2 = new P4ActionModel(actionId, params);

        actionId = PiActionId.of("send_to_cpu");
        PiActionModel sendToCpuAction =
                new P4ActionModel(actionId, new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>().build());

        actionId = PiActionId.of("drop");
        PiActionModel dropAction =
                new P4ActionModel(actionId, new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>().build());

        //check table0 actions
        assertThat("action dose not match",
                   table0Model.actions(), IsIterableContainingInAnyOrder.containsInAnyOrder(
                        setEgressPortAction, setEgressPortAction2, sendToCpuAction, dropAction));
    }

    /**
     * Gets P4Info message from the URL.
     * @param p4InfoUrl link to the p4Info file
     * @return a P4Info object
     * @throws IOException if any problem occurs while reading from the URL connection.
     */
    private P4Info getP4InfoMessage(URL p4InfoUrl) throws IOException {
        InputStream p4InfoStream = p4InfoUrl.openStream();
        P4Info.Builder p4InfoBuilder = P4Info.newBuilder();
        TextFormat.getParser().merge(new InputStreamReader(p4InfoStream),
                                     ExtensionRegistry.getEmptyRegistry(),
                                     p4InfoBuilder);
        return p4InfoBuilder.build();
    }
}
