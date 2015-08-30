/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */
public interface PcepFactory {

    /**
     * To get Builder Object for Open Message.
     *
     * @return Builder Object for Open Message
     */
    PcepOpenMsg.Builder buildOpenMsg();

    /**
     * To get Builder Object for Open Object.
     *
     * @return Builder Object for Open Object
     */
    PcepOpenObject.Builder buildOpenObject();

    /**
     * To get Builder Object for Keepalive Message.
     *
     * @return Builder Object for Keepalive Message
     */
    PcepKeepaliveMsg.Builder buildKeepaliveMsg();

    /**
     * To get Builder Object for Close Message.
     *
     * @return Builder Object for Close Message
     */
    PcepCloseMsg.Builder buildCloseMsg();

    /**
     * To get Builder Object for Report Message.
     *
     * @return Builder Object for Report Message
     */
    PcepReportMsg.Builder buildReportMsg();

    /**
     * To get Builder Object for Update Message.
     *
     * @return Builder Object for Update Message
     */
    PcepUpdateMsg.Builder buildUpdateMsg();

    /**
     * To get Builder Object for Initiate Message.
     *
     * @return Builder Object for Initiate Message
     */
    PcepInitiateMsg.Builder buildPcepInitiateMsg();

    /**
     * To get Builder Object for LSP Object.
     *
     * @return Builder Object for LSP Object
     */
    PcepLspObject.Builder buildLspObject();

    /**
     * To get Builder Object for SRP Object.
     *
     * @return Builder Object for SRP Object
     */
    PcepSrpObject.Builder buildSrpObject();

    /**
     * To get Builder Object for EndPoints Object.
     *
     * @return Builder Object for EndPoints Object
     */
    PcepEndPointsObject.Builder buildEndPointsObject();

    /**
     * To get Builder Object for ERO Object.
     *
     * @return Builder Object for ERO Object
     */
    PcepEroObject.Builder buildEroObject();

    /**
     * To get Builder Object for RRO Object.
     *
     * @return Builder Object for RRO Object
     */
    PcepRroObject.Builder buildRroObject();

    /**
     * To get Builder Object for LSPA Object.
     *
     * @return Builder Object for LSPA Object
     */
    PcepLspaObject.Builder buildLspaObject();

    /**
     * To get Builder Object for IRO Object.
     *
     * @return Builder Object for IRO Object
     */
    PcepIroObject.Builder buildIroObject();

    /**
     * To get Builder Object for METRIC Object.
     *
     * @return Builder Object for METRIC Object
     */
    PcepMetricObject.Builder buildMetricObject();

    /**
     * To get Builder Object for Bandwidth Object.
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
     * To get Builder Object for LabelUpdate message.
     *
     * @return Builder Object for LabelUpdate message
     */
    PcepLabelUpdateMsg.Builder buildPcepLabelUpdateMsg();

    /**
     * To get Builder Object for PcepLabelUpdate Object.
     *
     * @return Builder Object for PcepLabelUpdate Object
     */
    PcepLabelUpdate.Builder buildPcepLabelUpdateObject();

    /**
     * To get Builder Object for PcepLabel Object.
     *
     * @return Builder Object for PcepLabel Object
     */
    PcepLabelObject.Builder buildLabelObject();

    /**
     * To get Builder Object for Error Message.
     *
     * @return Builder Object for Error Message
     */
    PcepErrorMsg.Builder buildPcepErrorMsg();

    /**
     * To get Builder Object for Error Object.
     *
     * @return Builder Object for Error Object
     */
    PcepErrorObject.Builder buildPcepErrorObject();

    /**
     * To get Builder Object for FecIpv4Adjacency.
     *
     * @return Builder Object for FecIpv4Adjacency
     */
    PcepFecObjectIPv4Adjacency.Builder buildFecIpv4Adjacency();

    /**
     * To get Builder Object for ErrorInfo.
     *
     * @return Builder Object for ErrorInfo
     */
    PcepErrorInfo.Builder buildPcepErrorInfo();

    /**
     * To get Builder Object for PcepError.
     *
     * @return Builder Object for PcepError
     */
    PcepError.Builder buildPcepError();

    /**
     * To get Builder Object for PcepLabelRangeObject.
     *
     * @return Builder Object for PcepLabelRangeObject
     */
    PcepLabelRangeObject.Builder buildPcepLabelRangeObject();

    /**
     * To get Builder Object for PcepLabelRangeResvMsg.
     *
     * @return Builder Object for PcepLabelRangeResvMsg
     */
    PcepLabelRangeResvMsg.Builder buildPcepLabelRangeResvMsg();
}
