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
package org.onosproject.incubator.net.resource.label.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.resource.label.LabelResourcePool;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

@Service
@Command(scope = "onos", name = "global-label-pool",
      description = "Gets global label resource pool information.")
public class GlobalLabelCommand extends AbstractShellCommand {
    private static final String FMT = "deviceid=%s, beginLabel=%s,"
            + "endLabel=%s, totalNum=%s, usedNum=%s, currentUsedMaxLabelId=%s,"
            + "releaseLabelIds=%s";

    @Override
    protected void doExecute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        LabelResourcePool pool = lrs.getGlobalLabelResourcePool();
        if (pool != null) {
            print(FMT, pool.deviceId().toString(), pool.beginLabel(),
                  pool.endLabel(), pool.totalNum(), pool.usedNum(),
                  pool.currentUsedMaxLabelId(), pool.releaseLabelId()
                          .toString());
        }
    }

}
