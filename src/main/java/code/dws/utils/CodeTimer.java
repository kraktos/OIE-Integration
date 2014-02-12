
package code.dws.utils;

public class CodeTimer {
    static long start;

    public static void start() {
        start = System.nanoTime();
    }

    public static void end(String string) {
        System.out.println(string + "() took " + (System.nanoTime() - start));
        // could easily change it to print out time in seconds, with some
        // String before it, to return a value, etc.
    }

    public static void end() {
        end("");
    }
}
