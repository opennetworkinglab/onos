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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.TextFormat;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionParamModel;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchFieldModel;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiMeterType;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPacketMetadataModel;
import org.onosproject.net.pi.model.PiPacketOperationModel;
import org.onosproject.net.pi.model.PiPacketOperationType;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiRegisterId;
import org.onosproject.net.pi.model.PiRegisterModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.model.PiTableType;
import org.slf4j.Logger;
import p4.config.v1.P4InfoOuterClass;
import p4.config.v1.P4InfoOuterClass.Action;
import p4.config.v1.P4InfoOuterClass.ActionProfile;
import p4.config.v1.P4InfoOuterClass.ActionRef;
import p4.config.v1.P4InfoOuterClass.ControllerPacketMetadata;
import p4.config.v1.P4InfoOuterClass.Counter;
import p4.config.v1.P4InfoOuterClass.CounterSpec;
import p4.config.v1.P4InfoOuterClass.DirectCounter;
import p4.config.v1.P4InfoOuterClass.DirectMeter;
import p4.config.v1.P4InfoOuterClass.MatchField;
import p4.config.v1.P4InfoOuterClass.Meter;
import p4.config.v1.P4InfoOuterClass.MeterSpec;
import p4.config.v1.P4InfoOuterClass.P4Info;
import p4.config.v1.P4InfoOuterClass.Table;
import p4.config.v1.P4Types;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.p4runtime.model.P4InfoAnnotationUtils.MAX_GROUP_SIZE_ANNOTATION;
import static org.onosproject.p4runtime.model.P4InfoAnnotationUtils.ONE_SHOT_ONLY_ANNOTATION;
import static org.onosproject.p4runtime.model.P4InfoAnnotationUtils.getAnnotationValue;
import static org.onosproject.p4runtime.model.P4InfoAnnotationUtils.isAnnotationPresent;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Parser of P4Info to PI pipeline model instances.
 */
public final class P4InfoParser {

    private static final Logger log = getLogger(P4InfoParser.class);

    private static final String PACKET_IN = "packet_in";
    private static final String PACKET_OUT = "packet_out";


    private static final Map<CounterSpec.Unit, PiCounterModel.Unit> COUNTER_UNIT_MAP =
            new ImmutableMap.Builder<CounterSpec.Unit, PiCounterModel.Unit>()
                    .put(CounterSpec.Unit.BYTES, PiCounterModel.Unit.BYTES)
                    .put(CounterSpec.Unit.PACKETS, PiCounterModel.Unit.PACKETS)
                    .put(CounterSpec.Unit.BOTH, PiCounterModel.Unit.PACKETS_AND_BYTES)
                    // Don't map UNSPECIFIED as we don't support it at the moment.
                    .build();

    private static final Map<MeterSpec.Unit, PiMeterModel.Unit> METER_UNIT_MAP =
            new ImmutableMap.Builder<MeterSpec.Unit, PiMeterModel.Unit>()
                    .put(MeterSpec.Unit.BYTES, PiMeterModel.Unit.BYTES)
                    .put(MeterSpec.Unit.PACKETS, PiMeterModel.Unit.PACKETS)
                    // Don't map UNSPECIFIED as we don't support it at the moment.
                    .build();

    private static final Map<String, PiPacketOperationType> PACKET_OPERATION_TYPE_MAP =
            new ImmutableMap.Builder<String, PiPacketOperationType>()
                    .put(PACKET_IN, PiPacketOperationType.PACKET_IN)
                    .put(PACKET_OUT, PiPacketOperationType.PACKET_OUT)
                    .build();

    private static final Map<MatchField.MatchType, PiMatchType> MATCH_TYPE_MAP =
            new ImmutableMap.Builder<MatchField.MatchType, PiMatchType>()
                    .put(MatchField.MatchType.EXACT, PiMatchType.EXACT)
                    .put(MatchField.MatchType.LPM, PiMatchType.LPM)
                    .put(MatchField.MatchType.TERNARY, PiMatchType.TERNARY)
                    .put(MatchField.MatchType.RANGE, PiMatchType.RANGE)
                    .put(MatchField.MatchType.OPTIONAL, PiMatchType.OPTIONAL)
                    // Don't map UNSPECIFIED as we don't support it at the moment.
                    .build();
    public static final int NO_SIZE = -1;

    private P4InfoParser() {
        // Utility class, hides constructor.
    }

