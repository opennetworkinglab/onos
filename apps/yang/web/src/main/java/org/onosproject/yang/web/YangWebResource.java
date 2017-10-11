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

package org.onosproject.yang.web;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.yang.YangLiveCompilerService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

/**
 * Yang files upload resource.
 */
@Path("models")
public class YangWebResource extends AbstractWebResource {
    private static final String YANG_FILE_EXTENSION = ".yang";
    private static final String SER_FILE_EXTENSION = ".ser";
    private static final String JAR_FILE_EXTENSION = ".jar";
    private static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String REGISTER = "register";
    private static final String UNREGISTER = "unregister";
    private static final String CODE_GEN_DIR = "target/generated-sources/";
    private static final String YANG_RESOURCES = "target/yang/resources/";
    private static final String SERIALIZED_FILE_NAME = "YangMetaData.ser";
    private static final String UNKNOWN_KEY = "Key must be either register " +
            "or unregister.";
    private static final String SLASH = "/";

    /**
     * Compiles and registers the given yang files.
     *
     * @param modelId model identifier
     * @param stream  YANG, ZIP or JAR file
     * @return 200 OK
     * @throws IOException when fails to generate a file
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@QueryParam("modelId") @DefaultValue("org.onosproject.model.unknown") String modelId,
                           @FormDataParam("file") InputStream stream) throws IOException {
        YangLiveCompilerService compiler = get(YangLiveCompilerService.class);
        ApplicationAdminService appService = get(ApplicationAdminService.class);
        appService.install(compiler.compileYangFiles(modelId, stream));
        appService.activate(appService.getId(modelId));
        return Response.ok().build();
    }
}
