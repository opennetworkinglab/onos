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

package org.onosproject.p4runtime.ctl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionGroupId;
import org.onosproject.net.pi.runtime.PiActionGroupMemberId;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiExactFieldMatch;
import org.onosproject.net.pi.runtime.PiFieldMatch;
import org.onosproject.net.pi.runtime.PiLpmFieldMatch;
import org.onosproject.net.pi.runtime.PiMatchKey;
import org.onosproject.net.pi.runtime.PiRangeFieldMatch;
import org.onosproject.net.pi.runtime.PiTableAction;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.net.pi.runtime.PiTernaryFieldMatch;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass.Action;
import p4.v1.P4RuntimeOuterClass.FieldMatch;
import p4.v1.P4RuntimeOuterClass.TableAction;
import p4.v1.P4RuntimeOuterClass.TableEntry;
import p4.config.v1.P4InfoOuterClass;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.p4runtime.ctl.P4RuntimeUtils.assertPrefixLen;
import static org.onosproject.p4runtime.ctl.P4RuntimeUtils.assertSize;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Encoder/Decoder of table entries, from ONOS Pi* format, to P4Runtime protobuf messages, and vice versa.
 */
final class TableEntryEncoder {
    private static final Logger log = getLogger(TableEntryEncoder.class);

    private static final String VALUE_OF_PREFIX = "value of ";
    private static final String MASK_OF_PREFIX = "mask of ";
    private static final String HIGH_RANGE_VALUE_OF_PREFIX = "high range value of ";
    private static final String LOW_RANGE_VALUE_OF_PREFIX = "low range value of ";

    // TODO: implement cache of encoded entities.

    private TableEntryEncoder() {
        // hide.
    }

    /**
     * Returns a collection of P4Runtime table entry protobuf messages, encoded
     * from the given collection of PI table entries for the given pipeconf. If
     * a PI table entry cannot be encoded, an EncodeException is thrown.
     *
     * @param piTableEntries PI table entries
     * @param pipeconf       PI pipeconf
     * @return collection of P4Runtime table entry protobuf messages
     * @throws EncodeException if a PI table entry cannot be encoded
     */
    static Collection<TableEntry> encode(Collection<PiTableEntry> piTableEntries,
                                                PiPipeconf pipeconf)
            throws EncodeException {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            throw new EncodeException(format(
                    "Unable to get a P4Info browser for pipeconf %s", pipeconf.id()));
        }

        ImmutableList.Builder<TableEntry> tableEntryMsgListBuilder = ImmutableList.builder();

        for (PiTableEntry piTableEntry : piTableEntries) {
            try {
                tableEntryMsgListBuilder.add(encodePiTableEntry(piTableEntry, browser));
            } catch (P4InfoBrowser.NotFoundException e) {
                throw new EncodeException(e.getMessage());
            }
        }

