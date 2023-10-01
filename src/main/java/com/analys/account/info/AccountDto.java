package com.analys.account.info;

import lombok.Data;

import java.util.List;

/**
 * @Author gu丶
 * @Date 2023/7/22 6:00 PM
 * @Description
 */

@Data
public class AccountDto {

    //天数
    private String day;

    //卖出u
    private String income;

    //买入u
    private String outflow;

    //盈亏比
    private String profitRate;

    //买入
    private Integer totalBuy;

    //卖出
    private Integer totalSell;

    //买入/卖出数量
    private String totalBuyTotalSell;

    private List<String> sellTokens;

}
