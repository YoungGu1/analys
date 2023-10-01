package com.analys.controller;

import com.analys.account.AccountUtilsEth;
import com.analys.account.info.AccountDto;
import com.analys.controller.po.ReqInfo;
import com.analys.service.TokenService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Author gu丶
 * @Date 2023/7/24 9:59 PM
 * @Description
 */

@RestController
public class TokenController {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private AccountUtilsEth accountUtils;


    /**
     * 根据token查询盈利的地址
     *
     * @param
     * @return
     */
    @RequestMapping("/getAddressByToken")
    public String getAddress(@RequestBody ReqInfo reqInfo) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Map<Integer, AccountDto>> map = tokenService.batchGetThreadProfitByToken(reqInfo.getTokens());
        System.out.println("------------------------");
        System.out.println(map);
        String string = objectMapper.writeValueAsString(map);
        return string;
    }

    @RequestMapping("/getAddressByAddressStr")
    public String getAddressByAddressStr(@RequestBody ReqInfo reqInfo) throws JsonProcessingException {
        Map<String, Map<Integer, AccountDto>> map = accountUtils.batchSelect(reqInfo.getAddressStr());
        ObjectMapper objectMapper = new ObjectMapper();
        String s = objectMapper.writeValueAsString(map);
        System.out.println(s);
        return s;
    }


}