        return tableEntryMsgListBuilder.build();
    }

    /**
     * Same as {@link #encode(Collection, PiPipeconf)} but encodes only one entry.
     *
     * @param piTableEntry table entry
     * @param pipeconf     pipeconf
     * @return encoded table entry message
     * @throws EncodeException                 if entry cannot be encoded
     * @throws P4InfoBrowser.NotFoundException if the required information cannot be find in the pipeconf's P4info
     */
    static TableEntry encode(PiTableEntry piTableEntry, PiPipeconf pipeconf)
            throws EncodeException, P4InfoBrowser.NotFoundException {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            throw new EncodeException(format("Unable to get a P4Info browser for pipeconf %s", pipeconf.id()));
        }

        return encodePiTableEntry(piTableEntry, browser);
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

    /**
     * Same as {@link #decode(Collection, PiPipeconf)} but decodes only one entry.
     *
     * @param tableEntryMsg table entry message
     * @param pipeconf      pipeconf
     * @return decoded PI table entry
     * @throws EncodeException                 if message cannot be decoded
     * @throws P4InfoBrowser.NotFoundException if the required information cannot be find in the pipeconf's P4info
     */
    static PiTableEntry decode(TableEntry tableEntryMsg, PiPipeconf pipeconf)
            throws EncodeException, P4InfoBrowser.NotFoundException {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        if (browser == null) {
            throw new EncodeException(format("Unable to get a P4Info browser for pipeconf %s", pipeconf.id()));
        }
        return decodeTableEntryMsg(tableEntryMsg, browser);
    }

    /**
     * Returns a table entry protobuf message, encoded from the given table id and match key, for the given pipeconf.
     * The returned table entry message can be only used to reference an existing entry, i.e. a read operation, and not
     * a write one wince it misses other fields (action, priority, etc.).
     *
     * @param tableId  table identifier
     * @param matchKey match key
     * @param pipeconf pipeconf
     * @return table entry message
     * @throws EncodeException                 if message cannot be encoded
     * @throws P4InfoBrowser.NotFoundException if the required information cannot be find in the pipeconf's P4info
     */
    static TableEntry encode(PiTableId tableId, PiMatchKey matchKey, PiPipeconf pipeconf)
            throws EncodeException, P4InfoBrowser.NotFoundException {

        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        TableEntry.Builder tableEntryMsgBuilder = TableEntry.newBuilder();

        P4InfoOuterClass.Table tableInfo = browser.tables().getByName(tableId.id());

        // Table id.
        tableEntryMsgBuilder.setTableId(tableInfo.getPreamble().getId());

        // Field matches.
        if (matchKey.equals(PiMatchKey.EMPTY)) {
            tableEntryMsgBuilder.setIsDefaultAction(true);
        } else {
            for (PiFieldMatch piFieldMatch : matchKey.fieldMatches()) {
                tableEntryMsgBuilder.addMatch(encodePiFieldMatch(piFieldMatch, tableInfo, browser));
            }
        }

        return tableEntryMsgBuilder.build();
    }

    private static TableEntry encodePiTableEntry(PiTableEntry piTableEntry, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        TableEntry.Builder tableEntryMsgBuilder = TableEntry.newBuilder();

        P4InfoOuterClass.Table tableInfo = browser.tables().getByName(piTableEntry.table().id());

        // Table id.
        tableEntryMsgBuilder.setTableId(tableInfo.getPreamble().getId());

        // Priority.
        // FIXME: check on P4Runtime if/what is the default priority.
        piTableEntry.priority().ifPresent(tableEntryMsgBuilder::setPriority);

        // Controller metadata (cookie)
        tableEntryMsgBuilder.setControllerMetadata(piTableEntry.cookie());

        // Timeout.
        if (piTableEntry.timeout().isPresent()) {
            log.warn("Found PI table entry with timeout set, not supported in P4Runtime: {}", piTableEntry);
        }

        // Table action.
        if (piTableEntry.action() != null) {
            tableEntryMsgBuilder.setAction(encodePiTableAction(piTableEntry.action(), browser));
        }

        // Field matches.
        if (piTableEntry.matchKey().equals(PiMatchKey.EMPTY)) {
            tableEntryMsgBuilder.setIsDefaultAction(true);
        } else {
            for (PiFieldMatch piFieldMatch : piTableEntry.matchKey().fieldMatches()) {
                tableEntryMsgBuilder.addMatch(encodePiFieldMatch(piFieldMatch, tableInfo, browser));
            }
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
        if (tableEntryMsg.hasAction()) {
            piTableEntryBuilder.withAction(decodeTableActionMsg(tableEntryMsg.getAction(), browser));
        }

        // Timeout.
        // FIXME: how to decode table entry messages with timeout, given that the timeout value is lost after encoding?

        // Match key for field matches.
        piTableEntryBuilder.withMatchKey(decodeFieldMatchMsgs(tableEntryMsg.getMatchList(), tableInfo, browser));

        return piTableEntryBuilder.build();
    }

    private static FieldMatch encodePiFieldMatch(PiFieldMatch piFieldMatch, P4InfoOuterClass.Table tableInfo,
                                                 P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        FieldMatch.Builder fieldMatchMsgBuilder = FieldMatch.newBuilder();

        // FIXME: check how field names for stacked headers are constructed in P4Runtime.
        String fieldName = piFieldMatch.fieldId().id();
        int tableId = tableInfo.getPreamble().getId();
        P4InfoOuterClass.MatchField matchFieldInfo = browser.matchFields(tableId).getByName(fieldName);
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
            default:
                throw new EncodeException(format(
                        "Building of match type %s not implemented", piFieldMatch.type()));
        }
    }

    /**
     * Returns a PI match key, decoded from the given table entry protobuf message, for the given pipeconf.
     *
     * @param tableEntryMsg table entry message
     * @param pipeconf      pipeconf
     * @return PI match key
     * @throws EncodeException                 if message cannot be decoded
     * @throws P4InfoBrowser.NotFoundException if the required information cannot be find in the pipeconf's P4info
     */
    static PiMatchKey decodeMatchKey(TableEntry tableEntryMsg, PiPipeconf pipeconf)
            throws P4InfoBrowser.NotFoundException, EncodeException {
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);
        P4InfoOuterClass.Table tableInfo = browser.tables().getById(tableEntryMsg.getTableId());
        if (tableEntryMsg.getMatchCount() == 0) {
            return PiMatchKey.EMPTY;
        } else {
            return decodeFieldMatchMsgs(tableEntryMsg.getMatchList(), tableInfo, browser);
        }
    }

    private static PiMatchKey decodeFieldMatchMsgs(Collection<FieldMatch> fieldMatchs, P4InfoOuterClass.Table tableInfo,
                                                   P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {
        // Match key for field matches.
        PiMatchKey.Builder piMatchKeyBuilder = PiMatchKey.builder();
        for (FieldMatch fieldMatchMsg : fieldMatchs) {
            piMatchKeyBuilder.addFieldMatch(decodeFieldMatchMsg(fieldMatchMsg, tableInfo, browser));
        }
        return piMatchKeyBuilder.build();
    }

    private static PiFieldMatch decodeFieldMatchMsg(FieldMatch fieldMatchMsg, P4InfoOuterClass.Table tableInfo,
                                                    P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        int tableId = tableInfo.getPreamble().getId();
        String fieldMatchName = browser.matchFields(tableId).getById(fieldMatchMsg.getFieldId()).getName();
        PiMatchFieldId headerFieldId = PiMatchFieldId.of(fieldMatchName);

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
            default:
                throw new EncodeException(format(
                        "Decoding of field match type '%s' not implemented", typeCase.name()));
        }
    }

    static TableAction encodePiTableAction(PiTableAction piTableAction, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {
        checkNotNull(piTableAction, "Cannot encode null PiTableAction");
        TableAction.Builder tableActionMsgBuilder = TableAction.newBuilder();

        switch (piTableAction.type()) {
            case ACTION:
                PiAction piAction = (PiAction) piTableAction;
                Action theAction = encodePiAction(piAction, browser);
                tableActionMsgBuilder.setAction(theAction);
                break;
            case ACTION_GROUP_ID:
                PiActionGroupId actionGroupId = (PiActionGroupId) piTableAction;
                tableActionMsgBuilder.setActionProfileGroupId(actionGroupId.id());
                break;
            case GROUP_MEMBER_ID:
                PiActionGroupMemberId actionGroupMemberId = (PiActionGroupMemberId) piTableAction;
                tableActionMsgBuilder.setActionProfileMemberId(actionGroupMemberId.id());
                break;
            default:
                throw new EncodeException(
                        format("Building of table action type %s not implemented", piTableAction.type()));
        }

        return tableActionMsgBuilder.build();
    }

    static PiTableAction decodeTableActionMsg(TableAction tableActionMsg, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {
        TableAction.TypeCase typeCase = tableActionMsg.getTypeCase();
        switch (typeCase) {
            case ACTION:
                Action actionMsg = tableActionMsg.getAction();
                return decodeActionMsg(actionMsg, browser);
            case ACTION_PROFILE_GROUP_ID:
                return PiActionGroupId.of(tableActionMsg.getActionProfileGroupId());
            case ACTION_PROFILE_MEMBER_ID:
                return PiActionGroupMemberId.of(tableActionMsg.getActionProfileMemberId());
            default:
                throw new EncodeException(
                        format("Decoding of table action type %s not implemented", typeCase.name()));
        }
    }

    static Action encodePiAction(PiAction piAction, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        int actionId = browser.actions().getByName(piAction.id().toString()).getPreamble().getId();

        Action.Builder actionMsgBuilder =
                Action.newBuilder().setActionId(actionId);

        for (PiActionParam p : piAction.parameters()) {
            P4InfoOuterClass.Action.Param paramInfo = browser.actionParams(actionId).getByName(p.id().toString());
            ByteString paramValue = ByteString.copyFrom(p.value().asReadOnlyBuffer());
            assertSize(format("param '%s' of action '%s'", p.id(), piAction.id()),
                       paramValue, paramInfo.getBitwidth());
            actionMsgBuilder.addParams(Action.Param.newBuilder()
                                               .setParamId(paramInfo.getId())
                                               .setValue(paramValue)
                                               .build());
        }

        return actionMsgBuilder.build();
    }

    static PiAction decodeActionMsg(Action action, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        P4InfoBrowser.EntityBrowser<P4InfoOuterClass.Action.Param> paramInfo =
                browser.actionParams(action.getActionId());
        String actionName = browser.actions()
                .getById(action.getActionId())
                .getPreamble().getName();
        PiActionId id = PiActionId.of(actionName);
        List<PiActionParam> params = Lists.newArrayList();

        for (Action.Param p : action.getParamsList()) {
            String paramName = paramInfo.getById(p.getParamId()).getName();
            ImmutableByteSequence value = ImmutableByteSequence.copyFrom(p.getValue().toByteArray());
            params.add(new PiActionParam(PiActionParamId.of(paramName), value));
        }
        return PiAction.builder().withId(id).withParameters(params).build();
    }
}
