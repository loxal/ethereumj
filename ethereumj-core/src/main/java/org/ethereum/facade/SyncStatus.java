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

package org.ethereum.facade;

/**
 * Represents the current state of syncing process
 */
public class SyncStatus {
    private final SyncStage stage;
    private final long curCnt;
    private final long knownCnt;
    private final long blockLastImported;
    private final long blockBestKnown;

    public SyncStatus(final SyncStatus state, final long blockLastImported, final long blockBestKnown) {
        this(state.getStage(), state.getCurCnt(), state.getKnownCnt(), blockLastImported, blockBestKnown);
    }

    public SyncStatus(final SyncStage stage, final long curCnt, final long knownCnt, final long blockLastImported, final long blockBestKnown) {
        this.stage = stage;
        this.curCnt = curCnt;
        this.knownCnt = knownCnt;
        this.blockLastImported = blockLastImported;
        this.blockBestKnown = blockBestKnown;
    }

    public SyncStatus(final SyncStage stage, final long curCnt, final long knownCnt) {
        this(stage, curCnt, knownCnt, 0, 0);
    }

    /**
     * Gets the current stage of syncing
     */
    public SyncStage getStage() {
        return stage;
    }

    /**
     * Gets the current count of items processed for this syncing stage :
     * PivotBlock: number of seconds pivot block is searching for
     *          ( this number can be greater than getKnownCnt() if no peers found)
     * StateNodes: number of trie nodes downloaded
     * Headers: number of headers downloaded
     * BlockBodies: number of block bodies downloaded
     * Receipts: number of blocks receipts are downloaded for
     */
    private long getCurCnt() {
        return curCnt;
    }

    /**
     * Gets the known count of items for this syncing stage :
     * PivotBlock: number of seconds pivot is forced to be selected
     * StateNodes: number of currently known trie nodes. This number is not constant as new nodes
     *             are discovered when their parent is downloaded
     * Headers: number of headers to be downloaded
     * BlockBodies: number of block bodies to be downloaded
     * Receipts: number of blocks receipts are to be downloaded for
     */
    private long getKnownCnt() {
        return knownCnt;
    }

    /**
     * Reflects the blockchain state: the latest imported block
     * Blocks importing can run in parallel with other sync stages
     * (like header/blocks/receipts downloading)
     */
    private long getBlockLastImported() {
        return blockLastImported;
    }

    /**
     * Return the best known block from other peers
     */
    private long getBlockBestKnown() {
        return blockBestKnown;
    }

    @Override
    public String toString() {
        return stage +
                (stage != SyncStage.Off && stage != SyncStage.Complete ? " (" + getCurCnt() + " of " + getKnownCnt() + ")" : "") +
                ", last block #" + getBlockLastImported() + ", best known #" + getBlockBestKnown();
    }

    public enum SyncStage {
        /**
         * Fast sync: looking for a Pivot block.
         * Normally we need several peers to select the block but
         * the block can be selected from existing peers due to timeout
         */
        PivotBlock,
        /**
         * Fast sync: downloading state trie nodes and importing blocks
         */
        StateNodes,
        /**
         * Fast sync: downloading headers for securing the latest state
         */
        Headers,
        /**
         * Fast sync: downloading blocks
         */
        BlockBodies,
        /**
         * Fast sync: downloading receipts
         */
        Receipts,
        /**
         * Regular sync is in progress
         */
        Regular,
        /**
         * Sync is complete:
         * Fast sync: the state is secure, all blocks and receipt are downloaded
         * Regular sync: all blocks are imported up to the blockchain head
         */
        Complete,
        /**
         * Syncing is turned off
         */
        Off;

        /**
         * Indicates if this state represents ongoing FastSync
         */
        public boolean isFastSync() {
            return this == PivotBlock || this == StateNodes || this == Headers || this == BlockBodies || this == Receipts;
        }

        /**
         * Indicates the current state is secure
         * <p>
         * When doing fast sync UNSECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * but the state isn't yet confirmed with  the whole block chain and can't be
         * trusted.
         * At this stage historical blocks and receipts are unavailable yet
         * <p>
         * SECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * The state is now confirmed by the full chain (all block headers are
         * downloaded and verified) and can be trusted
         * At this stage historical blocks and receipts are unavailable yet
         */
        public boolean isSecure() {
            return this != PivotBlock || this != StateNodes && this != Headers;
        }

        /**
         * Indicates the blockchain state is up-to-date
         * Warning: the state could still be non-secure
         */
        public boolean hasLatestState() {
            return this == Headers || this == BlockBodies || this == Receipts || this == Complete;
        }
    }
}
