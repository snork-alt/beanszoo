package com.dataheaps.beanszoo.lifecycle;

import com.google.common.base.Optional;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.test.PathUtils;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * @author chandras
 */
public class YarnLifeCycleTest {

    private MiniYARNCluster yarnLocalCluster;
    private HdfsConfiguration hdfsConfiguration;
    static String CLUSTER_1 = "HDFSExtractor";

    @BeforeClass
    public static void setUPNew(){
        System.setProperty("HADOOP_CONF_DIR","/usr/local/Cellar/hadoop/2.8.0/libexec/etc/hadoop");
        System.setProperty("HADOOP_COMMON_HOME","/usr/local/Cellar/hadoop/2.8.0/libexec");
        System.setProperty("HADOOP_HDFS_HOME","/usr/local/Cellar/hadoop/2.8.0/libexec");
        System.setProperty("HADOOP_YARN_HOME","/usr/local/Cellar/hadoop/2.8.0/libexec");
    }

    @Before
    public void setUp() throws Exception {
        File testDataPath = new File(PathUtils.getTestDir(YarnLifeCycleTest.class
        ), "miniclusters-hdfsextraction");
        System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA);
        //HdfsConfiguration conf = new HdfsConfiguration();
        Optional<Integer> i = Optional.<Integer>absent();
        Integer ll = 64572;
        //String url = "hdfs://localhost:"+ll;

        //conf.set("dfs.namenode.http-address", url);
        File testDataCluster1 = new File(testDataPath, CLUSTER_1);
        String c1Path = testDataCluster1.getAbsolutePath();
        YarnConfiguration conf = new YarnConfiguration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, c1Path);
        conf.setInt(YarnConfiguration.RM_NM_HEARTBEAT_INTERVAL_MS, 100);
        conf.setLong(YarnConfiguration.NM_LOG_RETAIN_SECONDS, 1);
        conf.set("yarn.resourcemanager.webapp.address", "localhost:0");
        conf.setClass(YarnConfiguration.RM_SCHEDULER,
                      FifoScheduler.class, ResourceScheduler.class);
        for (String c : conf.getStrings(
            YarnConfiguration.YARN_APPLICATION_CLASSPATH,
            YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
            System.out.println(c);
        }
        this.yarnLocalCluster = new MiniYARNCluster(YarnLifeCycleTest.class.getName(), 1, 1, 1, 1);
        this.yarnLocalCluster.init(conf);
        this.yarnLocalCluster.start();
    }

    @After
    public void tearDown() throws Exception {
        // We want the cluster to be able to shut down
        this.yarnLocalCluster.stop();
    }

    @Test
    public void testYarn(){
        //Configuration yarncn = yarnLocalCluster.getConfig();
        //YarnBeansZooApplication ybza = new YarnBeansZooApplication();
        System.out.print("New Items");
    }
}
