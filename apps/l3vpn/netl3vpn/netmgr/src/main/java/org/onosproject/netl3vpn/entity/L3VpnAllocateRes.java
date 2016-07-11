package org.onosproject.netl3vpn.entity;

import java.util.List;

public class L3VpnAllocateRes {
    private List<String> routeTargets;
    private String routeDistinguisher;
    private String vrfName;

    public List<String> getRouteTargets() {
        return routeTargets;
    }

    public void setRouteTargets(List<String> routeTargets) {
        this.routeTargets = routeTargets;
    }

    public String getRouteDistinguisher() {
        return routeDistinguisher;
    }

    public void setRouteDistinguisher(String routeDistinguisher) {
        this.routeDistinguisher = routeDistinguisher;
    }

    public String getVrfName() {
        return vrfName;
    }

    public void setVrfName(String vrfName) {
        this.vrfName = vrfName;
    }

}
