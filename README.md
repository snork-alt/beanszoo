
# BeansZoo

BeansZoo is a small framework that enables distributing JavaBeans across multiple
machines and simplifies any interaction among them. BeansZoo is made up of two
main components:

- **A Service Directory Server and Client**: These components allow JavaBeans to be published and
discovered locally or across the network. Two implementations are provided: an In-Memory service directory
for working with local services (living in the same JVM) and a Zookeeper-based Service Directory,
allowing the publishing and discovery of services using ZooKeeper.

- **An RPC Server and Client**: These components allow two JavaBeans to interact with each other
remotely by simply invoking method calls. The method call together with the arguments are serialised
over the network in order to invoke the remote method. Serialization codecs are pluggable. Currently
a YAML-based serializer and a Java native serializer are provided.

## The Discovery Process

Publishing and lookup of JavaBeans always happen using interfaces. This means that whenever a bean
is published, BeansZoo will scan all exposed interfaces of the bean, and expose it ro the clients
using the implemented services. This means that, each client can lookup for a services implementing
a specific interface.

It is possible to expose multiple beans exposing the same interface. When this is done, the RPC client
 will invoke the method call from several services, automatically performing a load-balancing across
 multiple instances of the service.

# Example

The following class implements a simple service that we want to expose through BeansZoo

```java
public static interface ISmapleService {
    public String sayHello(String name);
}

@RequiredArgsConstructor @Name("example")
public static class SampleService implements ISmapleService {
    final String serviceName;
    @Override public String sayHello(String name) throws IllegalArgumentException {
        return "Hello " + name + " from service " + serviceName;
    }
}
```

On the server side we create a Service directory, an RPC Server and expose a new instance
of the SampleService class:

```java
    SocketRpcServerAddress addr = new SocketRpcServerAddress("localhost", 8050);
    ZookeperServiceDirectory sd = new ZookeperServiceDirectory("localhost:2181", addr);

    List<RPCRequestCodec> codecs = new ArrayList<>();
    codecs.add(new YamlRPCRequestCodec());
    codecs.add(new NativeRPCRequestCodec());
    SocketRpcServer server = new SocketRpcServer(addr, codecs, new ServiceDirectoryRPCRequestHandler(sd));

    sd.putService("SampleService", new SampleService("SampleService"));
    server.start();
    sd.start();
```

On the client side we can simply invoke the instantiated object with the following code:

```java

    RemoteServiceDirectory sd = new ZookeperServiceDirectory("localhost:2181");
    sd.start();

    RpcClient cli = new SocketRpcClient(new YamlRPCRequestCodec(), 10000);
    Services services = new Services(cli, sd);

    ISmapleService svc = (ISmapleService) services.getServiceByType(ISmapleService.class, "example");

    svc.sayHello("Matteo")

```

