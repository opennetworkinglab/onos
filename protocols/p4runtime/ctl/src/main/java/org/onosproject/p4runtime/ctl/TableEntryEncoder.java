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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTableId;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.onosproject.net.pi.runtime.PiValidFieldMatch;
import org.slf4j.Logger;
import p4.P4RuntimeOuterClass.Action;
import p4.P4RuntimeOuterClass.FieldMatch;
import p4.P4RuntimeOuterClass.TableAction;
import p4.P4RuntimeOuterClass.TableEntry;
import p4.config.P4InfoOuterClass;

import java.util.Collection;
import java.util.Collections;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Encoder of table entries, from ONOS Pi* format, to P4Runtime protobuf messages, and vice versa.
 */
final class TableEntryEncoder {


    private static final Logger log = getLogger(TableEntryEncoder.class);

    private static final String HEADER_PREFIX = "hdr.";
    private static final String VALUE_OF_PREFIX = "value of ";
    private static final String MASK_OF_PREFIX = "mask of ";
    private static final String HIGH_RANGE_VALUE_OF_PREFIX = "high range value of ";
    private static final String LOW_RANGE_VALUE_OF_PREFIX = "low range value of ";

    // TODO: implement cache of encoded entities.

    private TableEntryEncoder() {
        // hide.
    }

