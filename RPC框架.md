# RPC框架

![img](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9jbi1ndW96aXlhbmcuZ2l0aHViLmlvL015LVJQQy1GcmFtZXdvcmsvaW1nL1JQQyVFNiVBMSU4NiVFNiU5RSVCNiVFNiU4MCU5RCVFOCVCNyVBRi5qcGVn?x-oss-process=image/format,png)

## 1  一个最简单的实现

### 1.1 RPC框架原理

​	客户端和服务端都可以访问到通用的接口，但是只有服务端有这个接口的实现类，客户端调用这个接口的方式，是通过网络传输，告诉服务端我要调用这个接口，服务端收到之后找到这个接口的实现类，并且执行，将执行的结果返回给客户端，作为客户端调用接口方法的返回值。

#### 1.1.1通用接口

​	定义通用接口如下：

``` java
public interface HelloService{
    String hello(HelloObject object);//定义一个通用接口，接口有一个hello方法
}
```

​	hello方法需要传递一个对象，这个对象就可以认为是客户端传递过来的信息

``` java
@Data
@AllArgsConstructor
public class HelloObject implements Serializable {//这个对象要从服务端传递给客户端，因此需要实现序列化
    private Integer id;
    private String message;
}
```

​	实现HelloService接口，这里HelloServiceImpl是在服务端的，客户端把HelloObject对象传递过来，通过这个实现对应的处理程序.

```java
public class HelloServiceImpl implements HelloService {
    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);
    @Override
    public String hello(HelloObject object) {
        logger.info("接收到：{}", object.getMessage());
        return "这是掉用的返回值，id=" + object.getId();
    }
}
```

#### 1.1.2传输协议

​	思考：服务端怎么确定执行接口的哪个实现类呢？

​	客户端传递接口名字，方法名字，所有参数类型，所有参数值，这样就可以确定唯一的接口实现类，这里我们把这四个条件写到一个RPCRequest对象里面，客户端传递这个对象过去确定服务端实现哪个实现类.

```java
@Data
public class RpcRequest implements Serializable {
    /**
     * 待调用接口名称
     */
    private String interfaceName;
    /**
     * 待调用方法名称
     */
    private String methodName;
    /**
     * 调用方法的参数
     */
    private Object[] parameters;
    /**
     * 调用方法的参数类型
     */
    private Class<?>[] paramTypes;
}
```

​	服务器调用完成后，需要给用户返回信息，不管成功与否，都应该返回，那么就应该创建一个RPCResponse对象.

```java
@Data
public class RpcResponse<T> implements Serializable {
    /**
     * 响应状态码
     */
    private Integer statusCode;
    /**
     * 响应状态补充信息
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;
  
    public static <T> RpcResponse<T> success(T data) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setStatusCode(ResponseCode.SUCCESS.getCode());
        response.setData(data);
        return response;
    }
    public static <T> RpcResponse<T> fail(ResponseCode code) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setStatusCode(code.getCode());
        response.setMessage(code.getMessage());
        return response;
    }
}
```

#### 1.1.3客户端实现-动态代理

​	客户端是没有接口的实现类的，因此没办法生成实例对象，这是我们就需要通过动态代理的方法生成实例对象. 这里使用jdk动态代理，代理类是需要实现InvocationHandler接口的.

```java
public class RpcClientProxy implements InvocationHandler {
    private String host;//绑定ip地址
    private int port;//绑定端口号

    public RpcClientProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @SuppressWarnings("unchecked")//生成动态代理对象
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }
    /**
	动态代理类要实现invoke方法
    proxy jdk创建的代理对象
    method 目标类中的方法，jdk提供
    args 目标类中方法参数
	**/
    /**
    method.getDeclaringClass().getName():获得调用的接口名字
    method.getName()：获得调用的方法名字
    args：调用方法的参数
    method.getParameterTypes()：调用方法的参数类型 这个例子的method就是.hello方法
    */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .build();
        RpcClient rpcClient = new RpcClient();
        return ((RpcResponse) rpcClient.sendRequest(rpcRequest, host, port)).getData();//响应信息发送给服务端，服务端处理完成后，会返回RpcResponse信息
    }
}
```

```java
public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    public Object sendRequest(RpcRequest rpcRequest, String host, int port) {
        try (Socket socket = new Socket(host, port)) {//Socket传输,这里能得到服务端返回的数据
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream.writeObject(rpcRequest);
            objectOutputStream.flush();
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.error("调用时有错误发生：", e);
            return null;
        }
    }
}

```

