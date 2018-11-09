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

import java.util.Collection;
import java.util.Iterator;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

@Service
@Command(scope = "onos", name = "label-apply",
      description = "Apply label resource from device pool by specific device id")
public class LabelApplyCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device identity",
            required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "applyNum",
            description = "Applying number means how many labels applications want to use.",
            required = true, multiValued = false)
    String applyNum = null;

    private static final String FMT = "deviceid=%s, labelresourceid=%s";

    @Override
    protected void doExecute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        Collection<LabelResource> result = lrs.applyFromDevicePool(DeviceId
                .deviceId(deviceId), Long.parseLong(applyNum));
        if (!result.isEmpty()) {
            for (Iterator<LabelResource> iterator = result.iterator(); iterator
                    .hasNext();) {
                DefaultLabelResource defaultLabelResource = (DefaultLabelResource) iterator
                        .next();
                print(FMT, defaultLabelResource.deviceId().toString(),
                      defaultLabelResource.labelResourceId().toString());
            }
        }
    }

}
