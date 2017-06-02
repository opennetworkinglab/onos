/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.yang.compiler.api.YangCompilationParam;
import org.onosproject.yang.compiler.api.YangCompilerService;
import org.onosproject.yang.compiler.datamodel.YangNode;
import org.onosproject.yang.compiler.tool.DefaultYangCompilationParam;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.runtime.DefaultModelRegistrationParam;
import org.onosproject.yang.runtime.ModelRegistrationParam;
import org.onosproject.yang.runtime.YangModelRegistry;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.onosproject.yang.compiler.datamodel.utils.DataModelUtils.deSerializeDataModel;
import static org.onosproject.yang.compiler.utils.io.impl.YangIoUtils.deleteDirectory;
import static org.onosproject.yang.runtime.helperutils.YangApacheUtils.processYangModel;

/**
 * Yang files upload resource.
 */
@Path("compiler")
public class YangWebResource extends AbstractWebResource {
    private static final String YANG_FILE_EXTENSION = ".yang";
    private static final String SER_FILE_EXTENSION = ".ser";
    private static final String REGISTER = "register";
    private static final String UNREGISTER = "unregister";
    private static final String CODE_GEN_DIR = "target/generated-sources/";
    private static final String META_DATA_DIR = "target/yang/resources/";
    private static final String SERIALIZED_FILE_NAME = "YangMetaData.ser";
    private static final String UNKNOWN_KEY = "Key must be either register " +
            "or unregister.";

    /**
     * Compiles and registers the given yang files.
     *
     * @param formData YANG files or ser files
     * @return 200 OK
     * @throws IOException when fails to generate a file
     */
    @Path("upload")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(FormDataMultiPart formData) throws IOException {
        Map<String, List<File>> input = parseInputData(formData);

        for (Map.Entry<String, List<File>> entry : input.entrySet()) {
            deleteDirectory(CODE_GEN_DIR);
            deleteDirectory(META_DATA_DIR);

            YangCompilerService liveCompiler = get(YangCompilerService.class);
            liveCompiler.compileYangFiles(createCompilationParam(
                    entry.getValue()));

            YangModelRegistry modelRegistry = get(YangModelRegistry.class);
            String key = entry.getKey();
            if (key.equalsIgnoreCase(REGISTER)) {
                modelRegistry.registerModel(getModelRegParam());
            } else if (key.equalsIgnoreCase(UNREGISTER)) {
                modelRegistry.unregisterModel(getModelRegParam());
            } else {
                return Response.serverError().entity(UNKNOWN_KEY).build();
            }
        }

        // TODO : create bundles

        return Response.status(200).build();
    }

    private File getInputFile(InputStream stream, String fileName)
            throws IOException {
        byte[] content = IOUtils.toByteArray(stream);
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fop = new FileOutputStream(file);
        fop.write(content);
        fop.flush();
        fop.close();
        return file;
    }

    private Map<String, List<File>> parseInputData(FormDataMultiPart formData)
            throws IOException {
        Map<String, List<File>> input = new HashMap<>();
        Map<String, List<FormDataBodyPart>> fieldsByName = formData.getFields();
        for (Map.Entry<String, List<FormDataBodyPart>> entry :
                fieldsByName.entrySet()) {
            List<File> inputFiles = new LinkedList<>();
            for (FormDataBodyPart field : entry.getValue()) {
                InputStream stream = field.getEntityAs(InputStream.class);
                FormDataContentDisposition content = field
                        .getFormDataContentDisposition();
                String fileName = content.getFileName();
                inputFiles.add(getInputFile(stream, fileName));
            }
            input.put(entry.getKey(), inputFiles);
        }
        return input;
    }

    private YangCompilationParam createCompilationParam(List<File> inputFiles)
            throws IOException {
        YangCompilationParam param = new DefaultYangCompilationParam();
        for (File file : inputFiles) {
            if (file.getName().endsWith(YANG_FILE_EXTENSION)) {
                param.addYangFile(Paths.get(file.getAbsolutePath()));
            } else if (file.getName().endsWith(SER_FILE_EXTENSION)) {
                param.addDependentSchema(Paths.get(file.getAbsolutePath()));
            }
        }
        param.setCodeGenDir(Paths.get(CODE_GEN_DIR));
        param.setMetadataGenDir(Paths.get(META_DATA_DIR));
        return param;
    }

    private ModelRegistrationParam getModelRegParam() throws IOException {
        String metaPath = META_DATA_DIR + SERIALIZED_FILE_NAME;
        List<YangNode> curNodes = getYangNodes(metaPath);
        if (curNodes != null && !curNodes.isEmpty()) {
            YangModel model = processYangModel(metaPath, curNodes);
            return DefaultModelRegistrationParam.builder()
                    .setYangModel(model).build();
        }
        return null;
    }

    private List<YangNode> getYangNodes(String path) throws IOException {
        List<YangNode> nodes = new LinkedList<YangNode>();
        File file = new File(path);
        if (file.getName().endsWith(SER_FILE_EXTENSION)) {
            nodes.addAll(deSerializeDataModel(file.toString()));
        }
        return nodes;
    }
}
