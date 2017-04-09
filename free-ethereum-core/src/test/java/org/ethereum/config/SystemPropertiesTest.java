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

package org.ethereum.config;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Anton Nashatyrev on 26.08.2015.
 */
public class SystemPropertiesTest {
    @Test
    public void punchBindIpTest() {
        SystemProperties.getDefault().overrideParams("peer.bind.ip", "");
        final long st = System.currentTimeMillis();
        final String ip = SystemProperties.getDefault().bindIp();
        final long t = System.currentTimeMillis() - st;
        System.out.println(ip + " in " + t + " msec");
        Assert.assertTrue(t < 10 * 1000);
        Assert.assertFalse(ip.isEmpty());
    }

    @Test
    public void externalIpTest() {
        SystemProperties.getDefault().overrideParams("peer.discovery.external.ip", "");
        final long st = System.currentTimeMillis();
        final String ip = SystemProperties.getDefault().externalIp();
        final long t = System.currentTimeMillis() - st;
        System.out.println(ip + " in " + t + " msec");
        Assert.assertTrue(t < 10 * 1000);
        Assert.assertFalse(ip.isEmpty());
    }

    @Test
    public void blockchainNetConfigTest() {
        final SystemProperties systemProperties1 = new SystemProperties();
        systemProperties1.overrideParams("blockchain.config.name", "olympic");
        final BlockchainNetConfig blockchainConfig1 = systemProperties1.getBlockchainConfig();
        final SystemProperties systemProperties2 = new SystemProperties();
        systemProperties2.overrideParams("blockchain.config.name", "morden");
        final BlockchainNetConfig blockchainConfig2 = systemProperties2.getBlockchainConfig();
        Assert.assertNotEquals(blockchainConfig1.getClass(), blockchainConfig2.getClass());
    }
}