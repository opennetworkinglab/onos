/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.gnoi;

import com.google.protobuf.ByteString;
import gnoi.system.SystemOuterClass;
import gnoi.system.SystemOuterClass.SetPackageRequest;
import gnoi.system.SystemOuterClass.SetPackageResponse;
import gnoi.system.SystemOuterClass.SwitchControlProcessorRequest;
import gnoi.types.Types;
import org.onosproject.gnoi.api.GnoiClient;
import org.onosproject.gnoi.api.GnoiController;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.behaviour.SoftwareUpgrade;
import org.apache.commons.io.FilenameUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;


import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Implementation that upgrades the software on a device.
 */
public class GnoiSoftwareUpgradeImpl
        extends AbstractGrpcHandlerBehaviour<GnoiClient, GnoiController>
        implements SoftwareUpgrade {

    private static final String HASHING_METHOD = "MD5";
    private static final int MAX_CHUNK_SIZE = 64_000;
    private static final Path DEFAULT_PACKAGE_PATH = Paths.get("/tmp");
    private static final int STREAM_TIMEOUT_SECONDS = 10;

    public GnoiSoftwareUpgradeImpl() {
        super(GnoiController.class);
    }

    @Override
    public CompletableFuture<String> uploadPackage(String sourcePath, String dst) {
        if (!setupBehaviour("uploadPackage()")) {
            return CompletableFuture.completedFuture(null);
        }
        checkNotNull(sourcePath, "Source file not specified.");

        final CompletableFuture<String> future = new CompletableFuture<>();

        final File deb = Paths.get(sourcePath).toFile();
        final String destinationPath;

        final SetPackageRequest.Builder requestBuilder = SetPackageRequest.newBuilder();
        final SystemOuterClass.Package.Builder pkgBuilder = SystemOuterClass.Package.newBuilder();
        final Types.HashType.Builder hashBuilder = Types.HashType.newBuilder();

        final SynchronousQueue<SetPackageRequest> stream = new SynchronousQueue<>();
        final CompletableFuture<SetPackageResponse> futureResponse = client.setPackage(stream);

        if (dst == null) {
            destinationPath = getTempPath(sourcePath);
        } else {
            destinationPath = dst;
        }

        futureResponse.whenComplete((response, exception) -> {
            if (exception == null) {
                future.complete(destinationPath);
            } else {
                future.complete(null);
            }
        });

        // Handle reading file, creating requests, etc...
        CompletableFuture.runAsync(() -> {
            try {

                if (!deb.isFile()) {
                    log.error("File {} does not exist", sourcePath);
                    future.complete(null);
                    return;
                }
                // Set general package info (filename, version, etc...).
                pkgBuilder.setFilename(destinationPath);
                requestBuilder.setPackage(pkgBuilder.build());
                boolean requestSent = stream.offer(requestBuilder.build(), STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!requestSent) {
                    future.complete(null);
                    return;
                }

                final MessageDigest md = MessageDigest.getInstance(HASHING_METHOD);
                final FileInputStream buffer = new FileInputStream(deb);
                byte[] contents = new byte[MAX_CHUNK_SIZE];
                int read = 0;

                // Read file in 64k chunks.
                while ((read = buffer.read(contents, 0, MAX_CHUNK_SIZE)) != -1) {
                    // Calculate File hash.
                    md.update(contents, 0, read);

                    // Form next request.
                    requestBuilder.setContents(ByteString.copyFrom(contents, 0, read));

                    // Add file chunk to stream.
                    requestSent = stream.offer(requestBuilder.build(), STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (!requestSent) {
                        future.complete(null);
                        return;
                    }
                }

                // Convert hash to lowercase string.
                String hash = DatatypeConverter
                    .printHexBinary(md.digest())
                    .toLowerCase();

                hashBuilder
                    .setMethodValue(Types.HashType.HashMethod.MD5.getNumber())
                    .setHash(ByteString.copyFrom(hash.getBytes()));

                // Form last request with file hash.
                requestBuilder.setHash(hashBuilder.build());

                // Add file chunk to stream.
                requestSent = stream.offer(requestBuilder.build(), STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!requestSent) {
                    future.complete(null);
                    return;
                }

            } catch (IOException e) {
                log.error("Error while reading file {}", sourcePath, e);
                future.complete(null);
                return;
            } catch (InterruptedException e) {
                log.error("Interrupted while sending package", e);
                future.complete(null);
                return;
            } catch (SecurityException e) {
                log.error("File {} cannot be accessed", sourcePath, e);
                future.complete(null);
                return;
            } catch (NoSuchAlgorithmException e) {
                log.error("Invalid hashing algorithm {}", HASHING_METHOD, e);
                future.complete(null);
                return;
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Response> swapAgent(String packagePath) {
        if (!setupBehaviour("swapAgent()")) {
            return CompletableFuture.completedFuture(new Response());
        }
        checkNotNull(packagePath, "File path not specified.");

        final CompletableFuture<Response> future = new CompletableFuture<>();
        final Types.Path.Builder routeProcessor = Types.Path.newBuilder();
        final SwitchControlProcessorRequest.Builder requestMsg = SwitchControlProcessorRequest.newBuilder();

        Paths.get(packagePath)
            .iterator()
            .forEachRemaining(part -> {
                routeProcessor.addElem(
                        Types.PathElem.newBuilder()
                            .setName(part.toString())
                            .build());
            });

        requestMsg.setControlProcessor(routeProcessor.build());

        client.switchControlProcessor(requestMsg.build())
            .whenComplete((response, exception) -> {
                if (exception != null) {
                    future.complete(new Response());
                } else {
                    future.complete(new Response(response.getUptime(), response.getVersion()));
                }
            });

        return future;
    }

    private String getTempPath(String source) {
        String baseName = FilenameUtils.getBaseName(source);
        String extension = FilenameUtils.getExtension(source);

        if (extension.length() != 0) {
            extension = "." + extension;
        }

        String filename = baseName + "_" + UUID.randomUUID().toString() + extension;
        return DEFAULT_PACKAGE_PATH.resolve(filename).toString();
    }
}
