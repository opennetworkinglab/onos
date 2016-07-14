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
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of metric of reachability.
 */
public class MetricsOfReachability {
    private final String value1 = "10000000";
    private final String value2 = "00000000";
    private String neighborId;
    private byte defaultMetric;
    private byte delayMetric;
    private byte expenseMetric;
    private byte errorMetric;
    private boolean delayMetricSupported;
    private boolean expenseMetricSupported;
    private boolean errorMetricSupported;
    private boolean defaultIsInternal;
    private boolean delayIsInternal;
    private boolean expenseIsInternal;
    private boolean errorIsInternal;

    /**
     * Returns delay metric is internal or not.
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
     * Returns default metric is internal or not.
     *
     * @return true if internal else false
     */
    public boolean isDefaultIsInternal() {
        return defaultIsInternal;
    }

    /**
     * Sets default metric is internal or not.
     *
     * @param defaultIsInternal true if internal else false
     */
    public void setDefaultIsInternal(boolean defaultIsInternal) {
        this.defaultIsInternal = defaultIsInternal;
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
     * Returns neighbor ID of metric of reachability.
     *
     * @return neighbor ID
     */
    public String neighborId() {
        return neighborId;
    }

    /**
     * Sets neighbor ID for metric of reachability.
     *
     * @param neighborId neighbor ID
     */
    public void setNeighborId(String neighborId) {
        this.neighborId = neighborId;
    }


    /**
     * Returns default metric of metric of reachability.
     *
     * @return default metric
     */
    public byte defaultMetric() {
        return defaultMetric;
    }

    /**
     * Sets default metric for  of reachability.
     *
     * @param defaultMetric default metric
     */
    public void setDefaultMetric(byte defaultMetric) {
        this.defaultMetric = defaultMetric;
    }

    /**
     * Returns delay metric of metric of reachability.
     *
     * @return delay metric
     */
    public byte delayMetric() {
        return delayMetric;
    }

    /**
     * Sets delay metric for metric of reachability.
     *
     * @param delayMetric delay metric
     */
    public void setDelayMetric(byte delayMetric) {
        this.delayMetric = delayMetric;
    }

    /**
     * Returns Expense metric of metric of reachability.
     *
     * @return Expense metric
     */
    public byte expenseMetric() {
        return expenseMetric;
    }

    /**
     * Sets Expense metric for metric of reachability.
     *
     * @param expenseMetric Expense metric
     */
    public void setExpenseMetric(byte expenseMetric) {
        this.expenseMetric = expenseMetric;
    }

    /**
     * Returns Error metric of metric of reachability.
     *
     * @return Error metric
     */
    public byte errorMetric() {
        return errorMetric;
    }

    /**
     * Sets Error metric for metric of reachability.
     *
     * @param errorMetric Error metric
     */
    public void setErrorMetric(byte errorMetric) {
        this.errorMetric = errorMetric;
    }

    /**
     * Sets the metric of reachability values for metric of reachability from byte buffer.
     *
     * @param channelBuffer channel Buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte metric = channelBuffer.readByte();
        String metricInBinary = Integer.toBinaryString(Byte.toUnsignedInt(metric));
        metricInBinary = IsisUtil.toEightBitBinary(metricInBinary);
        this.setDefaultMetric(metric);
        this.setDelayMetric(metric);
        this.setExpenseMetric(metric);
        this.setErrorMetric(metric);
        this.setDelayMetric(metric);
        if (metricInBinary.charAt(0) == 0) {
            this.setDefaultIsInternal(true);
        } else {
            this.setDefaultIsInternal(false);
        }
        if (metricInBinary.charAt(0) == 0) {
            this.setDelayMetricSupported(true);
            this.setExpenseMetricSupported(true);
            this.setErrorMetricSupported(true);
        } else {
            this.setDelayMetricSupported(false);
            this.setExpenseMetricSupported(false);
            this.setErrorMetricSupported(false);
        }
        byte temp = channelBuffer.readByte();
        String internalBit = Integer.toBinaryString(Byte.toUnsignedInt(temp));
        internalBit = IsisUtil.toEightBitBinary(internalBit);
        if (internalBit.charAt(1) == 0) {
            this.setDelayIsInternal(true);
        }
        temp = channelBuffer.readByte();
        internalBit = Integer.toBinaryString(Byte.toUnsignedInt(temp));
        internalBit = IsisUtil.toEightBitBinary(internalBit);
        if (internalBit.charAt(1) == 0) {
            this.setExpenseIsInternal(true);
        }
        temp = channelBuffer.readByte();
        internalBit = Integer.toBinaryString(Byte.toUnsignedInt(temp));
        internalBit = IsisUtil.toEightBitBinary(internalBit);
        if (internalBit.charAt(1) == 0) {
            this.setErrorIsInternal(true);
        }
        byte[] tempByteArray = new byte[IsisUtil.ID_PLUS_ONE_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_ONE_BYTE);
        this.setNeighborId(IsisUtil.systemIdPlus(tempByteArray));
    }

    /**
     * Returns metric of reachability values as bytes of metric of reachability.
     *
     * @return byteArray metric of reachability values as bytes of metric of reachability
     */
    public byte[] asBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) this.defaultMetric());
        if (this.isDelayIsInternal()) {
            bytes.add((byte) Integer.parseInt(value1));
        } else {
            bytes.add((byte) Integer.parseInt(value2));
        }
        if (this.isExpenseIsInternal()) {
            bytes.add((byte) Integer.parseInt(value1));
        } else {
            bytes.add((byte) Integer.parseInt(value2));
        }
        if (this.isErrorIsInternal()) {
            bytes.add((byte) Integer.parseInt(value1));
        } else {
            bytes.add((byte) Integer.parseInt(value2));
        }
        bytes.addAll(IsisUtil.sourceAndLanIdToBytes(this.neighborId()));
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("neighborId", neighborId)
                .add("defaultMetric", defaultMetric)
                .add("delayMetric", delayMetric)
                .add("expenseMetric", expenseMetric)
                .add("errorMetric", errorMetric)
                .add("delayMetricSupported", delayMetricSupported)
                .add("expenseMetricSupported", expenseMetricSupported)
                .add("errorMetricSupported", errorMetricSupported)
                .add("defaultIsInternal", defaultIsInternal)
                .add("delayIsInternal", delayIsInternal)
                .add("expenseIsInternal", expenseIsInternal)
                .add("errorIsInternal", errorIsInternal)
                .toString();
    }
}
