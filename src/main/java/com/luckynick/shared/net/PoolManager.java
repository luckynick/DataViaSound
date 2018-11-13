package com.luckynick.shared.net;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public abstract class PoolManager<T> {

    private List<T> pool = new ArrayList<>();

    public void add(T conn) {
        pool.add(conn);
    }

    public T getConnection(int index) {
        return pool.get(index);
    }

    public Iterable<T> getConnectionIterator() {
        return pool;
    }

    public T remove(T obj) {
        return pool.remove(pool.indexOf(obj));
    }
}
