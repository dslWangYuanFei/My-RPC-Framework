package top.wangyuanfei.rpc.transort.netty.server;

import com.alibaba.nacos.api.exception.NacosException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.codec.CommonDecoder;
import top.wangyuanfei.rpc.codec.CommonEncoder;
import top.wangyuanfei.rpc.enumeration.RpcError;
import top.wangyuanfei.rpc.exception.RpcException;
import top.wangyuanfei.rpc.hook.ShutdownHook;
import top.wangyuanfei.rpc.providers.ServiceProvider;
import top.wangyuanfei.rpc.providers.ServiceProviderImpl;
import top.wangyuanfei.rpc.registry.NacosServerRegister;
import top.wangyuanfei.rpc.registry.NacosServerRegisterImpl;
import top.wangyuanfei.rpc.serializer.CommonSerializer;
import top.wangyuanfei.rpc.serializer.JsonSerializer;
import top.wangyuanfei.rpc.serializer.KryoSerializer;
import top.wangyuanfei.rpc.transort.AbstractRpcServer;
import top.wangyuanfei.rpc.transort.RpcServer;
import top.wangyuanfei.rpc.util.NacosUtil;

import java.net.InetSocketAddress;

@Slf4j
public class NettyServer extends AbstractRpcServer {
    // NettyServer 的实现为例，NettyServer 在创建时需要创建一个 NacosServiceRegistry
    private String host;
    private int port;
    private NacosServerRegister nacosServerRegister;
    private ServiceProvider serviceProvider;
    private CommonSerializer serializer;
    public NettyServer(String host, int port,Integer serializerId) {
        this.host = host;
        this.port = port;
        nacosServerRegister = new NacosServerRegisterImpl ();
        serviceProvider = new ServiceProviderImpl();
        this.serializer = CommonSerializer.getByCode(serializerId);
        scanServices();
    }
    @Override
    public void start() {
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
                            pipeline.addLast(new CommonEncoder(CommonSerializer.getByCode(CommonSerializer.DEFAULT_SERIALIZER)));
                            pipeline.addLast(new CommonDecoder());
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });
            /**
             * 服务器端的 ServerBootstrap 对象 ,
             * 调用 bind 方法 , 绑定本地的端口号 ,
             * 然后监听该端口的客户端连接请求 ;
             */
            ChannelFuture future = serverBootstrap.bind(port).sync();//监听port端口，这里一旦监听到port的请求，那么就会调用NettyServerHandler()方法
            ShutdownHook.getShutdownHook().addClearAllHook();//在服务启动之前调用addClearAllHook方法，注册钩子
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            log.error("启动服务器时有错误发生: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * 向nacos注册服务
     * @param service
     * @param serviceName
     * @param <T>
     */
    @Override
    public <T> void publishService(Object service, String serviceName) {
        if(serializer==null){
            log.error("未设置序列化器");
            throw new RpcException(RpcError.SERIALIZER_NOT_FOUND);
        }
        //serviceClass.getCanonicalName()获取当前接口的路径名，例如top.wangyuanfei.rpc.transort.RpcServer;
        serviceProvider.addServiceProvider(service,serviceName);
        //nacosServerRegister.register(serviceClass.getCanonicalName(), new InetSocketAddress(host, port));
        try {
            NacosUtil.registerService(serviceName,new InetSocketAddress(host,port));
        } catch (NacosException e) {
            log.error("注册服务失败");
            throw new RpcException(RpcError.REGISTER_SERVICE_FAILED);
        }
        start();//启动服务
    }
}
