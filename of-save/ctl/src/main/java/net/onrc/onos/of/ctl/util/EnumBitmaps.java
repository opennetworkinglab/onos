package net.onrc.onos.of.ctl.util;

import java.util.EnumSet;
import java.util.Set;

/**
 * A utility class to convert between integer based bitmaps for (OpenFlow)
 * flags and Enum and EnumSet based representations.
 *
 * The enum used to represent individual flags needs to implement the
 * BitmapableEnum interface.
 *
 * Example:
 * {@code
 *   int bitmap = 0x11; // OFPPC_PORT_DOWN | OFPPC_NO_STP
 *   EnumSet<OFPortConfig> s = toEnumSet(OFPortConfig.class, bitmap);
 *   // s will contain OFPPC_PORT_DOWN and OFPPC_NO_STP
 * }
 *
 * {@code
 *    EnumSet<OFPortConfig> s = EnumSet.of(OFPPC_NO_STP, OFPPC_PORT_DOWN);
 *    int bitmap = toBitmap(s); // returns 0x11
 * }
 *
 */
public final class EnumBitmaps {


    private EnumBitmaps() { }

    /**
     * Enums used to represent individual flags needs to implement this
     * interface.
     */
    public interface BitmapableEnum {
        /** Return the value in the bitmap that the enum constant represents.
         * The returned value must have only a single bit set. E.g.,1 << 3
         */
        int getValue();
    }


    /**
     * Convert an integer bitmap to an EnumSet.
     *
     * See class description for example
     * @param type The Enum class to use. Must implement BitmapableEnum
     * @param bitmap The integer bitmap
     * @return A newly allocated EnumSet representing the bits set in the
     * bitmap
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if any enum constant from type has
     * more than one bit set.
     * @throws IllegalArgumentException if the bitmap has any bits set not
     * represented by an enum constant.
     */
    public static <E extends Enum<E> & BitmapableEnum>
            EnumSet<E> toEnumSet(Class<E> type, int bitmap) {
        if (type == null) {
            throw new NullPointerException("Given enum type must not be null");
        }
        EnumSet<E> s = EnumSet.noneOf(type);
        // allSetBitmap will eventually have all valid bits for the given
        // type set.
        int allSetBitmap = 0;
        for (E element: type.getEnumConstants()) {
            if (Integer.bitCount(element.getValue()) != 1) {
                String msg = String.format("The %s (%x) constant of the " +
                        "enum %s is supposed to represent a bitmap entry but " +
                        "has more than one bit set.",
                        element.toString(), element.getValue(), type.getName());
                throw new IllegalArgumentException(msg);
            }
            allSetBitmap |= element.getValue();
            if ((bitmap & element.getValue()) != 0) {
                s.add(element);
            }
        }
        if (((~allSetBitmap) & bitmap) != 0) {
            // check if only valid flags are set in the given bitmap
            String msg = String.format("The bitmap %x for enum %s has " +
                    "bits set that are presented by any enum constant",
                    bitmap, type.getName());
            throw new IllegalArgumentException(msg);
        }
        return s;
    }

    /**
     * Return the bitmap mask with all possible bits set. E.g., If a bitmap
     * has the individual flags 0x1, 0x2, and 0x8 (note the missing 0x4) then
     * the mask will be 0xb (1011 binary)
     *
     * @param type The Enum class to use. Must implement BitmapableEnum
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if any enum constant from type has
     * more than one bit set
     * @return an integer with all possible bits for the given bitmap enum
     * type set.
     */
    public static <E extends Enum<E> & BitmapableEnum>
            int getMask(Class<E> type) {
        if (type == null) {
            throw new NullPointerException("Given enum type must not be null");
        }
        // allSetBitmap will eventually have all valid bits for the given
        // type set.
        int allSetBitmap = 0;
        for (E element: type.getEnumConstants()) {
            if (Integer.bitCount(element.getValue()) != 1) {
                String msg = String.format("The %s (%x) constant of the " +
                        "enum %s is supposed to represent a bitmap entry but " +
                        "has more than one bit set.",
                        element.toString(), element.getValue(), type.getName());
                throw new IllegalArgumentException(msg);
            }
            allSetBitmap |= element.getValue();
        }
        return allSetBitmap;
    }

    /**
     * Convert the given EnumSet to the integer bitmap representation.
     * @param set The EnumSet to convert. The enum must implement
     * BitmapableEnum
     * @return the integer bitmap
     * @throws IllegalArgumentException if an enum constant from the set (!) has
     * more than one bit set
     * @throws NullPointerException if the set is null
     */
    public static <E extends Enum<E> & BitmapableEnum>
            int toBitmap(Set<E> set) {
        if (set == null) {
            throw new NullPointerException("Given set must not be null");
        }
        int bitmap = 0;
        for (E element: set) {
            if (Integer.bitCount(element.getValue()) != 1) {
                String msg = String.format("The %s (%x) constant in the set " +
                        "is supposed to represent a bitmap entry but " +
                        "has more than one bit set.",
                        element.toString(), element.getValue());
                throw new IllegalArgumentException(msg);
            }
            bitmap |= element.getValue();
        }
        return bitmap;
    }
}
