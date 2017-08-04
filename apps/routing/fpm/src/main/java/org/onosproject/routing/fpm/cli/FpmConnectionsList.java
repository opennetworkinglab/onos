/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.fpm.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.routing.fpm.FpmInfoService;

/**
 * Displays the current FPM connections.
 */
@Command(scope = "onos", name = "fpm-connections",
        description = "Displays the current FPM connections")
public class FpmConnectionsList extends AbstractShellCommand {

    private static final String FORMAT = "%s:%s connected since %s";

    @Override
    protected void execute() {
        FpmInfoService fpmInfo = AbstractShellCommand.get(FpmInfoService.class);

        fpmInfo.peers().forEach((peer, timestamp) -> {
            print(FORMAT, peer.address(), peer.port(), Tools.timeAgo(timestamp));
        });
    }
}
