package top.wangyuanfei.test;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.annotation.Service;
import top.wangyuanfei.rpc.api.HelloObject;
import top.wangyuanfei.rpc.api.HelloService;
@Slf4j
@Service
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(HelloObject object) {
        log.info("接收到{}",object.getMessage());
        return "这是调用的返回值,id="+object.getId();
    }

    @Override
    public Integer add(int a, int b) {
        log.info("远程调用helloService的add方法");
        return a + b;
    }
}
