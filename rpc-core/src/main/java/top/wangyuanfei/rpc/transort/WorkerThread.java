package top.wangyuanfei.rpc.transort;

import javafx.beans.binding.ObjectExpression;
import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.entity.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
@Slf4j
public class WorkerThread implements Runnable {
    private Socket socket;
    private Object service;
    public WorkerThread(Socket socket, Object service) {
        this.socket = socket;
        this.service = service;
    }

    @Override
    public void run() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream())) {
            RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();//接收从客户端传来的请求信息
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());//获取service的rpcRequest.getMethodName()方法，这里server是helloServer
            Object returnObject = method.invoke(service, rpcRequest.getParameters());//执行rpcRequest.getMethodName()方法
            objectOutputStream.writeObject(RpcResponse.success(returnObject));//把结果传递给客户端
            objectOutputStream.flush();
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("调用或发送时有错误发生：", e);
        }
    }
}
