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

    /** @return the bitwise inverse of this value */
    H inverse();

    /** or this value with another value value of the same type */
    H or(H other);

    /** and this value with another value value of the same type */
    H and(H other);

    /** xor this value with another value value of the same type */
    H xor(H other);

    /** calculate a combined hash value of this hash value (the <b>Key</b>) and the hash value
     *  specified as a parameter (the <b>Value</b>).
     *  <p>
     *  The value is constructed as follows:
     *  <ul>
     *   <li>the first keyBits bits are taken only from the Key
     *   <li>the other bits are taken from key xor value.
     *  </ul>
     *  The overall result looks like this:
     *  <pre>
     *  MSB                      LSB
     *   +---------+--------------+
     *   | key     | key ^ value  |
     *   +---------+--------------+
     *   |-keyBits-|
     *  </pre>
     *
     * @param value - hash value to be compared with this value (the key)
     * @param keyBits number of prefix bits that are just taken from key
     * @return the combined value.
     */
    H combineWithValue(H value, int keyBits);
}