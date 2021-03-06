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

package org.ethereum.net.eth.message;

import org.ethereum.core.BlockIdentifier;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;

import java.math.BigInteger;

import static org.ethereum.util.ByteUtil.byteArrayToInt;
import static org.ethereum.util.ByteUtil.byteArrayToLong;

/**
 * Wrapper around an Ethereum GetBlockHeaders message on the network
 *
 * @see EthMessageCodes#GET_BLOCK_HEADERS
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class GetBlockHeadersMessage extends EthMessage {

    private static final int DEFAULT_SIZE_BYTES = 32;
    /**
     * Block number from which to start sending block headers
     */
    private long blockNumber;

    /**
     * Block hash from which to start sending block headers <br>
     * Initial block can be addressed by either {@code blockNumber} or {@code blockHash}
     */
    private byte[] blockHash;

    /**
     * The maximum number of headers to be returned. <br>
     * <b>Note:</b> the peer could return fewer.
     */
    private int maxHeaders;

    /**
     * Blocks to skip between consecutive headers. <br>
     * Direction depends on {@code reverse} param.
     */
    private int skipBlocks;

    /**
     * The direction of headers enumeration. <br>
     * <b>false</b> is for rising block numbers. <br>
     * <b>true</b> is for falling block numbers.
     */
    private boolean reverse;

    public GetBlockHeadersMessage(final byte[] encoded) {
        super(encoded);
    }

    public GetBlockHeadersMessage(final long blockNumber, final int maxHeaders) {
        this(blockNumber, null, maxHeaders, 0, false);
    }

    public GetBlockHeadersMessage(final long blockNumber, final byte[] blockHash, final int maxHeaders, final int skipBlocks, final boolean reverse) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.maxHeaders = maxHeaders;
        this.skipBlocks = skipBlocks;
        this.reverse = reverse;

        parsed = true;
        encode();
    }

    private void encode() {
        final byte[] maxHeaders = RLP.encodeInt(this.maxHeaders);
        final byte[] skipBlocks = RLP.encodeInt(this.skipBlocks);
        final byte[] reverse = RLP.encodeByte((byte) (this.reverse ? 1 : 0));

        if (this.blockHash != null) {
            final byte[] hash = RLP.encodeElement(this.blockHash);
            this.encoded = RLP.encodeList(hash, maxHeaders, skipBlocks, reverse);
        } else {
            final byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.blockNumber));
            this.encoded = RLP.encodeList(number, maxHeaders, skipBlocks, reverse);
        }
    }

    private synchronized void parse() {
        if (parsed) return;
        final RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        final byte[] blockBytes = paramsList.get(0).getRLPData();

        // it might be either a hash or number
        if (blockBytes == null) {
            this.blockNumber = 0;
        } else if (blockBytes.length == DEFAULT_SIZE_BYTES) {
            this.blockHash = blockBytes;
        } else {
            this.blockNumber = byteArrayToLong(blockBytes);
        }

        final byte[] maxHeaders = paramsList.get(1).getRLPData();
        this.maxHeaders = byteArrayToInt(maxHeaders);

        final byte[] skipBlocks = paramsList.get(2).getRLPData();
        this.skipBlocks = byteArrayToInt(skipBlocks);

        final byte[] reverse = paramsList.get(3).getRLPData();
        this.reverse = byteArrayToInt(reverse) == 1;

        parsed = true;
    }

    public long getBlockNumber() {
        parse();
        return blockNumber;
    }

    public byte[] getBlockHash() {
        parse();
        return blockHash;
    }

    public BlockIdentifier getBlockIdentifier() {
        parse();
        return new BlockIdentifier(blockHash, blockNumber);
    }

    public int getMaxHeaders() {
        parse();
        return maxHeaders;
    }

    public int getSkipBlocks() {
        parse();
        return skipBlocks;
    }

    public boolean isReverse() {
        parse();
        return reverse;
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<BlockHeadersMessage> getAnswerMessage() {
        return BlockHeadersMessage.class;
    }

    @Override
    public EthMessageCodes getCommand() {
        return EthMessageCodes.GET_BLOCK_HEADERS;
    }

    @Override
    public String toString() {
        parse();
        return "[" + this.getCommand().name() +
                " blockNumber=" + String.valueOf(blockNumber) +
                " blockHash=" + ByteUtil.toHexString(blockHash) +
                " maxHeaders=" + maxHeaders +
                " skipBlocks=" + skipBlocks +
                " reverse=" + reverse + "]";
    }
}
