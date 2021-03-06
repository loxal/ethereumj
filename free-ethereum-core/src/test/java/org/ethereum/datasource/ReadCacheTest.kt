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

package org.ethereum.datasource

import org.ethereum.crypto.HashUtil
import org.ethereum.datasource.inmem.HashMapDB
import org.ethereum.util.ByteUtil.longToBytes
import org.ethereum.vm.DataWord
import org.junit.Assert.*
import org.junit.Test
import org.spongycastle.util.encoders.Hex

/**
 * Testing [ReadCache]
 */
class ReadCacheTest {

    private fun intToKey(i: Int): ByteArray {
        return HashUtil.sha3(longToBytes(i.toLong()))
    }

    private fun intToValue(i: Int): ByteArray {
        return DataWord(i).data
    }

    private fun str(obj: Any?): String? {
        if (obj == null) return null
        return Hex.toHexString((obj as ByteArray?)!!)
    }

    @Test
    fun test1() {
        val src = HashMapDB<ByteArray>()
        val readCache = ReadCache.BytesKey(src)
        for (i in 0..9999) {
            src.put(intToKey(i), intToValue(i))
        }
        // Nothing is cached
        assertNull(readCache.getCached(intToKey(0)))
        assertNull(readCache.getCached(intToKey(9999)))

        for (i in 0..9999) {
            readCache[intToKey(i)]
        }
        // Everything is cached
        assertEquals(str(intToValue(0)), str(readCache.getCached(intToKey(0)).value()))
        assertEquals(str(intToValue(9999)), str(readCache.getCached(intToKey(9999)).value()))

        // Source changes doesn't affect cache
        src.delete(intToKey(13))
        assertEquals(str(intToValue(13)), str(readCache.getCached(intToKey(13)).value()))

        // Flush is not implemented
        assertFalse(readCache.flush())
    }

    @Test
    fun testMaxCapacity() {
        val src = HashMapDB<ByteArray>()
        val readCache = ReadCache.BytesKey(src).withMaxCapacity(100)
        for (i in 0..9999) {
            src.put(intToKey(i), intToValue(i))
            readCache[intToKey(i)]
        }

        // Only 100 latest are cached
        assertNull(readCache.getCached(intToKey(0)))
        assertEquals(str(intToValue(0)), str(readCache[intToKey(0)]))
        assertEquals(str(intToValue(0)), str(readCache.getCached(intToKey(0)).value()))
        assertEquals(str(intToValue(9999)), str(readCache.getCached(intToKey(9999)).value()))
        // 99_01 - 99_99 and 0 (totally 100)
        assertEquals(str(intToValue(9901)), str(readCache.getCached(intToKey(9901)).value()))
        assertNull(readCache.getCached(intToKey(9900)))
    }
}