#### 1.1.4服务端实现-反射调用

​	服务端的实现就简单多了，使用一个ServerSocket监听某个端口，循环接收连接请求，如果发来了请求就创建一个线程，在新线程中处理调用。这里创建线程采用线程池.

``` java
public class RpcServer {

    private final ExecutorService threadPool;
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    public RpcServer() {
        int corePoolSize = 5;
        int maximumPoolSize = 50;
        long keepAliveTime = 60;
        BlockingQueue<Runnable> workingQueue = new ArrayBlockingQueue<>(100);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, workingQueue, threadFactory);
    }
  
}
```

​	这里简化了一下，RpcServer暂时只能注册一个接口，即对外提供一个接口的调用服务，添加register方法，在注册完一个服务后立刻开始监听：

```java
    public void register(Object service, int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("服务器正在启动...");
            Socket socket;
            while((socket = serverSocket.accept()) != null) {
                logger.info("客户端连接！Ip为：" + socket.getInetAddress());
                threadPool.execute(new WorkerThread(socket, service));
            }
        } catch (IOException e) {
            logger.error("连接时有错误发生：", e);
        }
    }
```

​	这里向工作线程WorkerThread传入了socket和用于服务端实例service。

​	WorkerThread实现了Runnable接口，用于接收RpcRequest对象，解析并且调用，生成RpcResponse对象并传输回去。

```java
@Slf4j
public class WorkerThread implements Runnable {
    private Socket socket;
    private Object service;
    public WorkerThread(Socket socket, Object service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();//接收从客户端传来的请求信息
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());//获取service的rpcRequest.getMethodName()方法，这里server是helloServer
            Object returnObject = method.invoke(service, rpcRequest.getParameters());//执行rpcRequest.getMethodName()方法
            objectOutputStream.writeObject(RpcResponse.success(returnObject));//把结果传递给客户端
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("调用或发送时有错误发生：", e);
        }
    }
}
```

​	其中，通过class.getMethod方法，传入方法名和方法参数类型即可获得Method对象。如果你上面RpcRequest中使用String数组来存储方法参数类型的话，这里你就需要通过反射生成对应的Class数组了。通过method.invoke方法，传入对象实例和参数，即可调用并且获得返回值。

#### 1.1.5测试

​	服务端侧，我们已经在上面实现了一个HelloService的实现类HelloServiceImpl的实现类了，我们只需要创建一个RpcServer并且把这个实现类注册进去就行了：

```java
public class TestServer {
    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();//服务端存在接口的实现类
        RpcServer rpcServer = new RpcServer();
        rpcServer.register(helloService,9000);
    }
}
```

​	客户端方面，我们需要通过动态代理，生成代理对象，并且调用，动态代理会自动帮我们向服务端发送请求的：

```java
public class TestClient {
    public static void main(String[] args) {
        RpcClientProxy proxy = new RpcClientProxy("127.0.0.1", 9000);//绑定id地址和端口号
        HelloService helloService = proxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);//代理调用hello方法，会执行RpcClientProxy的invoke方法
        System.out.println(res);
    }
}
```

​	首先启动服务端，再启动客户端，服务端输出：

```html
服务器正在启动...
客户端连接！Ip为：127.0.0.1
接收到：This is a message
```

​	客户端输出：

```java
这是调用的返回值，id=12
```

## 2注册多个服务

### 2.1服务注册表

​	我们需要一个容积，该容器用来保存本地服务的信息，并且在获得一个服务名称的时候能够返回这个服务的信息，这里我定义了一个ServiceRegistry接口：

```java
public interface ServiceRegistry {
    <T> void register(T service);//注册服务信息
    Object getService(String serviceName);//获取服务信息
}
```

​	ServiceRegistry接口第一个方法用于注册服务信息，第二个方法用于获取服务信息.

​	服务注册接口定义好了，那么我们还需要定义该接口的具体实现类，提供服务注册等功能，这里我定义了DefaultServiceRegistry类来实现ServiceRegistry接口：

