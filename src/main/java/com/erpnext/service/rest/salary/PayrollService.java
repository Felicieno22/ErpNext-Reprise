package com.erpnext.service.rest.salary;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import com.erpnext.utils.ErpNextApiUtil;
import com.erpnext.service.rest.utils.ErpNextDocumentService;

@Service
public class PayrollService {
    public static final Logger logger = LoggerFactory.getLogger(PayrollService.class);
    @Autowired
    public ErpNextApiUtil erpNextApiUtil;
    @Autowired
    public ErpNextDocumentService erpNextDocumentService;

    public Map<String, Object> insertPayroll(Map<String, Object> payrollData) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Slip";
        return erpNextApiUtil.postData(url, payrollData);
    }

    // Vérifie s'il existe déjà un bulletin de salaire pour cet employé et cette période
    public Map<String, Object> getSalarySlip(Map<String, Object> payrollData) {
        // On suppose que payrollData contient "employees" (liste), "start_date" et "end_date"
        if (!payrollData.containsKey("employees") || !payrollData.containsKey("start_date") || !payrollData.containsKey("end_date")) {
            return null;
        }
        // On prend le premier employé (cas import classique)
        Object employeesObj = payrollData.get("employees");
        String employee = null;
        if (employeesObj instanceof java.util.List<?> list && !list.isEmpty()) {
            Object emp = list.get(0);
            if (emp instanceof Map<?,?> empMap && empMap.containsKey("employee")) {
                employee = String.valueOf(empMap.get("employee"));
            }
        }
        if (employee == null) return null;
        String startDate = String.valueOf(payrollData.get("start_date"));
        String endDate = String.valueOf(payrollData.get("end_date"));

        java.util.HashMap<String, String> filters = new java.util.HashMap<>();
        filters.put("employee", employee);
        filters.put("start_date", startDate);
        filters.put("end_date", endDate);
        // On peut ajouter "company" si besoin
        String[] fields = new String[]{"name"};
        return erpNextApiUtil.fetchDataWithFilters("Salary Slip", fields, filters);
    }

    // Nouvelle méthode pour soumettre un bulletin de salaire
    public Map<String, Object> submitPayroll(String payrollName) {
    String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Slip/" + payrollName;
    logger.info("[PayrollService] URL de soumission (PUT) : " + url); // Log de l'URL

    try {
        Map<String, Object> payload = new HashMap<>();
        payload.put("docstatus", 1);
        Map<String, Object> response = erpNextApiUtil.putData(url, payload); // Envoi d'une requête PUT pour soumettre le bulletin
        logger.info("[PayrollService] Réponse de soumission : " + response); // Log de la réponse

        // Vérifiez si la réponse contient un champ de succès
        if (response != null && response.containsKey("success") && Boolean.TRUE.equals(response.get("success"))) {
            logger.info("[PayrollService] Soumission réussie pour le bulletin de salaire : " + payrollName);
        } else {
            logger.error("[PayrollService] Échec de la soumission pour le bulletin de salaire : " + payrollName + ", réponse : " + response);
        }

        return response;
    } catch (Exception e) {
        logger.error("[PayrollService] Erreur lors de la soumission du bulletin de salaire : " + payrollName, e);
        throw e; // Relancer l'exception si nécessaire
    }
}

}
