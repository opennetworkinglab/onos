/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.ipran.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowext.FlowRuleExtService;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.match.MatchField;
/**
 * Lists all flowruleexts.
 */
@Command(scope = "onos", name = "ofmessages",
         description = "Lists all FlowRules")
public class FlowRuleListCommand extends AbstractShellCommand {

    private static final String FMT_OMSG =
            "   deviceid:%s,  version:%s,  type:%s,  match:%s,  instruction:%s";
    private static final String FMT_GMSG =
            "   deviceid:%s,  version:%s,  type:%s,  command:%s";

    @Override
    protected void execute() {
        FlowRuleExtService  flowRuleService = get(FlowRuleExtService.class);
        DeviceService deviceService = get(DeviceService.class); 
        Iterable<Device> devices = deviceService.getDevices();
        for (Device d: devices) {
                Iterable<?> ofs = flowRuleService.getExtMessages(d.id());
        	for (Object msg : ofs) {
        		if (msg instanceof OFFlowMod) {
	        		OFFlowMod ofMod = (OFFlowMod) msg;
	        		String matches = "";
	        		for (MatchField<?> field : ofMod.getMatch().getMatchFields()) {
	        			matches = matches + field.getName();
	        		}
	        		print(FMT_OMSG , d.id().toString(), ofMod.getVersion(), ofMod.getType() , matches, ofMod.getInstructions().toString());
        		} else if (msg instanceof OFGroupMod) {
        			OFGroupMod groupMod = (OFGroupMod)msg;
        			print(FMT_GMSG , d.id().toString(), groupMod.getVersion(), groupMod.getType() , groupMod.getCommand());
        		}
        		
        	}
        }
    }
}
