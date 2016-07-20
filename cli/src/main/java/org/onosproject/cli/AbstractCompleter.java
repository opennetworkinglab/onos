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
package org.onosproject.cli;

import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.ArgumentCompleter;

/**
 * Abstract argument completer.
 */
public abstract class AbstractCompleter implements Completer {

    /**
     * Returns the argument list.
     *
     * @return argument list
     */
    protected ArgumentCompleter.ArgumentList getArgumentList() {
        CommandSession session = CommandSessionHolder.getSession();
        return (ArgumentCompleter.ArgumentList)
                session.get(ArgumentCompleter.ARGUMENTS_LIST);
    }

}