```java
//默认的注册表类
@Slf4j
public class DefaultServiceRegistry implements ServiceRegistry{
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();//一个线程安全的map，保存服务名以及其对应提供服务的对象
    private final Set<String> registeredService = ConcurrentHashMap.newKeySet();//存放已经注册的服务名，注册服务时，默认采用这个对象实现的接口的完整类名作为服务名
    @Override
    /**
     * synchronized 关键字，代表这个方法加锁,相当于不管哪一个线程（例如线程A），
     * 运行到这个方法时,都要检查有没有其它线程B（或者C、 D等）正在用这个方法(或者该类的其他同步方法)，
     * 有的话要等正在使用synchronized方法的线程B（或者C 、D）运行完这个方法后再运行此线程A
     */
    //保证最多只有一个在注册
    public synchronized  <T> void register(T service) {
        String serviceName = service.getClass().getCanonicalName();//返回正常的包含路径的类名，比如java.util.Map;
        if(registeredService.contains(serviceName)) {
            return;//如果已经注册过了，不用重复注册
        }
        registeredService.add(serviceName);//标记为已经注册
        Class<?>[] interfaces = service.getClass().getInterfaces();//获取service实现的接口名，service是接口的实现类
        if(interfaces.length == 0) {//注册的服务未实现接口
            throw new RpcException(RpcError.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
        }
        for(Class<?> i : interfaces) {
            serviceMap.put(i.getCanonicalName(), service);//依次将注册服务实现的接口放入map集合中
        }
        log.info("向接口：{}注册服务：{}",interfaces,serviceName);
    }

    @Override
    public synchronized Object getService(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if(service == null) {
            throw new RpcException(RpcError.SERVICE_NOT_FOUND);
        }
        return service;
    }
}
```

​	我们将服务名与提供服务的对象的对应关系保存在一个 ConcurrentHashMap 中，并且使用一个 Set 来保存当前有哪些对象已经被注册。在注册服务时，默认采用这个对象实现的接口的完整类名作为服务名，例如某个对象 A 实现了接口 X 和 Y，那么将 A 注册进去后，会有两个服务名 X 和 Y 对应于 A 对象。这种处理方式也就说明了某个接口只能有一个对象提供服务。

### 2.2其他处理

​	为了降低耦合度，我们不会把 ServiceRegistry 和某一个 RpcServer 绑定在一起，而是在创建 RpcServer 对象时，传入一个 ServiceRegistry 作为这个服务的注册表。

​	那么RpcServer类就变成了如下：

```java
@Slf4j
public class RpcServer {
    private final ExecutorService threadPool;//创建一个线程池
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 50;
    private static final int KEEP_ALIVE_TIME = 60;
    private static final int BLOCKING_QUEUE_CAPACITY = 100;
    private final ServiceRegistry serviceRegistry;
    private RequestHandler requestHandler = new RequestHandler();
    public RpcServer(ServiceRegistry serviceRegistry) {
        //创建RpcServer类时进行的操作，给类中的serviceRegistry初始化，还会创建一个队列，还会创建一个线程池
        this.serviceRegistry = serviceRegistry;
        //这玩意其实还是一个队列，其特性是在任意时刻只有一个线程可以进行take或者put操作，并且BlockingQueue提供了超时return null的机制
        BlockingQueue<Runnable> workingQueue = new ArrayBlockingQueue<>(BLOCKING_QUEUE_CAPACITY);//有界队列
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        threadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, workingQueue, threadFactory);
    }
    //这里注册这一步交给了ServiceRegistry的实现类DefaultServiceRegistry去完成了，在启动之前就应该完成注册，DefaultServiceRegistry可以注册多个服务
    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("服务器启动……");
            Socket socket;
            while((socket = serverSocket.accept()) != null) {
                log.info("消费者连接: {}:{}", socket.getInetAddress(), socket.getPort());
                threadPool.execute(new RequestHandlerThread(socket, requestHandler, serviceRegistry));
            }
            threadPool.shutdown();
        } catch (IOException e) {
            log.error("服务器启动时有错误发生:", e);
        }
    }
    /**
     *
     * @param service 服务端的实现类 这里是HelloServiceImpl(),这个方法是只能注册一个服务
     * @param port 端口
     */
    public void register(Object service, int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {//serverSocket会监听port端口
            log.info("服务器正在启动...");
            Socket socket;
            while((socket = serverSocket.accept()) != null) {
                //这里会等待客户端发送数据，如果port端口有发送请求，
                // 那么可以得到socket对象，这个socket是客户端的，这样其实就完成了客户端通信
                log.info("客户端连接！Ip为：" + socket.getInetAddress());
                //WorkerThread实现了runnable接口，每一次接收到客户端发过来的信息都会创建一个新的进程
                //[pool-1-thread-1] INFO top.wangyuanfei.test.HelloServiceImpl - 接收到This is a message
                //[pool-1-thread-2] INFO top.wangyuanfei.test.HelloServiceImpl - 接收到This is a message
                threadPool.execute(new WorkerThread(socket, service));//这里的service就是注册的服务类，helloService
            }
        } catch (IOException e) {
            log.error("连接时有错误发生：", e);
        }
    }
}

```

