/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.aaa;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Shows the users in the aaa.
 */
@Command(scope = "onos", name = "aaa-users",
        description = "Shows the aaa users")
public class AaaShowUsersCommand extends AbstractShellCommand {
    @Override
    protected void execute() {
        String[] state = {
                "IDLE",
                "STARTED",
                "PENDING",
                "AUTHORIZED",
                "UNAUTHORIZED"
        };
        for (StateMachine stateMachine : StateMachine.sessionIdMap().values()) {
            String deviceId = stateMachine.supplicantConnectpoint().deviceId().toString();
            String portNum = stateMachine.supplicantConnectpoint().port().toString();
            String username = new String(stateMachine.username());
            print("UserName=%s,CurrentState=%s,DeviceId=%s,PortNumber=%s",
                  username, state[stateMachine.state()], deviceId, portNum);
        }
    }
}
