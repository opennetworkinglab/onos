/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.isispacket.tlv;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of metric of internal reachability.
 */
public class MetricOfInternalReachability {
    private Ip4Address ipAddress;
    private Ip4Address subnetAddres;
    private byte defaultMetric;
    private byte delayMetric;
    private byte expenseMetric;
    private byte errorMetric;
    private boolean delayMetricSupported;
    private boolean expenseMetricSupported;
    private boolean errorMetricSupported;
    private boolean defaultIsInternal;
    private boolean defaultDistributionDown;
    private boolean delayIsInternal;
    private boolean expenseIsInternal;
    private boolean errorIsInternal;

    /**
     * Returns the IP address of metric of internal reachability.
     *
     * @return ipAddress IP address of metric of internal reachability
     */
    public Ip4Address getIpAddress() {
        return ipAddress;
    }

    /**
     * Sets the IP address for metric of internal reachability.
     *
     * @param ipAddress ip address
     */
    public void setIpAddress(Ip4Address ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the subnet address of metric of internal reachability.
     *
     * @return subnetAddres subnet address of metric of internal reachability
     */
    public Ip4Address getSubnetAddres() {
        return subnetAddres;
    }

    /**
     * Sets the subnet address for metric of internal reachability.
     *
     * @param subnetAddres subnet address
     */
    public void setSubnetAddres(Ip4Address subnetAddres) {
        this.subnetAddres = subnetAddres;
    }

    /**
     * Returns error metric is internal or not.
     *
     * @return true if internal else false
     */
    public boolean isErrorIsInternal() {
        return errorIsInternal;
    }

    /**
     * Sets error metric is internal or not.
     *
     * @param errorIsInternal true if internal else false
     */
    public void setErrorIsInternal(boolean errorIsInternal) {
        this.errorIsInternal = errorIsInternal;
    }

    /**
     * Returns expense metric is internal or not.
     *
     * @return true if internal else false
     */
    public boolean isExpenseIsInternal() {
        return expenseIsInternal;
    }

    /**
     * Sets expense metric is internal or not.
     *
     * @param expenseIsInternal true if internal else false
     */
    public void setExpenseIsInternal(boolean expenseIsInternal) {
        this.expenseIsInternal = expenseIsInternal;
    }

    /**
     * Returns delays metric is internal or not.
     *
     * @return true if internal else false
     */
    public boolean isDelayIsInternal() {
        return delayIsInternal;
    }

    /**
     * Sets delay metric is internal or not.
     *
     * @param delayIsInternal true if internal else false
     */
    public void setDelayIsInternal(boolean delayIsInternal) {
        this.delayIsInternal = delayIsInternal;
    }

    /**
     * Returns is default distribution is up or down.
     *
     * @return true if down else false
     */
    public boolean isDefaultDistributionDown() {
        return defaultDistributionDown;
    }

    /**
     * Sets default distribution is up or down.
     *
     * @param defaultDistributionDown true if down else false
     */
    public void setDefaultDistributionDown(boolean defaultDistributionDown) {
        this.defaultDistributionDown = defaultDistributionDown;
    }

    /**
     * Returns is default metric is internal or not.
     *
     * @return true if internal else false
     */
    public boolean isDefaultIsInternal() {
        return defaultIsInternal;
    }

    /**
     * Sets default metric is internal or not.
     *
     * @param defaultIsInternal true is internal else false
     */
    public void setDefaultIsInternal(boolean defaultIsInternal) {
        this.defaultIsInternal = defaultIsInternal;
    }

    /**
     * Returns error metric is supported or not.
     *
     * @return true if supported else false
     */
    public boolean isErrorMetricSupported() {
        return errorMetricSupported;
    }

    /**
     * Sets error metric is supported or not.
     *
     * @param errorMetricSupported true if supported else false
     */
    public void setErrorMetricSupported(boolean errorMetricSupported) {
        this.errorMetricSupported = errorMetricSupported;
    }

    /**
     * Returns expense metric is supported or not.
     *
     * @return true if supported else false
     */
    public boolean isExpenseMetricSupported() {
        return expenseMetricSupported;
    }

    /**
     * Sets expense metric is supported or not.
     *
     * @param expenseMetricSupported true if supported else false
     */
    public void setExpenseMetricSupported(boolean expenseMetricSupported) {
        this.expenseMetricSupported = expenseMetricSupported;
    }

    /**
     * Returns delay metric is supported or not.
     *
     * @return true if supported else false
     */
    public boolean isDelayMetricSupported() {
        return delayMetricSupported;
    }

    /**
     * Sets delay metric is supported or not.
     *
     * @param delayMetricSupported true if supported else false
     */
    public void setDelayMetricSupported(boolean delayMetricSupported) {
        this.delayMetricSupported = delayMetricSupported;
    }

    /**
     * Returns error metric of metric of internal reachability.
     *
     * @return errorMetric error metric
     */
    public byte errorMetric() {
        return errorMetric;
    }

    /**
     * Sets error metric for metric of internal reachability.
     *
     * @param errorMetric error metric
     */
    public void setErrorMetric(byte errorMetric) {
        this.errorMetric = errorMetric;
    }

    /**
     * Returns expense metric of metric of internal reachability.
     *
     * @return expense metric
     */
    public byte expenseMetric() {
        return expenseMetric;
    }

    /**
     * Sets expense metric for metric of internal reachability.
     *
     * @param expenseMetric expense metric
     */
    public void setExpenseMetric(byte expenseMetric) {
        this.expenseMetric = expenseMetric;
    }

    /**
     * Returns delay metric of metric of internal reachability.
     *
     * @return delay metric
     */
    public byte delayMetric() {
        return delayMetric;
    }

    /**
     * Sets delay metric for metric of internal reachability.
     *
     * @param delayMetric delay metric
     */
    public void setDelayMetric(byte delayMetric) {
        this.delayMetric = delayMetric;
    }

    /**
     * Returns default metric of metric of internal reachability.
     *
     * @return default metric
     */
    public byte defaultMetric() {
        return defaultMetric;
    }

    /**
     * Sets default metric for metric of internal reachability.
     *
     * @param defaultMetric default metric
     */
    public void setDefaultMetric(byte defaultMetric) {
        this.defaultMetric = defaultMetric;
    }

    /**
     * Sets the metric of internal reachability
     * values for metric of internal reachability from byte buffer.
     *
     * @param channelBuffer channel Buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte metric = channelBuffer.readByte();
        this.setDefaultMetric(metric);
        String metricInBinary = Integer.toBinaryString(Byte.toUnsignedInt(metric));
        metricInBinary = IsisUtil.toEightBitBinary(metricInBinary);
        if (metricInBinary.charAt(1) == 0) {
            this.setDefaultIsInternal(true);
        } else {
            this.setDefaultIsInternal(false);
        }
        if (metricInBinary.charAt(0) == 0) {
            this.setDefaultDistributionDown(true);
        } else {
            this.setDefaultDistributionDown(false);
        }
        byte delayMetric = channelBuffer.readByte();
        metricInBinary = Integer.toBinaryString(Byte.toUnsignedInt(delayMetric));
        metricInBinary = IsisUtil.toEightBitBinary(metricInBinary);
        this.setDelayMetric(Byte.parseByte(metricInBinary.substring(5, metricInBinary.length()), 2));
        if (metricInBinary.charAt(1) == 0) {
            this.setDelayIsInternal(true);
        } else {
            this.setDelayIsInternal(false);
        }
        if (metricInBinary.charAt(0) == 0) {
            this.setDelayMetricSupported(true);
        } else {
            this.setDelayMetricSupported(false);
        }
        byte expenseMetric = channelBuffer.readByte();
        metricInBinary = Integer.toBinaryString(Byte.toUnsignedInt(expenseMetric));
        metricInBinary = IsisUtil.toEightBitBinary(metricInBinary);
        this.setExpenseMetric(Byte.parseByte(metricInBinary.substring(5, metricInBinary.length()), 2));
        if (metricInBinary.charAt(1) == 0) {
            this.setExpenseIsInternal(true);
        } else {
            this.setExpenseIsInternal(false);
        }
        if (metricInBinary.charAt(0) == 0) {
            this.setExpenseMetricSupported(true);
        } else {
            this.setExpenseMetricSupported(false);
        }
        byte errorMetric = channelBuffer.readByte();
        metricInBinary = Integer.toBinaryString(Byte.toUnsignedInt(errorMetric));
        metricInBinary = IsisUtil.toEightBitBinary(metricInBinary);
        this.setErrorMetric(Byte.parseByte(metricInBinary.substring(5, metricInBinary.length()), 2));
        if (metricInBinary.charAt(1) == 0) {
            this.setErrorIsInternal(true);
        } else {
            this.setErrorIsInternal(false);
        }
        if (metricInBinary.charAt(0) == 0) {
            this.setErrorMetricSupported(true);
        } else {
            this.setErrorMetricSupported(false);
        }

        byte[] tempByteArray = new byte[IsisUtil.FOUR_BYTES];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.FOUR_BYTES);
        this.setIpAddress(Ip4Address.valueOf(tempByteArray));

        tempByteArray = new byte[IsisUtil.FOUR_BYTES];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.FOUR_BYTES);
        this.setSubnetAddres(Ip4Address.valueOf(tempByteArray));
    }

    /**
     * Returns metric of internal reachability values as bytes of metric of internal reachability.
     *
     * @return byteArray metric of internal reachability values as bytes of metric of internal reachability
     */
    public byte[] asBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(this.defaultMetric());
        int temp = this.delayMetric();
        String hsbBits = "";
        if (this.isDelayMetricSupported()) {
            hsbBits = "0" + hsbBits;
        } else {
            hsbBits = "1" + hsbBits;
        }
        if (this.isDelayIsInternal()) {
            hsbBits = hsbBits + "0";
        } else {
            hsbBits = hsbBits + "1";
        }
        hsbBits = hsbBits + "00";
        String binary = hsbBits + IsisUtil.toFourBitBinary(Integer.toBinaryString(temp));
        bytes.add((byte) Integer.parseInt(binary, 2));

        temp = this.expenseMetric();
        hsbBits = "";
        if (this.isExpenseMetricSupported()) {
            hsbBits = "0" + hsbBits;
        } else {
            hsbBits = "1" + hsbBits;
        }
        if (this.isExpenseIsInternal()) {
            hsbBits = hsbBits + "0";
        } else {
            hsbBits = hsbBits + "1";
        }
        hsbBits = hsbBits + "00";
        binary = hsbBits + IsisUtil.toFourBitBinary(Integer.toBinaryString(temp));
        bytes.add((byte) Integer.parseInt(binary, 2));

        temp = this.errorMetric();
        hsbBits = "";
        if (this.isErrorMetricSupported()) {
            hsbBits = "0" + hsbBits;
        } else {
            hsbBits = "1" + hsbBits;
        }
        if (this.isExpenseIsInternal()) {
            hsbBits = hsbBits + "0";
        } else {
            hsbBits = hsbBits + "1";
        }
        hsbBits = hsbBits + "00";
        binary = hsbBits + IsisUtil.toFourBitBinary(Integer.toBinaryString(temp));
        bytes.add((byte) Integer.parseInt(binary, 2));

        bytes.addAll(Bytes.asList(this.getIpAddress().toOctets()));
        bytes.addAll(Bytes.asList(this.getSubnetAddres().toOctets()));
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("ipAddress", ipAddress)
                .add("subnetAddres", subnetAddres)
                .add("defaultMetric", defaultMetric)
                .add("delayMetric", delayMetric)
                .add("expenseMetric", expenseMetric)
                .add("errorMetric", errorMetric)
                .add("delayMetricSupported", delayMetricSupported)
                .add("expenseMetricSupported", expenseMetricSupported)
                .add("errorMetricSupported", errorMetricSupported)
                .add("defaultIsInternal", defaultIsInternal)
                .add("defaultDistributionDown", defaultDistributionDown)
                .add("delayIsInternal", delayIsInternal)
                .add("expenseIsInternal", expenseIsInternal)
                .add("errorIsInternal", errorIsInternal)
                .toString();
    }
}