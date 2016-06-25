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

package org.onosproject.pcepio.protocol;

/**
 * Abstraction of an Message factory providing Builder functions to PCEP Messages and Objects.
 */
public interface PcepFactory {

    /**
     * Returns Builder Object for Open Message.
     *
     * @return Builder Object for Open Message
     */
    PcepOpenMsg.Builder buildOpenMsg();

    /**
     * Returns Builder Object for Open Object.
     *
     * @return Builder Object for Open Object
     */
    PcepOpenObject.Builder buildOpenObject();

    /**
     * Returns Builder Object for Keepalive Message.
     *
     * @return Builder Object for Keepalive Message
     */
    PcepKeepaliveMsg.Builder buildKeepaliveMsg();

    /**
     * Returns Builder Object for Close Message.
     *
     * @return Builder Object for Close Message
     */
    PcepCloseMsg.Builder buildCloseMsg();

    /**
     * Returns Builder Object for Report Message.
     *
     * @return Builder Object for Report Message
     */
    PcepReportMsg.Builder buildReportMsg();

    /**
     * Returns Builder Object for Update Message.
     *
     * @return Builder Object for Update Message
     */
    PcepUpdateMsg.Builder buildUpdateMsg();

    /**
     * Returns Builder Object for Initiate Message.
     *
     * @return Builder Object for Initiate Message
     */
    PcepInitiateMsg.Builder buildPcepInitiateMsg();

    /**
     * Returns Builder Object for LSP Object.
     *
     * @return Builder Object for LSP Object
     */
    PcepLspObject.Builder buildLspObject();

    /**
     * Returns Builder Object for SRP Object.
     *
     * @return Builder Object for SRP Object
     */
    PcepSrpObject.Builder buildSrpObject();

    /**
     * Returns Builder Object for EndPoints Object.
     *
     * @return Builder Object for EndPoints Object
     */
    PcepEndPointsObject.Builder buildEndPointsObject();

    /**
     * Returns Builder Object for ERO Object.
     *
     * @return Builder Object for ERO Object
     */
    PcepEroObject.Builder buildEroObject();

    /**
     * Returns Builder Object for RRO Object.
     *
     * @return Builder Object for RRO Object
     */
    PcepRroObject.Builder buildRroObject();

    /**
     * Returns Builder Object for LSPA Object.
     *
     * @return Builder Object for LSPA Object
     */
    PcepLspaObject.Builder buildLspaObject();

    /**
     * Returns Builder Object for IRO Object.
     *
     * @return Builder Object for IRO Object
     */
    PcepIroObject.Builder buildIroObject();

    /**
     * Returns Builder Object for METRIC Object.
     *
     * @return Builder Object for METRIC Object
     */
    PcepMetricObject.Builder buildMetricObject();

    /**
     * Returns Builder Object for Bandwidth Object.
     *
     * @return Builder Object for Bandwidth Object
     */
    PcepBandwidthObject.Builder buildBandwidthObject();

    /**
     * Returns PCEP Message Reader.
     *
     * @return PCEP Message Reader
     */
    PcepMessageReader<PcepMessage> getReader();

    /**
     * Returns PCEP version.
     *
     * @return PCEP version
     */
    PcepVersion getVersion();

    /**
     * Returns PcepStateReport.
     *
     * @return PcepStateReport
     */
    PcepStateReport.Builder buildPcepStateReport();

    /**
     * Returns PcepUpdateRequest.
     *
     * @return PcepUpdateRequest
     */
    PcepUpdateRequest.Builder buildPcepUpdateRequest();

    /**
     * Returns PcInitiatedLspRequest.
     *
     * @return PcInitiatedLspRequest
     */
    PcInitiatedLspRequest.Builder buildPcInitiatedLspRequest();

    /**
     * Returns PcepMsgPath.
     *
     * @return PcepMsgPath
     */
    PcepMsgPath.Builder buildPcepMsgPath();

    /**
     * Return PcepAttribute list.
     *
     * @return PcepAttribute
     */
    PcepAttribute.Builder buildPcepAttribute();

    /**
     * Returns Builder Object for LabelUpdate message.
     *
     * @return Builder Object for LabelUpdate message
     */
    PcepLabelUpdateMsg.Builder buildPcepLabelUpdateMsg();

    /**
     * Returns Builder Object for PcepLabelUpdate Object.
     *
     * @return Builder Object for PcepLabelUpdate Object
     */
    PcepLabelUpdate.Builder buildPcepLabelUpdateObject();

    /**
     * Returns Builder Object for PcepLabel Object.
     *
     * @return Builder Object for PcepLabel Object
     */
    PcepLabelObject.Builder buildLabelObject();

    /**
     * Returns Builder Object for Error Message.
     *
     * @return Builder Object for Error Message
     */
    PcepErrorMsg.Builder buildPcepErrorMsg();

    /**
     * Returns Builder Object for Error Object.
     *
     * @return Builder Object for Error Object
     */
    PcepErrorObject.Builder buildPcepErrorObject();

    /**
     * Returns Builder Object for FecIpv4Adjacency.
     *
     * @return Builder Object for FecIpv4Adjacency
     */
    PcepFecObjectIPv4Adjacency.Builder buildFecIpv4Adjacency();

    /**
     * Returns Builder Object for FecObjectIPv4.
     *
     * @return Builder Object for FecObjectIPv4
     */
    PcepFecObjectIPv4.Builder buildFecObjectIpv4();

    /**
     * Returns Builder Object for ErrorInfo.
     *
     * @return Builder Object for ErrorInfo
     */
    PcepErrorInfo.Builder buildPcepErrorInfo();

    /**
     * Returns Builder Object for PcepError.
     *
     * @return Builder Object for PcepError
     */
    PcepError.Builder buildPcepError();

    /**
     * Returns Builder Object for PcepLabelRangeObject.
     *
     * @return Builder Object for PcepLabelRangeObject
     */
    PcepLabelRangeObject.Builder buildPcepLabelRangeObject();

    /**
     * Returns Builder Object for PcepLabelRangeResvMsg.
     *
     * @return Builder Object for PcepLabelRangeResvMsg
     */
    PcepLabelRangeResvMsg.Builder buildPcepLabelRangeResvMsg();
}
