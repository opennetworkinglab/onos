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
package org.onosproject.openstacktelemetry.codec.bytebuffer;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacktelemetry.api.ByteBufferCodec;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsInfo;
import org.onosproject.openstacktelemetry.api.DefaultFlowInfo;

import java.nio.ByteBuffer;

/**
 * FlowInfo ByteBuffer Codec.
 */
public class TinaFlowInfoByteBufferCodec extends ByteBufferCodec<FlowInfo> {

    private static final int NUM_RADIX = 16;
    private static final int MESSAGE_SIZE = 88;
    private static final String OF_PREFIX = "of:";

    @Override
    public ByteBuffer encode(FlowInfo flowInfo) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
        int srcPort = 0;
        int dstPort = 0;

        String  deviceId = flowInfo.deviceId().toString();
        short switchId = (short) Integer.parseInt(deviceId.substring(3,
                                      deviceId.length()), NUM_RADIX);

        if (flowInfo.srcPort() != null) {
            srcPort = flowInfo.srcPort().toInt();
        }

        if (flowInfo.dstPort() != null) {
            dstPort = flowInfo.dstPort().toInt();
        }

        byteBuffer.put(flowInfo.flowType())
                .putShort(switchId)
                .putInt(flowInfo.inputInterfaceId())
                .putInt(flowInfo.outputInterfaceId())
                .putShort(flowInfo.vlanId().toShort())
                .put(flowInfo.srcIp().address().toOctets())
                .put((byte) flowInfo.srcIp().prefixLength())
                .putShort((short) srcPort)
                .put(flowInfo.dstIp().address().toOctets())
                .put((byte) flowInfo.dstIp().prefixLength())
                .putShort((short) dstPort)
                .put(flowInfo.protocol())
                .put(flowInfo.srcMac().toBytes())
                .put(flowInfo.dstMac().toBytes());

        TinaStatsInfoByteBufferCodec statsInfoByteBufferCodec =
                new TinaStatsInfoByteBufferCodec();
        byteBuffer.put(statsInfoByteBufferCodec.encode(flowInfo.statsInfo()).array());

        return byteBuffer;
    }

    @Override
    public FlowInfo decode(ByteBuffer byteBuffer) {

        byte flowType = byteBuffer.get();
        String deviceIdStr = String.format("%016x", byteBuffer.getShort());
        DeviceId deviceId = DeviceId.deviceId(OF_PREFIX + deviceIdStr);
        int inputInterfaceId = byteBuffer.getInt();
        int outputInterfaceId = byteBuffer.getInt();
        VlanId vlanId = VlanId.vlanId(byteBuffer.getShort());
        IpAddress srcIp = IpAddress.valueOf(Version.INET, getIpv4Octets(byteBuffer));
        int srcPrefixLen = byteBuffer.get();
        TpPort srcPort = TpPort.tpPort((int) byteBuffer.getShort());
        IpAddress dstIp = IpAddress.valueOf(Version.INET, getIpv4Octets(byteBuffer));
        int dstPrefixLen = byteBuffer.get();
        TpPort dstPort = TpPort.tpPort((int) byteBuffer.getShort());

        byte protocol = byteBuffer.get();
        MacAddress srcMac = MacAddress.valueOf(getMacByteArray(byteBuffer));
        MacAddress dstMac = MacAddress.valueOf(getMacByteArray(byteBuffer));

        TinaStatsInfoByteBufferCodec statsInfoByteBufferCodec =
                new TinaStatsInfoByteBufferCodec();
        StatsInfo statsInfo = statsInfoByteBufferCodec.decode(byteBuffer);

        return new DefaultFlowInfo.DefaultBuilder()
                .withFlowType(flowType)
                .withDeviceId(deviceId)
                .withInputInterfaceId(inputInterfaceId)
                .withOutputInterfaceId(outputInterfaceId)
                .withVlanId(vlanId)
                .withSrcIp(IpPrefix.valueOf(srcIp, srcPrefixLen))
                .withSrcPort(srcPort)
                .withDstIp(IpPrefix.valueOf(dstIp, dstPrefixLen))
                .withDstPort(dstPort)
                .withProtocol(protocol)
                .withSrcMac(srcMac)
                .withDstMac(dstMac)
                .withStatsInfo(statsInfo)
                .build();
    }

    /**
     * Obtains IPv4 Octets from ByteBuffer.
     *
     * @param buffer byte buffer
     * @return Ipv4 Octets
     */
    private byte[] getIpv4Octets(ByteBuffer buffer) {
        byte[] octets = new byte[4];
        for (int i = 0; i < octets.length; i++) {
            octets[i] = buffer.get();
        }
        return octets;
    }

    /**
     * Obtains MAC address byte array from ByteBuffer.
     *
     * @param buffer byte buffer
     * @return MAC address byte array
     */
    private byte[] getMacByteArray(ByteBuffer buffer) {
        byte[] array = new byte[6];
        for (int i = 0; i < array.length; i++) {
            array[i] = buffer.get();
        }
        return array;
    }
}
