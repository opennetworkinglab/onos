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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.onosproject.yang.compiler.datamodel.utils.DataModelUtils.deSerializeDataModel;
import static org.onosproject.yang.compiler.utils.io.impl.YangIoUtils.deleteDirectory;
import static org.onosproject.yang.runtime.helperutils.YangApacheUtils.processYangModel;

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
     * @param formData YANG files or ser files
     * @return 200 OK
     * @throws IOException when fails to generate a file
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(FormDataMultiPart formData) throws IOException {
        Map<String, List<File>> input = parseInputData(formData);

        for (Map.Entry<String, List<File>> entry : input.entrySet()) {
            deleteDirectory(CODE_GEN_DIR);
            deleteDirectory(YANG_RESOURCES);

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
            if (file.getName().endsWith(JAR_FILE_EXTENSION)
                    || file.getName().endsWith(ZIP_FILE_EXTENSION)) {
                List<File> files = decompressFile(file);

                for (File f : files) {
                    param = addToParam(param, f);
                }
            } else {
                param = addToParam(param, file);
            }
        }
        param.setCodeGenDir(Paths.get(CODE_GEN_DIR));
        param.setMetadataGenDir(Paths.get(YANG_RESOURCES));
        return param;
    }

    private YangCompilationParam addToParam(YangCompilationParam param,
                                            File file) {
        if (file.getName().endsWith(YANG_FILE_EXTENSION)) {
            param.addYangFile(Paths.get(file.getAbsolutePath()));
        } else if (file.getName().endsWith(SER_FILE_EXTENSION)) {
            param.addDependentSchema(Paths.get(file.getAbsolutePath()));
        }
        return param;
    }

    private ModelRegistrationParam getModelRegParam() throws IOException {
        String metaPath = YANG_RESOURCES + SERIALIZED_FILE_NAME;
        List<YangNode> curNodes = getYangNodes(metaPath);
        if (curNodes != null && !curNodes.isEmpty()) {
            YangModel model = processYangModel(metaPath, curNodes);
            return DefaultModelRegistrationParam.builder()
                    .setYangModel(model).build();
        }
        return null;
    }

    private List<YangNode> getYangNodes(String path) throws IOException {
        List<YangNode> nodes = new LinkedList<>();
        File file = new File(path);
        if (file.getName().endsWith(SER_FILE_EXTENSION)) {
            nodes.addAll(deSerializeDataModel(file.toString()));
        }
        return nodes;
    }

    private static List<File> decompressFile(File jarFile)
            throws IOException {
        ZipFile zip = new ZipFile(jarFile);
        final List<File> unzipedFiles = new LinkedList<>();

        // first get all directories,
        // then make those directory on the destination Path
        for (Enumeration<? extends ZipEntry> enums = zip.entries();
             enums.hasMoreElements();) {
            ZipEntry entry = enums.nextElement();

            String fileName = YANG_RESOURCES + entry.getName();
            File f = new File(fileName);

            if (fileName.endsWith(SLASH)) {
                f.mkdirs();
            }
        }

        //now create all files
        for (Enumeration<? extends ZipEntry> enums = zip.entries();
             enums.hasMoreElements();) {
            ZipEntry entry = enums.nextElement();
            String fileName = YANG_RESOURCES + entry.getName();
            File f = new File(fileName);

            if (!fileName.endsWith(SLASH)) {
                InputStream is = zip.getInputStream(entry);
                FileOutputStream fos = new FileOutputStream(f);

                // write contents of 'is' to 'fos'
                while (is.available() > 0) {
                    fos.write(is.read());
                }
                unzipedFiles.add(f);
                fos.close();
                is.close();
            }
        }
        return unzipedFiles;
    }
}
