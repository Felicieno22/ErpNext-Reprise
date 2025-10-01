package com.erpnext.service.rest.employe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.erpnext.dto.EmployeeDTO;
import com.erpnext.utils.ErpNextApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmployeService {
    private static final Logger log = LoggerFactory.getLogger(EmployeService.class);
    private final ErpNextApiUtil erpNextApiUtil;

    @Autowired
    public EmployeService(ErpNextApiUtil erpNextApiUtil) {
        this.erpNextApiUtil = erpNextApiUtil;
    }

    

    public static final String DOCTYPE = "Employee";
    public static final String[] FIELDS = {
            "name", "employee_name", "employee_number", "date_of_joining", "company", "gender", "status",
            "department", "designation"
    };

    // Vérifie si un employé existe déjà par son numéro (employee_number)
    public EmployeeDTO getEmployeeByNumber(String employeeNumber) {
        if (employeeNumber == null || employeeNumber.isEmpty()) return null;
        String[] fields = FIELDS;
        java.util.HashMap<String, String> filters = new java.util.HashMap<>();
        filters.put("employee_number", employeeNumber);
        String url = erpNextApiUtil.buildApiUrl(DOCTYPE, fields, filters);
        List<EmployeeDTO> employees = fetchEmployees(url);
        return employees.isEmpty() ? null : employees.get(0);
    }

    public EmployeeDTO getEmployeeById(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) {
            return null;
        }

        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + DOCTYPE + "/" + employeeId;
        Map<String, Object> response = erpNextApiUtil.fetchData(url);

        if (response != null && response.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return dtoFromMap(data);
        }
        return null;
    }

    public List<EmployeeDTO> getAllEmploye() {
        String url = erpNextApiUtil.buildApiUrl(DOCTYPE, FIELDS, null);
        return fetchEmployees(url);
    }

    public List<EmployeeDTO> getEmployesByFilters(String status, String department, String company, String designation, String searchTerm) {
        Map<String, String> filters = new HashMap<>();

        if (status != null && !status.isEmpty()) filters.put("status", status);
        if (department != null && !department.isEmpty()) filters.put("department", department);
        if (company != null && !company.isEmpty()) filters.put("company", company);
        if (designation != null && !designation.isEmpty()) filters.put("designation", designation);

        String url = erpNextApiUtil.buildApiUrl(DOCTYPE, FIELDS, filters);

        List<EmployeeDTO> employees = fetchEmployees(url);

        if (searchTerm != null && !searchTerm.isEmpty()) {
            String lowerSearchTerm = searchTerm.toLowerCase();
            List<EmployeeDTO> filtered = new ArrayList<>();
            for (EmployeeDTO e : employees) {
                if (e.getEmployeeName() != null && e.getEmployeeName().toLowerCase().contains(lowerSearchTerm)) {
                    filtered.add(e);
                }
            }
            return filtered;
        }
        return employees;
    }

    public List<EmployeeDTO> fetchEmployees(String url) {
        Map<String, Object> body = erpNextApiUtil.fetchData(url);
        if (body == null || !body.containsKey("data")) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> dataList = (List<Map<String, Object>>) body.get("data");
        List<EmployeeDTO> result = new ArrayList<>();

        for (Map<String, Object> data : dataList) {
            result.add(dtoFromMap(data));
        }

        return result;
    }

    public List<String> getDistinctValues(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) return new ArrayList<>();

        List<EmployeeDTO> allEmployees = getAllEmploye();
        List<String> distinctValues = new ArrayList<>();

        for (EmployeeDTO employee : allEmployees) {
            String value = null;

            switch (fieldName) {
                case "department":
                    value = employee.getDepartment();
                    break;
                case "status":
                    value = employee.getStatus();
                    break;
                case "company":
                    value = employee.getCompany();
                    break;
                case "designation":
                    value = employee.getDesignation();
                    break;
                default:
                    break;
            }

            if (value != null && !value.isEmpty() && !distinctValues.contains(value)) {
                distinctValues.add(value);
            }
        }
        return distinctValues;
    }

    public EmployeeDTO createEmployee(EmployeeDTO employee) {
        if (employee == null) return null;

        try {
            Map<String, Object> employeeData = prepareEmployeeData(employee);

            String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + DOCTYPE;
            Map<String, Object> response = erpNextApiUtil.postData(url, employeeData);

            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String employeeId = (String) data.get("name");
                return getEmployeeById(employeeId);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'employé: {}", e.getMessage());
        }

        return null;
    }

    public EmployeeDTO updateEmployee(String employeeId, EmployeeDTO employee) {
        if (employeeId == null || employeeId.isEmpty() || employee == null) return null;

        try {
            EmployeeDTO existing = getEmployeeById(employeeId);
            if (existing == null) return null;

            Map<String, Object> employeeData = prepareEmployeeData(employee);

            String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + DOCTYPE + "/" + employeeId;
            Map<String, Object> response = erpNextApiUtil.putData(url, employeeData);

            if (response != null && response.containsKey("data")) {
                return getEmployeeById(employeeId);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'employé: {}", e.getMessage());
        }

        return null;
    }

    public boolean deleteEmployee(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) return false;

        try {
            EmployeeDTO existing = getEmployeeById(employeeId);
            if (existing == null) return false;

            String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + DOCTYPE + "/" + employeeId;
            return erpNextApiUtil.deleteData(url);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'employé: {}", e.getMessage());
        }
        return false;
    }

    // Conversion Map -> DTO
    public EmployeeDTO dtoFromMap(Map<String, Object> data) {
        if (data == null) return null;
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId((String) data.get("name"));
        dto.setEmployeeName((String) data.get("employee_name"));
        dto.setEmployeeNumber((String) data.get("employee_number"));
        dto.setDateOfJoining((String) data.get("date_of_joining"));
        dto.setCompany((String) data.get("company"));
        dto.setGender((String) data.get("gender"));
        dto.setStatus((String) data.get("status"));
        dto.setDepartment((String) data.get("department"));
        dto.setDesignation((String) data.get("designation"));
        return dto;
    }

    // Conversion DTO -> Map pour POST/PUT
    public Map<String, Object> prepareEmployeeData(EmployeeDTO employee) {
        Map<String, Object> map = new HashMap<>();
        map.put("doctype", DOCTYPE);

        if (employee.getFirstName() != null) map.put("first_name", employee.getFirstName());
        if (employee.getLastName() != null) map.put("last_name", employee.getLastName());
        if (employee.getDateOfBirth() != null) map.put("date_of_birth", employee.getDateOfBirth());
        if (employee.getEmployeeName() != null) map.put("employee_name", employee.getEmployeeName());
        if (employee.getEmployeeNumber() != null) map.put("employee_number", employee.getEmployeeNumber());
        if (employee.getDateOfJoining() != null) map.put("date_of_joining", employee.getDateOfJoining());
        if (employee.getCompany() != null) map.put("company", employee.getCompany());
        if (employee.getGender() != null) map.put("gender", employee.getGender());
        if (employee.getStatus() != null) map.put("status", employee.getStatus());
        if (employee.getDepartment() != null) map.put("department", employee.getDepartment());
        if (employee.getDesignation() != null) map.put("designation", employee.getDesignation());

        return map;
    }
}
