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

    private List<Transaction> send(String address, int num) {


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
            List<Transaction> a = send(address, i);
            if (CollectionUtils.isEmpty(a)) {
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
        String[] mainTokens = {"0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"};

        List<String> hashList = new ArrayList<>();

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

                //同一个交易已经进行过一次计算
                if(hashList.contains(tx)){
                    continue;
                }

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

                hashList.add(tx);

                if (swapSide == 1) {

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
            if ("transfer".equals(v.getTransactionType()) && Objects.equals(v.getFromAddress().toLowerCase(), address.toLowerCase())) {
                //同一个交易已经进行过一次计算
                if(hashList.contains(tx)){
                    continue;
                }
                hashList.add(tx);
                BigDecimal amountStable = new BigDecimal(v.getAmountStable());
                //过滤掉转给项目方手续费
                if (amountStable.doubleValue() - 10 < 0) {
                    continue;
                }
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

            //买入
            if ("transfer".equals(v.getTransactionType()) && Objects.equals(v.getToAddress().toLowerCase(), address.toLowerCase())) {
                //同一个交易已经进行过一次计算
                if(hashList.contains(tx)){
                    continue;
                }
                hashList.add(tx);
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
            if(Objects.isNull(map1)){
                System.out.println("当前地址没有找到记录" + s);
                return;
            }
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
        if(totalBuy == 0 || totalSell == 0){
            return false;
        }
        int i = totalSell / totalBuy;

        if (rate >= 1.5 && i <= 3) {
            return true;
        }

        return false;
    }


    public static void main(String[] args) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> list = Arrays.asList(
                "0x4423cbf50599b4daf01ce290b6a2c2ecca957a38",
                "0x1eb7ab6f171e53c5d871f78c9a54ecbc77a52356",
                "0x5e302dc8f41be15ad663e30d64027741556a2b22",
                "0x75d8ed3a26ed37cfac53fadfdcb5385ff0cea04b",
                "0x490d9e694967f8d5a0fc5b7080284192f1dbd673",
                "0xc3fcc310a93aa34d2de86fe0c00f9ebdc395e1d9",
                "0xc607459b0cc92e1bec3f60d7cbebb80ce7861f93",
                "0x84ab142e4014bba2499b65a9b0722e0f77a2bc59",
                "0xf94d633e8f81ef6ec7cd6116a0bf9b4b2f65a24b",
                "0xa6bcee95d8c4ecf7bfaa819f9f90042c1af81169",
                "0x4acf260a1c2e72a163996e62d07cfca6cbcfc1a3",
                "0xafef900b8ae866e2689e804fb794f67b80f21278",
                "0x0ecc4fd82e3f550b53a746443f289c193108a2fc",
                "0xe432086607a1b500caa36c95b766d91ff47114c6",
                "0xa2f893a8d5a72fa3ce8eaf8cb5579ea2d7866320",
                "0xd633821affc73205f1e7e615fad1d2dc1a996b05",
                "0xa3c0ec122e5f3d217880f9bfef5698ddb2dd79f1",
                "0xfc78848b7e7876abc450ec1d2639e94744d122e5",
                "0x4563f662e66fdac3183e7e0057d49c9db355fdeb",
                "0x72f9c3debf3508d7cc55beae9f541a78e631e99e",
                "0xe8288cba3ee3b8706c285c0bc8f13ff3941ddf8d",
                "0x09ac07ce5e7345a4a1df33405ad54871041e306e",
                "0xb24c8ef593563e1fff3ba6e8bb3a8b7042beff1f",
                "0x67054e66a44ef2be3fc349cc08ce62d31e0aae13",
                "0xaef7569b48d2a7f45bba98ec5616e3715a60d72f",
                "0x42479fe1c362887da9f67757bac27db17bd76ef8",
                "0xdcf9aa0f05deddffc1cb699795ca9cd0d9d0ae6f",
                "0x2e28f281628a784557a9646bd7b5bab27c1e53ff",
                "0xc479f01c262433848df4be12861ebed8169be5ae",
                "0x8ea7c218d19babbbb7ae9bb95b604503b3ca5f50",
                "0x3e9dd8088fe88472621f67ac502b784f0d852b71",
                "0x298084195200f42ec90385bd71a2db5ff93241de",
                "0xabf864065fdeb8996eb5469d6c25aae84507d231",
                "0xfb40b47cefcce911c67243870fbc199a3d9c3017",
                "0x139b9a8cc66e57a8124eb215afceff0cce16fd1b",
                "0x999999b2173a81c2b20e202c3d22e0473f6517b6",
                "0x7fcff11b8f4669a9b5599c2c0230dc6e6b8627fd",
                "0x8c33a15b3eb3e74500f544257606336cb0553352",
                "0x004d912c41847c638af23e53c03ac86009b27001",
                "0xc63d36c03a0609d11a4ce223f09d65d7cd8cf060",
                "0xb7b0c8f7035dd97efad1e508df4f07c81a8a0eab",
                "0x09ac07ce5e7345a4a1df33405ad54871041e306e",
                "0xa6ee0249dd5ccc040115152a9fc5a6c0b9be78e9",
                "0xc060a089eb849f2d91102fd08c3d40729711efc9",
                "0x8db443472986834bab2dcdc938ea1e995cbadd45",
                "0xf8ba2189968d8459a2e48b8cdff8ece030fdc8c6",
                "0x800d304399ddfbb0a8ef7acc3197dc99f35d8ae6",
                "0x95bfe21a7651854b32896fa1941525db77b5d302",
                "0xe572fe384eb21f7242e6f40fcf11b0de6ddf9bf2",
                "0x7cda0228c95a72089fbdf4183e358fca0abd43fb",
                "0x86e1d33ad028da2feb61a7138a22a472f383b8b8",
                "0xafef900b8ae866e2689e804fb794f67b80f21278",
                "0x5e302dc8f41be15ad663e30d64027741556a2b22",
                "0x9f682fa41e370a7d58128820b9595a1a7c98a403",
                "0x8c38a8ab7242896449935346d9bb0f76ce186607",
                "0x898a742e79a8877960dd5e88f71f44192b895c53",
                "0xdcf9aa0f05deddffc1cb699795ca9cd0d9d0ae6f",
                "0xdfda95adf745e904498e522d0ff69c6c303f3341",
                "0xa49c4d6bf8ab3f21b34432ba91980809ec5d0ed7",
                "0x088fa1f723c93ab3b67e6ffbb592c878d537ff41",
                "0xb8fe71a788b4bc22b58d1bcbd5f6099981947757",
                "0x2f78e09791f45a14b064503ea3b4d5e2357a5ebc",
                "0xd8c138fddcb0c422c1abf6ebb02e895c6f706590",
                "0xff45bb02779246a38861468a890d7715136d4ea8",
                "0xe23f26cab48f1dfb66e8cabfa6cfc47856a98787",
                "0x68ca69807b9b76610e234bfb5fe2f598c52bbb3d",
                "0xb2738e9b0f38b604c665329c917f74283f5f87cf",
                "0x05e47bbdedc57d53a2b969cfe98179560d999999",
                "0x38d671409156608fe1dd66c2d1ac9936ff973ef3",
                "0x73a3225e206b236c63c34d828d9324cce7ae20cb",
                "0x7fcff11b8f4669a9b5599c2c0230dc6e6b8627fd",
                "0x8c33a15b3eb3e74500f544257606336cb0553352",
                "0xf1962aab4ecec6f533e9532fd2738945bc623014",
                "0x50c9359bb284366155b4bb5b88403c96a5f7d2b3",
                "0x460aab75b5b4e08ca61bd9fe568f762a49b36f71",
                "0x618012c296f721e3afbfc288aca39c1d341c1ff2",
                "0x227e37cfe4a9f954d763614417644d31824cceca",
                "0x298084195200f42ec90385bd71a2db5ff93241de",
                "0xed0cd32bbfa16abf116819e66a683fbb9f311716",
                "0xf9c6126651cddd23e8c670170beca3d56cd4f2ce",
                "0x1bac8fb482d9603947bd39be9ccc601999d9d445",
                "0x66623b6a48998243fa28b7d6c7d63562885f4e2c",
                "0x42479fe1c362887da9f67757bac27db17bd76ef8",
                "0x3e97d87ed20a00f06bd4582de43ffb50c210a6e7",
                "0x01d8d6f2481ce17d83f19ff23b1dbcb41e6fdc7b",
                "0x4f204e5b75fe13abd9e656007dbaeae64e07939f",
                "0xe4f4674ad8e915869d9dca32d0e03503fd0a7be1",
                "0x95b09dfa9e6341ce19a9df99bf7b84dc3fd4d7b5",
                "0x50f26d9442e9b87856cf1257853a7b142d621960",
                "0x6199a78c6c8f2eb48adb39252d3ac1a48bac5e3b",
                "0x221fce6b6dac61520c1c283825e29bb556979111",
                "0xc0bf08682e29796f27652a26abd98650e374c97d",
                "0x76258e218decb8ae209151dacda5fb56b8ca8f8e",
                "0xb193d90ca445bc91652aae5f07b50b70efd354b6",
                "0x2b2f4b74adadec7af33230090458610a4a73556e",
                "0x022a1a15f507ccab6dce31ac56a644f3f3fd3be4",
                "0xa99e96dd0f86a3c7c64a31df7605c5728f77af2c",
                "0x3772a9878c9daea175f7ca1b6f7fbd68d463599e",
                "0x65dbe5ef037efee8f4a39403337ab8fea3a0ae5e",
                "0xf5b90defbc7c9415163dcc4e8623a25b93425e41",
                "0x08a3e1ae33190686835ac6713bc50fc8eee73303",
                "0xbea3b7eda9e6a9bb0e470e062025f4129412e9f1",
                "0x5db10cad9e5899f8a2f16239c30097e8405f088d",
                "0xe8d3c311912513ae9cc34016db69eae9e9f4e2cc",
                "0xe3382268800c2892f392741d6442ac9b73542087",
                "0x7933fa07e2fda700f8dab507599f677e1432b1a6",
                "0x3f34d22b55194d768cf4b2b41b501bc9daa8f5c3",
                "0xf6e0bccb72ac3e188d053f4f8c63f5721edf07a2",
                "0xc3fcc310a93aa34d2de86fe0c00f9ebdc395e1d9",
                "0xbef60a2d3e2a587e51a9a6d3cefdefd7fa0a7941",
                "0x0fde666a0f12a7d3bf708e1613834bd3937c3b3e",
                "0x84ab142e4014bba2499b65a9b0722e0f77a2bc59",
                "0x8c38026b67962b7b79a96d57a78fb22423d42da5",
                "0xabf864065fdeb8996eb5469d6c25aae84507d231",
                "0x893cc3bee790245cc5f9d81c883d2ebe3aaada5d",
                "0x6ce5f8b91bd7ddf2b1b55b182a8132442e4ead7a",
                "0x06df1645806ce293b747f0dc249ed674d55090d6",
                "0x707ea4dda42db173c5bbd89b769c3cc767b42360",
                "0x728d2bbd21686e51bf790c83092099bcc6d4e839"
        );
        List<String> list1 = Arrays.asList("0x6e77efcad4db1e016b8105e4e14c353097c4d25c");
        AccountUtilsEth accountUtils = new AccountUtilsEth();
        Map<String, Map<Integer, AccountDto>> map = accountUtils.batchSelect(list);
        String string = objectMapper.writeValueAsString(map);
        System.out.println(string);
    }

}
