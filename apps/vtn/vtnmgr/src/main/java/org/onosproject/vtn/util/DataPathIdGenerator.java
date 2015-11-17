package org.onosproject.vtn.util;

import static org.onlab.util.Tools.toHex;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import org.onosproject.core.IdGenerator;
import org.onosproject.net.DeviceId;

public final class DataPathIdGenerator implements IdGenerator {
    private static final String SCHEME = "of";
    private String ipAddress;
    private String timeStamp;

    private DataPathIdGenerator(Builder builder) {
        this.ipAddress = builder.ipAddress;
        Calendar cal = Calendar.getInstance();
        this.timeStamp = String.valueOf(cal.get(Calendar.SECOND))
                + String.valueOf(cal.get(Calendar.MILLISECOND));
    }

    @Override
    public long getNewId() {
        String dpid = ipAddress.replace(".", "") + timeStamp;
        return Long.parseLong(dpid);
    }

    public String getDpId() {
        return toHex(getNewId());
    }

    public DeviceId getDeviceId() {
        try {
            URI uri = new URI(SCHEME, toHex(getNewId()), null);
            return DeviceId.deviceId(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ipAddress;

        public Builder addIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public DataPathIdGenerator build() {
            return new DataPathIdGenerator(this);
        }
    }
}
