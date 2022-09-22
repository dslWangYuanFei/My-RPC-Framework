package top.wangyuanfei.rpc.transort.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import top.wangyuanfei.rpc.codec.CommonDecoder;
import top.wangyuanfei.rpc.codec.CommonEncoder;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.entity.RpcResponse;
import top.wangyuanfei.rpc.enumeration.RpcError;
import top.wangyuanfei.rpc.exception.RpcException;
import top.wangyuanfei.rpc.registry.NacosServerRegister;
import top.wangyuanfei.rpc.registry.NacosServerRegisterImpl;
import top.wangyuanfei.rpc.serializer.CommonSerializer;
import top.wangyuanfei.rpc.serializer.JsonSerializer;
import top.wangyuanfei.rpc.serializer.KryoSerializer;
import top.wangyuanfei.rpc.transort.RpcClient;
import top.wangyuanfei.rpc.util.NacosUtil;

import java.net.InetSocketAddress;


@Slf4j
@Getter
@NoArgsConstructor
public class NettyClient implements RpcClient {
    private String host;
    private int port;
    //private static final Bootstrap bootstrap;
    private  CommonSerializer serializer;
    private static NacosServerRegister nacosServerRegister = new NacosServerRegisterImpl();
    public NettyClient(Integer serializerId) {
        this.serializer = CommonSerializer.getByCode(serializerId);
    }
    public NettyClient(String host,Integer port) {
        this.host = host;
        this.port = port;
    }
    /*static {
        *//**
         * 在创建ServerBootstrap类实例前，先创建两个EventLoopGroup，
         * 它们实际上是两个独立的Reactor线程池，bossGroup负责接收客户端的连接，
         * workerGroup负责处理IO相关的读写操作，或者执行系统task、定时task等。
         *//*
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
                        pipeline.addLast(new CommonDecoder()) //解码
                                .addLast(new CommonEncoder(new KryoSerializer())) //编码
                                .addLast(new NettyClientHandler());
                    }
                });
    }*/
    /*@Override
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
                RpcResponse rpcResponse = channel.attr(key).get();//通过key获取value
                return rpcResponse;//虽然看不懂，但是就是netty的远程调用返回值
            }
        } catch (InterruptedException e) {
            log.error("发送消息时有错误发生: ", e);
        }
        return null;
    }*/

    @Override
    public Object sendRequest(RpcRequest rpcRequest) {
        if(serializer == null){
            log.error("未设置序列化器");
            throw new RpcException(RpcError.SERIALIZER_NOT_FOUND);
        }
        try{
            InetSocketAddress inetSocketAddress = nacosServerRegister.lookUpService(rpcRequest.getInterfaceName());
            log.info("端口：{}",inetSocketAddress.getPort());
            //InetSocketAddress inetSocketAddress = NacosUtil.lookUpService(rpcRequest.getInterfaceName());
            Channel channel = ChannelProvider.get(inetSocketAddress);
            //ChannelFuture future = bootstrap.connect(inetSocketAddress.getHostName(), inetSocketAddress.getPort()).sync();
            //Channel channel = future.channel();
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
                RpcResponse rpcResponse = channel.attr(key).get();//通过key获取value
                return rpcResponse;//虽然看不懂，但是就是netty的远程调用返回值
            }
        }catch (InterruptedException e){
            log.error("发送消息时有错误发生: ", e);
        }
        return null;
    }
}
