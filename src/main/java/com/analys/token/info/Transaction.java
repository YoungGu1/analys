package com.analys.token.info;

import lombok.Data;

import java.util.List;

/**
 * @Author gu丶
 * @Date 2023/7/22 10:48 AM
 * @Description  请求返回的数据
 */

@Data
public class Transaction {

    private String type;
    private String network;
    private long timestamp;
    private String transactionType;
    private String transactionAddress;
    private List<String> tokenAddresses;
    private List<String> symbols;
    private List<String> wallets;
    private List<String> walletsCategories;
    private List<Double> amounts;

    //价值多少U
    private double amountStable;
    private double amountNative;
    private List<Double> amountsStable;
    private List<Double> amountsNative;
    private List<Double> pricesStable;
    private List<Double> pricesNative;
    private String poolAddress;

    //如果是eth的话，意味着是买入
    private String fromAddress;

    //如果是eth的话，意味着是卖出
    private String toAddress;
    private LPToken lpToken;
    private String sender;
    private String to;
}

class LPToken {
    private String address;
    private String symbol;
    private double amount;
    private double amountStable;
    private double amountNative;
    private double priceStable;
    private double priceNative;

    // Getters and Setters (Constructor can also be added if needed)
}