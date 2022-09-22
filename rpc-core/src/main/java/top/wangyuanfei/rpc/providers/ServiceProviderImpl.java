package top.wangyuanfei.rpc.providers;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.enumeration.RpcError;
import top.wangyuanfei.rpc.exception.RpcException;
import top.wangyuanfei.rpc.registry.DefaultServiceRegistry;

import java.util.Map;
import java.util.Set;

@Slf4j
public class ServiceProviderImpl implements ServiceProvider {
    private static final Map<String, Object> serviceMap = DefaultServiceRegistry.getServiceMap();
    private  static final Set<String> registeredService = DefaultServiceRegistry.getRegisteredService();

    @Override
    public <T> void addServiceProvider(T service, String serviceName) {
        if (registeredService.contains(serviceName)) return;
        registeredService.add(serviceName);
        serviceMap.put(serviceName, service);
        log.info("向接口: {} 注册服务: {}", service.getClass().getInterfaces(), serviceName);
    }

    @Override
    public Object getServiceProvider(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if (service == null) {
            throw new RpcException(RpcError.SERVICE_NOT_FOUND);
        }
        return service;
    }
}
