package org.onlab.netty;

/**
 * State transitions a decoder goes through as it is decoding an incoming message.
 */
public enum DecoderState {
    READ_HEADER_VERSION,
    READ_PREAMBLE,
    READ_CONTENT_LENGTH,
    READ_SERIALIZER_VERSION,
    READ_CONTENT
}
