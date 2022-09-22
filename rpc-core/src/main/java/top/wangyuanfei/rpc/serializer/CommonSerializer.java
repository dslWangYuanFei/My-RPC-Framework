package top.wangyuanfei.rpc.serializer;

//序列化的接口
public interface CommonSerializer {
    Integer KRYO_SERIALIZER = 0;
    Integer JSON_SERIALIZER = 1;
    Integer HESSIAN_SERIALIZER = 2;
    Integer PROTOBUF_SERIALIZER = 3;
    Integer DEFAULT_SERIALIZER = 0;
    byte[] serialize(Object obj);//序列化方法接口

    Object deserialize(byte[] bytes, Class<?> clazz);//反序列化方法接口

    int getCode();

    static CommonSerializer getByCode(int code) {
        switch (code) {
            case 0:
                return new KryoSerializer();
            case 1:
                return new JsonSerializer();
            default:
                return null;
        }
    }
}
