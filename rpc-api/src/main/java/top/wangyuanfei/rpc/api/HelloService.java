package top.wangyuanfei.rpc.api;

/**
 * 测试api的接口，客户端和服务端的公共接口
 */
public interface HelloService {
    String hello(HelloObject object);
    Integer add(int a,int b);
}
