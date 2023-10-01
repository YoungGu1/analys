package com.analys.service;

import com.alibaba.fastjson.JSON;
import com.analys.account.AccountUtils;
import com.analys.account.info.AccountDto;
import com.analys.token.TokenUtils;
import com.analys.token.info.AccountInfo;
import com.analys.utils.ThreadPoolUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Author gu丶
 * @Date 2023/7/22 6:49 PM
 * @Description
 */
@Service
public class TokenService {


    /***
     * 根据合约获取所有的盈利情况
     * 地址：   天  ： 账号盈利信息
     * @param token
     * @return
     */
    public Map<String, Map<Integer, AccountDto>> getThreadProfitByToken(String token) {

        Executor service = ThreadPoolUtils.getThreadPool();
        TokenUtils tokenUtils = new TokenUtils();
        List<AccountInfo> accountInfos = tokenUtils.profitCalculation(token);
        if (CollectionUtils.isEmpty(accountInfos)) {
            return null;
        }
        Map<String, Map<Integer, AccountDto>> result = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> list = new ArrayList<>();
        AccountUtils accountUtils = new AccountUtils();

        //盈利地址集合
        List<String> addressId = accountInfos.stream().map(AccountInfo::getAddress).collect(Collectors.toList());
        System.out.println("盈利地址集合:" + JSON.toJSONString(addressId));

        for (String address : addressId) {

            //多线程执行
            CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {

                //获取账号盈利信息
                Map<Integer, AccountDto> map = accountUtils.analyzingProfit(address);

                //result.put(address, map);

                //过滤数据
                AccountDto accountDto3 = map.get(3);

                AccountDto accountDto7 = map.get(7);
                BigDecimal profitRate7 = new BigDecimal(accountDto7.getProfitRate());
                BigDecimal num7 = new BigDecimal("2");

                AccountDto accountDto15 = map.get(15);
                BigDecimal profitRate15 = new BigDecimal(accountDto15.getProfitRate());
                BigDecimal num15 = new BigDecimal("2");

                if((profitRate15.compareTo(num15)>0 || profitRate7.compareTo(num7)>0)
                        && accountDto3.getTotalBuy() >= 1){
                    result.put(address,map);
                }

            }, service);
            list.add(voidCompletableFuture);
        }

        try {
            CompletableFuture.allOf(list.stream().toArray(CompletableFuture[]::new)).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        ((ThreadPoolExecutor) service).shutdown();
        return result;

    }

    public Map<String, Map<Integer, AccountDto>> batchGetThreadProfitByToken(List<String> tokens) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        Map<String, Map<Integer, AccountDto>> map = new HashMap<>();

        tokens.stream().forEach(s -> {
            Map<String, Map<Integer, AccountDto>> threadProfitByToken = getThreadProfitByToken(s);
            if (!CollectionUtils.isEmpty(threadProfitByToken)) {
                threadProfitByToken.forEach((s1, integerAccountDtoMap) -> {
                    map.put(s1, integerAccountDtoMap);
                });
            }
        });
        return map;
    }


}
