package org.onosproject.netl3vpn.entity;

import java.util.List;

public class WebL3vpnInstance {
    private String id;
    private String name;
    private TopoModeType mode;
    private List<String> neIdList;
    private List<WebAc> acList;

    /**
     * The enumeration of topo mode type.
     */
    public enum TopoModeType {
        None(0), HubSpoke(1), FullMesh(2);

        int value;

        private TopoModeType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopoModeType getMode() {
        return mode;
    }

    public void setMode(TopoModeType mode) {
        this.mode = mode;
    }

    public List<String> getNeIdList() {
        return neIdList;
    }

    public void setNeIdList(List<String> neIdList) {
        this.neIdList = neIdList;
    }

    public List<WebAc> getAcList() {
        return acList;
    }

    public void setAcList(List<WebAc> acList) {
        this.acList = acList;
    }

}
