package xyz.eulix.space.util;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/3/23 09:29
 */
public class IntegerUtil {
    private IntegerUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean compare(Integer i1, Integer i2) {
        boolean isEqual = false;
        if (i1 == null && i2 == null) {
            isEqual = true;
        } else if (i1 != null && i2 != null) {
            isEqual = i1.equals(i2);
        }
        return isEqual;
    }
}
