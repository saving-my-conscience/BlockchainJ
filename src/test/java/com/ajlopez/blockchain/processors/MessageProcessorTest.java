package com.ajlopez.blockchain.processors;

import com.ajlopez.blockchain.core.Block;
import com.ajlopez.blockchain.core.Transaction;
import com.ajlopez.blockchain.net.peers.Peer;
import com.ajlopez.blockchain.net.Status;
import com.ajlopez.blockchain.net.messages.*;
import com.ajlopez.blockchain.test.simples.SimpleMessageChannel;
import com.ajlopez.blockchain.test.utils.FactoryHelper;
import javafx.util.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by ajlopez on 27/01/2018.
 */
public class MessageProcessorTest {
    @Test
    public void processBlockMessage() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();

        Block block = new Block(0, null);
        Message message = new BlockMessage(block);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor);

        processor.processMessage(message, null);

        Block result = blockProcessor.getBestBlock();

        Assert.assertNotNull(result);
        Assert.assertEquals(block.getHash(), result.getHash());
    }

    @Test
    public void processBlockMessageAndRelayBlockToPeers() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();

        Peer sender = FactoryHelper.createPeer();
        SendProcessor outputProcessor = new SendProcessor(sender);
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(sender, channel);

        Block block = new Block(0, null);
        Message message = new BlockMessage(block);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, outputProcessor);

        processor.processMessage(message, null);

        Block result = blockProcessor.getBestBlock();

        Assert.assertNotNull(result);
        Assert.assertEquals(block.getHash(), result.getHash());

        List<Pair<Peer,Message>> peerMessages = channel.getPeerMessages();

        Assert.assertNotNull(peerMessages);
        Assert.assertEquals(1, peerMessages.size());

        Peer senderPeer = peerMessages.get(0).getKey();

        Assert.assertNotNull(sender);
        Assert.assertEquals(sender.getId(), senderPeer.getId());

        Message outputMessage = peerMessages.get(0).getValue();

        Assert.assertNotNull(outputMessage);
        Assert.assertEquals(MessageType.BLOCK, outputMessage.getMessageType());
        Assert.assertEquals(block, ((BlockMessage)outputMessage).getBlock());
    }

    @Test
    public void processBlockMessageAndRelayBlockToOtherPeers() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();

        Peer sender = FactoryHelper.createPeer();
        SendProcessor sendProcessor = new SendProcessor(sender);

        Peer peer1 = FactoryHelper.createPeer();
        SimpleMessageChannel channel1 = new SimpleMessageChannel();
        sendProcessor.connectToPeer(peer1, channel1);

        Peer peer2 = FactoryHelper.createPeer();
        SimpleMessageChannel channel2 = new SimpleMessageChannel();
        sendProcessor.connectToPeer(peer2, channel2);

        Block block = new Block(0, null);
        Message message = new BlockMessage(block);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, sendProcessor);

        processor.processMessage(message, peer1);

        Block result = blockProcessor.getBestBlock();

        Assert.assertNotNull(result);
        Assert.assertEquals(block.getHash(), result.getHash());

        List<Pair<Peer, Message>> peerMessages1 = channel1.getPeerMessages();

        Assert.assertNotNull(peerMessages1);
        Assert.assertEquals(0, peerMessages1.size());

        List<Pair<Peer, Message>> peerMessages2 = channel2.getPeerMessages();

        Assert.assertNotNull(peerMessages2);
        Assert.assertEquals(1, peerMessages2.size());

        Peer senderPeer = peerMessages2.get(0).getKey();

        Assert.assertNotNull(senderPeer);
        Assert.assertEquals(sender.getId(), senderPeer.getId());

        Message outputMessage = peerMessages2.get(0).getValue();

        Assert.assertNotNull(outputMessage);
        Assert.assertEquals(MessageType.BLOCK, outputMessage.getMessageType());
        Assert.assertEquals(block, ((BlockMessage)outputMessage).getBlock());
    }

    @Test
    public void processGetBlockByHashMessage() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();
        SendProcessor outputProcessor = new SendProcessor(FactoryHelper.createPeer());

        Block block = new Block(0, null);
        Message blockMessage = new BlockMessage(block);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, outputProcessor);

        processor.processMessage(blockMessage, null);

        Message message = new GetBlockByHashMessage(block.getHash());

        Peer sender = FactoryHelper.createPeer();
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(sender, channel);

        processor.processMessage(message, sender);

        Message result = channel.getLastMessage();

        Assert.assertNotNull(result);
        Assert.assertEquals(MessageType.BLOCK, result.getMessageType());

        BlockMessage bmessage = (BlockMessage)result;

        Assert.assertNotNull(bmessage.getBlock());
        Assert.assertEquals(block.getHash(), bmessage.getBlock().getHash());
    }

    @Test
    public void processGetUnknownBlockByHashMessage() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();
        SendProcessor outputProcessor = new SendProcessor(FactoryHelper.createPeer());

        Block block = new Block(0, null);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, outputProcessor);

        Message message = new GetBlockByHashMessage(block.getHash());

        Peer sender = FactoryHelper.createPeer();
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(sender, channel);

        processor.processMessage(message, sender);

        Message result = channel.getLastMessage();

        Assert.assertNull(result);
    }

    @Test
    public void processGetBlockByNumberMessage() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();
        SendProcessor outputProcessor = new SendProcessor(FactoryHelper.createPeer());

        Block block = new Block(0, null);
        Message blockMessage = new BlockMessage(block);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, outputProcessor);

        processor.processMessage(blockMessage, null);

        Message getBlockMessage = new GetBlockByNumberMessage(block.getNumber());
        Peer sender = FactoryHelper.createPeer();
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(sender, channel);

        processor.processMessage(getBlockMessage, sender);

        Message result = channel.getLastMessage();

        Assert.assertNotNull(result);
        Assert.assertEquals(MessageType.BLOCK, result.getMessageType());

        BlockMessage bmessage = (BlockMessage)result;

        Assert.assertNotNull(bmessage.getBlock());
        Assert.assertEquals(block.getHash(), bmessage.getBlock().getHash());
    }

    @Test
    public void processGetUnknownBlockByNumberMessage() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();
        SendProcessor outputProcessor = new SendProcessor(FactoryHelper.createPeer());

        Block block = new Block(0, null);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, outputProcessor);

        Message getBlockMessage = new GetBlockByNumberMessage(block.getNumber());
        Peer sender = FactoryHelper.createPeer();
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(sender, channel);

        processor.processMessage(getBlockMessage, sender);

        Message result = channel.getLastMessage();

        Assert.assertNull(result);
    }

    @Test
    public void processTransactionMessage() {
        TransactionPool pool = new TransactionPool();
        TransactionProcessor transactionProcessor = new TransactionProcessor(pool);

        Transaction transaction = FactoryHelper.createTransaction(100);
        Message message = new TransactionMessage(transaction);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(transactionProcessor);

        processor.processMessage(message, null);

        List<Transaction> transactions = pool.getTransactions();

        Assert.assertNotNull(transactions);
        Assert.assertFalse(transactions.isEmpty());
        Assert.assertEquals(1, transactions.size());

        Transaction result = transactions.get(0);

        Assert.assertNotNull(result);
        Assert.assertEquals(transaction.getHash(), result.getHash());
    }

    @Test
    public void processTransactionMessageAndRelayToPeers() {
        TransactionPool pool = new TransactionPool();
        TransactionProcessor transactionProcessor = new TransactionProcessor(pool);

        Transaction transaction = FactoryHelper.createTransaction(100);
        Message message = new TransactionMessage(transaction);

        Peer sender = FactoryHelper.createPeer();
        SendProcessor outputProcessor = new SendProcessor(sender);
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(sender, channel);

        MessageProcessor processor = FactoryHelper.createMessageProcessor(transactionProcessor, outputProcessor);

        processor.processMessage(message, null);

        List<Transaction> transactions = pool.getTransactions();

        Assert.assertNotNull(transactions);
        Assert.assertEquals(1, transactions.size());
        Assert.assertEquals(transaction, transactions.get(0));

        Message outputMessage = channel.getLastMessage();

        Assert.assertNotNull(outputMessage);
        Assert.assertEquals(MessageType.TRANSACTION, outputMessage.getMessageType());
        Assert.assertEquals(transaction, ((TransactionMessage)outputMessage).getTransaction());
    }

    @Test
    public void processStatusMessageAndStartSync() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();
        PeerProcessor peerProcessor = new PeerProcessor();
        SendProcessor outputProcessor = new SendProcessor(FactoryHelper.createPeer());

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, peerProcessor, outputProcessor);

        Peer peer = FactoryHelper.createPeer();
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(peer, channel);

        Message message = new StatusMessage(new Status(peer.getId(), 1, 10));

        processor.processMessage(message, peer);

        Assert.assertEquals(10, peerProcessor.getBestBlockNumber());
        Assert.assertEquals(10, peerProcessor.getPeerBestBlockNumber(peer.getId()));

        Assert.assertEquals(11, channel.getPeerMessages().size());

        for (int k = 0; k < 11; k++) {
            Message msg = channel.getPeerMessages().get(k).getValue();

            Assert.assertNotNull(msg);
            Assert.assertEquals(MessageType.GET_BLOCK_BY_NUMBER, msg.getMessageType());

            GetBlockByNumberMessage gmsg = (GetBlockByNumberMessage)msg;

            Assert.assertEquals(k, gmsg.getNumber());
        }
    }

    @Test
    public void processStatusMessageTwiceWithSameHeightAndStartSync() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();
        PeerProcessor peerProcessor = new PeerProcessor();
        SendProcessor outputProcessor = new SendProcessor(FactoryHelper.createPeer());

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, peerProcessor, outputProcessor);

        Peer peer = FactoryHelper.createPeer();
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(peer, channel);

        Message message = new StatusMessage(new Status(peer.getId(), 1, 10));

        processor.processMessage(message, peer);
        processor.processMessage(message, peer);

        Assert.assertEquals(10, peerProcessor.getBestBlockNumber());
        Assert.assertEquals(10, peerProcessor.getPeerBestBlockNumber(peer.getId()));

        Assert.assertEquals(11, channel.getPeerMessages().size());

        for (int k = 0; k < 11; k++) {
            Message msg = channel.getPeerMessages().get(k).getValue();

            Assert.assertNotNull(msg);
            Assert.assertEquals(MessageType.GET_BLOCK_BY_NUMBER, msg.getMessageType());

            GetBlockByNumberMessage gmsg = (GetBlockByNumberMessage)msg;

            Assert.assertEquals(k, gmsg.getNumber());
        }
    }

    @Test
    public void processStatusMessageTwiceWithDifferentHeightsAndStartSync() {
        BlockProcessor blockProcessor = FactoryHelper.createBlockProcessor();
        PeerProcessor peerProcessor = new PeerProcessor();
        SendProcessor outputProcessor = new SendProcessor(FactoryHelper.createPeer());

        MessageProcessor processor = FactoryHelper.createMessageProcessor(blockProcessor, peerProcessor, outputProcessor);

        Peer peer = FactoryHelper.createPeer();
        SimpleMessageChannel channel = new SimpleMessageChannel();
        outputProcessor.connectToPeer(peer, channel);

        Message message1 = new StatusMessage(new Status(peer.getId(), 1, 5));
        Message message2 = new StatusMessage(new Status(peer.getId(), 1, 10));

        processor.processMessage(message1, peer);
        processor.processMessage(message2, peer);

        Assert.assertEquals(10, peerProcessor.getBestBlockNumber());
        Assert.assertEquals(10, peerProcessor.getPeerBestBlockNumber(peer.getId()));

        Assert.assertEquals(11, channel.getPeerMessages().size());

        for (int k = 0; k < 11; k++) {
            Message msg = channel.getPeerMessages().get(k).getValue();

            Assert.assertNotNull(msg);
            Assert.assertEquals(MessageType.GET_BLOCK_BY_NUMBER, msg.getMessageType());

            GetBlockByNumberMessage gmsg = (GetBlockByNumberMessage)msg;

            Assert.assertEquals(k, gmsg.getNumber());
        }
    }
}
