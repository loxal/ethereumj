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

package org.ethereum.config.blockchain;

import org.ethereum.config.BlockchainConfig;
import org.ethereum.core.BlockHeader;
import org.ethereum.util.BIUtil;

import java.math.BigInteger;

/**
 * Ethereum Classic HF on Block #3_000_000:
 * - EXP reprice (EIP-160)
 * - Replay Protection (EIP-155) (chainID: 61)
 * - Difficulty Bomb delay (ECIP-1010) (https://github.com/ethereumproject/ECIPs/blob/master/ECIPs/ECIP-1010.md)
 *
 * Created by Anton Nashatyrev on 13.01.2017.
 */
public class ETCFork3M extends Eip160HFConfig {
    public ETCFork3M(final BlockchainConfig parent) {
        super(parent);
    }

    @Override
    public Integer getChainId() {
        return 61;
    }

    @Override
    public boolean eip161() {
        return false;
    }

    @Override
    public BigInteger calcDifficulty(final BlockHeader curBlock, final BlockHeader parent) {
        final BigInteger pd = parent.getDifficultyBI();
        final BigInteger quotient = pd.divide(getConstants().getDifficultyBoundDivisor());

        final BigInteger sign = getCalcDifficultyMultiplier(curBlock, parent);

        final BigInteger fromParent = pd.add(quotient.multiply(sign));
        BigInteger difficulty = BIUtil.INSTANCE.max(getConstants().getMinimumDifficulty(), fromParent);

        final int explosion = getExplosion(curBlock, parent);

        if (explosion >= 0) {
            difficulty = BIUtil.INSTANCE.max(getConstants().getMinimumDifficulty(), difficulty.add(BigInteger.ONE.shiftLeft(explosion)));
        }

        return difficulty;
    }

    private BigInteger getCalcDifficultyMultiplier(final BlockHeader curBlock, final BlockHeader parent) {
        return BigInteger.valueOf(Math.max(1 - (curBlock.getTimestamp() - parent.getTimestamp()) / 10, -99));
    }


    private int getExplosion(final BlockHeader curBlock, final BlockHeader parent) {
        final int pauseBlock = 3000000;
        final int contBlock = 5000000;
        final int delay = (contBlock - pauseBlock) / 100000;
        final int fixedDiff = (pauseBlock / 100000) - 2;

        if (curBlock.getNumber() < contBlock) {
            return fixedDiff;
        } else {
            return (int) ((curBlock.getNumber() / 100000) - delay - 2);
        }
    }
}