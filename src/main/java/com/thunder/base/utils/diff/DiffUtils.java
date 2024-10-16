package com.thunder.base.utils.diff;

import java.lang.reflect.Array;

public class DiffUtils {

    static Object[] toObjectArray(Object array) {
        int length = Array.getLength(array);
        Object[] ret = new Object[length];
        for(int i = 0; i < length; i++)
            ret[i] = Array.get(array, i);
        return ret;
    }

}
