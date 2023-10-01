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

/**
 * @Author gu丶
 * @Date 2023/7/22 1:43 PM
 * @Description
 */

@Service
public class AccountUtils {

    private List<Transaction> send(String address) {

        String url = "https://api.dex.guru/v3/tokens/transactions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("sec-ch-ua", "\"Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"115\", \"Chromium\";v=\"115\"");
        headers.set("traceparent", "00-ed189441aa4ec8edf158c4d9801c8644-0243936e18a9e91d-01");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        headers.set("Referer", "https://dex.guru/");
        headers.set("X-Session-Id", "5324051a67de4dc9be994f60810ed3d7");
        headers.set("sec-ch-ua-platform", "\"macOS\"");


        TransactionRequest requestBody = new TransactionRequest();
        requestBody.setAccount(address);
        requestBody.setSort_by("timestamp");
        requestBody.setLimit(10000);
        requestBody.setOffset(0);
        requestBody.setOrder("desc");
        requestBody.setWith_full_totals(true);
        requestBody.setToken_status("all");
        requestBody.setNetwork("bsc");

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
        data = filterTransaction(data);


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

        String[] mainTokens = {
                "0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c",
                "0x55d398326f99059ff775485246999027b3197955",
                "0xe9e7cea3dedca5984780bafc599bd69add087d56",
                "0x8ac76a51cc950d9822d68b83fe1ad97b32cd580d"
        };

        List<String> listSell = new ArrayList<>();
        for (Transaction v : data) {
            if ("erc20".equals(v.getType())) {
                continue;
            }

            if ("mint".equals(v.getTransactionType())) {
                continue;
            }

            String tx = v.getTransactionAddress();
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

                long timestamp = Long.parseLong(String.valueOf(v.getTimestamp()));
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
            AccountDto accountDto3 = map1.get(3);
            double doubleNumber = Double.parseDouble(accountDto3.getOutflow());
            int a = (int) doubleNumber;
            double doubleNumber1 = Double.parseDouble(accountDto3.getIncome());
            int b = (int) doubleNumber1;

            Integer totalBuy = accountDto3.getTotalBuy();
            Integer totalSell = accountDto3.getTotalSell();

            if (b - a >= 100 && totalBuy >= 1 && totalSell >= 1) {
                map.put(s, map1);
            }
        });
        return map;

    }


    public static void main(String[] args) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> list = Arrays.asList("0xFc61D342b22891eb387A8B080ca38Ae11c63aB38", "0x529AD520c8cADb38B5762EdEb1131E26868cE47d", "0x128Dea78B1890e3402ee1c323E91a392a78bfe23", "0x3d6d0877bd7fA52B40455f6dc8e53fB12b71888A", "0x5e45CfD350D0FEF4982b4134ed5b6E7f88B89318", "0xA00F3925EB02a0deDc9d0762053ab19D7ec13023", "0x3E446ab9Bd01896D706E49b3b576519635173515", "0x4a75811c3457C5b65d8aEebc667A75F3C8D59B2E", "0x944633fD0d28102355C4AE1B8347528Fd8Df5C45", "0x31851b581566512E0DaA74d24E382e19aA6369d3", "0xBB2B03525b165AD435C4705Cd55B0C1ab6d4F6Af", "0x9A7a3D8Ac1Bcd2c583F380Fc0b0C50Ef2EFc8598", "0xbbB64ab3D75D2E37Ac4C7FB3d7E5eD8Dcf626902", "0x53700601B05FB39A5b77071a52826d7c3D911c7B", "0xE8ED3F98b365625F3010F8d2E2B824eb91F48826", "0x8393D540933DaE78DFa253E35d8e1Ac5271C775d", "0xFA4245a696b4903d5D72426676d64B955e6b5213", "0x17f371664bD22217eB1A60be3c3C1D6139D59d23", "0x9Bc1c8bDa3C74779fb546Cfa2e2c50F61718f4DF", "0xfFdb25D828e582B20A5B937367aC3401c0952892", "0xdaD21476Be6ffEf2D7B003623F7c3623aE9AE23f", "0x5808E746fA0BE6dc4bc515B7a3bE69cF4e73eF3A", "0x19239b6C31AeC70F96174e0e8C0791DC50eAa465", "0x9A7a3D8Ac1Bcd2c583F380Fc0b0C50Ef2EFc8598", "0xab2a296eD0c3A8e9282135153A766d84485c3E7f", "0x342894CEE98b2da987fD2aba9ca372c39eDe4e69", "0xD6a026E9C26C606A35122C3d55B218D5Da666666", "0x54b0E93d7d3cE9F3908A190c0ea75D1BF73bB674", "0x40aBDBbE7f58e4Dd5DB2AdB655c1bFd71C9CaB95", "0x6088ca510FB3e033e00c56aEA40CD4b04fa88640", "0x2411460d7A5B7a3F682ee0987857343BA2b4E988", "0x4d0596ffA992F6E1b31883e001A58f2cE892467B", "0x921489Dd09032Da710A99eF2A506965A6633f443", "0x8ddCaa6888aEE6738b9B31265DD20f31afDBC5ad", "0xa464936c99018B8C5442CaBa62d81E2B1d523AeE", "0x5C1917046982e7f90A07a252C30D0e92A2004a35", "0x5B846D06D2FBa651417a699A3744D4905871c71a", "0x21172Be83B21a1a463Df5Fd6DBEc1f8D7cA07488", "0x823D2c337b9Ec4ed0e2F17Ea27BaE1A6f603d589", "0x3FeE8eB848A1a4f4a11395B12c31b4BcAe9a2EF2", "0x62875db09c36A13278d530518c4fDC4a1Dc5c383", "0xaB66B0920c1cfe30F36405F373e67C0060F7F3C6", "0x0E044a98471561C7B59Cc8A3E6278aD768721BD0", "0x1724c82f45bE9De4D5B4d5B623FC188DB2d753CE", "0x780a5936f08f208d8d5eA96eA8375Bd4490F67b6", "0x171335D7Bc413763888bD9e485902e6Fa0548A3d", "0x577F0742296Bc4e1A8EBF12FCff03d635360da22", "0xbbB64ab3D75D2E37Ac4C7FB3d7E5eD8Dcf626902", "0x19239b6C31AeC70F96174e0e8C0791DC50eAa465", "0x9A7a3D8Ac1Bcd2c583F380Fc0b0C50Ef2EFc8598", "0x577180746102ba6Bf195ef9c234159540a528888", "0x4a75811c3457C5b65d8aEebc667A75F3C8D59B2E", "0x7BbEAC12aDE744f7BFaCadfAFCbd72bb0b783C76", "0x823D2c337b9Ec4ed0e2F17Ea27BaE1A6f603d589", "0x1F7613753E27006a8335972092B0048F476E6cBE", "0x524B6ef774b5B39275Bbb5922F8416a10C948127", "0xD0aBE1da6D50fC85d7851f4D605F6CeD07e518D0", "0x9Bc1c8bDa3C74779fb546Cfa2e2c50F61718f4DF");
        AccountUtils accountUtils = new AccountUtils();
        Map<String, Map<Integer, AccountDto>> map = accountUtils.batchSelect(list);
        String string = objectMapper.writeValueAsString(map);
        System.out.println(string);
    }

}
