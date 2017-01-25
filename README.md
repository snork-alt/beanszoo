
# BeansZoo

BeansZoo is a framework that enables distributing JavaBeans across multiple
machines and simplifies any interaction among them. BeansZoo is made up of two
main components:

- **A Service Directory Server and Client**: These components allow JavaBeans to be published and
discovered locally or across the network. A Zookeeper-based Service Directory implementation is provided.
This implementation allows the publishing and discovery of services using ZooKeeper as a backend.

- **An RPC Server and Client**: These components allow two JavaBeans to interact with each other
remotely by simply invoking method calls. The method call together with the arguments are serialised
over the network in order to invoke the remote method. Serialization codecs are pluggable. Currently
a Yaml-based serializer, a Java native serializer and an FST serializer are provided. Usage of FST
serializer os strongly suggested.

## The Discovery Process

Publishing and lookup of JavaBeans always happen using interfaces. This means that whenever a bean
is published, BeansZoo will scan all exposed interfaces of the bean, and expose them to the clients
using the implemented services. This means that, each client can lookup for a services implementing
a specific interface.

It is possible to expose multiple beans implementing the same interface, from different service directories.
When this is done, the RPC client will invoke the method call from multiple instances, using a particular
invocation policy. The default policy returns the local instance of the service (running in the same JVM, if
available) otherwise invoke a random instance in teh cluster.

The following additional policies are available:

- **Round-Robin Policy**: This policy invokes all the different implementations of a service in a cluster,
in a Round-Robin fashion

- **Partitioning Policy**: This policy allows the invocation to be partitioned across multiple instance in the
cluster

# Simple usage

The following class implements a simple service that we want to expose through BeansZoo

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