​	在创建 RpcServer 时需要传入一个已经注册好服务的 ServiceRegistry，而原来的 register 方法也被改成了 start 方法，因为服务的注册已经不由 RpcServer 处理了，它只需要启动就行了。

​	而在每一个请求处理线程（RequestHandlerThread）中也就需要传入 ServiceRegistry 了，这里把处理线程和处理逻辑分成了两个类：RequestHandlerThread 只是一个线程，从ServiceRegistry 获取到提供服务的对象后，就会把 RpcRequest 和服务对象直接交给 RequestHandler 去处理，反射等过程被放到了 RequestHandler 里。

​	RequesthandlerThread.java：处理线程，接受对象等.

```java
@Slf4j
public class RequestHandlerThread implements Runnable {//new这个对象后就会自动执行run方法
    private Socket socket;
    private RequestHandler requestHandler;
    private ServiceRegistry serviceRegistry;

    public RequestHandlerThread(Socket socket, RequestHandler requestHandler, ServiceRegistry serviceRegistry) {
        this.socket = socket;
        this.requestHandler = requestHandler;
        this.serviceRegistry = serviceRegistry;
    }
    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();//读取服务端传过来的信息
            String interfaceName = rpcRequest.getInterfaceName();//接口名
            Object service = serviceRegistry.getService(interfaceName);//获取这个接口名对应的服务
            Object result = requestHandler.handle(rpcRequest, service);//处理返回
            objectOutputStream.writeObject(RpcResponse.success(result));
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            log.error("调用或发送时有错误发生：", e);
        }
    }
}
```

​	RequestHandler.java：通过反射进行方法调用.

```java
@Slf4j
public class RequestHandler {
    private static final ServiceProvider serviceProvider;
    static {//静态代码块，调用该对象是最先被执行，并且对次调用静态代码块只执行一次
        //在类加载的时候做一些静态数据初始化的操作，以便后续使用。
        serviceProvider = new ServiceProviderImpl();
    }
    public Object handle(RpcRequest rpcRequest,Object service) {
        Object result = null;
        try {
            result = invokeTargetMethod(rpcRequest, service);
            log.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (Exception e) {
            log.error("调用或发送时有错误发生：", e);
        } return result;
    }

    /**
     *
     * @param rpcRequest 客户端传来的一些参数信息
     * @param service 服务实现类
     * @return
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());//得到对应的方法
            result = method.invoke(service, rpcRequest.getParameters());//执行方法
            log.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return RpcResponse.fail(ResponseCode.METHOD_NOT_FOUND, rpcRequest.getRequestId());
        }
        return result;
    }
}
```

### 2.3测试

#### 2.3.1 服务端测试

```java
public class TestServer {
    public static void main(String[] args) {
        /*HelloService helloService = new HelloServiceImpl();//服务端存在接口的实现类
        RpcServer rpcServer = new RpcServer();
        rpcServer.register(helloService,9000);*/
        HelloService helloService = new HelloServiceImpl();
        AddService addService = new AddServiceImpl();
        ServiceRegistry serviceRegistry = new DefaultServiceRegistry();//注册服务，可以注册多个，这里用的是map存储每个服务名及其对应的服务，kye：服务名，value：对应的服务
        serviceRegistry.register(helloService);//注册一个服务
        serviceRegistry.register(addService);//注册第二个服务
        RpcServer rpcServer = new RpcServer(serviceRegistry);//通过rpc框架进行远程调用
        rpcServer.start(9000);//rpc服务器启动
    }
}
```

#### 2.3.2客户端测试

