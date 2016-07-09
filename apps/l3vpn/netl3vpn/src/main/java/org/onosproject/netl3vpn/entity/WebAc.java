package org.onosproject.netl3vpn.entity;

public class WebAc {
    private String id;
    private String neId;
    private WebL2Access l2Access;
    private WebL3Access l3Access;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getNeId() {
        return neId;
    }
    public void setNeId(String neId) {
        this.neId = neId;
    }
    public WebL2Access getL2Access() {
        return l2Access;
    }
    public void setL2Access(WebL2Access l2Access) {
        this.l2Access = l2Access;
    }
    public WebL3Access getL3Access() {
        return l3Access;
    }
    public void setL3Access(WebL3Access l3Access) {
        this.l3Access = l3Access;
    }
}
