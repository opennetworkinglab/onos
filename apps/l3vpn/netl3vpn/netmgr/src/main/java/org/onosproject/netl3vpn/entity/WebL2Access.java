package org.onosproject.netl3vpn.entity;

public class WebL2Access {
    private L2AccessType accessType;
    private WebPort port;

    public enum L2AccessType {
        Untag(0), Port(1), Dot1q(2), Qing(3), Transport(4), Vxlan(5);

        int value;

        private L2AccessType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public L2AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(L2AccessType accessType) {
        this.accessType = accessType;
    }

    public WebPort getPort() {
        return port;
    }

    public void setPort(WebPort port) {
        this.port = port;
    }

}
