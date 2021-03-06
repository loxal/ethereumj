/*
 * The MIT License (MIT)
 *
 * Copyright 2017 Alexander Orlov <alexander.orlov@loxal.net>. All rights reserved.
 * Copyright (c) [2016] [ <ether.camp> ]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package org.ethereum.net.swarm;

/**
 * Manages the local ChunkStore
 *
 * Uses {@link DBStore} for slow access long living data
 * and {@link MemStore} for fast access short living
 *
 * Created by Anton Nashatyrev on 18.06.2015.
 */
public class LocalStore implements ChunkStore {

    final ChunkStore memStore;
    private final ChunkStore dbStore;

    public LocalStore(final ChunkStore dbStore, final ChunkStore memStore) {
        this.dbStore = dbStore;
        this.memStore = memStore;
    }

    @Override
    public void put(final Chunk chunk) {
        memStore.put(chunk);
        // TODO make sure this is non-blocking call
        dbStore.put(chunk);
    }

    @Override
    public Chunk get(final Key key) {
        Chunk chunk = memStore.get(key);
        if (chunk == null) {
            chunk = dbStore.get(key);
        }
        return chunk;
    }

    // for testing
    public void clean() {
        for (final ChunkStore chunkStore : new ChunkStore[]{dbStore, memStore}) {
            if (chunkStore instanceof MemStore) {
                ((MemStore)chunkStore).clear();
            }
        }
    }
}
