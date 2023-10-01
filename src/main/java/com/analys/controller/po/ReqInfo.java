package com.analys.controller.po;

import lombok.Data;

import java.util.List;

/**
 * @Author gu丶
 * @Date 2023/7/24 10:23 PM
 * @Description
 */

@Data
public class ReqInfo {

    private List<String> tokens;

    private List<String> addressStr;

}
