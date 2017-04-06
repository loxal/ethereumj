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

package org.ethereum.samples;

import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;

import java.math.BigInteger;

/**
 * The class demonstrates usage of the StandaloneBlockchain helper class
 * which greatly simplifies Solidity contract testing on a locally created
 * blockchain
 *
 * Created by Anton Nashatyrev on 04.04.2016.
 */
class StandaloneBlockchainSample {
    // Pretty simple (and probably the most expensive) Calculator
    private static final String contractSrc =
            "contract Calculator {" +
            "  int public result;" +  // public field can be accessed by calling 'result' function
            "  function add(int num) {" +
            "    result = result + num;" +
            "  }" +
            "  function sub(int num) {" +
            "    result = result - num;" +
            "  }" +
            "  function mul(int num) {" +
            "    result = result * num;" +
            "  }" +
            "  function div(int num) {" +
            "    result = result / num;" +
            "  }" +
            "  function clear() {" +
            "    result = 0;" +
            "  }" +
            "}";

    public static void main(final String[] args) throws Exception {
        // Creating a blockchain which generates a new block for each transaction
        // just not to call createBlock() after each call transaction
        final StandaloneBlockchain bc = new StandaloneBlockchain().withAutoblock(true);
        System.out.println("Creating first empty block (need some time to generate DAG)...");
        // warning up the block miner just to understand how long
        // the initial miner dataset is generated
        bc.createBlock();
        System.out.println("Creating a contract...");
        // This compiles our Solidity contract, submits it to the blockchain
        // internally generates the block with this transaction and returns the
        // contract interface
        final SolidityContract calc = bc.submitNewContract(contractSrc);
        System.out.println("Calculating...");
        // Creates the contract call transaction, submits it to the blockchain
        // and generates a new block which includes this transaction
        // After new block is generated the contract state is changed
        calc.callFunction("add", 100);
        // Check the contract state with a constant call which returns result
        // but doesn't generate any transactions and remain the contract state unchanged
        assertEqual(BigInteger.valueOf(100), (BigInteger) calc.callConstFunction("result")[0]);
        calc.callFunction("add", 200);
        assertEqual(BigInteger.valueOf(300), (BigInteger) calc.callConstFunction("result")[0]);
        calc.callFunction("mul", 10);
        assertEqual(BigInteger.valueOf(3000), (BigInteger) calc.callConstFunction("result")[0]);
        calc.callFunction("div", 5);
        assertEqual(BigInteger.valueOf(600), (BigInteger) calc.callConstFunction("result")[0]);
        System.out.println("Clearing...");
        calc.callFunction("clear");
        assertEqual(BigInteger.valueOf(0), (BigInteger) calc.callConstFunction("result")[0]);
        // We are done - the Solidity contract worked as expected.
        System.out.println("Done.");
    }

    private static void assertEqual(final BigInteger n1, final BigInteger n2) {
        if (!n1.equals(n2)) {
            throw new RuntimeException("Assertion failed: " + n1 + " != " + n2);
        }
    }
}
