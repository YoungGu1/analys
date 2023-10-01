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

    private List<Transaction> send(String address) {


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
        requestBody.setOffset(0);
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

        //发送请求
        List<Transaction> data = send(address);
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

        AccountDto fifteenDays = new AccountDto();
        fifteenDays.setDay("15d");
        fifteenDays.setIncome(income15d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        fifteenDays.setOutflow(outflow15d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        fifteenDays.setProfitRate(outflow15d.compareTo(BigDecimal.ZERO) != 0 ? income15d.divide(outflow15d, 2, BigDecimal.ROUND_DOWN).toString() : "0");
        fifteenDays.setTotalBuy(totalBuy15d);
        fifteenDays.setTotalSell(totalSell15d);
        fifteenDays.setTotalBuyTotalSell(totalBuy15d + "/" + totalSell15d);
        map.put(15, fifteenDays);

        AccountDto thirtyDays = new AccountDto();
        thirtyDays.setDay("30d");
        thirtyDays.setIncome(income30d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        thirtyDays.setOutflow(outflow30d.setScale(2, BigDecimal.ROUND_DOWN).toString());
        thirtyDays.setProfitRate(outflow30d.compareTo(BigDecimal.ZERO) != 0 ? income30d.divide(outflow30d, 2, BigDecimal.ROUND_DOWN).toString() : "0");
        thirtyDays.setTotalBuy(totalBuy30d);
        thirtyDays.setTotalSell(totalSell30d);
        thirtyDays.setTotalBuyTotalSell(totalBuy30d + "/" + totalSell30d);
        map.put(30, thirtyDays);

        return map;
    }

    public Map<String, Map<Integer, AccountDto>> batchSelect(List<String> list) {
        Map<String, Map<Integer, AccountDto>> map = new HashMap<>();
        list.forEach(s -> {
            Map<Integer, AccountDto> map1 = analyzingProfit(s);
            //过滤数据
            if (filter(1, map1) || filter(3, map1) || filter(7, map1) || filter(15, map1)) {
                map.put(s, map1);
            }
        });
        return map;

    }

    public Boolean filter(Integer day, Map<Integer, AccountDto> map) {

        AccountDto accountDto = map.get(day);
        String profitRate = accountDto.getProfitRate();
        double rate = Double.parseDouble(profitRate);
        if (rate >= 1.5) {
            return true;
        }

        return false;
    }


    public static void main(String[] args) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> list = Arrays.asList(
                "0xc6dfe719dd94d386c657cb42b2fb688f06e8f153",
                "0xbf21f6daf7c1fd5f0ea793ecd419934bedbe3bc6",
                "0x66ed3d288f613cf5ba8a33e6df295c5c406d483b",
                "0x1dfa01445f371196babf277d1025d34406918a75",
                "0xc31944b1ccd09d21246694fdf2f50fd19d2c0c7f",
                "0x78da108fd6223d5a10db5c79b8d7f0d3328b7011",
                "0xbe19629340e0eb5f2ab2567da7e555cd4ccd353c",
                "0x8b4e56a0f4c21f14f2e22f9cc9296d7c18c288ac",
                "0xf2ef28ee4ccc9a8c25ca963501b7c7f3274d205e",
                "0x156ee5dc423f7167ed2157c1c2ee628faadca15c",
                "0x5d3717399f813c1e31616740ff10827623dab7db",
                "0x0856e24ff939fb6ba1c59955b56acb7a38ec87d0",
                "0x4563f662e66fdac3183e7e0057d49c9db355fdeb",
                "0x10990353ac757362a45316ff265197419eae4b8b",
                "0xbb8024d7339b63e7586a1de0aa2f066a1c35cbb7",
                "0x17561a5db715190c3027ab9c35df0990ea4108ee",
                "0xfdd5dddf70653fbf75a46cc927d600013c9f8f50",
                "0xba04a27df6b95b4ef755429d30153473ca11657b",
                "0x6adadd20f709df0176d227f91b8cf6ce9d92c78e",
                "0x9014db7461f9cd147c8558767a59bcfa9c1e4801",
                "0xc5a1d2b790cdec2a39662da8cf3eb9ad80c5b6d2",
                "0xfec181056ba0116fe489755bb3b76110b359e6e9",
                "0xbb59752c7fa21aef83ce977a6d7e98f1dbc2f0c8",
                "0xec8cbaba9342028bb65fb12e21d602398d627669",
                "0xe8288cba3ee3b8706c285c0bc8f13ff3941ddf8d",
                "0xa6ee0249dd5ccc040115152a9fc5a6c0b9be78e9",
                "0x6b5276ca35b164a9192f084d049a4b2ffdfb3fa4",
                "0x37ec1c5bdb3863d783c581db0447d978b5ba3aae",
                "0x1b6c3ab4eb35cd47c980bd49efc5da504bc075e6",
                "0x29b6e3d356ba18c0e4d5ebe9e5296705fcd58755",
                "0x1d46429bd72cefff90842103437ed414859f76b1",
                "0x3fd5afbf2b6ba31e1248591f6c6a2dc14777ad6d",
                "0x8fbd1a56024ae38d28f971cb5720fdd005c396e9",
                "0x44474b972741d5db60a59e6b0eae97e8837db755",
                "0x5aca89d809b0fdf620321e17a9eaf12f0a6552ad",
                "0xeb75f1b15f60531dfb374c765666bb52edb5997c",
                "0x93c52f37b1a21fbdaf4cf2c5b3b1390d1170a967",
                "0xc5ae7c1280b76b94cf70ad622cba20e56a7bfc62",
                "0x8dc6a4c793d7b0299a48c7af1b08ab46b269e108",
                "0x00000ac04f69f8b33d7a578e1522a56d6b830bf6",
                "0x6666b6caacf4e8dbf4939c539a44b98d523608aa",
                "0x3fed7c2f2f9c153f79af62ff763c415302b74dea",
                "0xc3a8cbbfa2e3cceef1719b11b757875c7e1acb0f",
                "0xc612d675dc5b41367286dbca2430b6dba61e1f85",
                "0x2fa82ea202415147c9a35ecc85a65f8331d20a7f",
                "0x44895d12f341a1675bc2452be43a237fb044e26b",
                "0xf891bf97f1a51d57bd4d2568f94a9f8882162050",
                "0xd60b56a644b61680c2a45d898b5b2051f6fae7d4",
                "0xf928cf5ae2811d4591e76f29802189c08b8fea53",
                "0xa2b9dc38196059138c4e2c2f3461417cc6d59a97",
                "0x17b3570fb79c42443bd73afd6e348ad8fde6efaf",
                "0xf7008d476aab084bd9c916954267c837a47d4d4a",
                "0xba11577cd47b5c42394ee5da2ee5748c7e65b73c",
                "0xdbb643740ce08ad124dd5ca747845fe798f12624",
                "0x418a7169b213473bb6d9891d5489b8776c59690a",
                "0x9fc5389c66bbfac23b77bdef85700672928de555",
                "0xc6d065e48fc3848328d3b8eefbe38f0068542d9c",
                "0x2c2c9bbcca951aabaca414fe6a968d79e59f883a",
                "0xa09b5a000be35bbb21053d0f6b07eba4780146c0",
                "0xd1beffd3d190c5b8f1c19e861d519d2c80193541",
                "0x372db64511c736731c8670da062190a0d0fd0288",
                "0x949f02fc7428499a89bdc28675af6370719b272f",
                "0x2baad8eef4bb88a13827ac58b3b6d87de9c33c48",
                "0x034e4a9ce7e79bf6b2868c048111d9d45d930a69",
                "0x338c4ab1a185655275321e7c191d60c0dfcd821f",
                "0xb1f794d8f0d2a1099857a300c7598c0305af7747",
                "0xbad2750b1d8054104015ef3199bb3d53703e02c1",
                "0xe9a3cefd7808cdcfdab14910d00b954efa220dce",
                "0x64481b235029334dd23a6baa8b85e60759572424",
                "0x10ebbc1989d9283c6a27f46e8fb0c559857d3c33",
                "0x3fdc280398bb761919fb94550cc75c3a4749af5e",
                "0x592328950911934da188e55b37ff4d198b9447ae",
                "0x98e869e71bf4596135988ee07150f967b6096172",
                "0x404404b3dd1f55c978f44c05ead792c93d73568c",
                "0x081744f3dc400679f990d5cb863c53abf6cc3662",
                "0xcd9d9508330923996e2fddbfeab3c4858a863cf3",
                "0x85222076c6d45a76e65889776192c95eb24870d9",
                "0x2683e267b1dfd0a480f311c015a5b95e013fc905",
                "0xc479f01c262433848df4be12861ebed8169be5ae",
                "0xc3db93db068c94af870ac1cb1a8c8f81cdb7c205",
                "0x08eeaa21ff7006a45d3c6f75d91911ff7ebe7469",
                "0x7f011259b2d621ffa2882d1eece03c73a13c1573",
                "0xa35fd945981aec08994cc0db490edffa16c72570",
                "0x398cffe4bfcdffccb5a2a31c2a7f1d633bd24ddb",
                "0x84a21710101bd3227ffee5e5e7daa01a526037d1",
                "0xdd749e56e1dad9536a3e72418e701697a0bb5937",
                "0xd2dfbd9344b96f576af2cc86f9686124913c0390",
                "0xec4433d8d8edb6a6d11cf24ae6f46b9d3bb72a06",
                "0x11c2d91ea57c0560bc90895c8f7ad5ed9f28fbea",
                "0x421d3268b80d36d9e5288ae5d504cb066b2664e4",
                "0x00345bb8270bf08b561d0419000f2928e4b97194",
                "0x80af5fce2d0ba4888034c2b2a956c6ab9040dddf",
                "0x26e8285e16f120ca885d5f5d5e57315be45487f0",
                "0xf7b473b5eca831f9adc4d297f053f359dd7420b3",
                "0x5abed07b393f96d5396252ba7ba0fa1f1a7f5e60",
                "0xd2c39239a4215830fc347a5b5a043332a4df131b",
                "0x5bcce3eabdf5a1e862a38dbd3e5f983825263eec",
                "0x725735208fc6fcf939dc660a26941ac94dcc1b72",
                "0xf03ff260b31c781b69ed925dd91bf165613cef5d",
                "0x8afd79f9abe19a020288e6d2dbf5907334b5ef89",
                "0xf2d8b9b08a34a6c7f62fa0595509f114accf5c15",
                "0x9ee317a6f292c674647b6020fa0fbd92219e15ae",
                "0x40fe9085e13aedabfa302164fa8e7748c4a00564",
                "0x8650a8c2d583c9e171262b65a2d567cc9cb64bd2",
                "0xf7480e770ccf6ebbe815334dbc34b170533697d7",
                "0x173014b646799f7d1cb4a7b663929e4bca83bef2",
                "0x49aad2de6e5126266347b18b76077a86722d619b",
                "0x204ff27357228028d73a3b5263e442784e6c05b3",
                "0xe6c5c6430c24bc8c065aa80cbae36efa31f955de",
                "0xe83fa7236b50409719c86bab8d29c8ee6461213f",
                "0xbd07cafeded3acf4bf5c5ed25c56dde5618d97f0",
                "0x838d06e3c695a66ca62611a33026f2f3841ad5fc",
                "0xd460dd4f7ac93ffe7e3e899af729ed26efd267f3",
                "0x8dc885587c1e46ef49d9803e1cf1634d5adb121c",
                "0xb5e3cee62a5d46e15d520b6b3596dd4c8c079307",
                "0x2e28f281628a784557a9646bd7b5bab27c1e53ff",
                "0xaa9c56ad6bad9caf0c047dbc07896e790fc5f4c9",
                "0x2db1a90f9df69a35ef212d8c5aabcaf8570cfd7b",
                "0xf02696530693709b131e997954bca486aaba0376",
                "0x88fd308ac8b59e2415dbbbde6a6c39daff679b88",
                "0x77c63ac4b607b60188dffe35a80856ede452c50c",
                "0x1cf42a4eda4ec85e53d0d5d08ae5ddf8a81259f4",
                "0xcc1db1a7ef4571bda032abdbe63ad45126fa377b",
                "0x059d2de31da0af7b0fe48dc1317a95a8de3563ab",
                "0x88c5328b7a4447857d9431d5f4399d3607e4e0f9",
                "0x3d1ce143265f9d03696c09f15c9ae0bd4e217560",
                "0x2480ac800795786ac349a543af39bea2cddf432e",
                "0x78c6e8f13eaf80d2e3c78753d1832c555a24f945",
                "0x5b05c607b0c540a373140494c9a3c8f7aa859f98",
                "0x913675b4ba6ab19a42940080214cb0a4af509dce",
                "0x6835e5f524475e55ab12f8322efbfda4b2a8f2bc",
                "0x3278ee1f950d059ba8896bceb1553097b83f07f9",
                "0x66b34bdc3f1aac404c52c99471e02c8a3adf759b",
                "0xfbdf6c156bcd99943dadf5476a5dbeb53f7968b9",
                "0xa6bcee95d8c4ecf7bfaa819f9f90042c1af81169",
                "0x83ebbf250202da4308ab14d77f54da12a37cbb84",
                "0x2e20af0c28d355e40576bd3c821abddac675d96d",
                "0x017eef5cab4dbaf7cb3800485d5903a28ee0756f",
                "0x76ae071475a48cdb1d219def5171dd59bd5fd797",
                "0x0000005620c87c60dbb4c07ae7868366772ef40b",
                "0x8e486c8169bd2aeb7cfde5722505f45e0b9850c2",
                "0xfa5f16e35545900ac3e64c7d5ea349e79d9ffe71",
                "0x52b09709f434421af7591d70d78c98bffda8c7dc",
                "0x2926a3b0477b8da3bfd3d38e0dd97fc86a683fff",
                "0x0d16fb0393cfef68a119b2e545d7bafb793c8676",
                "0xa07bc41820efa2e86180aded46adbda0aeb89bb4",
                "0xb58157521b4c06c837c4fa8b9517984cc3401999",
                "0xfc18d7a465fb94a0b1db45559507b69734e4c389",
                "0xd33b9876c36b3afac3465876f8d9fea9a0351ae0",
                "0x18363d7664143dcdd76a5f0f1982f9c013817776",
                "0x319017a078dd367396a2c666b4a7db10fb65fa5a",
                "0xe0b0e346707bfc5346cd59adab0f58b0ba1ca344",
                "0x26bdf6bfa0677f86f8b27868823b4685f6b9b6e4",
                "0x3fe643095a759ebfa6186617e341b698c40c5233",
                "0xb95fb7fa5a72f701133d3174d4ebf6a8ceb66aef",
                "0x7ea58425fb3f871fd01ae18c157eeec8880701bc",
                "0x2930af95cb775f47d6a14d9317480cd06bfb4033",
                "0xda0e276cdc520232afaec4195b836dc5d790ea15",
                "0x9043020b39615b9331d48505f2638a45622fda58",
                "0x8e8bc7e7d08cdf05e1d42b669d5acbf60d8a81a0",
                "0x7e94cca9afe2a13503f2cbb106a460f8013e448b",
                "0x43c6c931eb89596a20545fc02d967ee89fd01520",
                "0x3ca9ec53d076784dca3d8bb467e7ff78c29c73c4",
                "0x0b347ad9dc91e6a5b90cd10a042c07fb2b69f302",
                "0x4d106a9ca15d358af6578c501140403d74f84d23",
                "0x08931d3cec8a73a9ff00b51279be9b5773f9e913",
                "0x60c6ff241cefabb5b25953666a89eac677a04597",
                "0xc1022678a2f79ef3fd9f079eac0940cc304299b1",
                "0xc2dbc338af8baba47ebb7e90a811894905b78492",
                "0x5f6910808de7c66c486adb648413aca26e1cfd78",
                "0xa48ccb5ceb730885f743b4b3c02392237782e313",
                "0x87ae4487461d258bbd856f399defc597472b8a71",
                "0xca15dd35b29206d6cf81d4e9c5d6bc2e5ae4fdca",
                "0x16d58abb9ca3410e839a30e3018105e9f9dd587d",
                "0xdda9a70a9b4d66e5cb7dbef1f9610f4fec6da1bc",
                "0x62c1fea9d3969abc80f80881f81e78988887aa65",
                "0x0c85ddf61a9ff85c53e757cf61544995a58cfe36",
                "0x77e06a2395f278c0c166dae10f54e56f1ea78a8d",
                "0x2ba58e79e31de14817b47c871803a5d3bccb54d9",
                "0xde703a13ac8c767865e07d3638e33be66abf8690",
                "0x472730ceb9e385f231632e3f4a96da2a43170072",
                "0x2a8c12159608f9a52c8c137b99d33f3cce9c9373",
                "0x664ff1397fe31727f1d47f7685bdbe67b13f5bf1",
                "0x99160e9b9ccfe4053323802662025762c7c28308",
                "0x9787241f1e7aedeff9935fef2e55c7fe4da7b30f",
                "0x237386610c5ee51c0c82b1dcf89c463aaab048ba",
                "0x8bfc38d3dd79fb83e52b7f41c2e9b2a64fd2d72f",
                "0xf1bff8b72aa4364c81be300c8e8d3b546bb733dd",
                "0x601e03f171f987fc703331b40e40d38f3388d505",
                "0x03b37edd43e8fd102b334d22a9d2422f1c2c2b42",
                "0x3333333dc5c061842b301fbac4230c597a24f8b8",
                "0x711ddfeca4ab0a305dbeab6a1b3f3f1a79b02d3c",
                "0xac2ff632874ea3ce1c64110ddc159580041a597d",
                "0x821491509e8abd73a72ee945610438bf0bfcace8",
                "0xae55dc13df5d4a7df92e683811696942f4bd5c62",
                "0xe13fd39119fa24fc503962188c45c1ad83f2bb69",
                "0xf29318ef75c5e25422fbc0814a865c45d62a46d8",
                "0x6a0c9049814dad09c7f36c20a5a4a94492b6a63d",
                "0x117dc38dfe7eb7a7f2d852778df871a548c6a5b8",
                "0xf5b161cc99296c880aa0b4a1495f06ddfd8b45da"
                );
        List<String> list1 = Arrays.asList("0xd42b35cb1fc08fa524c4bc566d20741146af432c");
        AccountUtilsEth accountUtils = new AccountUtilsEth();
        Map<String, Map<Integer, AccountDto>> map = accountUtils.batchSelect(list1);
        String string = objectMapper.writeValueAsString(map);
        System.out.println(string);
    }

}
