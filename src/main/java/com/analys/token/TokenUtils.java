package com.analys.token;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.analys.base.DateRange;
import com.analys.base.TransactionRequest;
import com.analys.token.info.AccountInfo;
import com.analys.token.info.Transaction;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author gu丶
 * @Date 2023/7/22 11:47 AM
 * @Description
 */

public class TokenUtils {

    private List<Transaction> send(String token) {
        String url = "https://api.dex.guru/v3/tokens/transactions";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json, text/plain, */*");
        headers.set("accept-language", "zh,zh-CN;q=0.9");
        headers.set("origin", "https://dex.guru");
        headers.set("referer", "https://dex.guru/");
        headers.set("sec-ch-ua", "\"Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"115\", \"Chromium\";v=\"115\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("sec-fetch-dest", "empty");
        headers.set("sec-fetch-mode", "cors");
        headers.set("sec-fetch-site", "same-site");
        headers.set("traceparent", "00-1bc2e417f3b8aab283840ac7eca096a7-759f53325b892f89-01");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        headers.set("x-session-id", "869afb682d454b4db42ef694acccf59d");


        TransactionRequest requestBody = new TransactionRequest();
        requestBody.setSort_by("timestamp");
        requestBody.setLimit(10000);
        requestBody.setOffset(0);
        requestBody.setOrder("desc");
        requestBody.setWith_full_totals(true);
        List<String> transactionTypes = new ArrayList<>();
        transactionTypes.add("swap");
        requestBody.setTransaction_types(transactionTypes);
        requestBody.setToken_status("all");
        requestBody.setCurrent_token_id(token + "-bsc");

        long end_date = System.currentTimeMillis();
        long start_date = end_date - (30L * 86400000L);
        DateRange dateRange = new DateRange();
        dateRange.setEnd_date(end_date);
        dateRange.setStart_date(start_date);
        requestBody.setDate(dateRange);


        HttpEntity<TransactionRequest> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        // Access the response body as a string
        String responseBody = response.getBody();
        JSONObject jsonObject = JSON.parseObject(responseBody);
        Object date = jsonObject.get("data");
        List<Transaction> transactions = JSON.parseArray(JSON.toJSONString(date), Transaction.class);
        return transactions;
    }

    //获取合约中的所有账号盈利信息
    public List<AccountInfo> profitCalculation(String token) {

        //发送请求
        List<Transaction> data = send(token);

        if (CollectionUtils.isEmpty(data)) {
            return null;
        }

        System.out.println(token + " total tx: " + data.size());

        Map<String, Map<String, Object>> tradUnique = new HashMap<>();
        List<Map<String, Object>> ret = new ArrayList<>();

        for (Transaction v : data) {
            // 判断是买入还是卖出（1=买入，2=卖出）
            int swap_side = Objects.equals(token, v.getToAddress().toLowerCase()) ? 1 : 2;

            String address = v.getWallets().get(0);

            String pool_address = v.getPoolAddress();  // 池子LP地址

            Map<String, Object> addressData = ret.stream()
                    .filter(map -> address.equals(map.get("address")))
                    .findFirst()
                    .orElse(null);

            if (addressData == null) {
                addressData = new HashMap<>();
                addressData.put("address", address);
                addressData.put("pool_address", pool_address);
                addressData.put("outflow", BigDecimal.ZERO);  // 流出U
                addressData.put("income", BigDecimal.ZERO);   // 流入U
                addressData.put("profit_rate", BigDecimal.ZERO);  // 流入/流出比
                addressData.put("profit_native", BigDecimal.ZERO);  // 原始收益
                addressData.put("profit_stable", BigDecimal.ZERO);  // usdt收益
                addressData.put("total_buy", 0);
                addressData.put("total_sell", 0);
                addressData.put("total_trade", 0);
                ret.add(addressData);
            }

            BigDecimal outflow = (BigDecimal) addressData.get("outflow");
            BigDecimal income = (BigDecimal) addressData.get("income");
            BigDecimal profit_native = (BigDecimal) addressData.get("profit_native");  // 盈利bnb
            BigDecimal profit_stable = (BigDecimal) addressData.get("profit_stable");  // 盈利usdt
            int total_buy = (int) addressData.get("total_buy");  // 买入次数
            int total_sell = (int) addressData.get("total_sell");  // 卖出次数
            int total_trade = (int) addressData.get("total_trade");  // 交易次数

            if (swap_side == 1) {
                outflow = outflow.add(BigDecimal.valueOf(v.getAmountStable()));

                profit_native = profit_native.subtract(new BigDecimal(v.getAmountNative()));

                profit_stable = profit_stable.subtract(new BigDecimal(v.getAmountStable()));
            } else if (swap_side == 2) {
                income = income.add(new BigDecimal(v.getAmountStable()));
                profit_native = profit_native.add(new BigDecimal(v.getAmountNative()));
                profit_stable = profit_stable.add(new BigDecimal(v.getAmountStable()));
            }

            // 把同一地址，同一交易哈希，同一时间戳，同一类型的操作计算为1次操作
            String uk = address + "_" + v.getTransactionAddress() + "_" + v.getTimestamp() + "_" + swap_side;
            if (!tradUnique.containsKey(uk)) {
                tradUnique.put(uk, addressData);
                total_trade++;
                if (swap_side == 1) {
                    total_buy++;
                }
                if (swap_side == 2) {
                    total_sell++;
                }
            }

            addressData.put("outflow", outflow);
            addressData.put("income", income);
            addressData.put("profit_rate", outflow.compareTo(BigDecimal.ZERO) > 0 ?
                    income.divide(outflow, 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            addressData.put("profit_native", profit_native);
            addressData.put("profit_stable", profit_stable);
            addressData.put("total_buy", total_buy);
            addressData.put("total_sell", total_sell);
            addressData.put("total_trade", total_trade);
        }

        List<AccountInfo> collect = ret.stream().map(stringObjectMap -> {
            String s = JSON.toJSONString(stringObjectMap);
            AccountInfo accountInfo = JSON.parseObject(s, AccountInfo.class);
            return accountInfo;
        }).collect(Collectors.toList());

        //collect = collect.stream().sorted((o1, o2) -> o2.getProfit_rate().compareTo(o1.getProfit_rate())).filter(accountInfo -> {
        //
        //    //过滤到利率小于1
        //    if (accountInfo.getProfit_rate().compareTo(new BigDecimal("1")) > 0) {
        //        return true;
        //    }
        //    return false;
        //}).collect(Collectors.toList());

        return collect;
    }

    public static void main(String[] args) {
        TokenUtils tokenUtils = new TokenUtils();
        List<AccountInfo> accountInfos = tokenUtils.profitCalculation("0x29b648ff14a259ab332dfd7845b751f30b7e1df0");
        System.out.println(accountInfos);
    }

}
