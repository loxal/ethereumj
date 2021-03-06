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

import org.ethereum.datasource.Source;
import org.ethereum.db.BlockStore;
import org.ethereum.db.IndexedBlockStore;
import org.ethereum.db.PruneManager;
import org.ethereum.db.TransactionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 * @author Roman Mandeleil
 * Created on: 27/01/2015 01:05
 */
@Configuration
@Import(CommonConfig.class)
public class DefaultConfig {
    private static final Logger logger = LoggerFactory.getLogger("general");

    final
    ApplicationContext appCtx;

    private final
    CommonConfig commonConfig;

    private final
    SystemProperties config;

    @Autowired
    public DefaultConfig(ApplicationContext appCtx, CommonConfig commonConfig, SystemProperties config) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception", e));
        this.appCtx = appCtx;
        this.commonConfig = commonConfig;
        this.config = config;
    }

    @Bean
    public BlockStore blockStore(){
        commonConfig.fastSyncCleanUp();
        final IndexedBlockStore indexedBlockStore = new IndexedBlockStore();
        final Source<byte[], byte[]> block = commonConfig.cachedDbSource("block");
        final Source<byte[], byte[]> index = commonConfig.cachedDbSource("index");
        indexedBlockStore.init(index, block);

        return indexedBlockStore;
    }

    @Bean
    public TransactionStore transactionStore() {
        commonConfig.fastSyncCleanUp();
        return new TransactionStore(commonConfig.cachedDbSource("transactions"));
    }

    @Bean
    public PruneManager pruneManager() {
        if (config.databasePruneDepth() >= 0) {
            return new PruneManager((IndexedBlockStore) blockStore(), commonConfig.stateSource().getJournalSource(),
                    config.databasePruneDepth());
        } else {
            return new PruneManager(null, null, -1); // dummy
        }
    }
}
