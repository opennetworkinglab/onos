package org.projectfloodlight.openflow.types;

import javax.annotation.concurrent.Immutable;

/** a hash value that supports bit-wise combinations, mainly to calculate hash values for
 *  reconciliation operations.
 *
 * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
 *
 * @param <H> - this type, for return type safety.
 */
@Immutable
public interface HashValue<H extends HashValue<H>> {
    /** return the "numBits" highest-order bits of the hash.
     *  @param numBits number of higest-order bits to return [0-32].
     *  @return a numberic value of the 0-32 highest-order bits.
     */
    int prefixBits(int numBits);

    /** perform an arithmetic addition of this value and other. Wraps around on
     * overflow of the defined word size.
     *
     * @param other
     * @return this + other
     */
    H add(H other);

    /**
     * arithmetically substract the given 'other' value from this value.
     * around on overflow.
     *
     * @param other
     * @return this - other
     */
    H subtract(H other);

    /** @return the bitwise inverse of this value */
    H inverse();

    /** or this value with another value value of the same type */
    H or(H other);

    /** and this value with another value value of the same type */
    H and(H other);

    /** xor this value with another value value of the same type */
    H xor(H other);

    /** create and return a builder */
    Builder<H> builder();

    /** a mutator for HashValues. Allows perfomring a series of
     *  operations on a hashv value without the associated cost of object
     *  reallocation.
     *
     * @author Andreas Wundsam <andreas.wundsam@bigswitch.com>
     *
     * @param <H> - the hashvalue
     */
    public interface Builder<H> {
        /** perform an arithmetic addition of this value and other. Wraps around on
         * overflow of the defined word size.
         *
         * @param other
         * @return this mutator
         */
        Builder<H> add(H other);

        /**
         * arithmetically substract the given 'other' value from the value stored in this mutator.
         * around on overflow.
         *
         * @param other
         * @return this mutator
         */
        Builder<H> subtract(H other);

        /** bitwise invert the value stored in this mutator
         *
         * @return this mutator
         */
        Builder<H> invert();

        /** or the value stored in this mutator with another value value of the same type
        * @return this mutator
        */
        Builder<H> or(H other);

        /** and the value stored in this mutator with another value value of the same type
        * @return this mutator
        */
        Builder<H> and(H other);

        /** xor the value stored in this mutator with another value value of the same type
        * @return this mutator
        */
        Builder<H> xor(H other);

        /** @return the hash value */
        public H build();
    }
}