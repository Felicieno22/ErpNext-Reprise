package com.erpnext.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ErpNextApiUtil {
    @Value("${erpnext.url}")
    public String erpnextUrl;

    @Value("${erpnext.apiKey}")
    public String apiKey;

    @Value("${erpnext.apiSecret}")
    public String apiSecret;

    public final RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates an HTTP entity with authorization headers for ERPNext API calls
     * @return HttpEntity with authorization headers
     */
    public HttpEntity<String> getAuthorizedEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }
    
    /**
     * Builds a URL for ERPNext API with filters
     * @param doctype The document type to query
     * @param fields List of fields to retrieve
     * @param filters Map of filters where key is field name and value is filter value
     * @return Complete URL for the API call
     */
    public String buildApiUrl(String doctype, String[] fields, Map<String, String> filters) {
        StringBuilder url = new StringBuilder(erpnextUrl);
        url.append("/api/resource/").append(doctype);
        
        // Add fields
        if (fields != null && fields.length > 0) {
            url.append("?fields=[\"" + String.join("\",\"", fields) + "\"]");
        }
        
        // Add filters if any
        if (filters != null && !filters.isEmpty()) {
            StringBuilder filterStr = new StringBuilder("[");
            boolean first = true;
            
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    if (!first) {
                        filterStr.append(",");
                    }
                    filterStr.append("[\"").append(entry.getKey()).append("\",\"=\",\"")
                            .append(entry.getValue()).append("\"]");
                    first = false;
                }
            }
            
            filterStr.append("]");
            
            if (url.toString().contains("?")) {
                url.append("&filters=").append(filterStr);
            } else {
                url.append("?filters=").append(filterStr);
            }
        }
        
        // Add limit_page_length
        if (url.toString().contains("?")) {
            url.append("&limit_page_length=500");
        } else {
            url.append("?limit_page_length=500");
        }
        
        return url.toString();
    }
    
    /**
     * Generic method to fetch data from ERPNext API
     * @param url The complete API URL
     * @return Response as a Map, or null if resource not found
     */
    public Map<String, Object> fetchData(String url) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getAuthorizedEntity(),
                Map.class
            );
            
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            // Ressource non trouvée (404)
            return null;
        } catch (Exception e) {
            // Log l'erreur mais ne la propage pas
            System.err.println("Erreur lors de l'appel à l'API ERPNext: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the base URL for ERPNext API
     * @return The base URL
     */
    public String getErpnextUrl() {
        return erpnextUrl;
    }
    
    /**
     * Fetch data with filters
     * @param doctype Document type
     * @param fields Fields to retrieve
     * @param filters Filters to apply
     * @return Response data
     */
    public Map<String, Object> fetchDataWithFilters(String doctype, String[] fields, Map<String, String> filters) {
        String url = buildApiUrl(doctype, fields, filters);
        return fetchData(url);
    }
    
    /**
     * Fetch data by ID
     * @param doctype Document type
     * @param id Document ID
     * @return Response data
     */
    public Map<String, Object> fetchDataById(String doctype, String id) {
        String url = erpnextUrl + "/api/resource/" + doctype + "/" + id;
        return fetchData(url);
    }
    
    /**
     * Create a new document via POST
     * @param url API endpoint
     * @param data Data to send
     * @return Response data
     */
    public Map<String, Object> postData(String url, Map<String, Object> data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(jsonData, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            return response.getBody();
        } catch (JsonProcessingException e) {
            System.err.println("Erreur lors de la conversion en JSON: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel POST: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Update an existing document via PUT
     * @param url API endpoint
     * @param data Data to update
     * @return Response data
     */
    public Map<String, Object> putData(String url, Map<String, Object> data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonData = mapper.writeValueAsString(data);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(jsonData, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Map.class
            );
            
            return response.getBody();
        } catch (JsonProcessingException e) {
            System.err.println("Erreur lors de la conversion en JSON: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel PUT: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Delete a document
     * @param url API endpoint
     * @return true if successful, false otherwise
     */
    public boolean deleteData(String url) {
        try {
            HttpEntity<String> entity = getAuthorizedEntity();
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                entity,
                Map.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            System.err.println("Erreur HTTP lors de la suppression: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel DELETE: " + e.getMessage());
            return false;
        }
    }

    /**
     * POST sans body pour run_method=submit (ERPNext)
     */
    public Map<String, Object> postWithoutBody(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + apiKey + ":" + apiSecret);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel POST sans body: " + e.getMessage());
            return null;
        }
    }
}
