package top.wangyuanfei.rpc.transort;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.entity.RpcResponse;
import top.wangyuanfei.rpc.transort.netty.client.NettyClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Rpc客户端动态代理
 * 我们需要传递host和port来指明服务端的位置。并且使用getProxy()方法来生成代理对象
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {
    /*private String host;
    private int port;*/
    private final RpcClient client;//rpcClient是一个接口，这里进来的其实是RpcClient的实现类，例如NettyClient
    /*public RpcClientProxy(String host,int port){
        this.port = port;
        this.host = host;
    }*/
    public RpcClientProxy(RpcClient client){
        this.client = client;
    }
    public <T> T getProxy(Class<T> clazz) {
        /**
         * ClassLoader loader 目标对象的类加载器：负责向内存中加载对象的
         * Class<?>[] interfaces :接口，目标对象实现的接口，也是通过反射获取
         * InvocationHandler h:我们自己写的，代理类要完成的功能
         */
        //返回值：代理对象
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);//得到一个代理对象
    }

    //InvocationHandler接口需要实现invoke()方法，来指明代理对象的方法被调用时的动作。在这里，我们显然就需要生成一个RpcRequest对象，发送出去，然后返回从服务端接收到的结果即可：
    @Override
    /**
     * proxy jdk创建的代理对象
     * method 目标类中的方法，jdk提供
     * args 目标类中方法参数
     */
   /* public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {//代理对象要执行的功能代码
        *//**
         * method.getDeclaringClass().getName():获得调用的接口名字
         * method.getName()：获得调用的方法名字
         * args：调用方法的参数
         * method.getParameterTypes()：调用方法的参数类型 这个例子的method就是.hello方法
         *//*
        //method.getDeclaringClass()：通俗来说就是得到定义该方法的类，即HelloService.Class
        RpcRequest request = new RpcRequest(UUID.randomUUID().toString(),method.getDeclaringClass().getName(),
                method.getName(), args, method.getParameterTypes());
        RpcClient rpcClient = new RpcClient();
        RpcResponse response =(RpcResponse) rpcClient.sendRequest(request,host,port);
        return response.getData();
    }*/
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {//代理对象要执行的功能代码
        /**
         * method.getDeclaringClass().getName():获得调用的接口名字 top.wangyuanfei.rpc.api.HelloService
         * method.getName()：获得调用的方法名字 hello
         * args：调用方法的参数 [HelloObject(id=12, message=This is a message)]
         * method.getParameterTypes()：调用方法的参数类型 这个例子的method就是.hello方法 class top.wangyuanfei.rpc.api.HelloObject
         */
        //method.getDeclaringClass()：通俗来说就是得到定义该方法的类，即HelloService.Class
        RpcRequest request = new RpcRequest(UUID.randomUUID().toString(),method.getDeclaringClass().getName(),
                method.getName(), args, method.getParameterTypes());

        /*RpcClient rpcClient = new NettyClient("127.0.0.1",9999);

        RpcResponse response =(RpcResponse) rpcClient.sendRequest(request);*/
        RpcResponse response = (RpcResponse) client.sendRequest(request);//发送请求
        return response.getData();
    }
}