    /**
     * Parse the given URL pointing to a P4Info file (in text format) to a PI pipeline model.
     *
     * @param p4InfoUrl URL to P4Info in text form
     * @return PI pipeline model
     * @throws P4InfoParserException if the P4Info file cannot be parsed (see message)
     */
    public static PiPipelineModel parse(URL p4InfoUrl) throws P4InfoParserException {

        final P4Info p4info;
        try {
            p4info = getP4InfoMessage(p4InfoUrl);
        } catch (IOException e) {
            throw new P4InfoParserException("Unable to parse protobuf " + p4InfoUrl.toString(), e);
        }

        // Generate fingerprint of the pipeline by hashing p4info file
        final int fingerprint;
        try {
            HashingInputStream hin = new HashingInputStream(Hashing.crc32(), p4InfoUrl.openStream());
            //noinspection StatementWithEmptyBody
            while (hin.read() != -1) {
                // Do nothing. Reading all input stream to update hash.
            }
            fingerprint = hin.hash().asInt();
        } catch (IOException e) {
            throw new P4InfoParserException("Unable to generate fingerprint " + p4InfoUrl.toString(), e);
        }

        // Start by parsing and mapping instances to to their integer P4Info IDs.
        // Convenient to build the table model at the end.

        final String architecture = parseArchitecture(p4info);

        // Counters.
        final Map<Integer, PiCounterModel> counterMap = Maps.newHashMap();
        counterMap.putAll(parseCounters(p4info));
        counterMap.putAll(parseDirectCounters(p4info));

        // Meters.
        final Map<Integer, PiMeterModel> meterMap = Maps.newHashMap();
        meterMap.putAll(parseMeters(p4info));
        meterMap.putAll(parseDirectMeters(p4info));

        // Registers.
        final Map<Integer, PiRegisterModel> registerMap = Maps.newHashMap();
        registerMap.putAll(parseRegisters(p4info));

        // Action profiles.
        final Map<Integer, PiActionProfileModel> actProfileMap = parseActionProfiles(p4info);

        // Actions.
        final Map<Integer, PiActionModel> actionMap = parseActions(p4info);

        // Controller packet metadatas.
        final Map<PiPacketOperationType, PiPacketOperationModel> pktOpMap = parseCtrlPktMetadatas(p4info);

        // Finally, parse tables.
        final ImmutableMap.Builder<PiTableId, PiTableModel> tableImmMapBuilder =
                ImmutableMap.builder();
        for (Table tableMsg : p4info.getTablesList()) {
            final PiTableId tableId = PiTableId.of(tableMsg.getPreamble().getName());
            // Parse match fields.
            final ImmutableMap.Builder<PiMatchFieldId, PiMatchFieldModel> tableFieldMapBuilder =
                    ImmutableMap.builder();
            for (MatchField fieldMsg : tableMsg.getMatchFieldsList()) {
                final PiMatchFieldId fieldId = PiMatchFieldId.of(fieldMsg.getName());
                tableFieldMapBuilder.put(
                        fieldId,
                        new P4MatchFieldModel(fieldId,
                                              isFieldString(p4info, fieldMsg.getTypeName().getName()) ?
                                                      P4MatchFieldModel.BIT_WIDTH_UNDEFINED :
                                                      fieldMsg.getBitwidth(),
                                              mapMatchFieldType(fieldMsg.getMatchType())));

            }
            // Retrieve action models by inter IDs.
            final ImmutableMap.Builder<PiActionId, PiActionModel> tableActionMapBuilder =
                    ImmutableMap.builder();
            tableMsg.getActionRefsList().stream()
                    .map(ActionRef::getId)
                    .map(actionMap::get)
                    .forEach(actionModel -> tableActionMapBuilder.put(actionModel.id(), actionModel));
            // Retrieve direct meters by integer IDs.
            final ImmutableMap.Builder<PiMeterId, PiMeterModel> tableMeterMapBuilder =
                    ImmutableMap.builder();
            tableMsg.getDirectResourceIdsList()
                    .stream()
                    .map(meterMap::get)
                    // Direct resource ID might be that of a counter.
                    // Filter out missed mapping.
                    .filter(Objects::nonNull)
                    .forEach(meterModel -> tableMeterMapBuilder.put(meterModel.id(), meterModel));
            // Retrieve direct counters by integer IDs.
            final ImmutableMap.Builder<PiCounterId, PiCounterModel> tableCounterMapBuilder =
                    ImmutableMap.builder();
            tableMsg.getDirectResourceIdsList()
                    .stream()
                    .map(counterMap::get)
                    // As before, resource ID might be that of a meter.
                    // Filter out missed mapping.
                    .filter(Objects::nonNull)
                    .forEach(counterModel -> tableCounterMapBuilder.put(counterModel.id(), counterModel));
            // Check if table supports one-shot only
            boolean oneShotOnly = isAnnotationPresent(ONE_SHOT_ONLY_ANNOTATION, tableMsg.getPreamble());
            tableImmMapBuilder.put(
                    tableId,
                    new P4TableModel(
                            PiTableId.of(tableMsg.getPreamble().getName()),
                            tableMsg.getImplementationId() == 0 ? PiTableType.DIRECT : PiTableType.INDIRECT,
                            actProfileMap.get(tableMsg.getImplementationId()),
                            tableMsg.getSize(),
                            tableCounterMapBuilder.build(),
                            tableMeterMapBuilder.build(),
                            !tableMsg.getIdleTimeoutBehavior()
                                    .equals(Table.IdleTimeoutBehavior.NO_TIMEOUT),
                            tableFieldMapBuilder.build(),
                            tableActionMapBuilder.build(),
                            actionMap.get(tableMsg.getConstDefaultActionId()),
                            tableMsg.getIsConstTable(), oneShotOnly));
        }

        // Get a map with proper PI IDs for some of those maps we created at the beginning.
        ImmutableMap<PiCounterId, PiCounterModel> counterImmMap = ImmutableMap.copyOf(
                counterMap.values().stream()
                        .collect(Collectors.toMap(PiCounterModel::id, c -> c)));
        ImmutableMap<PiMeterId, PiMeterModel> meterImmMap = ImmutableMap.copyOf(
                meterMap.values().stream()
                        .collect(Collectors.toMap(PiMeterModel::id, m -> m)));
        ImmutableMap<PiRegisterId, PiRegisterModel> registerImmMap = ImmutableMap.copyOf(
                registerMap.values().stream()
                        .collect(Collectors.toMap(PiRegisterModel::id, r -> r)));
        ImmutableMap<PiActionProfileId, PiActionProfileModel> actProfileImmMap = ImmutableMap.copyOf(
                actProfileMap.values().stream()
                        .collect(Collectors.toMap(PiActionProfileModel::id, a -> a)));

        return new P4PipelineModel(
                tableImmMapBuilder.build(),
                counterImmMap,
                meterImmMap,
                registerImmMap,
                actProfileImmMap,
                ImmutableMap.copyOf(pktOpMap),
                architecture,
                fingerprint);
    }

