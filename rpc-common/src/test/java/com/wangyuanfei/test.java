package com.wangyuanfei;
public class test {
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("钩子函数执行");
            }
        }));
        while (true);
    }
}
