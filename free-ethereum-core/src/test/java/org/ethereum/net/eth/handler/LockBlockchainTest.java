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

package org.ethereum.net.eth.handler;

import org.ethereum.config.NoAutoscan;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.BlockHeader;
import org.ethereum.core.Blockchain;
import org.ethereum.core.BlockchainImpl;
import org.ethereum.db.BlockStore;
import org.ethereum.db.BlockStoreDummy;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.net.eth.message.EthMessage;
import org.ethereum.net.eth.message.GetBlockBodiesMessage;
import org.ethereum.net.eth.message.GetBlockHeadersMessage;
import org.ethereum.sync.SyncManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Testing whether Eth handler {@link Eth62} is blocking {@link BlockchainImpl}
 */
public class LockBlockchainTest {

    private final static long DELAY = 1000;  //Default delay in ms
    private final static String BLOCK_RLP = "f901f8f901f3a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a09178d0f23c965d81f0834a4c72c6253ce6830f4022b1359aaebfc1ecba442d4ea056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008302000080832fefd8808080a0000000000000000000000000000000000000000000000000000000000000000088000000000000002ac0c0";
    private final AtomicBoolean result = new AtomicBoolean();
    private final Blockchain blockchain;

    public LockBlockchainTest() {

        SysPropConfig1.props.overrideParams(
                "peer.discovery.enabled", "false",
                "peer.listen.port", "37777",
                "peer.privateKey", "3ec771c31cac8c0dba77a69e503765701d3c2bb62435888d4ffa38fed60c445c",
                "genesis", "genesis-light.json",
                "database.dir", "testDB-1");

        final BlockStore blockStoreDummy = new BlockStoreDummy() {
            @Override
            public synchronized Block getChainBlockByNumber(final long blockNumber) {
                return super.getChainBlockByNumber(blockNumber);
            }

            @Override
            public synchronized List<BlockHeader> getListHeadersEndWith(final byte[] hash, final long qty) {
                return super.getListHeadersEndWith(hash, qty);
            }

            @Override
            public synchronized Block getBestBlock() {
                return new Block(Hex.decode(BLOCK_RLP));
            }
        };

        this.blockchain = new BlockchainImpl(SysPropConfig1.props) {
            @Override
            public synchronized boolean isBlockExist(final byte[] hash) {
                try {
                    this.blockStore = blockStoreDummy;
                    Thread.sleep(100000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                return true;
            }
        };

        SysPropConfig1.testHandler = new Eth62(SysPropConfig1.props, blockchain, blockStoreDummy,
                new CompositeEthereumListener()) {
            {
                this.blockstore = blockStoreDummy;
                this.syncManager = Mockito.mock(SyncManager.class);
            }
            @Override
            public synchronized void sendStatus() {
                super.sendStatus();
            }

            @Override
            protected void sendMessage(final EthMessage message) {
                result.set(true);
                System.out.println("Mocking message sending...");
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        result.set(false);
    }

    @Test
    public synchronized void testHeadersWithoutSkip() throws FileNotFoundException, InterruptedException {
        final ExecutorService executor1 = Executors.newSingleThreadExecutor();
        executor1.submit((Runnable) () -> blockchain.isBlockExist(null)
        );
        this.wait(DELAY);
        final ExecutorService executor2 = Executors.newSingleThreadExecutor();
        executor2.submit(() -> {
                    final GetBlockHeadersMessage msg = new GetBlockHeadersMessage(1L, new byte[0], 10, 0, false);
                    SysPropConfig1.testHandler.processGetBlockHeaders(msg);
                }
        );
        this.wait(DELAY);
        assert result.get();
    }

    @Test
    public synchronized void testHeadersWithSkip() throws FileNotFoundException, InterruptedException {
        final ExecutorService executor1 = Executors.newSingleThreadExecutor();
        executor1.submit((Runnable) () -> blockchain.isBlockExist(null)
        );
        this.wait(DELAY);
        final ExecutorService executor2 = Executors.newSingleThreadExecutor();
        executor2.submit(() -> {
                    final GetBlockHeadersMessage msg = new GetBlockHeadersMessage(1L, new byte[0], 10, 5, false);
                    SysPropConfig1.testHandler.processGetBlockHeaders(msg);
                }
        );
        this.wait(DELAY);
        assert result.get();
    }

    @Test
    public synchronized void testBodies() throws FileNotFoundException, InterruptedException {
        final ExecutorService executor1 = Executors.newSingleThreadExecutor();
        executor1.submit((Runnable) () -> blockchain.isBlockExist(null)
        );
        this.wait(DELAY);
        final ExecutorService executor2 = Executors.newSingleThreadExecutor();
        executor2.submit(() -> {
                    final List<byte[]> hashes = new ArrayList<>();
                    hashes.add(new byte[]{1, 2, 3});
                    hashes.add(new byte[]{4, 5, 6});
                    final GetBlockBodiesMessage msg = new GetBlockBodiesMessage(hashes);
                    SysPropConfig1.testHandler.processGetBlockBodies(msg);
                }
        );
        this.wait(DELAY);
        assert result.get();
    }

    @Test
    public synchronized void testStatus() throws FileNotFoundException, InterruptedException {
        final ExecutorService executor1 = Executors.newSingleThreadExecutor();
        executor1.submit((Runnable) () -> blockchain.isBlockExist(null)
        );
        this.wait(DELAY);
        final ExecutorService executor2 = Executors.newSingleThreadExecutor();
        executor2.submit(() -> {
                    try {
                        SysPropConfig1.testHandler.sendStatus();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
        );
        this.wait(DELAY);
        assert result.get();
    }

    @Configuration
    @NoAutoscan
    public static class SysPropConfig1 {
        static final SystemProperties props = new SystemProperties();
        static Eth62 testHandler = null;

        @Bean
        @Scope("prototype")
        public Eth62 eth62() {
            return testHandler;
        }

        @Bean
        public SystemProperties systemProperties() {
            return props;
        }
    }
}