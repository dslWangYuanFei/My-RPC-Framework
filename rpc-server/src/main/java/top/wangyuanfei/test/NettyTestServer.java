package top.wangyuanfei.test;

import top.wangyuanfei.rpc.annotation.ServiceScan;
import top.wangyuanfei.rpc.api.HelloService;
import top.wangyuanfei.rpc.serializer.CommonSerializer;
import top.wangyuanfei.rpc.transort.netty.server.NettyServer;
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
