package top.wangyuanfei.test;

import top.wangyuanfei.rpc.api.AddService;
import top.wangyuanfei.rpc.api.HelloService;
import top.wangyuanfei.rpc.registry.DefaultServiceRegistry;
import top.wangyuanfei.rpc.registry.ServiceRegister;
import top.wangyuanfei.rpc.serializer.CommonSerializer;
import top.wangyuanfei.rpc.transort.netty.server.NettyServer;

public class TestServer {
    public static void main(String[] args) {
        /*HelloService helloService = new HelloServiceImpl();//服务端存在接口的实现类
        RpcServer rpcServer = new RpcServer();
        rpcServer.register(helloService,9000);*/

        /*HelloService helloService = new HelloServiceImpl();
        AddService addService = new AddServiceImpl();
        ServiceRegistry serviceRegistry = new DefaultServiceRegistry();//注册服务，可以注册多个，这里用的是map存储每个服务名及其对应的服务，kye：服务名，value：对应的服务
        serviceRegistry.register(helloService);//注册一个服务
        serviceRegistry.register(addService);//注册第二个服务
        RpcServer rpcServer = new RpcServer(serviceRegistry);//通过rpc框架进行远程调用
        rpcServer.start(9000);//rpc服务器启动*/

        /*HelloService helloService = new HelloServiceImpl();
        ServiceRegister registry = new DefaultServiceRegistry();
        registry.register(helloService);//注册服务,这里是把服务注册进了一个静态serviceMap中，全局共用一份
        NettyServer server = new NettyServer();//创建一个NettyServer服务类
        server.start(9999);//启动服务类*/
            /*HelloService helloService = new HelloServiceImpl();
            NettyServer server = new NettyServer("192.168.1.141", 8888, CommonSerializer.DEFAULT_SERIALIZER);
            server.publishService(helloService,helloService.getClass().getInterfaces()[0]);*/


    }
}
