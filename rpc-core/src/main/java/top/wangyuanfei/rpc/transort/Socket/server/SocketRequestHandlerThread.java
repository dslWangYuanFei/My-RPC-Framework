package top.wangyuanfei.rpc.transort.Socket.server;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.entity.RpcResponse;
import top.wangyuanfei.rpc.handler.RequestHandler;
import top.wangyuanfei.rpc.registry.ServiceRegister;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
@Slf4j
public class SocketRequestHandlerThread implements Runnable {//new这个对象后就会自动执行run方法
    private Socket socket;
    private RequestHandler requestHandler;
    private ServiceRegister serviceRegister;

    public SocketRequestHandlerThread(Socket socket, RequestHandler requestHandler, ServiceRegister serviceRegister) {
        this.socket = socket;
        this.requestHandler = requestHandler;
        this.serviceRegister = serviceRegister;
    }
    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();//读取服务端传过来的信息
            String interfaceName = rpcRequest.getInterfaceName();//接口名
            Object service = serviceRegister.getService(interfaceName);//获取这个接口名对应的服务
            Object result = requestHandler.handle(rpcRequest, service);//处理返回
            objectOutputStream.writeObject(RpcResponse.success(result));
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException e) {
            log.error("调用或发送时有错误发生：", e);
        }
    }


}
