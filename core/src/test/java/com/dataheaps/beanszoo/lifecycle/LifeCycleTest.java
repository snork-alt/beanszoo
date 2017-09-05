package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.SocketRpcFactory;
import com.dataheaps.beanszoo.sd.ZookeeperServiceDirectoryFactory;
import com.dataheaps.beanszoo.sd.ZookeeperServiceDirectoryTest;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Created by admin on 9/2/17.
 */
public class LifeCycleTest {


    @Test
    public void testProgrammaticConfig() throws Exception {

        TestingServer server = new TestingServer(true);

        Configuration conf = new Configuration();
        conf.setRpcFactory(new SocketRpcFactory(new FstRPCRequestCodec()));
        conf.setSdFactory(new ZookeeperServiceDirectoryFactory(server.getConnectString(), 5000));

        RoleConfiguration role0 = new RoleConfiguration();
        role0.setId("role0");
        role0.setServices(new InstanceConfiguration[]{
                new InstanceConfiguration(SimpleService0Impl.class, new HashMap<>())
        });

        RoleConfiguration role1 = new RoleConfiguration();
        role1.setId("role1");
        role1.setServices(new InstanceConfiguration[]{
                new InstanceConfiguration(SimpleService1Impl.class, new HashMap<>())
        });

        conf.setRoles(new RoleConfiguration[] {role0, role1});

        ContainerConfiguration containerConfig0 = new ContainerConfiguration();
        containerConfig0.setInstances(1);
        containerConfig0.setRoles(new String[] {"role0"});

        ContainerConfiguration containerConfig1 = new ContainerConfiguration();
        containerConfig1.setInstances(1);
        containerConfig1.setRoles(new String[] {"role1"});

        conf.setContainers(new ContainerConfiguration[] {containerConfig0, containerConfig1});

        LocalLifeCycleManager lcm = new LocalLifeCycleManager(conf);
        lcm.start();

        List<String> res = new ArrayList<>();
        for (Container c : lcm.containers) {
            res.add(c.services.getService(SimpleService0.class).test0());
        }

        assert (res.size() == 2);
        for (String e: res)
            assert (e.equals(ZookeeperServiceDirectoryTest.SampleServiceImpl1.class.getCanonicalName()));



    }

    @Test
    public void testYamlConfig() throws Exception {

        new SimpleService0Impl();
        new SimpleService1Impl();

        TestingServer server = new TestingServer(true);
        Properties p = new Properties();
        p.put("zkQuorum", server.getConnectString());

        YamlConfigurationReader reader = new YamlConfigurationReader();
        reader.props = p;
        String path = new File("src/test/java/com/dataheaps/beanszoo/lifecycle/conf.yaml").getAbsolutePath();
        Configuration conf = reader.load("file:" + path);

        LocalLifeCycleManager lcm = new LocalLifeCycleManager(conf);
        lcm.start();

        List<String> res = new ArrayList<>();
        for (Container c : lcm.containers) {
            res.add(c.services.getService(SimpleService0.class).test0());
        }

        assert (res.size() == 2);
        for (String e: res)
            assert (e.equals(ZookeeperServiceDirectoryTest.SampleServiceImpl1.class.getCanonicalName()));



    }

    @Test
    public void testYamlConfigNested() throws Exception {

        TestingServer server = new TestingServer(true);
        Properties p = new Properties();
        p.put("zkQuorum", server.getConnectString());

        YamlConfigurationReader reader = new YamlConfigurationReader();
        reader.props = p;
        String path = new File("src/test/java/com/dataheaps/beanszoo/lifecycle/conf_nested.yaml").getAbsolutePath();
        Configuration conf = reader.load("file:" + path);

        LocalLifeCycleManager lcm = new LocalLifeCycleManager(conf);
        lcm.start();

        Container c = lcm.containers.get(0);
        assert(c.services.getService(NestedService.class).test().equals("test"));


    }

}