    private static String parseArchitecture(P4Info p4info) {
        if (p4info.hasPkgInfo()) {
            return p4info.getPkgInfo().getArch();
        }
        return null;
    }

    private static Map<Integer, PiCounterModel> parseCounters(P4Info p4info)
            throws P4InfoParserException {
        final Map<Integer, PiCounterModel> counterMap = Maps.newHashMap();
        for (Counter counterMsg : p4info.getCountersList()) {
            counterMap.put(
                    counterMsg.getPreamble().getId(),
                    new P4CounterModel(
                            PiCounterId.of(counterMsg.getPreamble().getName()),
                            PiCounterType.INDIRECT,
                            mapCounterSpecUnit(counterMsg.getSpec()),
                            null,
                            counterMsg.getSize()));
        }
        return counterMap;
    }

    private static Map<Integer, PiCounterModel> parseDirectCounters(P4Info p4info)
            throws P4InfoParserException {
        final Map<Integer, PiCounterModel> counterMap = Maps.newHashMap();
        for (DirectCounter dirCounterMsg : p4info.getDirectCountersList()) {
            counterMap.put(
                    dirCounterMsg.getPreamble().getId(),
                    new P4CounterModel(
                            PiCounterId.of(dirCounterMsg.getPreamble().getName()),
                            PiCounterType.DIRECT,
                            mapCounterSpecUnit(dirCounterMsg.getSpec()),
                            PiTableId.of(getTableName(dirCounterMsg.getDirectTableId(), p4info)),
                            NO_SIZE));
        }
        return counterMap;
    }

