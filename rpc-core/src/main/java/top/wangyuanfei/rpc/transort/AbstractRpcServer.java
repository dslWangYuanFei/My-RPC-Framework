package top.wangyuanfei.rpc.transort;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.annotation.Service;
import top.wangyuanfei.rpc.annotation.ServiceScan;
import top.wangyuanfei.rpc.enumeration.RpcError;
import top.wangyuanfei.rpc.exception.RpcException;
import top.wangyuanfei.rpc.providers.ServiceProvider;
import top.wangyuanfei.rpc.registry.NacosServerRegister;
import top.wangyuanfei.rpc.util.ReflectUtil;

import java.net.InetSocketAddress;
import java.util.Set;

@Slf4j
public class AbstractRpcServer implements RpcServer{
    protected String host;
    protected int port;

    protected NacosServerRegister serviceRegistry;
    protected ServiceProvider serviceProvider;
    public void scanServices() {
        String mainClassName = ReflectUtil.getStackTrace();
        Class<?> startClass;
        try {
            startClass = Class.forName(mainClassName);//获取主方法的类：top.wangyuanfei.test.NettyTestServer
            if(!startClass.isAnnotationPresent(ServiceScan.class)) {
                log.error("启动类缺少 @ServiceScan 注解");
                throw new RpcException(RpcError.SERVICE_SCAN_PACKAGE_NOT_FOUND);
            }
        } catch (ClassNotFoundException e) {
            log.error("出现未知错误");
            throw new RpcException(RpcError.UNKNOWN_ERROR);
        }
        String basePackage = startClass.getAnnotation(ServiceScan.class).value();
        if("".equals(basePackage)) {
            basePackage = mainClassName.substring(0, mainClassName.lastIndexOf("."));//如果没有设置默认扫描包，那么把主程序所在的包作为扫描包
        }
        Set<Class<?>> classSet = ReflectUtil.getClasses(basePackage);//获取当前扫描包下的所有类，即这个包top.wangyuanfei.test下的所有的类
        //top.wangyuanfei.test.AddServiceImpl top.wangyuanfei.test.HelloServiceImpl top.wangyuanfei.test.NettyTestServer top.wangyuanfei.test.TestServer
        for(Class<?> clazz : classSet) {
            if(clazz.isAnnotationPresent(Service.class)) {//包含这个Service注解
                String serviceName = clazz.getAnnotation(Service.class).name();//得到默认的服务名
                Object obj;
                try {
                    obj = clazz.newInstance();//创建一个实例
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("创建 " + clazz + " 时有错误发生");
                    continue;
                }
                if("".equals(serviceName)) {
                    Class<?>[] interfaces = clazz.getInterfaces();//得到这个实现类的接口class
                    for (Class<?> oneInterface: interfaces){
                        publishService(obj, oneInterface.getCanonicalName());//注册这个服务
                    }
                } else {
                    publishService(obj, serviceName);
                }
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public <T> void publishService(Object service, String serviceName) {
        serviceProvider.addServiceProvider(service, serviceName);
        serviceRegistry.register(serviceName, new InetSocketAddress(host, port));
    }
}
