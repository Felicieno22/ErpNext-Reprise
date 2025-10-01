package com.erpnext.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration web pour activer les méthodes HTTP supplémentaires (PUT, DELETE, etc.)
 * à partir de formulaires HTML standards
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Filtre qui permet de convertir les requêtes POST en PUT, DELETE, etc.
     * en utilisant un champ caché _method dans le formulaire
     */
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();
        filter.setMethodParam("_method");
        return filter;
    }
    
    /**
     * Filtre qui permet de traiter correctement le contenu des formulaires
     * pour les méthodes autres que GET et POST
     */
    @Bean
    public FormContentFilter formContentFilter() {
        return new FormContentFilter();
    }
}
