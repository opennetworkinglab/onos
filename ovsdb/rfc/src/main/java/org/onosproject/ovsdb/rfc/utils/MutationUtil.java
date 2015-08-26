package org.onosproject.ovsdb.rfc.utils;

import org.onosproject.ovsdb.rfc.notation.Mutation;
import org.onosproject.ovsdb.rfc.notation.Mutation.Mutator;

public final class MutationUtil {

    /**
     * Constructs a MutationUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private MutationUtil() {
    }

    /**
     * Returns a Mutation that means += .
     * @param columnName column name
     * @param data column value
     * @return Mutation
     */
    public static Mutation sum(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Mutation(columnName, Mutator.SUM, value);
    }

    /**
     * Returns a Mutation that means -= .
     * @param columnName column name
     * @param data column value
     * @return Mutation
     */
    public static Mutation difference(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Mutation(columnName, Mutator.DIFFERENCE, value);
    }

    /**
     * Returns a Mutation that means *= .
     * @param columnName column name
     * @param data column value
     * @return Mutation
     */
    public static Mutation product(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Mutation(columnName, Mutator.PRODUCT, value);
    }

    /**
     * Returns a Mutation that means /= .
     * @param columnName column name
     * @param data column value
     * @return Mutation
     */
    public static Mutation quotient(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Mutation(columnName, Mutator.QUOTIENT, value);
    }

    /**
     * Returns a Mutation that means %= .
     * @param columnName column name
     * @param data column value
     * @return Mutation
     */
    public static Mutation remainder(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Mutation(columnName, Mutator.REMAINDER, value);
    }

    /**
     * Returns a Mutation that means insert .
     * @param columnName column name
     * @param data column value
     * @return Mutation
     */
    public static Mutation insert(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Mutation(columnName, Mutator.INSERT, value);
    }

    /**
     * Returns a Mutation that means delete .
     * @param columnName column name
     * @param data column value
     * @return Mutation
     */
    public static Mutation delete(String columnName, Object data) {
        Object value = TransValueUtil.getFormatData(data);
        return new Mutation(columnName, Mutator.DELETE, value);
    }
}
