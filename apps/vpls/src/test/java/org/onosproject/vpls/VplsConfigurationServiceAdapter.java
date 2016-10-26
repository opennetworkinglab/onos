package org.onosproject.vpls;

import com.google.common.collect.SetMultimap;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.config.VplsConfigurationService;

import java.util.Map;
import java.util.Set;

/**
 * Test adapter for VPLS configuration service.
 */
public class VplsConfigurationServiceAdapter implements VplsConfigurationService {
    @Override
    public void addVpls(String vplsName, Set<String> ifaces, String encap) {}

    @Override
    public void removeVpls(String vplsName) {}

    @Override
    public void addIface(String vplsName, String iface) {}

    @Override
    public void setEncap(String vplsName, String encap) {}

    @Override
    public EncapsulationType encap(String vplsName) {
        return null;
    }

    @Override
    public void removeIface(String iface) {}

    @Override
    public void cleanVplsConfig() {}

    @Override
    public Set<String> vplsAffectedByApi() {
        return null;
    }

    @Override
    public Set<Interface> allIfaces() {
        return null;
    }

    @Override
    public Set<Interface> ifaces(String vplsName) {
        return null;
    }

    @Override
    public Set<String> vplsNames() {
        return null;
    }

    @Override
    public Set<String> vplsNamesOld() {
        return null;
    }

    @Override
    public SetMultimap<String, Interface> ifacesByVplsName() {
        return null;
    }

    @Override
    public SetMultimap<String, Interface> ifacesByVplsName(VlanId vlan, ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Map<String, EncapsulationType> encapByVplsName() {
        return null;
    }
}
