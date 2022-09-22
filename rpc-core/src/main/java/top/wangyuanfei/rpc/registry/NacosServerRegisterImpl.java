package top.wangyuanfei.rpc.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.enumeration.RpcError;
import top.wangyuanfei.rpc.exception.RpcException;
import top.wangyuanfei.rpc.loadbalancer.LoadBalancer;
import top.wangyuanfei.rpc.loadbalancer.RoundRobinLoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;


@Slf4j
public class NacosServerRegisterImpl implements NacosServerRegister {
    private final LoadBalancer loadBalancer;
    private static final String SERVER_ADDR = "192.168.1.30:8848";
    private static final NamingService namingService;//这个应该就是用来连接nacos的东西吧
    public NacosServerRegisterImpl(LoadBalancer loadBalancer){
        this.loadBalancer = loadBalancer;
    }
    public NacosServerRegisterImpl(){
        this.loadBalancer = new RoundRobinLoadBalancer();//默认为轮询
    }
    static {
        try{
            namingService = NamingFactory.createNamingService(SERVER_ADDR);//加载这个类的时候就会连接nacos
        }catch (NacosException e){
            log.error("连接到nacos时有错误：",e);
            throw new RpcException(RpcError.FAILED_TO_CONNECT_TO_SERVICE_REGISTRY);
        }
    }
    @Override
    public void register(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            //namingService.registerInstance(serviceName, "10.26.15.125", 8848); // 注册中心的地址
            namingService.registerInstance(serviceName,inetSocketAddress.getHostName(),inetSocketAddress.getPort());
        } catch (NacosException e) {
            log.error("注册服务时有错误");
            throw new RpcException(RpcError.REGISTER_SERVICE_FAILED);
        }
    }

    @Override
    public InetSocketAddress lookUpService(String serviceName) {
        try {
            System.out.println(serviceName);
            List<Instance> instances = namingService.getAllInstances(serviceName);//分布式的话可能会有多个服务
            Instance instance = loadBalancer.select(instances);
            return new InetSocketAddress(instance.getIp(),instance.getPort());
        } catch (NacosException e) {
            log.error("获取服务是有错误发生");
        }
        return null;
    }
}
