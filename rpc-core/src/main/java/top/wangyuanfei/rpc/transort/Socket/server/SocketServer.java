package top.wangyuanfei.rpc.transort.Socket.server;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.handler.RequestHandler;
import top.wangyuanfei.rpc.registry.ServiceRegister;
import top.wangyuanfei.rpc.transort.WorkerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

@Slf4j
public class SocketServer {
    private final ExecutorService threadPool;//创建一个线程池
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 50;
    private static final int KEEP_ALIVE_TIME = 60;
    private static final int BLOCKING_QUEUE_CAPACITY = 100;
    private final ServiceRegister serviceRegister;
    private RequestHandler requestHandler = new RequestHandler();
    public SocketServer(ServiceRegister serviceRegister) {
        //创建RpcServer类时进行的操作，给类中的serviceRegistry初始化，还会创建一个队列，还会创建一个线程池
        this.serviceRegister = serviceRegister;
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
                threadPool.execute(new SocketRequestHandlerThread(socket, requestHandler, serviceRegister));
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
