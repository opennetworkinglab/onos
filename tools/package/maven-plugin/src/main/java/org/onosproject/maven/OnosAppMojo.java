/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

/**
 * Produces ONOS application archive.
 */
@Mojo(name = "app", defaultPhase = LifecyclePhase.PACKAGE)
public class OnosAppMojo extends AbstractMojo {

    @Parameter
    private String name;

    @Parameter
    private String version;

    @Parameter
    private String origin;

    @Parameter
    private String description;

    @Parameter
    private String featuresRepo;

    @Parameter
    private String features;

    @Parameter
    private String permissions;

    @Parameter
    private List<String> artifacts;


    public void execute() throws MojoExecutionException {
        getLog().info("Building ONOS application archive " + name + " version " + version);

    }
}


