package top.wangyuanfei.rpc.registry;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.enumeration.RpcError;
import top.wangyuanfei.rpc.exception.RpcException;
import top.wangyuanfei.rpc.providers.ServiceProviderImpl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//默认的注册表类
@Slf4j
public class DefaultServiceRegistry implements ServiceRegister{
    private static final Map<String, Object> serviceMap = new ConcurrentHashMap<>();//一个线程安全的map，保存服务名以及其对应提供服务的对象
    private static final Set<String> registeredService = ConcurrentHashMap.newKeySet();//存放已经注册的服务名，注册服务时，默认采用这个对象实现的接口的完整类名作为服务名

    public static Map<String, Object> getServiceMap() {
        return serviceMap;
    }

    public static Set<String> getRegisteredService() {
        return registeredService;
    }

    @Override
    /**
     * synchronized 关键字，代表这个方法加锁,相当于不管哪一个线程（例如线程A），
     * 运行到这个方法时,都要检查有没有其它线程B（或者C、 D等）正在用这个方法(或者该类的其他同步方法)，
     * 有的话要等正在使用synchronized方法的线程B（或者C 、D）运行完这个方法后再运行此线程A
     */
    //保证最多只有一个在注册
    public synchronized  <T> void register(T service) {
        String serviceName = service.getClass().getCanonicalName();//返回正常的包含路径的类名，比如java.util.Map;
        if(registeredService.contains(serviceName)) {
            return;//如果已经注册过了，不用重复注册
        }
        registeredService.add(serviceName);//标记为已经注册
        Class<?>[] interfaces = service.getClass().getInterfaces();//获取service实现的接口名，service是接口的实现类
        if(interfaces.length == 0) {//注册的服务未实现接口
            throw new RpcException(RpcError.SERVICE_NOT_IMPLEMENT_ANY_INTERFACE);
        }
        for(Class<?> i : interfaces) {
            serviceMap.put(i.getCanonicalName(), service);//依次将注册服务实现的接口放入map集合中
        }
        log.info("向接口：{}注册服务：{}",interfaces,serviceName);
    }

    @Override
    public synchronized Object getService(String serviceName) {
        Object service = serviceMap.get(serviceName);
        if(service == null) {
            throw new RpcException(RpcError.SERVICE_NOT_FOUND);
        }
        return service;
    }
}
