package com.griddynamics;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Work
 * Date: 03.11.13
 * Time: 13:13
 * To change this template use File | Settings | File Templates.
 */
public class PrimitiveContainer {
        private static Set<Class> primitives = new HashSet<Class>();

        static {
            primitives.add(int.class);
            primitives.add(boolean.class);
            primitives.add(long.class);
            primitives.add(float.class);
            primitives.add(double.class);
            primitives.add(byte[].class);
            primitives.add(int[].class);
            primitives.add(Double.class);
            primitives.add(Float.class);
            primitives.add(Integer.class);
            primitives.add(Number.class);
            primitives.add(String.class);
        }

        private PrimitiveContainer(){

        }

        public static boolean contains(Class clazz) {
            return primitives.contains(clazz);
        }
}
