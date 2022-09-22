package top.wangyuanfei.rpc.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SerializaeCode {
    KRYO(0),
    JSON(1);
    private final int code;
}
