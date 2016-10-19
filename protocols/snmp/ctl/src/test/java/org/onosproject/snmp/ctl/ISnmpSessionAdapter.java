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

package org.onosproject.snmp.ctl;

import com.btisystems.pronx.ems.core.model.DeviceEntityDescription;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.IVariableBindingHandler;
import com.btisystems.pronx.ems.core.snmp.SnmpIoException;
import com.btisystems.pronx.ems.core.snmp.WalkResponse;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * AdapterClass for ISnmpSession.
 */
public class ISnmpSessionAdapter implements ISnmpSession {
    @Override
    public String identifyDevice() {
        return null;
    }

    @Override
    public String getVariable(String oid) {
        return null;
    }

    @Override
    public Integer getVariableAsInt(String oid) {
        return null;
    }

    @Override
    public WalkResponse walkDevice(IVariableBindingHandler networkDevice, List<OID> oids)
            throws IOException {
        return null;
    }

    @Override
    public WalkResponse getTableRowsForColumns(IVariableBindingHandler iVariableBindingHandler,
                                               Map<String, OID[]> map) {
        return null;
    }

    @Override
    public WalkResponse getTableRows(IVariableBindingHandler networkDevice,
                                     Map<DeviceEntityDescription, List<OID>> tableIndexes)
            throws IOException {
        return null;
    }

    @Override
    public InetAddress getAddress() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setVariables(VariableBinding[] bindings) {

    }

    @Override
    public void checkErrorCodeAndDescription() throws SnmpIoException {

    }
}
