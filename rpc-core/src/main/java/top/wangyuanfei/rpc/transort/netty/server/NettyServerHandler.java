package top.wangyuanfei.rpc.transort.netty.server;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.entity.RpcResponse;
import top.wangyuanfei.rpc.handler.RequestHandler;
import top.wangyuanfei.rpc.registry.DefaultServiceRegistry;
import top.wangyuanfei.rpc.registry.NacosServerRegister;
import top.wangyuanfei.rpc.registry.NacosServerRegisterImpl;
import top.wangyuanfei.rpc.registry.ServiceRegister;

@Slf4j
//接收客户端发送过来的信号并响应
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private static RequestHandler requestHandler;
    private static ServiceRegister serviceRegister;

    static {
        requestHandler = new RequestHandler();
        serviceRegister = new DefaultServiceRegistry();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        try {
            log.info("服务器接收到请求: {}", msg);
            String interfaceName = msg.getInterfaceName();//接口名字
            System.out.println(interfaceName);

            System.out.println();
            Object service = serviceRegister.getService(interfaceName);//服务类，比如这里的helloServerImpl
            Object result = requestHandler.handle(msg, service);//执行helloServer中的方法
            ChannelFuture future = ctx.writeAndFlush(RpcResponse.success(result));//调用方法成功，res就是调用的方法的执行结果，把结果写入channel里面
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
