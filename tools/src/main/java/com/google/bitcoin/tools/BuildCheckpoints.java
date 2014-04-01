package com.google.bitcoin.tools;

import com.google.bitcoin.core.*;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.MemoryBlockStore;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.bitcoin.utils.Threading;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Date;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkState;

/**
 * Downloads and verifies a full chain from your local peer, emitting checkpoints at each difficulty transition period
 * to a file which is then signed with your key.
 */
public class BuildCheckpoints {

    private static final NetworkParameters PARAMS = MainNetParams.get();
    private static final File CHECKPOINTS_FILE = new File("checkpoints");

    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();

        // Sorted map of UNIX time of block to StoredBlock object.
        final TreeMap<Integer, StoredBlock> checkpoints = new TreeMap<Integer, StoredBlock>();

        // Configure bitcoinj to fetch only headers, not save them to disk, connect to a local fully synced/validated
        // node and to save block headers that are on interval boundaries, as long as they are <1 month old.
<<<<<<< HEAD
        final BlockStore store = new MemoryBlockStore(params);
        final BlockChain chain = new BlockChain(params, store);
        final PeerGroup peerGroup = new PeerGroup(params, chain);
        //peerGroup.addAddress(/*InetAddress.getLocalHost()*/InetAddress.getByName("94.23.16.150"));
        //peerGroup.addAddress(/*InetAddress.getLocalHost()*/InetAddress.getByName("treasurequarry.com"));
        peerGroup.addAddress(/*InetAddress.getLocalHost()*/InetAddress.getByName("exploretheblocks.com"));
        //peerGroup.addAddress(InetAddress.getLocalHost());
=======
        final BlockStore store = new MemoryBlockStore(PARAMS);
        final BlockChain chain = new BlockChain(PARAMS, store);
        final PeerGroup peerGroup = new PeerGroup(PARAMS, chain);
        peerGroup.addAddress(InetAddress.getLocalHost());
>>>>>>> upstream/master
        long now = new Date().getTime() / 1000;
        peerGroup.setFastCatchupTimeSecs(now);

        final long oneMonthAgo = now - (86400 * 30);

        chain.addListener(new AbstractBlockChainListener() {
            @Override
            public void notifyNewBestBlock(StoredBlock block) throws VerificationException {
                int height = block.getHeight();

<<<<<<< HEAD
                boolean addCheckpoint = false;
                //For the first set of blocks, the difficulty retarget is every 120 blocks.
                //Seems like we could increase the interval by 10 fold to reduce the size of the checkpoint file.
                if(height < (CoinDefinition.IFC_RETARGET_SWITCH_BLOCK - 120) && height % (CoinDefinition.getInterval(height, false)*10) == 0)
                    addCheckpoint = true;
                //  There is no need to checkpoint any blocks that came between the 2nd and 4th fork.
                //
                //This is after the diff fork which only requires the previous block to calc the adjustment
                //We can choose any interval we want.  Let us choose 1 day.
                if(height >= CoinDefinition.IFC_RETARGET_SWITCH_BLOCK3 && height % 2880 == 0)
                    addCheckpoint = true;
                if (addCheckpoint && block.getHeader().getTimeSeconds() <= oneMonthAgo) {
=======
                if (height % CoinDefinition.getIntervalCheckpoints() == 0 && block.getHeader().getTimeSeconds() <= oneMonthAgo) {

 //               if (height % PARAMS.getInterval() == 0 && block.getHeader().getTimeSeconds() <= oneMonthAgo) {

>>>>>>> upstream/master
                    System.out.println(String.format("Checkpointing block %s at height %d",
                            block.getHeader().getHash(), block.getHeight()));
                    checkpoints.put(height, block);
                }
            }
        }, Threading.SAME_THREAD);

        peerGroup.startAndWait();
        peerGroup.downloadBlockChain();

        checkState(checkpoints.size() > 0);

        // Write checkpoint data out.
        final FileOutputStream fileOutputStream = new FileOutputStream(CHECKPOINTS_FILE, false);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final DigestOutputStream digestOutputStream = new DigestOutputStream(fileOutputStream, digest);
        digestOutputStream.on(false);
        final DataOutputStream dataOutputStream = new DataOutputStream(digestOutputStream);
        dataOutputStream.writeBytes("CHECKPOINTS 1");
        dataOutputStream.writeInt(0);  // Number of signatures to read. Do this later.
        digestOutputStream.on(true);
        dataOutputStream.writeInt(checkpoints.size());
        ByteBuffer buffer = ByteBuffer.allocate(StoredBlock.COMPACT_SERIALIZED_SIZE);
        for (StoredBlock block : checkpoints.values()) {
            block.serializeCompact(buffer);
            dataOutputStream.write(buffer.array());
            buffer.position(0);
        }
        dataOutputStream.close();
        Sha256Hash checkpointsHash = new Sha256Hash(digest.digest());
        System.out.println("Hash of checkpoints data is " + checkpointsHash);
        digestOutputStream.close();
        fileOutputStream.close();

        peerGroup.stopAndWait();
        store.close();

        // Sanity check the created file.
        CheckpointManager manager = new CheckpointManager(PARAMS, new FileInputStream(CHECKPOINTS_FILE));
        checkState(manager.numCheckpoints() == checkpoints.size());
<<<<<<< HEAD
        //StoredBlock test = manager.getCheckpointBefore(1348310800);  // Just after block 200,000
        //checkState(test.getHeight() == 199584);
        //checkState(test.getHeader().getHashAsString().equals("000000000000002e00a243fe9aa49c78f573091d17372c2ae0ae5e0f24f55b52"));
=======

        if (PARAMS.getId() == NetworkParameters.ID_MAINNET) {
            StoredBlock test = manager.getCheckpointBefore(1390500000); // Thu Jan 23 19:00:00 CET 2014
            checkState(test.getHeight() == 280224);
            checkState(test.getHeader().getHashAsString()
                    .equals("00000000000000000b5d59a15f831e1c45cb688a4db6b0a60054d49a9997fa34"));
        } else if (PARAMS.getId() == NetworkParameters.ID_TESTNET) {
            StoredBlock test = manager.getCheckpointBefore(1390500000); // Thu Jan 23 19:00:00 CET 2014
            checkState(test.getHeight() == 167328);
            checkState(test.getHeader().getHashAsString()
                    .equals("0000000000035ae7d5025c2538067fe7adb1cf5d5d9c31b024137d9090ed13a9"));
        }

        System.out.println("Checkpoints written to '" + CHECKPOINTS_FILE.getCanonicalPath() + "'.");
>>>>>>> upstream/master
    }
}
