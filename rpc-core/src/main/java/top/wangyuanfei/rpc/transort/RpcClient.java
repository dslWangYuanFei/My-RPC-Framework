package top.wangyuanfei.rpc.transort;

import top.wangyuanfei.rpc.entity.RpcRequest;

public interface RpcClient {
    Object sendRequest(RpcRequest rpcRequest);//客户端发送请求
}
