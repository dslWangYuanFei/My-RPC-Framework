package top.wangyuanfei.test;

import top.wangyuanfei.rpc.api.AddService;
import top.wangyuanfei.rpc.api.HelloObject;
import top.wangyuanfei.rpc.api.HelloService;
import top.wangyuanfei.rpc.serializer.CommonSerializer;
import top.wangyuanfei.rpc.transort.RpcClient;
import top.wangyuanfei.rpc.transort.RpcClientProxy;
import top.wangyuanfei.rpc.transort.netty.client.NettyClient;

public class TestClient {
    public static void main(String[] args) {
        /*RpcClientProxy proxy = new RpcClientProxy("127.0.0.1", 9000);//绑定id地址和端口号
        HelloService helloService = proxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);//代理调用hello方法，会执行RpcClientProxy的invoke方法
        System.out.println(res);
        Integer addRes = helloService.add(10, 20);
        System.out.println(addRes);
        AddService addService = proxy.getProxy(AddService.class);
        Integer addres = addService.add(10, 20);
        System.out.println(addres);*/
        RpcClient client = new NettyClient(CommonSerializer.DEFAULT_SERIALIZER);//创建NettyClient客户端，并且定义ip地址和端口号
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);//创建rpcClientProxy动态代理类
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);//得到helloService的动态代理类
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);//远程调用helloService的hello方法
        System.out.println(res);
    }
}
