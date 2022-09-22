package top.wangyuanfei.test;

import top.wangyuanfei.rpc.api.HelloObject;
import top.wangyuanfei.rpc.api.HelloService;
import top.wangyuanfei.rpc.serializer.CommonSerializer;
import top.wangyuanfei.rpc.transort.RpcClient;
import top.wangyuanfei.rpc.transort.RpcClientProxy;
import top.wangyuanfei.rpc.transort.netty.client.NettyClient;

public class NettyTestClient {
    public static void main(String[] args) {
       /* RpcClient client = new NettyClient("127.0.0.1", 9999);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);
        System.out.println(res);*/
        RpcClient client = new NettyClient(CommonSerializer.DEFAULT_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);
        helloService.hello(object);
        System.out.println(res);
    }
}
