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
 *
 */

package org.onlab.packet.dhcp;

import com.google.common.base.MoreObjects;
import org.onlab.packet.BasePacket;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Dhcp6Duid extends BasePacket {
    private static final int DEFAULT_LLT_LEN = 8;
    private static final int DEFAULT_EN_LEN = 6;
    private static final int DEFAULT_LL_LEN = 4;
    public enum DuidType {
        DUID_LLT((short) 1),
        DUID_EN((short) 2),
        DUID_LL((short) 3);

        private short value;
        DuidType(short value) {
            this.value = value;
        }

        public short getValue() {
            return value;
        }

        public static DuidType of(short type) {
            switch (type) {
                case 1:
                    return DUID_LLT;
                case 2:
                    return DUID_EN;
                case 3:
                    return DUID_LL;
                default:
                    throw new RuntimeException("Unknown type: " + type);
            }
        }
    }
    // general field
    private DuidType duidType;

    // fields for DUID_LLT & DUID_LL
    private short hardwareType;
    private int duidTime;
    private byte[] linkLayerAddress;

    // fields for DUID_EN
    private int enterpriseNumber;
    private byte[] identifier;

    public DuidType getDuidType() {
        return duidType;
    }

    public void setDuidType(DuidType duidType) {
        this.duidType = duidType;
    }

    public short getHardwareType() {
        return hardwareType;
    }

    public void setHardwareType(short hardwareType) {
        this.hardwareType = hardwareType;
    }

    public int getDuidTime() {
        return duidTime;
    }

    public void setDuidTime(int duidTime) {
        this.duidTime = duidTime;
    }

    public byte[] getLinkLayerAddress() {
        return linkLayerAddress;
    }

    public void setLinkLayerAddress(byte[] linkLayerAddress) {
        this.linkLayerAddress = linkLayerAddress;
    }

    public int getEnterpriseNumber() {
        return enterpriseNumber;
    }

    public void setEnterpriseNumber(int enterpriseNumber) {
        this.enterpriseNumber = enterpriseNumber;
    }

    public byte[] getIdentifier() {
        return identifier;
    }

    public void setIdentifier(byte[] identifier) {
        this.identifier = identifier;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer byteBuffer;
        switch (duidType) {
            case DUID_LLT:
                byteBuffer = ByteBuffer.allocate(DEFAULT_LLT_LEN + linkLayerAddress.length);
                byteBuffer.putShort(duidType.value);
                byteBuffer.putShort(hardwareType);
                byteBuffer.putInt(duidTime);
                byteBuffer.put(linkLayerAddress);
                break;
            case DUID_EN:
                byteBuffer = ByteBuffer.allocate(DEFAULT_EN_LEN + identifier.length);
                byteBuffer.putShort(duidType.value);
                byteBuffer.putInt(enterpriseNumber);
                byteBuffer.put(identifier);
                break;
            case DUID_LL:
                byteBuffer = ByteBuffer.allocate(DEFAULT_LL_LEN + linkLayerAddress.length);
                byteBuffer.putShort(duidType.value);
                byteBuffer.putShort(hardwareType);
                byteBuffer.put(linkLayerAddress);
                break;
            default:
                throw new RuntimeException("Unknown duidType: " + duidType.toString());
        }
        return byteBuffer.array();
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        try {
            return deserializer().deserialize(data, offset, length);
        } catch (DeserializationException e) {
            throw new RuntimeException("Can't deserialize duid due to {}", e);
        }
    }

    public static Deserializer<Dhcp6Duid> deserializer() {
        return (data, offset, length) -> {
            Dhcp6Duid duid = new Dhcp6Duid();
            ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, length);

            DuidType duidType = DuidType.of(byteBuffer.getShort());
            duid.setDuidType(duidType);
            switch (duidType) {
                case DUID_LLT:
                    duid.setHardwareType(byteBuffer.getShort());
                    duid.setDuidTime(byteBuffer.getInt());
                    duid.linkLayerAddress = new byte[length - DEFAULT_LLT_LEN];
                    byteBuffer.get(duid.linkLayerAddress);
                    break;
                case DUID_EN:
                    duid.setEnterpriseNumber(byteBuffer.getInt());
                    duid.identifier = new byte[length - DEFAULT_EN_LEN];
                    byteBuffer.get(duid.identifier);
                    break;
                case DUID_LL:
                    duid.setHardwareType(byteBuffer.getShort());
                    duid.linkLayerAddress = new byte[length - DEFAULT_LL_LEN];
                    byteBuffer.get(duid.linkLayerAddress);
                    break;
                default:
                    throw new RuntimeException("Unknown type: " + duidType);
            }
            return duid;
        };
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(getClass());

        switch (duidType) {
            case DUID_LLT:
                helper.add("type", "DUID_LLT");
                helper.add("hardwareType", hardwareType);
                helper.add("duidTime", duidTime);
                helper.add("linkLayerAddress", Arrays.toString(linkLayerAddress));
                break;
            case DUID_EN:
                helper.add("type", "DUID_EN");
                helper.add("enterpriseNumber", enterpriseNumber);
                helper.add("id", Arrays.toString(identifier));
                break;
            case DUID_LL:
                helper.add("type", "DUID_LL");
                helper.add("hardwareType", hardwareType);
                helper.add("linkLayerAddress", Arrays.toString(linkLayerAddress));
                break;
            default:
                helper.add("type", "Unknown");
        }
        return helper.toString();
    }
}
