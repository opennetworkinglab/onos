/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import java.util.Collection;
import java.util.Iterator;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

@Command(scope = "onos", name = "global-label-apply",
      description = "Apply global labels from global resource pool")
public class GlobalLabelApplyCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "applyNum",
            description = "Applying number means how many labels applications want to use.",
            required = true, multiValued = false)
    String applyNum = null;

    private static final String FMT = "deviceid=%s, labelresourceid=%s";

    @Override
    protected void execute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        Collection<LabelResource> result =
                lrs.applyFromGlobalPool(Long.parseLong(applyNum));
        if (result.size() > 0) {
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
