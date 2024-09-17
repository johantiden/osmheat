package com.github.johantiden.osmheat.cache;

public interface CheckedFunction<V, T> {
    T apply(V v) throws Exception;
}
