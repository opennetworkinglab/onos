/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.clusterha.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.clusterha.ClusterHATest;

@Service
@Command(scope = "onos", name = "cluster-ha-test",
        description = "test addition & deletion on consistent map")
public class ClusterHATestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "operation", description = "add/del", required = true)
    private String operation = null;

    @Argument(index = 1, name = "strStartIdx", description = "start index", required = true)
    private String strStartIdx = null;

    @Argument(index = 2, name = "strEndIdx", description = "end index", required = true)
    private String strEndIdx = null;

    @Argument(index = 3, name = "strInterOpDelay", description = "inter operation delay(ms)", required = true)
    private String strInterOpDelay = null;

    private static final String OP_ADD = "add";
    private static final String OP_DEL = "del";

    private static final int COUNT = 100;
    private static final String STR = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CONSTANT = new String(new char[COUNT]).replace("\0", STR);

    @Override
    protected void doExecute() {
        ClusterHATest service = get(ClusterHATest.class);
        int iStartIdx = 0, iEndIdx = 0;
        long lInterOpDelay = 0L;

        try {
            iStartIdx = Integer.parseInt(strStartIdx);
            iEndIdx = Integer.parseInt(strEndIdx);
            lInterOpDelay = Long.parseLong(strInterOpDelay);
        } catch (Exception e) {
            print(e.getMessage());
            return;
        }

        if (!OP_ADD.equals(operation) && !OP_DEL.equals(operation)) {
            print("invalid operation " + operation);
            return;
        }

        print("cluster-ha-test " + operation + " " + iStartIdx + " " + iEndIdx + " " + lInterOpDelay);

        if (OP_ADD.equals(operation)) {
            for (int i = iStartIdx; i <= iEndIdx; i++) {
                service.addToStringStore(i, CONSTANT);
                try {
                    Thread.sleep(lInterOpDelay);
                } catch (Exception e) {
                    return;
                }
            }

        } else if (OP_DEL.equals(operation)) {
            for (int i = iStartIdx; i <= iEndIdx; i++) {
                service.removeStringFromStore(i);
                try {
                    Thread.sleep(lInterOpDelay);
                } catch (Exception e) {

                }
            }
        } else {
            print("Invalid operation " + operation);
        }
    }
}