package org.projectfloodlight.openflow.protocol.match;

import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFValueType;

/**
 * Generic interface for version-agnostic immutable Match structure.
 * The Match structure is defined in the OpenFlow protocol, and it contains information on
 * the fields to be matched in a specific flow record.
 * This interface does not assume anything on the fields in the Match structure. If in
 * some version, the match structure cannot handle a certain field, it may return <code>false</code>
 * for <code>supports(...)</code> calls, and throw <code>UnsupportedOperationException</code> from all
 * other methods in such cases.
 * <br><br>
 * On wildcards and masks:<br>
 * This interface defines the following masking notations for fields:
 * <ul>
 * <li><b>Exact</b>: field is matched exactly against a single, fixed value (no mask, or mask is all ones).
 * <li><b>Wildcarded</b>: field is not being matched. It is fully masked (mask=0) and any value of it
 * will match the flow record having this match.
 * <li><b>Partially masked</b>: field is matched using a specified mask which is neither 0 nor all ones. Mask can
 * be either arbitrary or require some specific structure.
 * </ul>
 * Implementing classes may or may not support all types of these masking types. They may also support
 * them in part. For example, OF1.0 supports exact match and (full) wildcarding for all fields, but it
 * does only supports partial masking for IP source/destination fields, and this partial masking must be
 * in the CIDR prefix format. Thus, OF1.0 implementation may throw <code>UnsupportedOperationException</code> if given
 * in <code>setMasked</code> an IP mask of, for example, 255.0.255.0, or if <code>setMasked</code> is called for any field
 * which is not IP source/destination address.
 * <br><br>
 * On prerequisites:<br>
 * From the OF1.1 spec, page 28, the OF1.0 spec failed to explicitly specify this, but it
 * is the behavior of OF1.0 switches:
 * "Protocol-specific fields within ofp_match will be ignored within a single table when
 * the corresponding protocol is not specified in the match. The MPLS match fields will
 * be ignored unless the Ethertype is specified as MPLS. Likewise, the IP header and
 * transport header fields will be ignored unless the Ethertype is specified as either
 * IPv4 or ARP. The tp_src and tp_dst fields will be ignored unless the network protocol
 * specified is as TCP, UDP or SCTP. Fields that are ignored don't need to be wildcarded
 * and should be set to 0."
 * <br><br>
 * This interface uses generics to assure type safety in users code. However, implementing classes may have to suppress
 * 'unchecked cast' warnings while making sure they correctly cast base on their implementation details.
 *
 * @author Yotam Harchol (yotam.harchol@bigswitch.com)
 */
public interface Match extends OFObject {

    /**
     * Returns a value for the given field if:
     * <ul>
     * <li>Field is supported
     * <li>Field is not fully wildcarded
     * <li>Prerequisites are ok
     * </ul>
     * If one of the above conditions does not hold, returns null. Value is returned masked if partially wildcarded.
     *
     * @param field Match field to retrieve
     * @return Value of match field (may be masked), or <code>null</code> if field is one of the conditions above does not hold.
     * @throws UnsupportedOperationException If field is not supported.
     */
    public <F extends OFValueType<F>> F get(MatchField<F> field) throws UnsupportedOperationException;

    /**
     * Returns the masked value for the given field from this match, along with the mask itself.
     * Prerequisite: field is partially masked.
     * If prerequisite is not met, a <code>null</code> is returned.
     *
     * @param field Match field to retrieve.
     * @return Masked value of match field or null if no mask is set.
     * @throws UnsupportedOperationException If field is not supported.
     */
    public <F extends OFValueType<F>> Masked<F> getMasked(MatchField<F> field) throws UnsupportedOperationException;

    /**
     * Returns true if and only if this match object supports the given match field.
     *
     * @param field Match field
     * @return true if field is supported, false otherwise.
     */
    public boolean supports(MatchField<?> field);

    /**
     * Returns true if and only if this match object supports partially bitmasking of the given field.
     * (note: not all possible values of this bitmask have to be acceptable)
     *
     * @param field Match field.
     * @return true if field can be partially masked, false otherwise.
     * @throws UnsupportedOperationException If field is not supported.
     */
    public boolean supportsMasked(MatchField<?> field) throws UnsupportedOperationException;

