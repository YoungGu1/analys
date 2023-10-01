package com.analys.token.info;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author gu丶
 * @Date 2023/7/22 11:55 AM
 * @Description
 */

@Data
public class AccountInfo {

    //卖出u
    private BigDecimal income;

    //买入u
    private BigDecimal outflow;

    //地址
    private String address;

    //利润U
    private BigDecimal profit_stable;

    //利润(bnb)
    private BigDecimal profit_native;

    //总交易量
    private Integer total_trade;

    //盈亏比
    private BigDecimal profit_rate;

}
