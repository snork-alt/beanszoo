
# BeansZoo


BeansZoo is a framework that enables to distribute Java services across multiple
machines and simplifies any interaction among them. BeansZoo was initially designed to run
distributed applications on top of YARN. 

## Components

### Service Directory

Service Directory provides methods for registering new services as well as querying existing services. Services can either be  be local (running in the same JVM instance) or remote (either a remote machine or a different JVM running on the same machine). Whenever a service is available locally, Service Directory returns a direct reference to the local instance, while, if the service is only available remotely, Service Directory returns a reference to an instance proxy, which will use RPC to perform remote method invocations.

Registration of services in the Service Directory happens using an id as well as type. Id can be directly defined by the user during a service registration, while types are automatically discovered by Service Directory. In fact, upon a registration, Service Directory indexes all interfaces exposed by the service, so that services can be queries by exposed interfaces as well. This is particularly useful when access to a feature is requested (think about accessing the File System) without worrying about the specific implementation. 

In cases where multiple implementation of the same service are needed, a service can be annotated with the ```@name``` annotation. Whenever needed, a client can request access to a specific implementation of a service, by supplying the name togetehr with the requested service. 

### RPC Server and Client

RPC client and server are used to perform remote method invocation between all the services registered using the Service Directory. These components have pluggable transp[ort layers as well as encoding formats. The current implementation uses sockets as a transport layer, while multiple encoding format are provided. An FST-based codec, a Java-native codec as well as a YAML-based codec, useful for debugging and message inspection. 

## Invocation policies

Whenever a service is lookup up, the Service Directory will always try to locate the implementation running in the same JVM of the client. If that is not available, Service Directory will choose randomly any of the remote implementations and return a proxy to it.

This policy can be modified by annotating a service with the ```@InvocationPolicy``` annotation, and passing the alternative invocation policy implementation. Currently, two alternative invocation policies are available:

### Round-Robin Policy
With this policy, each client will invoke different instances of the service in a round-robing fashion. This policy does not give a higher priority to the local instannce, like described previously in the default behavior.

### Partitioning Policy
With this policy, a partitioning algorithm determines which instances of the service should be invoked. When this policy is used, methods of the interfaces that are to be partitioned must be marked with the ```@Partitioned``` and the ```@PartitionKey``` annotations. Check below for more details on how to use partitioning. 

## Instantiation policies

By default, Service Directory will expose all instances of a particular service and use any of the invocation policies when services are invoked. However, in some circumstances only a single instance should be exposed, while all others should stay in stand-by and be exposed only if the primary instance sails. This includes a coordination batween services and a leader election mechanism. BenasZoo can support this kind of policy using the ```@OnRegister``` annotation. Through this annotation, it is possible to control in which way instances are registered and activated. The annotation takes a registration handler parameter, which implemens the custom logic for the service registration. Currently, a ```LeaderElectionHandler``` is provided, which implements the custom logic needed for the leader election and instance standby. 

## Examples
 
### Basic usage

The following class implements a simple service exposed through BeansZoo

```java
public interface SampleService {
    String test();
}

public class SampleServiceImpl implements SampleService {

    String name;

    public SampleService2Impl(String name) {
        this.name = name;
    }

    @Override
    public String test() {
        return name;
    }
}
```

On the server side we create a Service directory, an RPC Server and expose a new instance
of the SampleService class:

```java
SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090);
ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
        serverAddress, zkServer.getConnectString()
);
serverSd.start();
serverSd.putService(new SampleServiceImpl("TestService"));

RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
rpcServer.start();
```

On the client side we can simply invoke the instantiated object with the following code:

```java
RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
SocketRpcServerAddress clientAddress = new SocketRpcServerAddress("localhost", 9091);
ZookeeperServiceDirectory clientSd = new ZookeeperServiceDirectory(
        clientAddress, zkServer.getConnectString()
);
clientSd.start();

Services clientServices = new Services(rpcClient, clientSd);
SampleService svc = clientServices.getService(SampleService.class);
svc.test();
```

### Using partitioning

In order to defined a partitioned service it is necessary to use three different annotations:

- ```@InvocationPolicy(PartitioningPolicy.class)```: This will instruct Service Directory to use a partitiong policy when invoking the service
- ```@Partitioned(partitioner = RandomPartitioner.class)```: By default, all methods are not partitined. In order for a method to be partitioned, this annotation must be used. A partitioner defining how data is to be partitioned and which instances of the service must be invoked must be supplied to the annotation.
- ```@PartitionKey```: This defines which argument of the method will be partitioned. This parameter must necessarily be a list. In fact, during the invocation, this list is split into multiple sub-list, and each of them is processed by different instances. The partitioner defined above determines how the list is split and which instances must be invoked.

```java
public interface SamplePartitionedService {
    @Partitioned(partitioner = RandomPartitioner.class)
    List<String> process(@PartitionKey List<String> ls);
}

@InvocationPolicy(PartitioningPolicy.class)
public static class SamplePartitionedServiceImpl implements SamplePartitionedService {

    String id;

    public SamplePartitionedServiceImpl(String id) {
        this.id = id;
    }

    @Override
    public List<String> process(List<String> ls) {
        List<String> res = new ArrayList();
        for (String s : ls) {
            res.add(id + ";" + s);
        }
        return res;
    }
}
```

### Using stand-by instances and leader election

Using stand-by instances requires the usage of the ```@OnRegister(LeaderElectionHandler.class)``` annotation. The ```@name``` annotation is also used to group instances together. This means that only one instance of type ```SampleSingleInstanceService``` and name ```SingleInstance``` can be active at the time. By implementing the ```Activable``` interface, a particular instance can be notified before getting into or out of service.

```java
public interface SampleSingleInstanceService {
        String test();
    }

    @OnRegister(LeaderElectionHandler.class) @Name("SingleInstance")
    public static class SampleSingleInstanceServiceImpl
            implements SampleSingleInstanceService, Activable {

        String id;

        public SampleSingleInstanceServiceImpl(String id) {
            this.id = id;
        }

        @Override
        public String test() {
            return id;
        }

        @Override
        public void activate() throws Exception {
            System.out.print(id + " activated\n");
        }

        @Override
        public void deactivate() throws Exception {
            System.out.print(id + " deactivated\n");
        }
    }
```