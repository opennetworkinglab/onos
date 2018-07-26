"""
 Copyright 2018-present Open Networking Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""


"""
    Workspace to build GRPC java binaries. please see the injected bazel build file
    grpc_BUILD for additional information.
"""

def generate_grpc():
    native.new_http_archive(
        name = "grpc_src_zip_131",
        urls = ["https://github.com/grpc/grpc-java/archive/v1.3.1.zip"],
        sha256 = "c529b4c2d80a6df8ddc0aa25d7599e46ffcd155cb67122513f8fb536cd96eca4",
        build_file = "//tools/build/bazel:grpc_BUILD",
        strip_prefix = "grpc-java-1.3.1",
    )

def generated_maven_jars():
    native.maven_jar(
        name = "guava",
        artifact = "com.google.guava:guava:22.0",
        sha1 = "3564ef3803de51fb0530a8377ec6100b33b0d073",
    )

    native.maven_jar(
        name = "google_instrumentation_0_3_0",
        artifact = "com.google.instrumentation:instrumentation-api:0.3.0",
        sha1 = "a2e145e7a7567c6372738f5c5a6f3ba6407ac354",
    )

    native.maven_jar(
        name = "javax_annotation_api",
        artifact = "javax.annotation:javax.annotation-api:1.2",
        sha1 = "479c1e06db31c432330183f5cae684163f186146",
    )

    native.maven_jar(
        name = "jsr305",
        artifact = "com.google.code.findbugs:jsr305:3.0.1",
        sha1 = "f7be08ec23c21485b9b5a1cf1654c2ec8c58168d",
    )

    native.java_library(
        name = "google_errorprone_2_0_19",
        visibility = ["//visibility:public"],
        exports = ["@google_errorprone_2_0_19//jar"],
    )

def generated_java_libraries():
    native.java_library(
        name = "guava",
        visibility = ["//visibility:public"],
        exports = ["@guava//jar"],
    )

    native.java_library(
        name = "google_instrumentation_0_3_0",
        visibility = ["//visibility:public"],
        exports = ["@google_instrumentation_0_3_0//jar"],
    )

    native.java_library(
        name = "javax_annotation_api",
        visibility = ["//visibility:public"],
        exports = ["@javax_annotation_api//jar"],
    )

    native.java_library(
        name = "jsr305",
        visibility = ["//visibility:public"],
        exports = ["@jsr305//jar"],
    )

    native.java_library(
        name = "google_errorprone_2_0_19",
        visibility = ["//visibility:public"],
        exports = ["@google_errorprone_2_0_19//jar"],
    )