    private static Map<Integer, PiMeterModel> parseMeters(P4Info p4info)
            throws P4InfoParserException {
        final Map<Integer, PiMeterModel> meterMap = Maps.newHashMap();
        for (Meter meterMsg : p4info.getMetersList()) {
            meterMap.put(
                    meterMsg.getPreamble().getId(),
                    new P4MeterModel(
                            PiMeterId.of(meterMsg.getPreamble().getName()),
                            PiMeterType.INDIRECT,
                            mapMeterSpecUnit(meterMsg.getSpec()),
                            null,
                            meterMsg.getSize()));
        }
        return meterMap;
    }

    private static Map<Integer, PiMeterModel> parseDirectMeters(P4Info p4info)
            throws P4InfoParserException {
        final Map<Integer, PiMeterModel> meterMap = Maps.newHashMap();
        for (DirectMeter dirMeterMsg : p4info.getDirectMetersList()) {
            meterMap.put(
                    dirMeterMsg.getPreamble().getId(),
                    new P4MeterModel(
                            PiMeterId.of(dirMeterMsg.getPreamble().getName()),
                            PiMeterType.DIRECT,
                            mapMeterSpecUnit(dirMeterMsg.getSpec()),
                            PiTableId.of(getTableName(dirMeterMsg.getDirectTableId(), p4info)),
                            NO_SIZE));
        }
        return meterMap;
    }

    private static Map<Integer, PiRegisterModel> parseRegisters(P4Info p4info) {
        final Map<Integer, PiRegisterModel> registerMap = Maps.newHashMap();
        for (P4InfoOuterClass.Register registerMsg : p4info.getRegistersList()) {
            registerMap.put(registerMsg.getPreamble().getId(),
                            new P4RegisterModel(PiRegisterId.of(registerMsg.getPreamble().getName()),
                                                registerMsg.getSize()));
        }
        return registerMap;
    }

    private static Map<Integer, PiActionProfileModel> parseActionProfiles(P4Info p4info)
            throws P4InfoParserException {
        final Map<Integer, PiActionProfileModel> actProfileMap = Maps.newHashMap();
        for (ActionProfile actProfileMsg : p4info.getActionProfilesList()) {
            final ImmutableSet.Builder<PiTableId> tableIdSetBuilder = ImmutableSet.builder();
            for (int tableId : actProfileMsg.getTableIdsList()) {
                tableIdSetBuilder.add(PiTableId.of(getTableName(tableId, p4info)));
            }
            // TODO: we should copy all annotations to model classes for later
            //  use in the PI framework.
            // This is a temporary workaround to the inability of p4c to
            // correctly interpret P4Runtime-defined max_group_size annotation:
            // https://s3-us-west-2.amazonaws.com/p4runtime/docs/master/
            // P4Runtime-Spec.html#sec-p4info-action-profile
            final String maxSizeAnnString = getAnnotationValue(
                    MAX_GROUP_SIZE_ANNOTATION, actProfileMsg.getPreamble());
            final int maxSizeAnn = maxSizeAnnString != null
                    ? Integer.valueOf(maxSizeAnnString) : 0;
            final int maxGroupSize;
            if (actProfileMsg.getMaxGroupSize() == 0 && maxSizeAnn != 0) {
                log.warn("Found valid 'max_group_size' annotation for " +
                                 "ActionProfile {}, using that...",
                         actProfileMsg.getPreamble().getName());
                maxGroupSize = maxSizeAnn;
            } else {
                maxGroupSize = actProfileMsg.getMaxGroupSize();
            }

            actProfileMap.put(
                    actProfileMsg.getPreamble().getId(),
                    new P4ActionProfileModel(
                            PiActionProfileId.of(actProfileMsg.getPreamble().getName()),
                            tableIdSetBuilder.build(),
                            actProfileMsg.getWithSelector(),
                            actProfileMsg.getSize(),
                            maxGroupSize));
        }
        return actProfileMap;
    }

    private static Map<Integer, PiActionModel> parseActions(P4Info p4info) {
        final Map<Integer, PiActionModel> actionMap = Maps.newHashMap();
        for (Action actionMsg : p4info.getActionsList()) {
            final ImmutableMap.Builder<PiActionParamId, PiActionParamModel> paramMapBuilder =
                    ImmutableMap.builder();
            actionMsg.getParamsList().forEach(paramMsg -> {
                final PiActionParamId paramId = PiActionParamId.of(paramMsg.getName());
                paramMapBuilder.put(paramId,
                                    new P4ActionParamModel(
                                            PiActionParamId.of(paramMsg.getName()),
                                            isFieldString(p4info, paramMsg.getTypeName().getName()) ?
                                                    P4ActionParamModel.BIT_WIDTH_UNDEFINED :
                                                    paramMsg.getBitwidth()));
            });
            actionMap.put(
                    actionMsg.getPreamble().getId(),
                    new P4ActionModel(
                            PiActionId.of(actionMsg.getPreamble().getName()),
                            paramMapBuilder.build()));

        }
        return actionMap;
    }