```java
public class TestClient {
    public static void main(String[] args) {
        RpcClientProxy proxy = new RpcClientProxy("127.0.0.1", 9000);//绑定id地址和端口号
        HelloService helloService = proxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);//代理调用hello方法，会执行RpcClientProxy的invoke方法
        System.out.println(res);
        Integer addRes = helloService.add(10, 20);
        System.out.println(addRes);
        AddService addService = proxy.getProxy(AddService.class);
        Integer addres = addService.add(10, 20);
        System.out.println(addres);
    }
}
```

#### 2.3.3测试结果

```java
服务端测试结果：
[main] INFO top.wangyuanfei.rpc.registry.DefaultServiceRegistry - 向接口：[interface top.wangyuanfei.rpc.api.HelloService]注册服务：top.wangyuanfei.test.HelloServiceImpl
[main] INFO top.wangyuanfei.rpc.registry.DefaultServiceRegistry - 向接口：[interface top.wangyuanfei.rpc.api.AddService]注册服务：top.wangyuanfei.test.AddServiceImpl
[main] INFO top.wangyuanfei.rpc.transort.RpcServer - 服务器启动……
[main] INFO top.wangyuanfei.rpc.transort.RpcServer - 消费者连接: /127.0.0.1:28982
[pool-1-thread-1] INFO top.wangyuanfei.test.HelloServiceImpl - 接收到This is a message
[pool-1-thread-1] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.HelloService 成功调用方法:hello
[pool-1-thread-1] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.HelloService 成功调用方法:hello
[main] INFO top.wangyuanfei.rpc.transort.RpcServer - 消费者连接: /127.0.0.1:28985
[pool-1-thread-2] INFO top.wangyuanfei.test.HelloServiceImpl - 远程调用helloService的add方法
[pool-1-thread-2] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.HelloService 成功调用方法:add
[pool-1-thread-2] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.HelloService 成功调用方法:add
[main] INFO top.wangyuanfei.rpc.transort.RpcServer - 消费者连接: /127.0.0.1:28986
[pool-1-thread-3] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.AddService 成功调用方法:add
[pool-1-thread-3] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.AddService 成功调用方法:add

客户端测试结果
这是调用的返回值,id=12
30
30
```

## 3Netty传输和通用序列化接口

### 3.1Netty服务端与客户端

​	为了保证通用性，我们可以把 Server 和 Client 抽象成两个接口，分别是 RpcServer 和 RpcClient：

```java
public interface RpcServer {
    void start(int port);//服务端启动服务
}
public interface RpcClient {
    Object sendRequest(RpcRequest rpcRequest);//客户端发送请求
}
```

​	我们的任务，就是要实现 NettyServer 和 NettyClient。这里提一个改动，就是在 DefaultServiceRegistry.java 中，将包含注册信息的 serviceMap 和 registeredService 都改成了 static ，这样就能保证全局唯一的注册信息，并且在创建 RpcServer 时也就不需要传入了。

​	NettyServer的实现很传统：

