package com.erpnext.service.rest.salary;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.erpnext.utils.ErpNextApiUtil;

@Service
public class SalaryStructureService {
    public static final Logger log = LoggerFactory.getLogger(SalaryStructureService.class);
    @Autowired
    public ErpNextApiUtil erpNextApiUtil;

    public Map<String, Object> createSalaryStructure(Map<String, Object> structureData) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Structure";
        return erpNextApiUtil.postData(url, structureData);
    }

    // Soumet une Salary Structure (docstatus=1)
    public Map<String, Object> submitSalaryStructure(String name) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Structure/" + name;
        Map<String, Object> body = new HashMap<>();
        body.put("run_method", "submit");
        Map<String, Object> response = erpNextApiUtil.postData(url, body);
        log.info("[ERPNext][DEBUG] Soumission Salary Structure '" + name + "' : " + response);
        return response;
    }

    public Map<String, Object> insertSalaryStructure(Map<String, Object> structureData) {
        // Ancienne méthode, à migrer vers createSalaryStructure si besoin
        return createSalaryStructure(structureData);
    }

    // Vérifie si une structure salariale existe déjà par son nom
    public Map<String, Object> getSalaryStructure(String name) {
        String[] fields = new String[]{"name"};
        java.util.HashMap<String, String> filters = new java.util.HashMap<>();
        filters.put("name", name);
        return erpNextApiUtil.fetchDataWithFilters("Salary Structure", fields, filters);
    }
}
