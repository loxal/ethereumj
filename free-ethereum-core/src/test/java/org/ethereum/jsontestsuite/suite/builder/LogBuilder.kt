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

package org.ethereum.jsontestsuite.suite.builder

import org.ethereum.jsontestsuite.suite.Utils.parseData
import org.ethereum.jsontestsuite.suite.model.LogTck
import org.ethereum.vm.DataWord
import org.ethereum.vm.LogInfo

object LogBuilder {

    private fun build(logTck: LogTck): LogInfo {

        val address = parseData(logTck.address)
        val data = parseData(logTck.data)

        val topics = logTck.topics!!.map { DataWord(parseData(it)) }

        return LogInfo(address, topics, data)
    }

    fun build(logs: List<LogTck>): List<LogInfo> {

        val outLogs = logs.map { build(it) }

        return outLogs
    }
}