    /**
     * Returns true if and only if this field is currently specified in the match with an exact value and
     * no mask. I.e., the specified match will only select packets that match the exact value of getValue(field).
     *
     * @param field Match field.
     * @return true if field has a specific exact value, false if not.
     * @throws UnsupportedOperationException If field is not supported.
     */
    public boolean isExact(MatchField<?> field) throws UnsupportedOperationException;

    /**
     * True if and only if this field is currently logically unspecified in the match, i.e, the
     * value returned by getValue(f) has no impact on whether a packet will be selected
     * by the match or not.
     *
     * @param field Match field.
     * @return true if field is fully wildcarded, false if not.
     * @throws UnsupportedOperationException If field is not supported.
     */
    public boolean isFullyWildcarded(MatchField<?> field) throws UnsupportedOperationException;

    /**
     * True if and only if this field is currently partially specified in the match, i.e, the
     * match will only select packets that match (p.value & getMask(field)) == getValue(field),
     * and getMask(field) != 0.
     *
     * @param field Match field.
     * @return true if field is partially masked, false if not.
     * @throws UnsupportedOperationException If field is not supported.
     */
    public boolean isPartiallyMasked(MatchField<?> field) throws UnsupportedOperationException;

    /**
     * Get an Iterable over the match fields that have been specified for the
     * match. This includes the match fields that are exact or masked match
     * (but not fully wildcarded).
     *
     * @return
     */
    public Iterable<MatchField<?>> getMatchFields();

    /**
     * Returns a builder to build new instances of this type of match object.
     * @return Match builder
     */
    public Builder createBuilder();

    /**
     * Builder interface for Match objects.
     * Builder is used to create new Match objects and it creates the match according to the version it
     * corresponds to. The builder uses the same notation of wildcards and masks, and can also throw
     * <code>UnsupportedOperationException</code> if it is asked to create some matching that is not supported in
     * the version it represents.
     *
     * While used, MatchBuilder may not be consistent in terms of field prerequisites. However, user must
     * solve these before using the generated Match object as these prerequisites should be enforced in the
     * getters.
     *
     * @author Yotam Harchol (yotam.harchol@bigswitch.com)
     */
    interface Builder {
        public <F extends OFValueType<F>> F get(MatchField<F> field) throws UnsupportedOperationException;

        public <F extends OFValueType<F>> Masked<F> getMasked(MatchField<F> field) throws UnsupportedOperationException;

        public boolean supports(MatchField<?> field);

        public boolean supportsMasked(MatchField<?> field) throws UnsupportedOperationException;

        public boolean isExact(MatchField<?> field) throws UnsupportedOperationException;

        public boolean isFullyWildcarded(MatchField<?> field) throws UnsupportedOperationException;

        public boolean isPartiallyMasked(MatchField<?> field) throws UnsupportedOperationException;

        /**
         * Sets a specific exact value for a field.
         *
         * @param field Match field to set.
         * @param value Value of match field.
         * @return the Builder instance used.
         * @throws UnsupportedOperationException If field is not supported.
         */
        public <F extends OFValueType<F>> Builder setExact(MatchField<F> field, F value) throws UnsupportedOperationException;

        /**
         * Sets a masked value for a field.
         *
         * @param field Match field to set.
         * @param value Value of field.
         * @param mask Mask value.
         * @return the Builder instance used.
         * @throws UnsupportedOperationException If field is not supported, if field is supported but does not support masking, or if mask structure is not supported.
         */
        public <F extends OFValueType<F>> Builder setMasked(MatchField<F> field, F value, F mask) throws UnsupportedOperationException;

        /**
         * Sets a masked value for a field.
         *
         * @param field Match field to set.
         * @param valueWithMask Compound Masked object contains the value and the mask.
         * @return the Builder instance used.
         * @throws UnsupportedOperationException If field is not supported, if field is supported but does not support masking, or if mask structure is not supported.
         */
        public <F extends OFValueType<F>> Builder setMasked(MatchField<F> field, Masked<F> valueWithMask) throws UnsupportedOperationException;

        /**
         * Unsets any value given for the field and wildcards it so that it matches any value.
         *
         * @param field Match field to unset.
         * @return the Builder instance used.
         * @throws UnsupportedOperationException If field is not supported.
         */
        public <F extends OFValueType<F>> Builder wildcard(MatchField<F> field) throws UnsupportedOperationException;

        /**
         * Returns the match created by this builder.
         *
         * @return a Match object.
         */
        public Match build();
    }
}