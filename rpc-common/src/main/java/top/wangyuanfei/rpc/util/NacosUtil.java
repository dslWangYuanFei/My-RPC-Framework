package top.wangyuanfei.rpc.util;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.enumeration.RpcError;
import top.wangyuanfei.rpc.exception.RpcException;
import top.wangyuanfei.rpc.loadbalancer.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 管理nacos连接等工具类
 */
@Slf4j
public class NacosUtil {
    private static final Set<String> serviceNames = new HashSet<>();
    private static InetSocketAddress address;
    private static final NamingService namingService;
    private static final String SERVER_ADDE = "192.168.1.30:8848";
    static {
        namingService = getNacosNamingService();//连接服务器
    }

    private static NamingService getNacosNamingService() {
        try {
            return NamingFactory.createNamingService(SERVER_ADDE);
        }catch (NacosException e){
            log.error("连接到Nacos时有错误发生");
            throw new RpcException(RpcError.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
        }
    }
    public static void clearRegistry(){//清除所有服务
        log.info("开始清除服务");
        if(!serviceNames.isEmpty()&&address!=null){
            String host = address.getHostName();
            Integer port = address.getPort();
            Iterator<String> iterator = serviceNames.iterator();
            while (iterator.hasNext()){
                String serviceName = iterator.next();
                log.info(serviceName);
                try{
                    //所有服务名称都存放在NacosUtils类中的serviceNames中，在注销是只需要用迭代器迭代所有服务名，调用deregisterInstance即可
                    namingService.deregisterInstance(serviceName,host,port);
                }catch (NacosException e){
                    log.error("注销服务{}失败",serviceName,e);
                }
            }
        }

    }
    //注册服务
    public static void registerService(String serviceName,InetSocketAddress address) throws NacosException{
        try {
            //namingService.registerInstance(serviceName, "10.26.15.125", 8848); // 注册中心的地址
            namingService.registerInstance(serviceName,address.getHostName(),address.getPort());
            serviceNames.add(serviceName);
        } catch (NacosException e) {
            log.error("注册服务时有错误");
            throw new RpcException(RpcError.REGISTER_SERVICE_FAILED);
        }
    }
    //发现服务
    public static InetSocketAddress lookUpService(String serviceName) {
        try {
            //System.out.println(serviceName);
            List<Instance> instances = namingService.getAllInstances(serviceName);//分布式的话可能会有多个服务
            Instance instance = instances.get(0);
            return new InetSocketAddress(instance.getIp(),instance.getPort());
        } catch (NacosException e) {
            log.error("获取服务是有错误发生");
        }
        return null;
    }
}
