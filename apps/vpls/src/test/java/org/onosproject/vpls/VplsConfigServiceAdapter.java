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
package org.onosproject.vpls;

import com.google.common.collect.SetMultimap;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.config.VplsConfigService;

import java.util.Map;
import java.util.Set;

/**
 * Test adapter for VPLS configuration service.
 */
public class VplsConfigServiceAdapter implements VplsConfigService {
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
    public Set<Interface> ifaces() {
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
