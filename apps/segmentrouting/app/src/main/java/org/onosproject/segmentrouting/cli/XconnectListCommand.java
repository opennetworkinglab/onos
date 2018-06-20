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
package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;

/**
 * Lists Xconnects.
 */
@Command(scope = "onos", name = "sr-xconnect", description = "Lists all Xconnects")
public class XconnectListCommand extends AbstractShellCommand {
    @Override
    protected void execute() {
        XconnectService xconnectService = get(XconnectService.class);
        xconnectService.getXconnects().forEach(desc -> print("%s", desc));
    }
}
