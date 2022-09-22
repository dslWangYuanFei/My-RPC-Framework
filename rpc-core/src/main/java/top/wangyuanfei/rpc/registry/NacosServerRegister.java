package top.wangyuanfei.rpc.registry;

import java.net.InetSocketAddress;

//远程注册表
public interface NacosServerRegister {
    void register(String serviceName, InetSocketAddress inetSocketAddress);//把服务名和注册地址注册进服务中心
    InetSocketAddress lookUpService(String serviceName);//根据服务名从注册中心获取到一个服务提供者的地址
}