```java
@Slf4j
public class NettyServer implements RpcServer {
    @Override
    public void start(int port) {
        /**
         * 在创建ServerBootstrap类实例前，先创建两个EventLoopGroup，
         * 它们实际上是两个独立的Reactor线程池，bossGroup负责接收客户端的连接，
         * workerGroup负责处理IO相关的读写操作，或者执行系统task、定时task等。
         */
        // 1. BossGroup 线程池 : 负责客户端的连接
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 2. WorkerGroup 线程池 : 负责客户端连接的数据读写
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        // 3. 服务器启动对象, 需要为该对象配置各种参数
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();//引导服务端ServerBootstrap
            serverBootstrap.group(bossGroup, workerGroup) // 设置 主从 线程组 , 分别对应 主 Reactor 和 从 Reactor
                    .channel(NioServerSocketChannel.class)// 设置 NIO 网络套接字通道类型
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 256)// 设置线程队列维护的连接个数
                    .option(ChannelOption.SO_KEEPALIVE, true)//接状态行为, 保持连接状态
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {// 创建通道初始化对象
                            // 该方法在服务器与客户端连接建立成功后会回调
                            // 为 管道 Pipeline 设置处理器 Hanedler
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new CommonEncoder(new JsonSerializer()));
                            pipeline.addLast(new CommonDecoder());
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });
            /**
             * 服务器端的 ServerBootstrap 对象 ,
             * 调用 bind 方法 , 绑定本地的端口号 ,
             * 然后监听该端口的客户端连接请求 ;
             */
            ChannelFuture future = serverBootstrap.bind(port).sync();
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            log.error("启动服务器时有错误发生: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

​	Netty 中有一个很重要的设计模式——责任链模式，责任链上有多个处理器，每个处理器都会对数据进行加工，并将处理后的数据传给下一个处理器。代码中的 CommonEncoder、CommonDecoder和NettyServerHandler 分别就是编码器，解码器和数据处理器。因为数据从外部传入时需要解码，而传出时需要编码，类似计算机网络的分层模型，每一层向下层传递数据时都要加上该层的信息，而向上层传递时则需要对本层信息进行解码。

​	NettyServerHandler 和 NettyClientHandler 都分别位于服务器端和客户端责任链的尾部，直接和 RpcServer 对象或 RpcClient 对象打交道，而无需关心字节序列的情况。

```java
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static RequestHandler requestHandler;
    private static ServiceRegistry serviceRegistry;

    static {
        requestHandler = new RequestHandler();
        serviceRegistry = new DefaultServiceRegistry();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        try {
            log.info("服务器接收到请求: {}", msg);
            String interfaceName = msg.getInterfaceName();
            Object service = serviceRegistry.getService(interfaceName);
            Object result = requestHandler.handle(msg, service);
            ChannelFuture future = ctx.writeAndFlush(RpcResponse.success(result));
            future.addListener(ChannelFutureListener.CLOSE);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理过程调用时有错误发生:");
        cause.printStackTrace();
        ctx.close();
    }
}
```

​	NettyClient 的实现也很类似：

​	在静态代码块中就直接配置好了 Netty 客户端，等待发送数据时启动，channel 将 RpcRequest 对象写出，并且等待服务端返回的结果。注意这里的发送是非阻塞的，所以发送后会立刻返回，而无法得到结果。这里通过 `AttributeKey` 的方式阻塞获得返回结果：

```java
AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse");
RpcResponse rpcResponse = channel.attr(key).get();
```

​	通过这种方式获得全局可见的返回结果，在获得返回结果 RpcResponse 后，将这个对象以 key 为 rpcResponse 放入 ChannelHandlerContext 中，这里就可以立刻获得结果并返回，我们会在 `NettyClientHandler` 中看到放入的过程。

```java
@Slf4j
@Getter
@NoArgsConstructor
public class NettyClient implements RpcClient {
    private String host;
    private int port;
    private static final Bootstrap bootstrap;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    static {
        /**
         * 在创建ServerBootstrap类实例前，先创建两个EventLoopGroup，
         * 它们实际上是两个独立的Reactor线程池，bossGroup负责接收客户端的连接，
         * workerGroup负责处理IO相关的读写操作，或者执行系统task、定时task等。
         */
        //客户端只需要一个 时间循环组 , 即 NioEventLoopGroup 线程池
        EventLoopGroup group = new NioEventLoopGroup();//new NioEventLoopGroup()是bossGroup负责接收客户端的连接
        //在netty中有两种Bootstrap：客户端的Bootstrap和服务器端的ServerBootstrap。
        //配置 Netty 服务器 / 客户端的各种配置 ;关联各种组件
        bootstrap = new Bootstrap();//引导类，Bootstrap指的是引导程序，通过Bootstrap可以轻松构建和启动程序
        //设置相关参数
        bootstrap.group(group) //设置客户端线程池
                .channel(NioSocketChannel.class) //设置客户端网络套字节通道类型
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {//设置客户端的线程池对应的 NioEventLoop 设置对应的事件处理器 Handler
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 该方法在服务器与客户端连接建立成功后会回调
                        // 为 管道 Pipeline 设置处理器 Hanedler
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new CommonDecoder())
                                .addLast(new CommonEncoder(new JsonSerializer()))
                                .addLast(new NettyClientHandler());
                    }
                });
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) {
        try {
            // 开始连接服务器, 并进行同步操作
            // ChannelFuture 类分析 , Netty 异步模型
            // sync 作用是该方法不会再次阻塞
            ChannelFuture future = bootstrap.connect(host, port).sync();
            log.info("客户端连接到服务器 {}:{}", host, port);
            Channel channel = future.channel();
            if(channel != null) {
                channel.writeAndFlush(rpcRequest).addListener(future1 -> {//客户端把请求信息rpcRequest发送出去
                    if(future1.isSuccess()) {
                        log.info(String.format("客户端发送消息: %s", rpcRequest.toString()));
                    } else {
                        log.error("发送消息时有错误发生: ", future1.cause());
                    }
                });
                channel.closeFuture().sync();// 连接成功后就可以关闭通道, 开始监听了
                AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse");
                RpcResponse rpcResponse = channel.attr(key).get();
                return rpcResponse;//虽然看不懂，但是就是netty的远程调用返回值
            }
        } catch (InterruptedException e) {
            log.error("发送消息时有错误发生: ", e);
        }
        return null;
    }
}

