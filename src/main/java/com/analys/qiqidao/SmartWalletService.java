package com.analys.qiqidao;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

/**
 * @Author gu丶
 * @Date 2023/8/29 11:11 PM
 * @Description 根据77dao网站找聪明钱包
 */
public class SmartWalletService {

    public static void main(String[] args) throws IOException {
        String url = "https://api.77dao.io/smart/contract?address=0xb1c2389946b34fab89ed9ee2d3573225bc80317e";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "*/*");
        headers.set("Accept-Language", "zh,zh-CN;q=0.9");
        headers.set("Connection", "keep-alive");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Origin", "https://77dao.io");
        headers.set("Referer", "https://77dao.io/");
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-site");
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36");
        headers.set("sec-ch-ua", "\"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Google Chrome\";v=\"116\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiI3N2Rhby5pbyIsImlhdCI6MTY5MzMyMTI3NiwiZXhwIjoxNjkzMzIzMDc2LCJuYmYiOjE2OTMzMjEyNzUsInN1YiI6Ijc3ZGFvLmlvIiwianRpIjoiMHhlMjgxYWIyMjExMzVlYjE3MTY4OWFiNzg4YWYyZmQxZjQ3YWZjZTg5In0.v0RYsHCtDAE8EVINBmpHak43GkPUGnoFGC22QMXwuBQ");

        RestTemplate restTemplate = new RestTemplate();

        RequestEntity<Object> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, URI.create(url));

        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

        // You can process the response using responseEntity.getBody()

    }


}
