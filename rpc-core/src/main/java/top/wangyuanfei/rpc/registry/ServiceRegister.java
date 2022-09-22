package top.wangyuanfei.rpc.registry;

public interface ServiceRegister {
    <T> void register(T service);//注册服务信息
    Object getService(String serviceName);//获取服务信息
}
