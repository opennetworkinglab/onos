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

import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;

@Service
@Command(scope = "onos", name = "global-label-release",
description = "Releases labels to global label resource pool.")
public class GlobalLabelReleaseCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "releaseLabelIds",
            description = "Represents for the label ids that are released. They are splited by dot symbol",
            required = true, multiValued = false)
    String releaseLabelIds = null;

    @Override
    protected void doExecute() {
        LabelResourceService lrs = get(LabelResourceService.class);
        Set<LabelResourceId> release = new HashSet<LabelResourceId>();
        String[] labelIds = releaseLabelIds.split(",");
        LabelResourceId resource = null;
        for (int i = 0; i < labelIds.length; i++) {
            resource = LabelResourceId.labelResourceId(Long.parseLong(labelIds[i]));
            release.add(resource);
        }
        lrs.releaseToGlobalPool(release);
    }

}
