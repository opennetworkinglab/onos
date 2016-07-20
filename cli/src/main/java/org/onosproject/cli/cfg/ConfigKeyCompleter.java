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
package org.onosproject.cli.cfg;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.karaf.shell.console.completer.ArgumentCompleter.ArgumentList;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.SubjectFactory;

import com.google.common.collect.ImmutableList;

/**
 * Network configuration config key completer.
 *
 * Assumes 2 argument before the one being completed is SubjectClassKey
 * and argument right before the one being completed is SubjectKey.
 */
public class ConfigKeyCompleter extends AbstractChoicesCompleter {

    // FIXME ConfigKeyCompleter never gets called??
    @Override
    protected List<String> choices() {
        NetworkConfigRegistry service = AbstractShellCommand.get(NetworkConfigRegistry.class);
        ArgumentList args = getArgumentList();

        checkArgument(args.getCursorArgumentIndex() >= 2);
        String subjectClassKey = args.getArguments()[args.getCursorArgumentIndex() - 2];

        SubjectFactory<?> subjectFactory = service.getSubjectFactory(subjectClassKey);
        if (subjectFactory == null) {
            return ImmutableList.of();
        }

        String subjectKey = args.getArguments()[args.getCursorArgumentIndex() - 1];

        Object subject = subjectFactory.createSubject(subjectKey);
        Set<? extends Config<Object>> configs = service.getConfigs(subject);
        return configs.stream().map(Config::key).collect(Collectors.toList());
    }

}
