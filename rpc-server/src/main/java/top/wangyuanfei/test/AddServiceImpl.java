package top.wangyuanfei.test;

import top.wangyuanfei.rpc.api.AddService;

public class AddServiceImpl implements AddService {
    @Override
    public Integer add(int a, int b) {
        return a + b;
    }
}
