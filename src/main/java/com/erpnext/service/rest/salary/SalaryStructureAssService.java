package com.erpnext.service.rest.salary;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import com.erpnext.utils.ErpNextApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SalaryStructureAssService {
    public static final Logger log = LoggerFactory.getLogger(SalaryStructureAssService.class);
    @Autowired
    public ErpNextApiUtil erpNextApiUtil;

    @Autowired
    public com.erpnext.service.rest.utils.ErpNextDocumentService erpNextDocumentService;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE;

    public Map<String, Object> createSalaryStructureAssignment(Map<String, Object> assignmentData) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Structure Assignment";
        return erpNextApiUtil.postData(url, assignmentData);
    }

    public Map<String, Object> assignSalaryStructure(Map<String, Object> assignmentData) {
        // Ancienne méthode, à migrer vers createSalaryStructureAssignment si besoin
        return createSalaryStructureAssignment(assignmentData);
    }

    /**
     * Génère l'URL de soumission pour une Salary Structure Assignment
     */
    public String getSubmitAssignmentUrl(String assignmentName) {
        // Format standard ERPNext pour soumettre un document
        // /api/resource/Salary Structure Assignment/{name}?run_method=submit
        return erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Structure Assignment/" + assignmentName + "?run_method=submit";
    }

    /**
     * Appelle l'API de soumission pour une Salary Structure Assignment
     */
    public Map<String, Object> submitSalaryStructureAssignment(String submitUrl) {
        // POST sans body (run_method=submit)
    Map<String, Object> body = new java.util.HashMap<>();
    body.put("run_method", "submit");
    Map<String, Object> response = erpNextApiUtil.postData(submitUrl, body);
    // Log pour le débogage, similaire à SalaryStructureService
    log.info("[ERPNext][DEBUG] Soumission Salary Structure Assignment via URL '" + submitUrl + "' avec body '" + body + "' : " + response);
    return response;
    }

    /**
     * Récupère toutes les Salary Structure Assignments d'un employé.
     * Le filtrage par date est effectué côté Java afin d'éviter une dépendance
     * aux opérateurs de filtre ERPNext.
     */
    public List<Map<String, Object>> getAssignmentsByEmployee(String employeeId) {
        String[] fields = {"name", "from_date", "docstatus"};
        Map<String, String> filters = new HashMap<>();
        filters.put("employee", employeeId);
        String url = erpNextApiUtil.buildApiUrl("Salary Structure Assignment", fields, filters);
        Map<String, Object> resp = erpNextApiUtil.fetchData(url);
        List<Map<String, Object>> result = new ArrayList<>();
        if (resp != null && resp.get("data") instanceof List) {
            result.addAll((List<Map<String, Object>>) resp.get("data"));
        }
        return result;
    }

    /**
     * Supprime (cancel + delete) toutes les SSA d'un employé dont "from_date"
     * se situe dans l'intervalle donné (inclusif).
     */
    public Map<String, Object> getActiveAssignmentByEmployee(String employeeId) {
        List<Map<String, Object>> assignments = getAssignmentsByEmployee(employeeId);
        LocalDate today = LocalDate.now();

        return assignments.stream()
            .filter(ass -> {
                Object statusObj = ass.get("docstatus");
                if (!(statusObj instanceof Number) || ((Number) statusObj).intValue() != 1) {
                    return false;
                }

                Object fromDateObj = ass.get("from_date");
                if (fromDateObj == null) {
                    return false;
                }
                try {
                    LocalDate fromDate = LocalDate.parse(String.valueOf(fromDateObj), ISO);
                    return !fromDate.isAfter(today);
                } catch (Exception e) {
                    log.warn("[SSA][FILTER] Impossible de parser from_date '{}' pour l'assignation {}", fromDateObj, ass.get("name"));
                    return false;
                }
            })
            .max(java.util.Comparator.comparing(ass -> LocalDate.parse(String.valueOf(ass.get("from_date")), ISO)))
            .orElse(null);
    }

    public void deleteAssignmentsInPeriod(String employeeId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> assignments = getAssignmentsByEmployee(employeeId);
        for (Map<String, Object> ass : assignments) {
            String fromStr = String.valueOf(ass.get("from_date"));
            if (fromStr == null || fromStr.isEmpty()) continue;
            LocalDate fromDate;
            try {
                fromDate = LocalDate.parse(fromStr, ISO);
            } catch (Exception ex) {
                log.warn("[SSA][SKIP] Impossible de parser from_date '{}' de l'assignation {}", fromStr, ass.get("name"));
                continue;
            }
            if (!fromDate.isBefore(start) && !fromDate.isAfter(end)) {
                String name = String.valueOf(ass.get("name"));
                try {
                    erpNextDocumentService.cancelAndDelete("Salary Structure Assignment", name);
                    log.info("[SSA][DELETE] Assignation '{}' supprimée (from_date={})", name, fromStr);
                } catch (Exception e) {
                    log.error("[SSA][ERROR] Suppression de l'assignation '{}' échouée : {}", name, e.getMessage());
                }
            }
        }
    }
}

