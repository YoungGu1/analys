package com.analys.account;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.analys.account.info.AccountDto;
import com.analys.base.DateRange;
import com.analys.base.TransactionRequest;
import com.analys.token.info.Transaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author gu丶
 * @Date 2023/7/22 1:43 PM
 * @Description
 */

@Service
public class AccountUtilsEth {

    private List<Transaction> send(String address,int num) {


        //headers.setContentType(MediaType.APPLICATION_JSON);
        //headers.set("sec-ch-ua", "\"Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"115\", \"Chromium\";v=\"115\"");
        //headers.set("traceparent", "00-8b2f0bcf7a00cdeb3e70da2f229dad12-8ede0233e6ee7861-01");
        //headers.set("sec-ch-ua-mobile", "?0");
        //headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        //headers.set("Referer", "https://dex.guru/");
        //headers.set("X-Session-Id", "8b23eaacac4d4cd19d5c113a9d3021ea");
        //headers.set("sec-ch-ua-platform", "\"macOS\"");

        String url = "https://api.dex.guru/v3/tokens/transactions";
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authority", "api.dex.guru");
        headers.set("accept", "application/json, text/plain, */*");
        headers.set("accept-language", "zh,zh-CN;q=0.9");
        headers.set("origin", "https://dex.guru");
        headers.set("referer", "https://dex.guru/");
        headers.set("sec-ch-ua", "\"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Google Chrome\";v=\"116\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("sec-fetch-dest", "empty");
        headers.set("sec-fetch-mode", "cors");
        headers.set("sec-fetch-site", "same-site");
        headers.set("traceparent", "00-8b2f0bcf7a00cdeb3e70da2f229dad12-8ede0233e6ee7861-01");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");
        headers.set("x-session-id", "8b23eaacac4d4cd19d5c113a9d3021ea");


        TransactionRequest requestBody = new TransactionRequest();
        requestBody.setAccount(address);
        requestBody.setSort_by("timestamp");
        requestBody.setLimit(100);
        requestBody.setOffset(num * 100);
        requestBody.setOrder("desc");
        requestBody.setWith_full_totals(true);
        requestBody.setToken_status("all");
        requestBody.setNetwork("eth");
        //List<String> transactionTypes = new ArrayList<>();
        //transactionTypes.add("swap");
        //requestBody.setTransaction_types(transactionTypes);


        long end_date = System.currentTimeMillis();
        long start_date = end_date - (30L * 86400000L);
        DateRange dateRange = new DateRange();
        dateRange.setEnd_date(end_date);
        dateRange.setStart_date(start_date);
        requestBody.setDate(dateRange);

        HttpEntity<TransactionRequest> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        String responseBody = response.getBody();
        JSONObject jsonObject = JSON.parseObject(responseBody);
        Object date = jsonObject.get("data");
        List<Transaction> transactions = JSON.parseArray(JSON.toJSONString(date), Transaction.class);
        return transactions;

    }

    private List<Transaction> filterTransaction(List<Transaction> data) {
        if (CollectionUtils.isEmpty(data)) {
            return null;
        }

        // Count the occurrences of each transaction
        Map<String, Integer> txMap = new HashMap<>();
        for (Transaction v : data) {
            String tx = v.getTransactionAddress().toLowerCase();
            txMap.put(tx, txMap.getOrDefault(tx, 0) + 1);
        }

        // Remove records with less than 2 occurrences of the same transaction
        List<Transaction> filteredData = new ArrayList<>();
        for (Transaction v : data) {
            String tx = v.getTransactionAddress().toLowerCase();
            int total = txMap.get(tx);
            if (total >= 2) {
                filteredData.add(v);
            }
        }

        return filteredData;
    }

    //public List<AccountDto> analyzingProfit(String address) {
    public Map<Integer, AccountDto> analyzingProfit(String address) {

        List<Transaction> data = new ArrayList<>();

        //发送请求
        for (int i = 0; i < 10; i++) {
            List<Transaction> a = send(address,i);
            if(CollectionUtils.isEmpty(a)){
                break;
            }
            data.addAll(a);

        }
        //去重
        //data = filterTransaction(data);

        //eth链
        data = data.stream().filter(transaction -> !Objects.equals("native", transaction.getType())).collect(Collectors.toList());

        if (data == null || data.isEmpty()) {
            return null;
        }


        long tsNow = System.currentTimeMillis() / 1000; // 当前时间戳（秒）
        long ts1d = tsNow - 86400; // 1天之前的时间戳
        long ts3d = tsNow - 86400 * 3;
        long ts7d = tsNow - 86400 * 7;
        long ts15d = tsNow - 86400 * 15;
        long ts30d = tsNow - 86400 * 30;

        BigDecimal income1d = BigDecimal.ZERO;
        BigDecimal outflow1d = BigDecimal.ZERO;
        int totalBuy1d = 0;
        int totalSell1d = 0;

        BigDecimal income3d = BigDecimal.ZERO;
        BigDecimal outflow3d = BigDecimal.ZERO;
        int totalBuy3d = 0;
        int totalSell3d = 0;

        BigDecimal income7d = BigDecimal.ZERO;
        BigDecimal outflow7d = BigDecimal.ZERO;
        int totalBuy7d = 0;
        int totalSell7d = 0;

        BigDecimal income15d = BigDecimal.ZERO;
        BigDecimal outflow15d = BigDecimal.ZERO;
        int totalBuy15d = 0;
        int totalSell15d = 0;

        BigDecimal income30d = BigDecimal.ZERO;
        BigDecimal outflow30d = BigDecimal.ZERO;
        int totalBuy30d = 0;
        int totalSell30d = 0;

        //换成weth地址
        String[] mainTokens = {
                "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"
        };

        List<String> listSell = new ArrayList<>();

        for (Transaction v : data) {
            //if ("erc20".equals(v.getType())) {
            //    continue;
            //}

            if ("mint".equals(v.getTransactionType())) {
                continue;
            }

            String tx = v.getTransactionAddress();

            long timestamp = Long.parseLong(String.valueOf(v.getTimestamp()));
            if ("swap".equals(v.getTransactionType())) {
                if (Arrays.asList(mainTokens).contains(v.getFromAddress()) && Arrays.asList(mainTokens).contains(v.getToAddress())) {
                    continue;
                }

                int swapSide;
                if (Arrays.asList(mainTokens).contains(v.getFromAddress())) {
                    swapSide = 2; // 买入
                } else if (Arrays.asList(mainTokens).contains(v.getToAddress())) {
                    swapSide = 1; // 卖出
                } else {
                    continue;
                }

                //long timestamp = Long.parseLong(String.valueOf(v.getTimestamp()));
                if (swapSide == 1) {
                    if (listSell.contains(tx)) {
                        continue;
                    }
                    listSell.add(tx);

                    BigDecimal amountStable = new BigDecimal(v.getAmountStable());
                    if (timestamp >= ts1d) {
                        income1d = income1d.add(amountStable);
                        totalSell1d++;
                    }
                    if (timestamp >= ts3d) {
                        income3d = income3d.add(amountStable);
                        totalSell3d++;
                    }
                    if (timestamp >= ts7d) {
                        income7d = income7d.add(amountStable);
                        totalSell7d++;
                    }
                    if (timestamp >= ts15d) {
                        income15d = income15d.add(amountStable);
                        totalSell15d++;
                    }
                    if (timestamp >= ts30d) {
                        income30d = income30d.add(amountStable);
                        totalSell30d++;
                    }
                } else if (swapSide == 2) {
                    BigDecimal amountStable = new BigDecimal(v.getAmountStable());
                    if (timestamp >= ts1d) {
                        outflow1d = outflow1d.add(amountStable);
                        totalBuy1d++;
                    }
                    if (timestamp >= ts3d) {
                        outflow3d = outflow3d.add(amountStable);
                        totalBuy3d++;
                    }
                    if (timestamp >= ts7d) {
                        outflow7d = outflow7d.add(amountStable);
                        totalBuy7d++;
                    }
                    if (timestamp >= ts15d) {
                        outflow15d = outflow15d.add(amountStable);
                        totalBuy15d++;
                    }
                    if (timestamp >= ts30d) {
                        outflow30d = outflow30d.add(amountStable);
                        totalBuy30d++;
                    }
                }
            }


            //卖出
            if ("transfer".equals(v.getTransactionType()) && Objects.equals(v.getFromAddress(), address)) {
                BigDecimal amountStable = new BigDecimal(v.getAmountStable());
                if (timestamp >= ts1d) {
                    income1d = income1d.add(amountStable);
                    totalSell1d++;
                }
                if (timestamp >= ts3d) {
                    income3d = income3d.add(amountStable);
                    totalSell3d++;
                }
                if (timestamp >= ts7d) {
                    income7d = income7d.add(amountStable);
                    totalSell7d++;
                }
                if (timestamp >= ts15d) {
                    income15d = income15d.add(amountStable);
                    totalSell15d++;
                }
                if (timestamp >= ts30d) {
                    income30d = income30d.add(amountStable);
                    totalSell30d++;
                }
            }

        }


        Map<Integer, AccountDto> map = new HashMap<>();

        AccountDto one = new AccountDto();
        one.setDay("1d");
        one.setIncome(income1d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        one.setOutflow(outflow1d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        one.setProfitRate(outflow1d.compareTo(BigDecimal.ZERO) != 0 ? income1d.divide(outflow1d, 2, BigDecimal.ROUND_DOWN).toString() : "0");
        one.setTotalBuy(totalBuy1d);
        one.setTotalSell(totalSell1d);
        one.setTotalBuyTotalSell(totalBuy1d + "/" + totalSell1d);

        map.put(1, one);

        AccountDto threeDays = new AccountDto();
        threeDays.setDay("3d");
        threeDays.setIncome(income3d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        threeDays.setOutflow(outflow3d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        threeDays.setProfitRate(outflow3d.compareTo(BigDecimal.ZERO) != 0 ? income3d.divide(outflow3d, 2, BigDecimal.ROUND_DOWN).toString() : "0");
        threeDays.setTotalBuy(totalBuy3d);
        threeDays.setTotalSell(totalSell3d);
        threeDays.setTotalBuyTotalSell(totalBuy3d + "/" + totalSell3d);
        map.put(3, threeDays);

        AccountDto sevenDays = new AccountDto();
        sevenDays.setDay("7d");
        sevenDays.setIncome(income7d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        sevenDays.setOutflow(outflow7d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        sevenDays.setProfitRate(outflow7d.compareTo(BigDecimal.ZERO) != 0 ? income7d.divide(outflow7d, 2, BigDecimal.ROUND_DOWN).toString() : "0");
        sevenDays.setTotalBuy(totalBuy7d);
        sevenDays.setTotalSell(totalSell7d);
        sevenDays.setTotalBuyTotalSell(totalBuy7d + "/" + totalSell7d);
        map.put(7, sevenDays);

        //AccountDto fifteenDays = new AccountDto();
        //fifteenDays.setDay("15d");
        //fifteenDays.setIncome(income15d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        //fifteenDays.setOutflow(outflow15d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        //fifteenDays.setProfitRate(outflow15d.compareTo(BigDecimal.ZERO) != 0 ? income15d.divide(outflow15d, 2, BigDecimal.ROUND_DOWN).toString() : "0");
        //fifteenDays.setTotalBuy(totalBuy15d);
        //fifteenDays.setTotalSell(totalSell15d);
        //fifteenDays.setTotalBuyTotalSell(totalBuy15d + "/" + totalSell15d);
        //map.put(15, fifteenDays);
        //
        //AccountDto thirtyDays = new AccountDto();
        //thirtyDays.setDay("30d");
        //thirtyDays.setIncome(income30d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        //thirtyDays.setOutflow(outflow30d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        //thirtyDays.setProfitRate(outflow30d.compareTo(BigDecimal.ZERO) != 0 ? income30d.divide(outflow30d, 2, BigDecimal.ROUND_DOWN).toString() : "0");
        //thirtyDays.setTotalBuy(totalBuy30d);
        //thirtyDays.setTotalSell(totalSell30d);
        //thirtyDays.setTotalBuyTotalSell(totalBuy30d + "/" + totalSell30d);
        //map.put(30, thirtyDays);

        return map;
    }

    public Map<String, Map<Integer, AccountDto>> batchSelect(List<String> list) {
        Map<String, Map<Integer, AccountDto>> map = new HashMap<>();
        list.forEach(s -> {
            Map<Integer, AccountDto> map1 = analyzingProfit(s);
            //过滤数据
            if (filter(3, map1) || filter(7, map1)) {
                map.put(s, map1);
            }
        });
        return map;

    }

    public Boolean filter(Integer day, Map<Integer, AccountDto> map) {

        AccountDto accountDto = map.get(day);
        String profitRate = accountDto.getProfitRate();
        Integer totalBuy = accountDto.getTotalBuy();
        Integer totalSell = accountDto.getTotalSell();
        double rate = Double.parseDouble(profitRate);
        int i = totalSell / totalBuy;

        //3天7天，买卖次数必须大于1
        if(day != 1){
            if(totalBuy <= 1 || totalSell <= 1){
                return false;
            }
        }

        if (rate >= 1.5 && i <= 3) {
            return true;
        }

        return false;
    }


    public static void main(String[] args) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> list = Arrays.asList(
                "0x221fce6b6dac61520c1c283825e29bb556979111",
                "0xb217b5c081afb18b7cb14347128db7fa3ec499d5",
                "0x31c6d90c9d73f08547ab299c935a8b8a80ebe603",
                "0x01d8d6f2481ce17d83f19ff23b1dbcb41e6fdc7b",
                "0xeb002328393201038fc68eba0fb895a4ae17397e",
                "0xb16e5ece0acda8a113720ff9d6514ebd32fd3038",
                "0xa7308d4d83b14859916671a17370476e6c6cd1a9",
                "0x35805dc419ddf64d9cda679839a31b0c114e6122",
                "0x022a1a15f507ccab6dce31ac56a644f3f3fd3be4",
                "0xbea3b7eda9e6a9bb0e470e062025f4129412e9f1",
                "0xa99e96dd0f86a3c7c64a31df7605c5728f77af2c",
                "0x772f0bb499ad06490f988359b5a1df96ac32b9e2",
                "0x50f26d9442e9b87856cf1257853a7b142d621960",
                "0xc0bf08682e29796f27652a26abd98650e374c97d",
                "0x2db15d5240c157ac5077f7fac18ba84db954bb28",
                "0x7b69782c0a2c1acb059a77f013980719a77a3da8",
                "0xf5b90defbc7c9415163dcc4e8623a25b93425e41",
                "0xe8d3c311912513ae9cc34016db69eae9e9f4e2cc",
                "0x2b2f4b74adadec7af33230090458610a4a73556e",
                "0xbb28c484121554663b601783a8effac3a7ab646a",
                "0x3772a9878c9daea175f7ca1b6f7fbd68d463599e",
                "0xe29eab306d99aa821da18352611216240adaed00",
                "0x08a3e1ae33190686835ac6713bc50fc8eee73303",
                "0x4f204e5b75fe13abd9e656007dbaeae64e07939f",
                "0x0cdd4a6b9991b651d961c3b5464ab2cab1ac3749",
                "0x6199a78c6c8f2eb48adb39252d3ac1a48bac5e3b",
                "0x6bcc7ad891179efcccee3e1e0240b79f24a70a4f",
                "0x76258e218decb8ae209151dacda5fb56b8ca8f8e",
                "0x65dbe5ef037efee8f4a39403337ab8fea3a0ae5e",
                "0xb278c6c11111d6d6f3f7f10f8d19e4124c9ef20e",
                "0x5b91114ef2814f1f844bf3b08cbcea8bd212a148",
                "0xe4f4674ad8e915869d9dca32d0e03503fd0a7be1",
                "0x8c8e23fa04557d9e0b579ea5fe45051e934f147d",
                "0xca23c3396a56d5f2bf6f566440e777049dd8e2f8",
                "0xb193d90ca445bc91652aae5f07b50b70efd354b6",
                "0x181f2be9d59eeda2eddeb7b94492c650be1dc07d",
                "0x1d49af587f325917b1467600a180a96388b97dff",
                "0x5db10cad9e5899f8a2f16239c30097e8405f088d",
                "0x95b09dfa9e6341ce19a9df99bf7b84dc3fd4d7b5",
                "0xe3382268800c2892f392741d6442ac9b73542087"
        );
        List<String> list1 = Arrays.asList("0x6e77efcad4db1e016b8105e4e14c353097c4d25c");
        AccountUtilsEth accountUtils = new AccountUtilsEth();
        Map<String, Map<Integer, AccountDto>> map = accountUtils.batchSelect(list1);
        String string = objectMapper.writeValueAsString(map);
        System.out.println(string);
    }

}
