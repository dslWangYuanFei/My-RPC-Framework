package top.wangyuanfei.rpc.transort.Socket.client;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.transort.RpcClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@Slf4j
public class SocketClient {
    public Object sendRequest(RpcRequest rpcRequest, String host, int port) {
        try (Socket socket = new Socket(host, port)) {//发送消息，此时它已经和server通讯了
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream.writeObject(rpcRequest);//写消息
            objectOutputStream.flush();//把消息传递给客户端
            return objectInputStream.readObject();//得到客户端返回的消息
        } catch (IOException | ClassNotFoundException e) {
            log.error("调用时有错误发生：", e);
            return null;
        }
    }
}
