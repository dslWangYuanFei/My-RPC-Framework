package top.wangyuanfei.rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.entity.RpcRequest;
import top.wangyuanfei.rpc.enumeration.PackageType;
import top.wangyuanfei.rpc.serializer.CommonSerializer;
//编码器，数据从外部传入时需要编码，把 Message（实际要发送的对象）转化成 Byte 数组。
//CommonEncoder 的工作很简单，就是把 RpcRequest 或者 RpcResponse 包装成协议包。 根据上面提到的协议格式，将各个字段写到管道里就可以了
//这里serializer.getCode() 获取序列化器的编号，之后使用传入的序列化器将请求或响应包序列化为字节数组写入管道即可。
@Slf4j
public class CommonEncoder extends MessageToByteEncoder {
    private static final int MAGIC_NUMBER = 0xCAFEBABE;

    private final CommonSerializer serializer;

    public CommonEncoder(CommonSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        out.writeInt(MAGIC_NUMBER);
        if (msg instanceof RpcRequest) {//判断msg是不是RpcRequest类或其子类，如果是为true，否则为false
            out.writeInt(PackageType.REQUEST_PACK.getCode());
        } else {
            out.writeInt(PackageType.RESPONSE_PACK.getCode());
        }
        out.writeInt(serializer.getCode());
        byte[] bytes = serializer.serialize(msg);//msg序列化后的字节组
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
        //这一套流程下来，ByteBuf写了 MAGIC_NUMBER，PackageType.REQUEST_PACK.getCode()/PackageType.RESPONSE_PACK.getCode()
        //msg bytes.length bytes
    }
}
