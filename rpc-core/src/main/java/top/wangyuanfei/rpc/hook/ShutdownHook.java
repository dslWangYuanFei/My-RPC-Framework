package top.wangyuanfei.rpc.hook;

import lombok.extern.slf4j.Slf4j;
import top.wangyuanfei.rpc.factory.ThreadPoolFactory;
import top.wangyuanfei.rpc.util.NacosUtil;

import java.util.concurrent.ExecutorService;

@Slf4j
public class ShutdownHook {
    private final ExecutorService threadPool = ThreadPoolFactory.createDefaultThreadPool("shutdown-hook");
    private static final ShutdownHook shutdownHook = new ShutdownHook();

    public static ShutdownHook getShutdownHook() {
        return shutdownHook;
    }
    public void addClearAllHook(){
        log.info("关闭后将自动注销所有服务");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                NacosUtil.clearRegistry();
                threadPool.shutdown();
            }
        }));
    }
}