    /**
     * Returns a collection of P4Runtime table entry protobuf messages, encoded from the given collection of PI
     * table entries for the given pipeconf. If a PI table entry cannot be encoded, it is skipped, hence the returned
     * collection might have different size than the input one.
     * <p>
     * Please check the log for an explanation of any error that might have occurred.
     *
     * @param piTableEntries PI table entries
     * @param pipeconf       PI pipeconf
     * @return collection of P4Runtime table entry protobuf messages
     */
    static Collection<TableEntry> encode(Collection<PiTableEntry> piTableEntries, PiPipeconf pipeconf) {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}, skipping encoding of all table entries");
            return Collections.emptyList();
        }

        ImmutableList.Builder<TableEntry> tableEntryMsgListBuilder = ImmutableList.builder();

        for (PiTableEntry piTableEntry : piTableEntries) {
            try {
                tableEntryMsgListBuilder.add(encodePiTableEntry(piTableEntry, browser));
            } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                log.error("Unable to encode PI table entry: {}", e.getMessage());
            }
        }

        return tableEntryMsgListBuilder.build();
    }

    /**
     * Returns a collection of PI table entry objects, decoded from the given collection of P4Runtime table entry
     * messages for the given pipeconf. If a table entry message cannot be decoded, it is skipped, hence the returned
     * collection might have different size than the input one.
     * <p>
     * Please check the log for an explanation of any error that might have occurred.
     *
     * @param tableEntryMsgs P4Runtime table entry messages
     * @param pipeconf       PI pipeconf
     * @return collection of PI table entry objects
     */
    static Collection<PiTableEntry> decode(Collection<TableEntry> tableEntryMsgs, PiPipeconf pipeconf) {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}, skipping decoding of all table entries");
            return Collections.emptyList();
        }

        ImmutableList.Builder<PiTableEntry> piTableEntryListBuilder = ImmutableList.builder();

        for (TableEntry tableEntryMsg : tableEntryMsgs) {
            try {
                piTableEntryListBuilder.add(decodeTableEntryMsg(tableEntryMsg, browser));
            } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                log.error("Unable to decode table entry message: {}", e.getMessage());
            }
        }

        return piTableEntryListBuilder.build();
    }

    private static TableEntry encodePiTableEntry(PiTableEntry piTableEntry, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        TableEntry.Builder tableEntryMsgBuilder = TableEntry.newBuilder();

        P4InfoOuterClass.Table tableInfo = browser.tables().getByName(piTableEntry.table().id());

        // Table id.
        tableEntryMsgBuilder.setTableId(tableInfo.getPreamble().getId());

        // Priority.
        // FIXME: check on P4Runtime if/what is the defaulr priority.
        int priority = piTableEntry.priority().orElse(0);
        tableEntryMsgBuilder.setPriority(priority);

        // Controller metadata (cookie)
        tableEntryMsgBuilder.setControllerMetadata(piTableEntry.cookie());

        // Timeout.
        if (piTableEntry.timeout().isPresent()) {
            log.warn("Found PI table entry with timeout set, not supported in P4Runtime: {}", piTableEntry);
        }

        // Table action.
        tableEntryMsgBuilder.setAction(encodePiTableAction(piTableEntry.action(), browser));

        // Field matches.
        for (PiFieldMatch piFieldMatch : piTableEntry.fieldMatches()) {
            tableEntryMsgBuilder.addMatch(encodePiFieldMatch(piFieldMatch, tableInfo, browser));
        }

        return tableEntryMsgBuilder.build();
    }

    private static PiTableEntry decodeTableEntryMsg(TableEntry tableEntryMsg, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        PiTableEntry.Builder piTableEntryBuilder = PiTableEntry.builder();

        P4InfoOuterClass.Table tableInfo = browser.tables().getById(tableEntryMsg.getTableId());

        // Table id.
        piTableEntryBuilder.forTable(PiTableId.of(tableInfo.getPreamble().getName()));

        // Priority.
        piTableEntryBuilder.withPriority(tableEntryMsg.getPriority());

        // Controller metadata (cookie)
        piTableEntryBuilder.withCookie(tableEntryMsg.getControllerMetadata());

        // Table action.
        piTableEntryBuilder.withAction(decodeTableActionMsg(tableEntryMsg.getAction(), browser));

        // Timeout.
        // FIXME: how to decode table entry messages with timeout, given that the timeout value is lost after encoding?

        // Field matches.
        for (FieldMatch fieldMatchMsg : tableEntryMsg.getMatchList()) {
            piTableEntryBuilder.withFieldMatch(decodeFieldMatchMsg(fieldMatchMsg, tableInfo, browser));
        }

        return piTableEntryBuilder.build();
    }

    private static FieldMatch encodePiFieldMatch(PiFieldMatch piFieldMatch, P4InfoOuterClass.Table tableInfo,
                                                 P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        FieldMatch.Builder fieldMatchMsgBuilder = FieldMatch.newBuilder();

        // FIXME: check how field names for stacked headers are constructed in P4Runtime.
        String fieldName = piFieldMatch.fieldId().id();
        int tableId = tableInfo.getPreamble().getId();
        P4InfoOuterClass.MatchField matchFieldInfo = browser.matchFields(tableId).getByNameOrAlias(fieldName);
        String entityName = format("field match '%s' of table '%s'",
                                   matchFieldInfo.getName(), tableInfo.getPreamble().getName());
        int fieldId = matchFieldInfo.getId();
        int fieldBitwidth = matchFieldInfo.getBitwidth();

        fieldMatchMsgBuilder.setFieldId(fieldId);

        switch (piFieldMatch.type()) {
            case EXACT:
                PiExactFieldMatch fieldMatch = (PiExactFieldMatch) piFieldMatch;
                ByteString exactValue = ByteString.copyFrom(fieldMatch.value().asReadOnlyBuffer());
                assertSize(VALUE_OF_PREFIX + entityName, exactValue, fieldBitwidth);
                return fieldMatchMsgBuilder.setExact(
                        FieldMatch.Exact
                                .newBuilder()
                                .setValue(exactValue)
                                .build())
                        .build();
            case TERNARY:
                PiTernaryFieldMatch ternaryMatch = (PiTernaryFieldMatch) piFieldMatch;
                ByteString ternaryValue = ByteString.copyFrom(ternaryMatch.value().asReadOnlyBuffer());
                ByteString ternaryMask = ByteString.copyFrom(ternaryMatch.mask().asReadOnlyBuffer());
                assertSize(VALUE_OF_PREFIX + entityName, ternaryValue, fieldBitwidth);
                assertSize(MASK_OF_PREFIX + entityName, ternaryMask, fieldBitwidth);
                return fieldMatchMsgBuilder.setTernary(
                        FieldMatch.Ternary
                                .newBuilder()
                                .setValue(ternaryValue)
                                .setMask(ternaryMask)
                                .build())
                        .build();
            case LPM:
                PiLpmFieldMatch lpmMatch = (PiLpmFieldMatch) piFieldMatch;
                ByteString lpmValue = ByteString.copyFrom(lpmMatch.value().asReadOnlyBuffer());
                int lpmPrefixLen = lpmMatch.prefixLength();
                assertSize(VALUE_OF_PREFIX + entityName, lpmValue, fieldBitwidth);
                assertPrefixLen(entityName, lpmPrefixLen, fieldBitwidth);
                return fieldMatchMsgBuilder.setLpm(
                        FieldMatch.LPM.newBuilder()
                                .setValue(lpmValue)
                                .setPrefixLen(lpmPrefixLen)
                                .build())
                        .build();
            case RANGE:
                PiRangeFieldMatch rangeMatch = (PiRangeFieldMatch) piFieldMatch;
                ByteString rangeHighValue = ByteString.copyFrom(rangeMatch.highValue().asReadOnlyBuffer());
                ByteString rangeLowValue = ByteString.copyFrom(rangeMatch.lowValue().asReadOnlyBuffer());
                assertSize(HIGH_RANGE_VALUE_OF_PREFIX + entityName, rangeHighValue, fieldBitwidth);
                assertSize(LOW_RANGE_VALUE_OF_PREFIX + entityName, rangeLowValue, fieldBitwidth);
                return fieldMatchMsgBuilder.setRange(
                        FieldMatch.Range.newBuilder()
                                .setHigh(rangeHighValue)
                                .setLow(rangeLowValue)
                                .build())
                        .build();
            case VALID:
                PiValidFieldMatch validMatch = (PiValidFieldMatch) piFieldMatch;
                return fieldMatchMsgBuilder.setValid(
                        FieldMatch.Valid.newBuilder()
                                .setValue(validMatch.isValid())
                                .build())
                        .build();
            default:
                throw new EncodeException(format(
                        "Building of match type %s not implemented", piFieldMatch.type()));
        }
    }

    private static PiFieldMatch decodeFieldMatchMsg(FieldMatch fieldMatchMsg, P4InfoOuterClass.Table tableInfo,
                                                    P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        int tableId = tableInfo.getPreamble().getId();
        String fieldMatchName = browser.matchFields(tableId).getById(fieldMatchMsg.getFieldId()).getName();
        if (fieldMatchName.startsWith(HEADER_PREFIX)) {
            fieldMatchName = fieldMatchName.substring(HEADER_PREFIX.length());
        }

        // FIXME: Add support for decoding of stacked header names.
        String[] pieces = fieldMatchName.split("\\.");
        if (pieces.length != 2) {
            throw new EncodeException(format("unrecognized field match name '%s'", fieldMatchName));
        }
        PiHeaderFieldId headerFieldId = PiHeaderFieldId.of(pieces[0], pieces[1]);

        FieldMatch.FieldMatchTypeCase typeCase = fieldMatchMsg.getFieldMatchTypeCase();

        switch (typeCase) {
            case EXACT:
                FieldMatch.Exact exactFieldMatch = fieldMatchMsg.getExact();
                ImmutableByteSequence exactValue = copyFrom(exactFieldMatch.getValue().asReadOnlyByteBuffer());
                return new PiExactFieldMatch(headerFieldId, exactValue);
            case TERNARY:
                FieldMatch.Ternary ternaryFieldMatch = fieldMatchMsg.getTernary();
                ImmutableByteSequence ternaryValue = copyFrom(ternaryFieldMatch.getValue().asReadOnlyByteBuffer());
                ImmutableByteSequence ternaryMask = copyFrom(ternaryFieldMatch.getMask().asReadOnlyByteBuffer());
                return new PiTernaryFieldMatch(headerFieldId, ternaryValue, ternaryMask);
            case LPM:
                FieldMatch.LPM lpmFieldMatch = fieldMatchMsg.getLpm();
                ImmutableByteSequence lpmValue = copyFrom(lpmFieldMatch.getValue().asReadOnlyByteBuffer());
                int lpmPrefixLen = lpmFieldMatch.getPrefixLen();
                return new PiLpmFieldMatch(headerFieldId, lpmValue, lpmPrefixLen);
            case RANGE:
                FieldMatch.Range rangeFieldMatch = fieldMatchMsg.getRange();
                ImmutableByteSequence rangeHighValue = copyFrom(rangeFieldMatch.getHigh().asReadOnlyByteBuffer());
                ImmutableByteSequence rangeLowValue = copyFrom(rangeFieldMatch.getLow().asReadOnlyByteBuffer());
                return new PiRangeFieldMatch(headerFieldId, rangeLowValue, rangeHighValue);
            case VALID:
                FieldMatch.Valid validFieldMatch = fieldMatchMsg.getValid();
                return new PiValidFieldMatch(headerFieldId, validFieldMatch.getValue());
            default:
                throw new EncodeException(format(
                        "Decoding of field match type '%s' not implemented", typeCase.name()));
        }
    }

    private static TableAction encodePiTableAction(PiTableAction piTableAction, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        TableAction.Builder tableActionMsgBuilder = TableAction.newBuilder();

        switch (piTableAction.type()) {
            case ACTION:
                PiAction piAction = (PiAction) piTableAction;
                int actionId = browser.actions().getByName(piAction.id().name()).getPreamble().getId();

                Action.Builder actionMsgBuilder = Action.newBuilder().setActionId(actionId);

                for (PiActionParam p : piAction.parameters()) {
                    P4InfoOuterClass.Action.Param paramInfo = browser.actionParams(actionId).getByName(p.id().name());
                    ByteString paramValue = ByteString.copyFrom(p.value().asReadOnlyBuffer());
                    assertSize(format("param '%s' of action '%s'", p.id(), piAction.id()),
                               paramValue, paramInfo.getBitwidth());
                    actionMsgBuilder.addParams(Action.Param.newBuilder()
                                                       .setParamId(paramInfo.getId())
                                                       .setValue(paramValue)
                                                       .build());
                }

                tableActionMsgBuilder.setAction(actionMsgBuilder.build());
                break;

            default:
                throw new EncodeException(
                        format("Building of table action type %s not implemented", piTableAction.type()));
        }

        return tableActionMsgBuilder.build();
    }

    private static PiTableAction decodeTableActionMsg(TableAction tableActionMsg, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        TableAction.TypeCase typeCase = tableActionMsg.getTypeCase();

        switch (typeCase) {
            case ACTION:
                PiAction.Builder piActionBuilder = PiAction.builder();
                Action actionMsg = tableActionMsg.getAction();
                // Action ID.
                int actionId = actionMsg.getActionId();
                String actionName = browser.actions().getById(actionId).getPreamble().getName();
                piActionBuilder.withId(PiActionId.of(actionName));
                // Params.
                for (Action.Param paramMsg : actionMsg.getParamsList()) {
                    String paramName = browser.actionParams(actionId).getById(paramMsg.getParamId()).getName();
                    ImmutableByteSequence paramValue = copyFrom(paramMsg.getValue().asReadOnlyByteBuffer());
                    piActionBuilder.withParameter(new PiActionParam(PiActionParamId.of(paramName), paramValue));
                }
                return piActionBuilder.build();

            default:
                throw new EncodeException(
                        format("Decoding of table action type %s not implemented", typeCase.name()));
        }
    }

    private static void assertSize(String entityDescr, ByteString value, int bitWidth)
            throws EncodeException {

        int byteWidth = (int) Math.ceil((float) bitWidth / 8);
        if (value.size() != byteWidth) {
            throw new EncodeException(format("Wrong size for %s, expected %d bytes, but found %d",
                                             entityDescr, byteWidth, value.size()));
        }
    }

    private static void assertPrefixLen(String entityDescr, int prefixLength, int bitWidth)
            throws EncodeException {

        if (prefixLength > bitWidth) {
            throw new EncodeException(format(
                    "wrong prefix length for %s, field size is %d bits, but found one is %d",
                    entityDescr, bitWidth, prefixLength));
        }
    }
}
