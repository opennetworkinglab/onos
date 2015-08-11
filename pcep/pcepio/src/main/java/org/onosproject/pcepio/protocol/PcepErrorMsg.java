package org.onosproject.pcepio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.types.ErrorObjListWithOpen;

/**
 * Abstraction of an entity providing PCEP Error Message.
 */
public interface PcepErrorMsg extends PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns Object of ErrorObjListWithOpen.
     *
     * @return Object of ErrorObjListWithOpen
     */
    public ErrorObjListWithOpen getErrorObjListWithOpen();

    /**
     * Sets errObjListWithOpen object.
     *
     * @param errObjListWithOpen error object List with open object
     */
    public void setErrorObjListWithOpen(ErrorObjListWithOpen errObjListWithOpen);

    /**
     * Returns Object of PcepErrorInfo.
     *
     * @return Object of PcepErrorInfo
     */
    public PcepErrorInfo getPcepErrorInfo();

    /**
     * Sets errInfo Object.
     *
     * @param errInfo error information
     */
    public void setPcepErrorInfo(PcepErrorInfo errInfo);

    @Override
    void writeTo(ChannelBuffer channelBuffer);

    /**
     * Builder interface with get and set functions to build PCEP Error message.
     */
    public interface Builder extends PcepMessage.Builder {

        @Override
        PcepErrorMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns Object of ErrorObjListWithOpen.
         *
         * @return Object of ErrorObjListWithOpen
         */
        public ErrorObjListWithOpen getErrorObjListWithOpen();

        /**
         * Sets errObjListWithOpen object.
         *
         * @param errObjListWithOpen error object with open object
         * @return builder by setting Object of ErrorObjListWithOpen
         */
        public Builder setErrorObjListWithOpen(ErrorObjListWithOpen errObjListWithOpen);

        /**
         * Returns Object of PcepErrorInfo.
         *
         * @return Object of PcepErrorInfo
         */
        public PcepErrorInfo getPcepErrorInfo();

        /**
         * Sets errInfo Object.
         *
         * @param errInfo error information
         * @return builder by getting Object of PcepErrorInfo
         */
        public Builder setPcepErrorInfo(PcepErrorInfo errInfo);
    }
}
