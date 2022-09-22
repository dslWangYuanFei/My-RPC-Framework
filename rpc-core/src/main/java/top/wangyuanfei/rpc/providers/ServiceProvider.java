package top.wangyuanfei.rpc.providers;

public interface ServiceProvider {
    <T> void addServiceProvider(T service, String serviceName);

    Object getServiceProvider(String serviceName);
}
