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

package org.onosproject.p4runtime.ctl.codec;

import com.google.protobuf.ByteString;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiData;
import org.onosproject.net.pi.runtime.data.PiBitString;
import org.onosproject.net.pi.runtime.data.PiBool;
import org.onosproject.net.pi.runtime.data.PiEnumString;
import org.onosproject.net.pi.runtime.data.PiErrorString;
import org.onosproject.net.pi.runtime.data.PiHeader;
import org.onosproject.net.pi.runtime.data.PiHeaderStack;
import org.onosproject.net.pi.runtime.data.PiHeaderUnion;
import org.onosproject.net.pi.runtime.data.PiHeaderUnionStack;
import org.onosproject.net.pi.runtime.data.PiStruct;
import org.onosproject.net.pi.runtime.data.PiTuple;

import java.util.List;
import java.util.stream.Collectors;

import static p4.v1.P4DataOuterClass.P4Data;
import static p4.v1.P4DataOuterClass.P4Header;
import static p4.v1.P4DataOuterClass.P4HeaderStack;
import static p4.v1.P4DataOuterClass.P4HeaderUnion;
import static p4.v1.P4DataOuterClass.P4HeaderUnionStack;
import static p4.v1.P4DataOuterClass.P4StructLike;

/**
 * Encoder/decoder of PI Data entry to P4 Data entry protobuf messages, and vice
 * versa.
 * <p>
 * TODO: implement codec for each P4Data type using AbstractP4RuntimeCodec.
 */
final class P4DataCodec {

    private P4DataCodec() {
        // Hides constructor.
    }

    private static P4Header encodeHeader(PiHeader piHeader) {
        P4Header.Builder builder = P4Header.newBuilder();
        int i = 0;
        for (ImmutableByteSequence bitString : piHeader.bitStrings()) {
            builder.setBitstrings(i, ByteString.copyFrom(bitString.asArray()));
            i++;
        }
        return builder.setIsValid(piHeader.isValid()).build();
    }

    private static PiHeader decodeHeader(P4Header p4Header) {
        List<ImmutableByteSequence> bitStrings = p4Header.getBitstringsList().stream()
                .map(bit -> ImmutableByteSequence.copyFrom(bit.asReadOnlyByteBuffer()))
                .collect(Collectors.toList());

        return PiHeader.of(p4Header.getIsValid(), bitStrings);
    }

    private static P4HeaderUnion encodeHeaderUnion(PiHeaderUnion headerUnion) {

        P4HeaderUnion.Builder builder = P4HeaderUnion.newBuilder();
        if (headerUnion.isValid()) {
            builder.setValidHeader(encodeHeader(headerUnion.header()));
            builder.setValidHeaderName(headerUnion.headerName());
        } else {
            // An empty string indicates that none of the union members are valid and
            // valid_header must therefore be unset.
            builder.setValidHeaderName("");
        }

        return builder.build();
    }

    private static PiHeaderUnion decodeHeaderUnion(P4HeaderUnion p4HeaderUnion) {

        return PiHeaderUnion.of(p4HeaderUnion.getValidHeaderName(),
                                decodeHeader(p4HeaderUnion.getValidHeader()));
    }

    private static P4StructLike encodeStruct(PiStruct piStruct) {
        P4StructLike.Builder builder = P4StructLike.newBuilder();
        builder.addAllMembers(piStruct.struct().stream()
                                      .map(P4DataCodec::encodeP4Data)
                                      .collect(Collectors.toList()));
        return builder.build();
    }

    private static PiStruct decodeStruct(P4StructLike p4StructLike) {

        return PiStruct.of(p4StructLike.getMembersList().stream()
                                   .map(P4DataCodec::decodeP4Data)
                                   .collect(Collectors.toList()));
    }

