package com.erpnext.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.erpnext.utils.ErpNextApiUtil;

@Configuration
public class ErpNextConfig {
    
    @Bean
    public ErpNextApiUtil erpNextApiUtil() {
        return new ErpNextApiUtil();
    }
}
