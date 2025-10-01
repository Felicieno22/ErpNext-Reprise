package com.erpnext.service.rest.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Value("${erpnext.url}")
    public String erpnextUrl;

    public String apiKey;

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, Object> login(String username, String password) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> loginData = new HashMap<>();
        loginData.put("usr", username);
        loginData.put("pwd", password);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(loginData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            erpnextUrl + "/api/method/login",
            request,
            Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }

        return null;
    }

    public void logout() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "token " + apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        restTemplate.postForEntity(
            erpnextUrl + "/api/method/logout",
            request,
            String.class
        );
    }
}
