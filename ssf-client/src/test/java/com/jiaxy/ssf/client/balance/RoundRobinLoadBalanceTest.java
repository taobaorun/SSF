package com.jiaxy.ssf.client.balance;

import com.jiaxy.ssf.client.LoadBalance;
import com.jiaxy.ssf.message.MessageBuilder;
import com.jiaxy.ssf.registry.Provider;
import com.jiaxy.ssf.service.TestSuiteService;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalanceTest extends LoadBalanceBaseTest {

    @Override
    @Before
    public void setUp() throws Exception {
        providers.add(new Provider("172.0.0.1",31616,0));
        providers.add(new Provider("172.0.0.2",31616,200));
        providers.add(new Provider("172.0.0.3",31616,300));
        providers.add(new Provider("172.0.0.4",31616,400));
        providers.add(new Provider("172.0.0.5",31616,500));
        providers.add(new Provider("172.0.0.6",31616,600));
        providers.add(new Provider("172.0.0.7",31616,700));
    }

    @Test
    public void testSelect() throws Exception {
        LoadBalance balance = new RoundRobinLoadBalance();
        for ( int i = 0 ;i< 100;i++){
            Provider provider = balance.select(MessageBuilder.buildRequestMessage(TestSuiteService.class, "echo", new Class[0], new Object[0]), providers);
            AtomicInteger selected = statistics.putIfAbsent(provider,new AtomicInteger(0));
            if (selected == null){
                selected = statistics.get(provider);
            }
            selected.incrementAndGet();
        }
        report();
    }
}