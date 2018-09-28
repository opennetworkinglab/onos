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
package org.onosproject.yang.impl;

import com.google.common.io.ByteStreams;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.yang.YangLiveCompilerService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Compiles the provided YANG source files and installs the resulting model extension.
 */
@Service
@Command(scope = "onos", name = "compile-model",
        description = "Compiles the provided YANG source files and installs the resulting model extension")
public class YangCompileCommand extends AbstractShellCommand {

    @Option(name = "-c", aliases = "--compile-only",
            description = "Only compile, but do not install the model")
    private boolean compileOnly = false;

    @Option(name = "-f", aliases = "--force",
            description = "Force reinstall if already installed")
    private boolean forceReinstall = false;

    @Argument(index = 0, name = "modelId",
            description = "Model ID", required = true)
    String modelId = null;

    @Argument(index = 1, name = "url",
            description = "URL to the YANG source file(s); .yang, .zip or .jar file",
            required = true)
    String url = null;

    @Override
    protected void doExecute() {
        try {
            InputStream yangJar = new URL(url).openStream();
            YangLiveCompilerService compiler = get(YangLiveCompilerService.class);
            if (compileOnly) {
                ByteStreams.copy(compiler.compileYangFiles(modelId, yangJar), System.out);
            } else {
                ApplicationAdminService appService = get(ApplicationAdminService.class);

                if (forceReinstall) {
                    ApplicationId appId = appService.getId(modelId);
                    if (appId != null && appService.getApplication(appId) != null) {
                        appService.uninstall(appId);
                    }
                }

                appService.install(compiler.compileYangFiles(modelId, yangJar));
                appService.activate(appService.getId(modelId));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
