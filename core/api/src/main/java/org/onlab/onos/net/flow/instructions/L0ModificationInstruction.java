package org.onlab.onos.net.flow.instructions;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

public abstract class L0ModificationInstruction implements Instruction {

    /**
     * Represents the type of traffic treatment.
     */
    public enum L0SubType {
        /**
         * Lambda modification.
         */
        LAMBDA

        //TODO: remaining types
    }

    public abstract L0SubType subtype();

    @Override
    public Type type() {
        return Type.L0MODIFICATION;
    }

    /**
     * Represents a L0 lambda modification instruction.
     */
    public static final class ModLambdaInstruction extends L0ModificationInstruction {

        private final L0SubType subtype;
        private final short lambda;

        public ModLambdaInstruction(L0SubType subType, short lambda) {
            this.subtype = subType;
            this.lambda = lambda;
        }

        @Override
        public L0SubType subtype() {
            return this.subtype;
        }

        public short lambda() {
            return this.lambda;
        }

        @Override
        public String toString() {
            return toStringHelper(subtype().toString())
                    .add("lambda", lambda).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(lambda, type(), subtype);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModLambdaInstruction) {
                ModLambdaInstruction that = (ModLambdaInstruction) obj;
                return  Objects.equals(lambda, that.lambda) &&
                        Objects.equals(this.type(), that.type()) &&
                        Objects.equals(subtype, that.subtype);
            }
            return false;
        }
    }
}