```

```java
@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        try {
            log.info(String.format("客户端接收到消息: %s", msg));//这里得到服务端的调用返回值
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse");
            ctx.channel().attr(key).set(msg);
            ctx.channel().close();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("过程调用时有错误发生:");
        cause.printStackTrace();
        ctx.close();
    }
}
```

### 3.2测试

​	NettyTestServer 如下：

```java
public class TestServer {
    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        ServiceRegistry registry = new DefaultServiceRegistry();
        registry.register(helloService);//注册服务
        NettyServer server = new NettyServer();
        server.start(9999);
    }
}
```

​	NettyTestClient如下：

```java
public class TestClient {
    public static void main(String[] args) {
        RpcClient client = new NettyClient("127.0.0.1", 9999);//创建NettyClient客户端，并且定义ip地址和端口号
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);//创建rpcClientProxy动态代理类
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);//得到helloService的动态代理类
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);//远程调用helloService的hello方法
        System.out.println(res);
    }
}
```

 测试结果：

```java
服务端：
[main] INFO top.wangyuanfei.rpc.registry.DefaultServiceRegistry - 向接口：[interface top.wangyuanfei.rpc.api.HelloService]注册服务：top.wangyuanfei.test.HelloServiceImpl
[main] WARN io.netty.bootstrap.ServerBootstrap - Unknown channel option 'SO_KEEPALIVE' for channel '[id: 0x1b784c0c]'
[nioEventLoopGroup-2-1] INFO io.netty.handler.logging.LoggingHandler - [id: 0x1b784c0c] REGISTERED
[nioEventLoopGroup-2-1] INFO io.netty.handler.logging.LoggingHandler - [id: 0x1b784c0c] BIND: 0.0.0.0/0.0.0.0:9999
[nioEventLoopGroup-2-1] INFO io.netty.handler.logging.LoggingHandler - [id: 0x1b784c0c, L:/0:0:0:0:0:0:0:0:9999] ACTIVE
[nioEventLoopGroup-2-1] INFO io.netty.handler.logging.LoggingHandler - [id: 0x1b784c0c, L:/0:0:0:0:0:0:0:0:9999] READ: [id: 0xd1d383aa, L:/127.0.0.1:9999 - R:/127.0.0.1:36672]
[nioEventLoopGroup-2-1] INFO io.netty.handler.logging.LoggingHandler - [id: 0x1b784c0c, L:/0:0:0:0:0:0:0:0:9999] READ COMPLETE
[nioEventLoopGroup-3-1] INFO top.wangyuanfei.rpc.transort.netty.server.NettyServerHandler - 服务器接收到请求: RpcRequest(requestId=c7d159b4-6bf4-4032-b123-6d592042577e, interfaceName=top.wangyuanfei.rpc.api.HelloService, methodName=hello, parameters=[HelloObject(id=12, message=This is a message)], paramTypes=[class top.wangyuanfei.rpc.api.HelloObject])
[nioEventLoopGroup-3-1] INFO top.wangyuanfei.test.HelloServiceImpl - 接收到This is a message
[nioEventLoopGroup-3-1] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.HelloService 成功调用方法:hello
[nioEventLoopGroup-3-1] INFO top.wangyuanfei.rpc.handler.RequestHandler - 服务:top.wangyuanfei.rpc.api.HelloService 成功调用方法:hello

