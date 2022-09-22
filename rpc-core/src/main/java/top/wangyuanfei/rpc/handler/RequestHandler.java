package top.wangyuanfei.rpc.handler;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.entity.RpcResponse;
import top.wangyuanfei.rpc.enumeration.ResponseCode;
import top.wangyuanfei.rpc.providers.ServiceProvider;
import top.wangyuanfei.rpc.providers.ServiceProviderImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 进行过程调用的处理器
 */
@Slf4j
public class RequestHandler {
    private static final ServiceProvider serviceProvider;
    static {//静态代码块，调用该对象是最先被执行，并且对次调用静态代码块只执行一次
        //在类加载的时候做一些静态数据初始化的操作，以便后续使用。
        serviceProvider = new ServiceProviderImpl();
    }
    public Object handle(RpcRequest rpcRequest,Object service) {
        Object result = null;
        try {
            result = invokeTargetMethod(rpcRequest, service);//执行目标方法
            log.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (Exception e) {
            log.error("调用或发送时有错误发生：", e);
        } return result;
    }

    /**
     *
     * @param rpcRequest 客户端传来的一些参数信息
     * @param service 服务实现类
     * @return
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());//得到对应的方法
            result = method.invoke(service, rpcRequest.getParameters());//执行方法
            log.info("服务:{} 成功调用方法:{}", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return RpcResponse.fail(ResponseCode.METHOD_NOT_FOUND, rpcRequest.getRequestId());
        }
        return result;
    }
}
