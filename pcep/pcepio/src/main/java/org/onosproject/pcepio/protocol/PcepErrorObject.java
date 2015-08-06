package org.onosproject.pcepio.protocol;

import java.util.LinkedList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;

/**
 * Abstraction of an entity providing PCEP Error Object.
 */
public interface PcepErrorObject {

    /**
     * Returns Error Type in Error Object.
     *
     * @return Error Type in Error Object
     */
    int getErrorType();

    /**
     * Sets Error Type in Error Object.
     *
     * @param value Error Type
     */
    void setErrorType(byte value);

    /**
     * Returns Error Value in Error Object.
     *
     * @return Error Value
     */
    byte getErrorValue();

    /**
     * Sets Error Value in Error Object.
     *
     * @param value Error Value
     */
    void setErrorValue(byte value);

    /**
     * Returns Optional Tlvs in Error Object.
     *
     * @return list of Optional Tlvs in Error Object
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets Optional Tlvs in Error Object.
     *
     * @param llOptionalTlv list of Optional Tlvs
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Prints attributes of Error object.
     */
    void print();

    /**
     * Writes the Error Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing Error Object into ChannelBuffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Error object.
     */
    public interface Builder {

        /**
         * Builds Error Object.
         *
         * @return Error Object.
         */
        PcepErrorObject build();

        /**
         * Returns Error Object header.
         *
         * @return Error Object header
         */
        PcepObjectHeader getErrorObjHeader();

        /**
         * Sets Error Object header and returns its Builder.
         *
         * @param obj Error Object header
         * @return Builder by setting Error Object header
         */
        Builder setErrorObjHeader(PcepObjectHeader obj);

        /**
         * Returns Error Type in Error Object.
         *
         * @return Error Type in Error Object
         */
        int getErrorType();

        /**
         * Sets Error Type and returns its builder.
         *
         * @param value of Error-Type field
         * @return builder by setting Error Type field.
         */
        Builder setErrorType(byte value);

        /**
         * Returns Error Value in Error Object.
         *
         * @return Error Value
         */
        byte getErrorValue();

        /**
         * Sets Error Value and returns its builder.
         *
         * @param value of Error-Value field
         * @return Builder by setting Error Value field.
         */
        Builder setErrorValue(byte value);

        /**
         * Returns list of Optional Tlvs of Error Object.
         *
         * @return list of Optional Tlvs of Error Object
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * Sets Optional Tlvs of Error Object and returns its Builder.
         *
         * @param llOptionalTlv Optional Tlvs of Error Object
         * @return Builder by setting Optional Tlvs.
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in Error object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in Error object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
