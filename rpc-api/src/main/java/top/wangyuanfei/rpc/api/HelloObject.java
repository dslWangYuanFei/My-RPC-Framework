package top.wangyuanfei.rpc.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 测试api的实体，HelloService接口的需要传递一个HelloObject对象
 * 这个对象要实现序列化接口，因为它需要从客户端传递给服务端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelloObject implements Serializable {
    private Integer id;
    private String message;
}
