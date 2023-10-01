package com.analys.base;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author gu丶
 * @Date 2023/7/22 10:23 AM
 * @Description
 */
@Data
public class TransactionRequest implements Serializable {

    private String sort_by;
    private int limit;
    private int offset;
    private String order;
    private boolean with_full_totals;
    private List<String> transaction_types;
    private String token_status;
    private DateRange date;
    private String current_token_id;

    private String account;
    private String network;


}
