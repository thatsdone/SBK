/**
 * Copyright (c) KMG. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package io.sbk.FoundationDB;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.tuple.Tuple;
import io.sbk.api.Parameters;
import io.sbk.api.Writer;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Class for Writer.
 */
public class FoundationDBWriter implements Writer<byte[]> {
    final private Database db;
    private long key;

    public FoundationDBWriter(int id, Parameters params, Database db) throws IOException {
        this.key = id * Integer.MAX_VALUE;
        this.db = db;
        this.db.run(tr -> {
            tr.clear(Tuple.from(key + 1).pack(), Tuple.from(key + 1 + Integer.MAX_VALUE).pack());
            return null;
        });
    }

    @Override
    public CompletableFuture writeAsync(byte[] data) throws IOException {
        key++;
        return db.run(tr -> {
            tr.set(Tuple.from(key).pack(), data);
            return null;
        });
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws  IOException {
    }
}
