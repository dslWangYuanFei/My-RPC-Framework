package top.wangyuanfei.rpc.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

//轮训算法
public class RoundRobinLoadBalancer implements LoadBalancer {
    private Integer index = 0;
    @Override
    public Instance select(List<Instance> instances) {
        if(index==instances.size()){
            index = 0;
        }
        Instance instance = instances.get(index);
        index++;
        return instance;
    }
}
