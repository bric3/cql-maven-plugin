package com.libon.maven.cql;

import java.util.function.Function;

public class Sneaky {
    @FunctionalInterface
    public interface SneakyThrowFunction<T, R> {
        R apply(T t) throws Exception;
    }
 
    public static <T, R> Function<T, R> function(SneakyThrowFunction<T, R> function) {
        return o -> {
            try {
                return function.apply(o);
            } catch (Exception e) {
                throw Sneaky.<RuntimeException>sneakyException(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyException(Throwable t) throws T {
        throw (T) t;
    }
}
