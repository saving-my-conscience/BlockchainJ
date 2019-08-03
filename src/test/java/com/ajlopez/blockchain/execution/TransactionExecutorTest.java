package com.ajlopez.blockchain.execution;

import com.ajlopez.blockchain.core.Account;
import com.ajlopez.blockchain.core.Transaction;
import com.ajlopez.blockchain.core.types.Address;
import com.ajlopez.blockchain.core.types.DataWord;
import com.ajlopez.blockchain.state.Trie;
import com.ajlopez.blockchain.store.AccountStore;
import com.ajlopez.blockchain.store.CodeStore;
import com.ajlopez.blockchain.store.HashMapStore;
import com.ajlopez.blockchain.store.TrieStore;
import com.ajlopez.blockchain.test.utils.FactoryHelper;
import com.ajlopez.blockchain.vms.eth.*;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ajlopez on 27/11/2018.
 */
public class TransactionExecutorTest {
    @Test
    public void executeTransaction() {
        AccountStore accountStore = new AccountStore(new Trie());

        Address senderAddress = FactoryHelper.createAccountWithBalance(accountStore, 1000);
        Address receiverAddress = FactoryHelper.createRandomAddress();

        Transaction transaction = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 0, null, 6000000, BigInteger.ZERO);

        TransactionExecutor executor = new TransactionExecutor(new TopExecutionContext(accountStore, null, null));