    private static P4StructLike encodeTuple(PiTuple piTuple) {
        P4StructLike.Builder builder = P4StructLike.newBuilder();
        builder.addAllMembers(piTuple.tuple().stream()
                                      .map(P4DataCodec::encodeP4Data)
                                      .collect(Collectors.toList()));
        return builder.build();
    }

    private static PiTuple decodeTuple(P4StructLike p4StructLike) {

        return PiTuple.of(p4StructLike.getMembersList().stream()
                                  .map(P4DataCodec::decodeP4Data)
                                  .collect(Collectors.toList()));
    }

    static P4Data encodeP4Data(PiData piData) {

        P4Data.Builder builder = P4Data.newBuilder();
        switch (piData.type()) {
            case BITSTRING:
                builder.setBitstring(ByteString.copyFrom(((PiBitString) piData).bitString().asArray()));
                break;
            case ENUMSTRING:
                builder.setEnum(((PiEnumString) piData).enumString());
                break;
            case ERRORSTRING:
                builder.setError(((PiErrorString) piData).errorString());
                break;
            case BOOL:
                builder.setBool(((PiBool) piData).bool());
                break;
            case STRUCT:
                builder.setStruct(encodeStruct((PiStruct) piData));
                break;
            case TUPLE:
                builder.setTuple(encodeTuple((PiTuple) piData));
                break;
            case HEADER:
                builder.setHeader(encodeHeader((PiHeader) piData));
                break;
            case HEADERSTACK:
                P4HeaderStack.Builder headerStack = P4HeaderStack.newBuilder();
                int i = 0;
                for (PiHeader header : ((PiHeaderStack) piData).headers()) {
                    headerStack.setEntries(i, encodeHeader(header));
                    i++;
                }
                builder.setHeaderStack(headerStack.build());
                break;
            case HEADERUNION:
                builder.setHeaderUnion(encodeHeaderUnion((PiHeaderUnion) piData));
                break;
            case HEADERUNIONSTACK:
                P4HeaderUnionStack.Builder headerUnionStack = P4HeaderUnionStack.newBuilder();
                int j = 0;
                for (PiHeaderUnion headerUnion : ((PiHeaderUnionStack) piData).headerUnions()) {
                    headerUnionStack.setEntries(j, encodeHeaderUnion(headerUnion));
                    j++;
                }
                builder.setHeaderUnionStack(headerUnionStack.build());
                break;
            default:
                break;
        }

        return builder.build();
    }

    static PiData decodeP4Data(P4Data p4Data) {
        PiData piData = null;

        switch (p4Data.getDataCase()) {
            case BITSTRING:
                piData = PiBitString.of(ImmutableByteSequence.copyFrom(p4Data.getBitstring().asReadOnlyByteBuffer()));
                break;
            case BOOL:
                piData = PiBool.of(p4Data.getBool());
                break;
            case TUPLE:
                piData = decodeTuple(p4Data.getTuple());
                break;
            case STRUCT:
                piData = decodeStruct(p4Data.getStruct());
                break;
            case HEADER:
                piData = decodeHeader(p4Data.getHeader());
                break;
            case HEADER_UNION:
                piData = decodeHeaderUnion(p4Data.getHeaderUnion());
                break;
            case HEADER_STACK:
                piData = PiHeaderStack.of(p4Data.getHeaderStack().getEntriesList().stream()
                                                  .map(P4DataCodec::decodeHeader)
                                                  .collect(Collectors.toList()));
                break;
            case HEADER_UNION_STACK:
                piData = PiHeaderUnionStack.of(p4Data.getHeaderUnionStack()
                                                       .getEntriesList().stream()
                                                       .map(P4DataCodec::decodeHeaderUnion)
                                                       .collect(Collectors.toList()));
                break;
            case ENUM:
                piData = PiEnumString.of(p4Data.getEnum());
                break;
            case ERROR:
                piData = PiErrorString.of(p4Data.getError());
                break;
            case DATA_NOT_SET:
                break;
            default:
                break;
        }

        return piData;
    }
}
