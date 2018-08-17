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
import com.google.common.io.Files;
import org.onlab.util.FilePathValidator;
import org.onosproject.yang.YangLiveCompilerService;
import org.onosproject.yang.compiler.tool.DefaultYangCompilationParam;
import org.onosproject.yang.compiler.tool.YangCompilerManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Files.createParentDirs;
import static com.google.common.io.Files.write;
import static java.nio.file.Files.walkFileTree;

/**
 * Represents implementation of YANG live compiler manager.
 */
@Component(immediate = true, service = YangLiveCompilerService.class)
public class YangLiveCompilerManager implements YangLiveCompilerService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ZIP_MAGIC = "PK";

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public InputStream compileYangFiles(String modelId,
                                        InputStream yangSources) throws IOException {
        // Generate temporary directory where the work will happen.
        File root = Files.createTempDir();
        log.info("Compiling YANG model to {}", root);

        // Unpack the input stream
        File yangRoot = unpackYangSources(root, yangSources);

        // Run the YANG compilation phase
        File javaRoot = runYangCompiler(root, yangRoot, modelId);

        // Run the Java compilation phase
        File classRoot = runJavaCompiler(root, javaRoot, modelId);

        // Run the JAR assembly phase
        File jarFile = runJarAssembly(root, classRoot, modelId);

        // Return the final JAR file as input stream
        return new FileInputStream(jarFile);
    }

    // Unpacks the given input stream into the YANG root subdirectory of the specified root directory.
    private File unpackYangSources(File root, InputStream yangSources) throws IOException {
        File yangRoot = new File(root, "yang/");
        if (yangRoot.mkdirs()) {
            // Unpack the yang sources into the newly created directory
            byte[] cache = toByteArray(yangSources);
            InputStream bis = new ByteArrayInputStream(cache);
            if (isZipArchive(cache)) {
                extractZipArchive(yangRoot, bis);
            } else {
                extractYangFile(yangRoot, bis);
            }
            return yangRoot;
        }
        throw new IOException("Unable to create yang source root");
    }

    // Extracts the YANG source stream into the specified directory.
    private void extractYangFile(File dir, InputStream stream) throws IOException {
        ByteStreams.copy(stream, new FileOutputStream(new File(dir, "model.yang")));
    }

    // Extracts the ZIP stream into the specified directory.
    private void extractZipArchive(File dir, InputStream stream) throws IOException {
        ZipInputStream zis = new ZipInputStream(stream);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (FilePathValidator.validateZipEntry(entry, dir)) {
                if (!entry.isDirectory()) {
                    byte[] data = toByteArray(zis);
                    zis.closeEntry();
                    File file = new File(dir, entry.getName());
                    createParentDirs(file);
                    write(data, file);
                }
            } else {
                throw new IOException("Zip archive is attempting to create a file outside of its root");
            }
        }
        zis.close();
    }

    // Runs the YANG compiler on the YANG sources in the specified directory.
    private File runYangCompiler(File root, File yangRoot, String modelId) throws IOException {
        File javaRoot = new File(root, "java/");
        if (javaRoot.mkdirs()) {
            // Prepare the compilation parameter
            DefaultYangCompilationParam.Builder param = DefaultYangCompilationParam.builder()
                    .setCodeGenDir(new File(javaRoot, "src").toPath())
                    .setMetadataGenDir(new File(javaRoot, "schema").toPath())
                    .setModelId(modelId);

            // TODO: How to convey YANG dependencies? "/dependencies" directory?

            // Iterate over all files and add all YANG sources.
            walkFileTree(Paths.get(yangRoot.getAbsolutePath()),
                         new SimpleFileVisitor<Path>() {
                             @Override
                             public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                                     throws IOException {
                                 if (attributes.isRegularFile() && file.toString().endsWith(".yang")) {
                                     param.addYangFile(file);
                                 }
                                 return FileVisitResult.CONTINUE;
                             }
                         });

            // Run the YANG compiler and collect the results
            new YangCompilerManager().compileYangFiles(param.build());
            return javaRoot;
        }
        throw new IOException("Unable to create Java results root");
    }

    // Runs the Java compilation on the Java sources generated by YANG compiler.
    private File runJavaCompiler(File root, File javaRoot, String modelId) throws IOException {
        File classRoot = new File(root, "classes/");
        if (classRoot.mkdirs()) {
            File compilerScript = writeResource("onos-yang-javac", root);
            writeResource("YangModelRegistrator.java", root);
            execute(new String[]{
                    "bash",
                    compilerScript.getAbsolutePath(),
                    javaRoot.getAbsolutePath(),
                    classRoot.getAbsolutePath(),
                    modelId
            });
            return classRoot;
        }
        throw new IOException("Unable to create class results root");
    }

    // Run the JAR assembly on the classes root and include any YANG sources as well.
    private File runJarAssembly(File root, File classRoot, String modelId) throws IOException {
        File jarFile = new File(root, "model.jar");
        File jarScript = writeResource("onos-yang-jar", root);
        writeResource("app.xml", root);
        writeResource("features.xml", root);
        writeResource("YangModelRegistrator.xml", root);
        execute(new String[]{
                "bash",
                jarScript.getAbsolutePath(),
                classRoot.getAbsolutePath(),
                jarFile.getAbsolutePath(),
                modelId
        });
        return jarFile;
    }

    // Writes the specified resource as a file in the given directory.
    private File writeResource(String resourceName, File dir) throws IOException {
        File script = new File(dir, resourceName);
        write(toByteArray(getClass().getResourceAsStream("/" + resourceName)), script);
        return script;
    }

    // Indicates whether the stream encoded in the given bytes is a ZIP archive.
    private boolean isZipArchive(byte[] bytes) {
        return substring(bytes, ZIP_MAGIC.length()).equals(ZIP_MAGIC);
    }

    // Returns the substring of maximum possible length from the specified bytes.
    private String substring(byte[] bytes, int length) {
        return new String(bytes, 0, Math.min(bytes.length, length), StandardCharsets.UTF_8);
    }

    // Executes the given command arguments as a system command.
    private void execute(String[] command) throws IOException {
        try {
            Process process = Runtime.getRuntime().exec(command);
            byte[] output = toByteArray(process.getInputStream());
            byte[] error = toByteArray(process.getErrorStream());
            int code = process.waitFor();
            if (code != 0) {
                log.info("Command failed: status={}, output={}, error={}",
                         code, new String(output), new String(error));
            }
        } catch (InterruptedException e) {
            log.error("Interrupted executing command {}", command, e);
            Thread.currentThread().interrupt();
        }
    }
}
