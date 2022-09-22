package top.wangyuanfei.rpc.transort;

public interface RpcServer {
    void start();//服务端启动服务
    <T> void publishService(Object service,String serverName);
}
