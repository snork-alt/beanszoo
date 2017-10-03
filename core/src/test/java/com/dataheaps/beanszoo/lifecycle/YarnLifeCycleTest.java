package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.app.YarnBeansZooApplication;
import com.dataheaps.beanszoo.utils.CommonUtils;
import com.github.sakserv.minicluster.impl.ZookeeperLocalCluster;
import com.google.common.base.Optional;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.server.common.HdfsServerConstants;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.apache.twill.api.TwillController;
import org.apache.twill.api.TwillRunner;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
/**
 * @author chandras
 */
public class YarnLifeCycleTest {

    static String CLUSTER_1 = "HDFSExtractor";
    private MiniDFSCluster hdfsCluster = null;
    private MiniYARNCluster yarnCluster = null;
    private org.apache.hadoop.conf.Configuration hdfsConf;
    private YarnConfiguration yarnConf;
    private String testName = null;
    private FileSystem fs = null;
    private File baseDir = null;
    ZookeeperLocalCluster zookeeperLocalCluster = null;
    YarnBeansZooApplication ybza = null;

    @BeforeClass
    public static void setUPNew(){
        //System.setProperty("HADOOP_CONF_DIR","/usr/local/Cellar/hadoop/2.8.0/libexec/etc/hadoop");
        //System.setProperty("HADOOP_COMMON_HOME","/usr/local/Cellar/hadoop/2.8.0/libexec");
        //System.setProperty("HADOOP_HDFS_HOME","/usr/local/Cellar/hadoop/2.8.0/libexec");
        //System.setProperty("HADOOP_YARN_HOME","/usr/local/Cellar/hadoop/2.8.0/libexec");
    }

    @Before
    public void setUP1() throws Exception {
        final int noOfNodeManagers = 1;
        final int numLocalDirs = 1;
        final int numLogDirs = 1;
        baseDir = new File("./target/hdfs/" + YarnLifeCycleTest.class.getName()).getAbsoluteFile();
        FileUtil.fullyDelete(baseDir);
        hdfsConf = new HdfsConfiguration();
        hdfsConf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(hdfsConf).nameNodePort(CommonUtils.getAvailablePort(Optional.<Integer>absent())).startupOption(HdfsServerConstants.StartupOption.REGULAR);
        hdfsCluster = builder.build();
        String hdfsURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/";
        fs = FileSystem.get(hdfsConf);
        yarnConf = new YarnConfiguration();
        hdfsConf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 64);
        hdfsConf.setClass(YarnConfiguration.RM_SCHEDULER,
                          FifoScheduler.class, ResourceScheduler.class);
        yarnCluster = new MiniYARNCluster(YarnLifeCycleTest.class.getName(), noOfNodeManagers, numLocalDirs, numLogDirs);
        yarnCluster.init(hdfsCluster.getConfiguration(0));
        yarnCluster.start();
        hdfsConf = yarnCluster.getConfig();
        int portZK = CommonUtils.getAvailablePort(Optional.<Integer>absent());
        zookeeperLocalCluster = new ZookeeperLocalCluster.Builder()
            .setPort(portZK)
            .setTempDir("embedded_zookeeper")
            .setZookeeperConnectionString("localhost:"+portZK)
            .setMaxClientCnxns(20)
            .setElectionPort(20001)
            .setQuorumPort(20002)
            .setDeleteDataDirectoryOnClose(true)
            .setServerId(1)
            .setTickTime(1000)
            .build();
        zookeeperLocalCluster.start();
    }

    @After
    public void tearDown() throws Exception {
        // We want the cluster to be able to shut down
        zookeeperLocalCluster.stop(true);
        //yarnCluster.stop();
        hdfsCluster.shutdown();
    }

    @Test
    public void testYarn() throws Exception {
        YamlConfigurationReader reader = new YamlConfigurationReader();
        Properties p = new Properties();
        p.put("zkQuorum", zookeeperLocalCluster.getZookeeperConnectionString());
        reader.props = p;
        String path = new File("src/test/java/com/dataheaps/beanszoo/lifecycle/conf.yaml").getAbsolutePath();
        Configuration conf = reader.load(new FileInputStream(path), p);
        ybza = new YarnBeansZooApplication((YarnConfiguration) hdfsConf, conf, zookeeperLocalCluster.getZookeeperConnectionString(), YarnLifeCycleTest.class.getName());
        ybza.start();
        ContainerConfiguration [] cc = ybza.getConfig().getContainers();
        int countOfInnst = 0;
        Iterable<TwillRunner.LiveInfo> it = ybza.getRunner().lookupLive();
        for(TwillRunner.LiveInfo live : it){
            System.out.println(live.getApplicationName());
            Iterable<TwillController> itc = live.getControllers();
            for(TwillController tc : itc){
                tc.changeInstances("container0",4);
            }
        }
        for(ContainerConfiguration c : cc){
            countOfInnst = countOfInnst+c.getInstances();
        }
        Assert.assertTrue("waiting for instances",countOfInnst==2);
    }
}