        List<Transaction> result = executor.executeTransactions(Collections.singletonList(transaction), null);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.size());

        Transaction tresult = result.get(0);

        Assert.assertEquals(transaction, tresult);

        BigInteger senderBalance = accountStore.getAccount(senderAddress).getBalance();
        Assert.assertNotNull(senderBalance);
        Assert.assertEquals(BigInteger.valueOf(1000 - 100), senderBalance);

        BigInteger receiverBalance = accountStore.getAccount(receiverAddress).getBalance();
        Assert.assertNotNull(receiverAddress);
        Assert.assertEquals(BigInteger.valueOf(100), receiverBalance);

        Assert.assertEquals(0, accountStore.getAccount(receiverAddress).getNonce());
        Assert.assertEquals(1, accountStore.getAccount(senderAddress).getNonce());
    }

    @Test
    public void executeTransactionWithGasPrice() {
        AccountStore accountStore = new AccountStore(new Trie());

        Address senderAddress = FactoryHelper.createAccountWithBalance(accountStore, 100000);
        Address receiverAddress = FactoryHelper.createRandomAddress();

        Transaction transaction = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 0, null, 60000, BigInteger.ONE);

        TransactionExecutor executor = new TransactionExecutor(new TopExecutionContext(accountStore, null, null));

        Address coinbase = FactoryHelper.createRandomAddress();
        BlockData blockData = new BlockData(1,2,coinbase, DataWord.ONE);
        List<Transaction> result = executor.executeTransactions(Collections.singletonList(transaction), blockData);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.size());

        Transaction tresult = result.get(0);

        Assert.assertEquals(transaction, tresult);

        BigInteger senderBalance = accountStore.getAccount(senderAddress).getBalance();
        Assert.assertNotNull(senderBalance);
        Assert.assertEquals(BigInteger.valueOf(100000 - FeeSchedule.TRANSFER.getValue() - 100), senderBalance);

        BigInteger receiverBalance = accountStore.getAccount(receiverAddress).getBalance();
        Assert.assertNotNull(receiverAddress);
        Assert.assertEquals(BigInteger.valueOf(100), receiverBalance);

        BigInteger coinbaseBalance = accountStore.getAccount(coinbase).getBalance();
        Assert.assertNotNull(coinbase);
        Assert.assertEquals(BigInteger.valueOf(FeeSchedule.TRANSFER.getValue()), coinbaseBalance);

        Assert.assertEquals(0, accountStore.getAccount(receiverAddress).getNonce());
        Assert.assertEquals(1, accountStore.getAccount(senderAddress).getNonce());
    }

    @Test
    public void executeTwoTransactions() {
        AccountStore accountStore = new AccountStore(new Trie());

        Address senderAddress = FactoryHelper.createAccountWithBalance(accountStore, 1000);
        Address receiverAddress = FactoryHelper.createRandomAddress();

        Transaction transaction1 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 0, null, 6000000, BigInteger.ZERO);
        Transaction transaction2 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(50), 1, null, 6000000, BigInteger.ZERO);
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        TransactionExecutor executor = new TransactionExecutor(new TopExecutionContext(accountStore, null, null));

        List<Transaction> result = executor.executeTransactions(transactions, null);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(2, result.size());

        Transaction tresult1 = result.get(0);
        Assert.assertEquals(transaction1, tresult1);
        Transaction tresult2 = result.get(1);
        Assert.assertEquals(transaction2, tresult2);

        BigInteger senderBalance = accountStore.getAccount(senderAddress).getBalance();
        Assert.assertNotNull(senderBalance);
        Assert.assertEquals(BigInteger.valueOf(1000 - 150), senderBalance);

        BigInteger receiverBalance = accountStore.getAccount(receiverAddress).getBalance();
        Assert.assertNotNull(receiverAddress);
        Assert.assertEquals(BigInteger.valueOf(150), receiverBalance);

        Assert.assertEquals(0, accountStore.getAccount(receiverAddress).getNonce());
        Assert.assertEquals(2, accountStore.getAccount(senderAddress).getNonce());
    }

    @Test
    public void secondTransactionRejectedByNonce() {
        AccountStore accountStore = new AccountStore(new Trie());

        Address senderAddress = FactoryHelper.createAccountWithBalance(accountStore, 1000);
        Address receiverAddress = FactoryHelper.createRandomAddress();

        Transaction transaction1 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 0, null, 6000000, BigInteger.ZERO);
        Transaction transaction2 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(50), 0, null, 6000000, BigInteger.ZERO);
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        TransactionExecutor executor = new TransactionExecutor(new TopExecutionContext(accountStore, null, null));

        List<Transaction> result = executor.executeTransactions(transactions, null);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.size());

        Transaction tresult1 = result.get(0);
        Assert.assertEquals(transaction1, tresult1);

        BigInteger senderBalance = accountStore.getAccount(senderAddress).getBalance();
        Assert.assertNotNull(senderBalance);
        Assert.assertEquals(BigInteger.valueOf(1000 - 100), senderBalance);

        BigInteger receiverBalance = accountStore.getAccount(receiverAddress).getBalance();
        Assert.assertNotNull(receiverAddress);
        Assert.assertEquals(BigInteger.valueOf(100), receiverBalance);

        Assert.assertEquals(0, accountStore.getAccount(receiverAddress).getNonce());
        Assert.assertEquals(1, accountStore.getAccount(senderAddress).getNonce());
    }

    @Test
    public void secondTransactionRejectedByInsufficientBalance() {
        AccountStore accountStore = new AccountStore(new Trie());

        Address senderAddress = FactoryHelper.createAccountWithBalance(accountStore, 1000);
        Address receiverAddress = FactoryHelper.createRandomAddress();

        Transaction transaction1 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 0, null, 6000000, BigInteger.ZERO);
        Transaction transaction2 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(5000), 1, null, 6000000, BigInteger.ZERO);
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        TransactionExecutor executor = new TransactionExecutor(new TopExecutionContext(accountStore, null, null));

        List<Transaction> result = executor.executeTransactions(transactions, null);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.size());

        Transaction tresult1 = result.get(0);
        Assert.assertEquals(transaction1, tresult1);

        BigInteger senderBalance = accountStore.getAccount(senderAddress).getBalance();
        Assert.assertNotNull(senderBalance);
        Assert.assertEquals(BigInteger.valueOf(1000 - 100), senderBalance);

        BigInteger receiverBalance = accountStore.getAccount(receiverAddress).getBalance();
        Assert.assertNotNull(receiverAddress);
        Assert.assertEquals(BigInteger.valueOf(100), receiverBalance);

        Assert.assertEquals(0, accountStore.getAccount(receiverAddress).getNonce());
        Assert.assertEquals(1, accountStore.getAccount(senderAddress).getNonce());
    }

    @Test
    public void secondTransactionRejectedByInsufficientBalanceToCoverGasLimit() {
        AccountStore accountStore = new AccountStore(new Trie());

        Address senderAddress = FactoryHelper.createAccountWithBalance(accountStore, 1000);
        Address receiverAddress = FactoryHelper.createRandomAddress();

        Transaction transaction1 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 0, null, 6000000, BigInteger.ZERO);
        Transaction transaction2 = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 1, null, 6000000, BigInteger.ONE);
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);

        TransactionExecutor executor = new TransactionExecutor(new TopExecutionContext(accountStore, null, null));

        List<Transaction> result = executor.executeTransactions(transactions, null);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.size());

        Transaction tresult1 = result.get(0);
        Assert.assertEquals(transaction1, tresult1);

        BigInteger senderBalance = accountStore.getAccount(senderAddress).getBalance();
        Assert.assertNotNull(senderBalance);
        Assert.assertEquals(BigInteger.valueOf(1000 - 100), senderBalance);

        BigInteger receiverBalance = accountStore.getAccount(receiverAddress).getBalance();
        Assert.assertNotNull(receiverAddress);
        Assert.assertEquals(BigInteger.valueOf(100), receiverBalance);

        Assert.assertEquals(0, accountStore.getAccount(receiverAddress).getNonce());
        Assert.assertEquals(1, accountStore.getAccount(senderAddress).getNonce());
    }

    @Test
    public void execute999Transactions() {
        int ntxs = 1000;
        AccountStore accountStore = new AccountStore(new Trie());

        List<Address> addresses = new ArrayList<>();

        for (int k = 0; k < ntxs; k++)
            addresses.add(FactoryHelper.createRandomAddress());

        Account account = new Account(BigInteger.valueOf(ntxs + 1), 0, null, null);

        accountStore.putAccount(addresses.get(0), account);

        List<Transaction> transactions = new ArrayList<>();

        for (int k = 0; k < ntxs - 1; k++) {
            Transaction transaction = new Transaction(addresses.get(k), addresses.get(k + 1), BigInteger.valueOf(ntxs - k), 0, null, 6000000, BigInteger.ZERO);
            transactions.add(transaction);
        }

        TransactionExecutor transactionExecutor = new TransactionExecutor(new TopExecutionContext(accountStore, null, null));

        long millis = System.currentTimeMillis();
        List<Transaction> executed = transactionExecutor.executeTransactions(transactions, null);
        millis = System.currentTimeMillis() - millis;
        System.out.println(millis);

        millis = System.currentTimeMillis();
        accountStore.getRootHash();
        millis = System.currentTimeMillis() - millis;
        System.out.println(millis);

        Assert.assertNotNull(executed);
        Assert.assertEquals(transactions.size(), executed.size());
    }

    private static Storage executeTransactionInvokingCode(byte[] code) {
        return executeTransactionInvokingCode(code, FactoryHelper.createRandomAddress(), FactoryHelper.createRandomAddress());
    }

    @Test
    public void executeTransactionInvokingContractCode() {
        byte[] code = new byte[] { OpCodes.PUSH1, 0x01, OpCodes.PUSH1, 0x00, OpCodes.SSTORE };

        Storage storage = executeTransactionInvokingCode(code);

        Assert.assertNotNull(storage);
        Assert.assertEquals(DataWord.ONE, storage.getValue(DataWord.ZERO));
    }

    @Test
    public void executeTransactionInvokingContractCodeGettingMessageData() {
        byte[] code = new byte[] {
                OpCodes.ORIGIN, OpCodes.PUSH1, 0x00, OpCodes.SSTORE,
                OpCodes.CALLER, OpCodes.PUSH1, 0x01, OpCodes.SSTORE,
                OpCodes.ADDRESS, OpCodes.PUSH1, 0x02, OpCodes.SSTORE,
                OpCodes.CALLVALUE, OpCodes.PUSH1, 0x03, OpCodes.SSTORE,
                OpCodes.GAS, OpCodes.PUSH1, 0x04, OpCodes.SSTORE,
                OpCodes.GASPRICE, OpCodes.PUSH1, 0x05, OpCodes.SSTORE
        };

        Address senderAddress = FactoryHelper.createRandomAddress();
        Address receiverAddress = FactoryHelper.createRandomAddress();

        Storage storage = executeTransactionInvokingCode(code, senderAddress, receiverAddress);

        Assert.assertNotNull(storage);
        Assert.assertEquals(DataWord.fromAddress(senderAddress), storage.getValue(DataWord.ZERO));
        Assert.assertEquals(DataWord.fromAddress(senderAddress), storage.getValue(DataWord.ONE));
        Assert.assertEquals(DataWord.fromAddress(receiverAddress), storage.getValue(DataWord.TWO));
        Assert.assertEquals(DataWord.fromUnsignedInteger(100), storage.getValue(DataWord.fromUnsignedInteger(3)));
        Assert.assertEquals(DataWord.fromUnsignedLong(6000000L - FeeSchedule.BASE.getValue() - 4 * (FeeSchedule.BASE.getValue() + FeeSchedule.VERYLOW.getValue() + FeeSchedule.SSET.getValue())), storage.getValue(DataWord.fromUnsignedInteger(4)));
        Assert.assertEquals(DataWord.ZERO, storage.getValue(DataWord.fromUnsignedInteger(5)));
    }

    private static Storage executeTransactionInvokingCode(byte[] code, Address senderAddress, Address receiverAddress) {
        CodeStore codeStore = new CodeStore(new HashMapStore());
        TrieStorageProvider trieStorageProvider = new TrieStorageProvider(new TrieStore(new HashMapStore()));
        AccountStore accountStore = new AccountStore(new Trie());

        FactoryHelper.createAccountWithBalance(accountStore, senderAddress, 1000000);
        FactoryHelper.createAccountWithCode(accountStore, codeStore, receiverAddress, code);

        Transaction transaction = new Transaction(senderAddress, receiverAddress, BigInteger.valueOf(100), 0, null, 50000, BigInteger.ONE);

        TransactionExecutor executor = new TransactionExecutor(new TopExecutionContext(accountStore, trieStorageProvider, codeStore));

        Address coinbase = FactoryHelper.createRandomAddress();
        BlockData blockData = new BlockData(1,2,coinbase, DataWord.ONE);

        List<Transaction> result = executor.executeTransactions(Collections.singletonList(transaction), blockData);

        Account receiver = accountStore.getAccount(receiverAddress);

        Assert.assertNotNull(receiver);
        Assert.assertNotNull(receiver.getStorageHash());

        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(1, result.size());

        Transaction tresult = result.get(0);

        Assert.assertEquals(transaction, tresult);

        BigInteger coinbaseBalance = accountStore.getAccount(coinbase).getBalance();
        Assert.assertNotNull(coinbaseBalance);
        Assert.assertNotEquals(BigInteger.ZERO, coinbaseBalance);

        BigInteger senderBalance = accountStore.getAccount(senderAddress).getBalance();
        Assert.assertNotNull(senderBalance);
        Assert.assertEquals(BigInteger.valueOf(1000000 - 100).subtract(coinbaseBalance), senderBalance);

        Assert.assertNotNull(receiverAddress);

        BigInteger receiverBalance = receiver.getBalance();
        Assert.assertEquals(BigInteger.valueOf(100), receiverBalance);

        Assert.assertEquals(0, accountStore.getAccount(receiverAddress).getNonce());
        Assert.assertEquals(1, accountStore.getAccount(senderAddress).getNonce());

        return trieStorageProvider.retrieve(receiver.getStorageHash());
    }
}
