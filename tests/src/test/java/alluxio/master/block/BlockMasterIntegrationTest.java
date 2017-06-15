package alluxio.master.block;

import static org.junit.Assert.fail;

import alluxio.AlluxioURI;
import alluxio.client.WriteType;
import alluxio.client.file.FileSystem;
import alluxio.client.file.FileSystemTestUtils;
import alluxio.client.file.URIStatus;
import alluxio.exception.BlockInfoException;
import alluxio.master.LocalAlluxioCluster;
import alluxio.master.MasterRegistry;
import alluxio.master.MasterTestUtils;
import alluxio.wire.WorkerNetAddress;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

/**
 * Integration test for block master functionality.
 */
public class BlockMasterIntegrationTest {

  private ExecutorService mExecutorsForClient;
  private LocalAlluxioCluster mCluster;

  @Before
  public void before() throws Exception {
    mCluster = new LocalAlluxioCluster();
    mCluster.initConfiguration();
    mCluster.start();
  }

  @Test
  public void journalBlockCreation() throws Exception {
    FileSystem fs = mCluster.getClient();
    BlockMaster blockMaster = mCluster.getLocalAlluxioMaster().getMasterProcess().getMaster(BlockMaster.class);
    AlluxioURI file = new AlluxioURI("/test");
    FileSystemTestUtils.createByteFile(fs, file, WriteType.MUST_CACHE, 10);
    URIStatus status = fs.getStatus(file);
    Long blockId = status.getBlockIds().get(0);
    Assert.assertNotNull(blockMaster.getBlockInfo(blockId));
    mCluster.stop();
    MasterRegistry registry = MasterTestUtils.createLeaderFileSystemMasterFromJournal();
    Assert.assertNotNull(registry.get(BlockMaster.class).getBlockInfo(blockId));
    registry.stop();
  }

  @Test
  public void journalBlockDeletion() throws Exception {
    FileSystem fs = mCluster.getClient();
    BlockMaster blockMaster = mCluster.getLocalAlluxioMaster().getMasterProcess().getMaster(BlockMaster.class);
    AlluxioURI file = new AlluxioURI("/test");
    FileSystemTestUtils.createByteFile(fs, file, WriteType.MUST_CACHE, 10);
    URIStatus status = fs.getStatus(file);
    Long blockId = status.getBlockIds().get(0);
    Assert.assertNotNull(blockMaster.getBlockInfo(blockId));
    fs.delete(file);
    WorkerNetAddress workerAddress = mCluster.getWorkerAddress();
    try {
      blockMaster.getBlockInfo(blockId);
      fail("Expected the block to be deleted");
    } catch (BlockInfoException e) {
      // expected
    }
    mCluster.stop();
    MasterRegistry registry = MasterTestUtils.createLeaderFileSystemMasterFromJournal();
    try {
      registry.get(BlockMaster.class).getBlockInfo(blockId);
      fail("Expected the block to be deleted after restart");
    } catch (BlockInfoException e) {
      // expected
    }
    registry.stop();
  }
}