客户端：
[main] INFO top.wangyuanfei.rpc.transort.netty.client.NettyClient - 客户端连接到服务器 127.0.0.1:9999
[nioEventLoopGroup-2-1] INFO top.wangyuanfei.rpc.transort.netty.client.NettyClient - 客户端发送消息: RpcRequest(requestId=c7d159b4-6bf4-4032-b123-6d592042577e, interfaceName=top.wangyuanfei.rpc.api.HelloService, methodName=hello, parameters=[HelloObject(id=12, message=This is a message)], paramTypes=[class top.wangyuanfei.rpc.api.HelloObject])
[nioEventLoopGroup-2-1] INFO top.wangyuanfei.rpc.transort.netty.client.NettyClientHandler - 客户端接收到消息: RpcResponse(requestId=null, statusCode=200, message=null, data=这是调用的返回值,id=12)
这是调用的返回值,id=12
```

## 4注解开发

​	客户端不变，服务端使用注解更加方便的进行注册.

​	定义两个注解类，Service：表示一个服务提供类，用于远程接口的实现；ServiceScan：表示一个包扫描类，这个需要加在主启动类上，开启包扫描，就是扫描包里的类需不需要注册.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    public String name() default "";//默认服务名
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceScan {
    public String value() default "";//默认可扫描包的地址
}
```

​	服务端代码：

```java
@ServiceScan
public class NettyTestServer {
    public static void main(String[] args) {
        /*HelloService helloService = new HelloServiceImpl();
        NettyServer server = new NettyServer("192.168.1.141", 9999, CommonSerializer.DEFAULT_SERIALIZER);
        server.publishService(helloService,helloService.getClass().getInterfaces()[0].getCanonicalName());*/
        NettyServer server = new NettyServer("127.0.0.1", 9999, CommonSerializer.DEFAULT_SERIALIZER);
        server.start();
    }
}
```

```java
public NettyServer(String host, int port,Integer serializerId) {
            this.host = host;
            this.port = port;
            nacosServerRegister = new NacosServerRegisterImpl ();
            serviceProvider = new ServiceProviderImpl();
            this.serializer = CommonSerializer.getByCode(serializerId);
            scanServices();
        }
```

```java
 public void scanServices() {
        String mainClassName = ReflectUtil.getStackTrace();
        Class<?> startClass;
        try {
            startClass = Class.forName(mainClassName);//获取主方法的类：top.wangyuanfei.test.NettyTestServer
            if(!startClass.isAnnotationPresent(ServiceScan.class)) {
                log.error("启动类缺少 @ServiceScan 注解");
                throw new RpcException(RpcError.SERVICE_SCAN_PACKAGE_NOT_FOUND);
            }
        } catch (ClassNotFoundException e) {
            log.error("出现未知错误");
            throw new RpcException(RpcError.UNKNOWN_ERROR);
        }
        String basePackage = startClass.getAnnotation(ServiceScan.class).value();
        if("".equals(basePackage)) {
            basePackage = mainClassName.substring(0, mainClassName.lastIndexOf("."));//如果没有设置默认扫描包，那么把主程序所在的包作为扫描包
        }
        Set<Class<?>> classSet = ReflectUtil.getClasses(basePackage);//获取当前扫描包下的所有类，即这个包top.wangyuanfei.test下的所有的类
        //top.wangyuanfei.test.AddServiceImpl top.wangyuanfei.test.HelloServiceImpl top.wangyuanfei.test.NettyTestServer top.wangyuanfei.test.TestServer
        for(Class<?> clazz : classSet) {
            if(clazz.isAnnotationPresent(Service.class)) {//包含这个Service注解
                String serviceName = clazz.getAnnotation(Service.class).name();//得到默认的服务名
                Object obj;
                try {
                    obj = clazz.newInstance();//创建一个实例
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("创建 " + clazz + " 时有错误发生");
                    continue;
                }
                if("".equals(serviceName)) {
                    Class<?>[] interfaces = clazz.getInterfaces();//得到这个实现类的接口class
                    for (Class<?> oneInterface: interfaces){
                        publishService(obj, oneInterface.getCanonicalName());//注册这个服务
                    }
                } else {
                    publishService(obj, serviceName);
                }
            }
        }
    }
```

```java
public static String getStackTrace() {
        /**
         * stack[0]:top.wangyuanfei.rpc.util.ReflectUtil
         * stack[1]:top.wangyuanfei.rpc.transort.AbstractRpcServer
         * stack[2]:top.wangyuanfei.rpc.transort.netty.server.NettyServer
         * stack[3]:top.wangyuanfei.test.NettyTestServer
         */
        StackTraceElement[] stack = new Throwable().getStackTrace();//获取当前调用的类的栈数组
        return stack[stack.length - 1].getClassName();//获取主方法名称：top.wangyuanfei.test.NettyTestServer
    }
```