    private static Map<PiPacketOperationType, PiPacketOperationModel> parseCtrlPktMetadatas(P4Info p4info)
            throws P4InfoParserException {
        final Map<PiPacketOperationType, PiPacketOperationModel> packetOpMap = Maps.newHashMap();
        for (ControllerPacketMetadata ctrlPktMetaMsg : p4info.getControllerPacketMetadataList()) {
            final ImmutableList.Builder<PiPacketMetadataModel> metadataListBuilder =
                    ImmutableList.builder();
            ctrlPktMetaMsg.getMetadataList().forEach(metadataMsg -> metadataListBuilder.add(
                    new P4PacketMetadataModel(PiPacketMetadataId.of(metadataMsg.getName()),
                                              isFieldString(p4info, metadataMsg.getTypeName().getName()) ?
                                                      P4PacketMetadataModel.BIT_WIDTH_UNDEFINED :
                                                      metadataMsg.getBitwidth())));
            packetOpMap.put(
                    mapPacketOpType(ctrlPktMetaMsg.getPreamble().getName()),
                    new P4PacketOperationModel(mapPacketOpType(ctrlPktMetaMsg.getPreamble().getName()),
                                               metadataListBuilder.build()));

        }
        return packetOpMap;
    }

    private static P4Info getP4InfoMessage(URL p4InfoUrl) throws IOException {
        InputStream p4InfoStream = p4InfoUrl.openStream();
        P4Info.Builder p4InfoBuilder = P4Info.newBuilder();
        TextFormat.getParser().merge(new InputStreamReader(p4InfoStream),
                                     ExtensionRegistry.getEmptyRegistry(),
                                     p4InfoBuilder);
        return p4InfoBuilder.build();
    }

    private static String getTableName(int id, P4Info p4info)
            throws P4InfoParserException {
        return p4info.getTablesList().stream()
                .filter(t -> t.getPreamble().getId() == id)
                .findFirst()
                .orElseThrow(() -> new P4InfoParserException(format(
                        "Not such table with ID %d", id)))
                .getPreamble()
                .getName();
    }

    private static PiCounterModel.Unit mapCounterSpecUnit(CounterSpec spec)
            throws P4InfoParserException {
        if (!COUNTER_UNIT_MAP.containsKey(spec.getUnit())) {
            throw new P4InfoParserException(format(
                    "Unrecognized counter unit '%s'", spec.getUnit()));
        }
        return COUNTER_UNIT_MAP.get(spec.getUnit());
    }

    private static PiMeterModel.Unit mapMeterSpecUnit(MeterSpec spec)
            throws P4InfoParserException {
        if (!METER_UNIT_MAP.containsKey(spec.getUnit())) {
            throw new P4InfoParserException(format(
                    "Unrecognized meter unit '%s'", spec.getUnit()));
        }
        return METER_UNIT_MAP.get(spec.getUnit());
    }

    private static PiPacketOperationType mapPacketOpType(String name)
            throws P4InfoParserException {
        if (!PACKET_OPERATION_TYPE_MAP.containsKey(name)) {
            throw new P4InfoParserException(format(
                    "Unrecognized controller packet metadata name '%s'", name));
        }
        return PACKET_OPERATION_TYPE_MAP.get(name);
    }

    private static PiMatchType mapMatchFieldType(MatchField.MatchType type)
            throws P4InfoParserException {
        if (!MATCH_TYPE_MAP.containsKey(type)) {
            throw new P4InfoParserException(format(
                    "Unrecognized match field type '%s'", type));
        }
        return MATCH_TYPE_MAP.get(type);
    }

    private static boolean isFieldString(P4Info p4info, String fieldTypeName) {
        P4Types.P4TypeInfo p4TypeInfo = p4info.getTypeInfo();
        return p4TypeInfo.containsNewTypes(fieldTypeName) &&
                p4TypeInfo.getNewTypesOrThrow(fieldTypeName).hasTranslatedType() &&
                p4TypeInfo.getNewTypesOrThrow(fieldTypeName).getTranslatedType().hasSdnString();
    }
}
