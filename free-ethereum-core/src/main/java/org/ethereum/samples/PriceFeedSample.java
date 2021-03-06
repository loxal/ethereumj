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

import org.ethereum.core.CallTransaction;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.Utils;
import org.ethereum.vm.program.ProgramResult;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.Bean;

import java.math.BigInteger;
import java.util.Date;

/**
 * This sample demonstrates how constant calls works (that is transactions which are
 * not broadcasted to network, executed locally, don't change the blockchain state and can
 * report function return values).
 * Constant calls can actually invoke contract functions which are not formally 'const'
 * i.e. which change the contract storage state, but after such calls the contract
 * storage will remain unmodified.
 *
 * As a side effect this sample shows how Java wrappers for Ethereum contracts can be
 * created and then manipulated as regular Java objects
 *
 * Created by Anton Nashatyrev on 05.02.2016.
 */
public class PriceFeedSample extends BasicSample {

    public static void main(final String[] args) throws Exception {
        sLogger.info("Starting EthereumJ!");

        // Based on Config class the sample would be created by Spring
        // and its springInit() method would be called as an entry point
        EthereumFactory.INSTANCE.createEthereum(Config.class);
    }

    @Override
    public void onSyncDone() {
        try {
            // after all blocks are synced perform the work
//            worker();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void waitForDiscovery() throws Exception {
        super.waitForDiscovery();
        worker();
    }

    /**
     * The method retrieves the information from the PriceFeed contract once in a minute and prints
     * the result in log.
     */
    private void worker() throws Exception {
        final NameRegContract nameRegContract = new NameRegContract();
        if (!nameRegContract.isExist()) {
            throw new RuntimeException("Namereg contract not exist on the blockchain");
        }
        final String priceFeedAddress = Hex.toHexString(nameRegContract.addressOf("ether-camp/price-feed"));
        logger.info("Got PriceFeed address from name registry: " + priceFeedAddress);
        final PriceFeedContract priceFeedContract = new PriceFeedContract(priceFeedAddress);

        logger.info("Polling cryptocurrency exchange rates once a minute (prices are normally updated each 10 mins)...");
        final String[] tickers = {"BTC_ETH", "USDT_BTC", "USDT_ETH"};
        while (true) {
            if (priceFeedContract.isExist()) {
                final StringBuilder s = new StringBuilder(priceFeedContract.updateTime() + ": ");
                for (final String ticker : tickers) {
                    s.append(ticker).append(" ").append(priceFeedContract.getPrice(ticker)).append(" (").append(priceFeedContract.getTimestamp(ticker)).append("), ");
                }
                logger.info(s.toString());
            } else {
                logger.info("PriceFeed contract not exist. Likely it was not yet created until current block");
            }
            Thread.sleep(60 * 1000);
        }
    }

    private static class Config {
        @Bean
        public PriceFeedSample priceFeedSample() {
            return new PriceFeedSample();
        }
    }

    /**
     * Base class for a Ethereum Contract wrapper
     * It can be used by two ways:
     * 1. for each function specify its name and input/output formal parameters
     * 2. Pass the contract JSON ABI to the constructor and then refer the function by name only
     */
    abstract class EthereumContract {
        private final static String zeroAddr = "0000000000000000000000000000000000000000";
        private final String contractAddr;

        private CallTransaction.Contract contractFromABI = null;

        /**
         * @param contractAddr address of the target contract as a hex String
         */
        EthereumContract(final String contractAddr) {
            this.contractAddr = contractAddr;
        }

        /**
         *  Use this variant if you have the contract ABI then you call the functions
         *  by their names only
         */
        public EthereumContract(final String contractAddr, final String contractABI) {
            this.contractAddr = contractAddr;
            this.contractFromABI = new CallTransaction.Contract(contractABI);
        }

        /**
         *  The main method of this demo which illustrates how to call a constant function.
         *  To identify the Solidity contract function (calculate its signature) we need :
         *  - function name
         *  - a list of function formal params how they are declared in declaration
         *  Output parameter types are required only for decoding the return values.
         *
         *  Input arguments Java -> Solidity mapping is the following:
         *    Number, BigInteger, String (hex) -> any integer type
         *    byte[], String (hex) -> bytesN, byte[]
         *    String -> string
         *    Java array of the above types -> Solidity dynamic array of the corresponding type
         *
         *  Output arguments Solidity -> Java mapping:
         *    any integer type -> BigInteger
         *    string -> String
         *    bytesN, byte[] -> byte[]
         *    Solidity dynamic array -> Java array
         */
        Object[] callFunction(final String name, final String[] inParamTypes, final String[] outParamTypes, final Object... args) {
            final CallTransaction.Function function = CallTransaction.Function.fromSignature(name, inParamTypes, outParamTypes);
            final ProgramResult result = ethereum.callConstantFunction(contractAddr, function, args);
            return function.decodeResult(result.getHReturn());
        }

        /**
         *  Use this method if the contract ABI was passed
         */
        Object[] callFunction(final String functionName, final Object... args) {
            if (contractFromABI == null) {
                throw new RuntimeException("The contract JSON ABI should be passed to constructor to use this method");
            }
            final CallTransaction.Function function = contractFromABI.getByName(functionName);
            final ProgramResult result = ethereum.callConstantFunction(contractAddr, function, args);
            return function.decodeResult(result.getHReturn());
        }

        /**
         * Checks if the contract exist in the repository
         */
        public boolean isExist() {
            return !contractAddr.equals(zeroAddr) && ethereum.getRepository().isExist(Hex.decode(contractAddr));
        }
    }

    /**
     * NameReg contract which manages a registry of contracts which can be accessed by name
     *
     * Here we resolve contract functions by specifying their name and input/output types
     *
     * Contract sources, live state and many more here:
     * https://live.ether.camp/account/985509582b2c38010bfaa3c8d2be60022d3d00da
     */
    class NameRegContract extends EthereumContract {

        public NameRegContract() {
            super("985509582b2c38010bfaa3c8d2be60022d3d00da");
        }

        public byte[] addressOf(final String name) {
            final BigInteger bi = (BigInteger) callFunction("addressOf", new String[]{"bytes32"}, new String[]{"address"}, name)[0];
            return ByteUtil.bigIntegerToBytes(bi, 20);
        }

        public String nameOf(final byte[] addr) {
            return (String) callFunction("nameOf", new String[]{"address"}, new String[]{"bytes32"}, (Object) addr)[0];
        }
    }

    /**
     * PriceFeed contract where prices for several securities are stored and updated periodically
     *
     * This contract is created using its JSON ABI representation
     *
     * Contract sources, live state and many more here:
     * https://live.ether.camp/account/1194e966965418c7d73a42cceeb254d875860356
     */
    class PriceFeedContract extends EthereumContract {

        private static final String contractABI =
                "[{" +
                "  'constant': true," +
                "  'inputs': [{" +
                "    'name': 'symbol'," +
                "    'type': 'bytes32'" +
                "  }]," +
                "  'name': 'getPrice'," +
                "  'outputs': [{" +
                "    'name': 'currPrice'," +
                "    'type': 'uint256'" +
                "  }]," +
                "  'type': 'function'" +
                "}, {" +
                "  'constant': true," +
                "  'inputs': [{" +
                "    'name': 'symbol'," +
                "    'type': 'bytes32'" +
                "  }]," +
                "  'name': 'getTimestamp'," +
                "  'outputs': [{" +
                "    'name': 'timestamp'," +
                "    'type': 'uint256'" +
                "  }]," +
                "  'type': 'function'" +
                "}, {" +
                "  'constant': true," +
                "  'inputs': []," +
                "  'name': 'updateTime'," +
                "  'outputs': [{" +
                "    'name': ''," +
                "    'type': 'uint256'" +
                "  }]," +
                "  'type': 'function'" +
                "}, {" +
                "  'inputs': []," +
                "  'type': 'constructor'" +
                "}]";

        public PriceFeedContract(final String contractAddr) {
            super(contractAddr, contractABI.replace("'", "\"")); //JSON parser doesn't like single quotes :(
        }

        public Date updateTime() {
            final BigInteger ret = (BigInteger) callFunction("updateTime")[0];
            // All times in Ethereum are Unix times
            return new Date(Utils.fromUnixTime(ret.longValue()));
        }

        public double getPrice(final String ticker) {
            final BigInteger ret = (BigInteger) callFunction("getPrice", ticker)[0];
            // since Ethereum has no decimal numbers we are storing prices with
            // virtual fixed point
            return ret.longValue() / 1_000_000d;
        }

        public Date getTimestamp(final String ticker) {
            final BigInteger ret = (BigInteger) callFunction("getTimestamp", ticker)[0];
            return new Date(Utils.fromUnixTime(ret.longValue()));
        }
    }
}
