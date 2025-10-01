package com.erpnext.service.rest.salary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.erpnext.dto.EmployeeDTO;
import com.erpnext.dto.SalarySlipDTO;
import com.erpnext.dto.SalaryComponentDTO;
import com.erpnext.utils.ErpNextApiUtil;
import com.erpnext.utils.DataParseUtils;

import com.erpnext.service.rest.utils.ErpNextDocumentService;
import com.erpnext.service.rest.employe.EmployeService;

@Service
public class SalaryService {
    public static final Logger log = LoggerFactory.getLogger(SalaryService.class);
    @Autowired
    public ErpNextApiUtil erpNextApiUtil;
    @Autowired
    public EmployeService employeService;
    
    @Autowired
    public ErpNextDocumentService erpNextDocumentService;
    
    public static final String SALARY_SLIP_DOCTYPE = "Salary Slip";
    // Champs à récupérer depuis ERPNext, alignés avec SalarySlipDTO et besoins de listing
    public static final String[] SALARY_SLIP_FIELDS = {
        "name",             // ID interne ERPNext
        "employee", 
        "employee_name",    // Souvent utile pour l'affichage
        "start_date", 
        "end_date", 
        "payroll_frequency",
        "salary_structure", 
        "company",
        "status",           // Le statut est souvent utile à voir
        "gross_pay",
        "total_deduction",
        "net_pay"
    };

    // Utilitaire optimisé : un seul appel API, pas de liste complète
    public SalarySlipDTO getSalarySlipById(String salarySlipId) {
        return fetchSalarySlipDetailById(salarySlipId);
    }

    // Nouvelle méthode : récupération détaillée par ID
    public SalarySlipDTO fetchSalarySlipDetailById(String salarySlipId) {
        if (salarySlipId == null || salarySlipId.isEmpty()) return null;
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + SALARY_SLIP_DOCTYPE + "/" + salarySlipId;
        Map<String, Object> response = erpNextApiUtil.fetchData(url);
        if (response != null && response.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return convertToSalarySlipDTO(data);
        }
        return null;
    }

    
    
    // Récupère les bulletins de salaire selon les critères de filtrage
    public List<SalarySlipDTO> fetchSalarySlips(String url) {
        Map<String, Object> response = erpNextApiUtil.fetchData(url);
        List<SalarySlipDTO> result = new ArrayList<>();
        
        if (response != null && response.containsKey("data")) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            
            for (Map<String, Object> item : data) {
                SalarySlipDTO dto = convertToSalarySlipDTO(item);
                result.add(dto);
            }
        }
        
        return result;
    }
    
    // Convertit les données brutes en objet SalarySlipDTO
    public SalarySlipDTO convertToSalarySlipDTO(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        SalarySlipDTO dto = new SalarySlipDTO();
        dto.setName((String) data.get("name")); // ID ERPNext
        dto.setEmployee((String) data.get("employee"));
        dto.setEmployeeName((String) data.get("employee_name"));
        // Si EmployeeDTO est imbriqué ou si un champ employeeName est ajouté à SalarySlipDTO, on pourra le mapper.
        dto.setStartDate(DataParseUtils.parseLocalDate(data.get("start_date")));
        dto.setEndDate(DataParseUtils.parseLocalDate(data.get("end_date")));
        dto.setPayrollFrequency((String) data.get("payroll_frequency"));
        dto.setSalaryStructure((String) data.get("salary_structure"));
        dto.setCompany((String) data.get("company"));

        // Map new fields
        dto.setPostingDate(DataParseUtils.parseLocalDate(data.get("posting_date")));
        dto.setStatus((String) data.get("status"));
        dto.setGrossPay(DataParseUtils.parseDouble(data.get("gross_pay")));
        dto.setNetPay(DataParseUtils.parseDouble(data.get("net_pay")));
        dto.setTotalWorkingDays(DataParseUtils.parseDouble(data.get("total_working_days")));
        dto.setCurrency((String) data.get("currency"));

        // Map champs directs base_salary et total_deduction si présents
        dto.setBaseSalary(DataParseUtils.parseDouble(data.get("base_salary")));
        dto.setTotalDeduction(DataParseUtils.parseDouble(data.get("total_deduction")));

        // Map earnings
        if (data.containsKey("earnings") && data.get("earnings") instanceof List) {
            List<Map<String, Object>> earningsData = (List<Map<String, Object>>) data.get("earnings");
            List<SalaryComponentDTO> earningsList = new ArrayList<>();
            Double baseSalary = null;
            for (Map<String, Object> earningItem : earningsData) {
                SalaryComponentDTO component = new SalaryComponentDTO();
                component.setName((String) earningItem.get("name"));
                component.setSalaryComponent((String) earningItem.get("salary_component"));
                component.setAbbreviation((String) earningItem.get("abbr"));
                component.setType((String) earningItem.get("type") != null ? (String) earningItem.get("type") : "Earning");
                component.setAmount(DataParseUtils.parseDouble(earningItem.get("amount")));
                component.setDefaultAmount(DataParseUtils.parseDouble(earningItem.get("default_amount")));
                component.setIsTaxApplicable(DataParseUtils.toBoolean(earningItem.get("is_tax_applicable")));
                component.setDependsOnLwp(DataParseUtils.toBoolean(earningItem.get("depends_on_lwp")));
                component.setDoNotIncludeInTotal(DataParseUtils.toBoolean(earningItem.get("do_not_include_in_total")));
                // Chercher le salaire de base (toutes variantes)
                String compName = component.getSalaryComponent();
                String compName2 = component.getName();
                String[] baseLabels = {"Salaire de base", "Salaire Base", "Basic", "Base", "BASE", "salaire de base", "salaire base", "basic", "base"};
                for (String label : baseLabels) {
                    if ((compName != null && compName.equalsIgnoreCase(label)) || (compName2 != null && compName2.equalsIgnoreCase(label))) {
                        baseSalary = component.getAmount();
                        System.out.println("[DEBUG][Service] Salaire de base détecté: " + label + " -> " + component.getAmount());
                        break;
                    }
                }
                earningsList.add(component);
            }
            dto.setEarnings(earningsList);
            dto.setBaseSalary(baseSalary);
        }
        // Map deductions
        if (data.containsKey("deductions") && data.get("deductions") instanceof List) {
            List<Map<String, Object>> deductionsData = (List<Map<String, Object>>) data.get("deductions");
            List<SalaryComponentDTO> deductionsList = new ArrayList<>();
            for (Map<String, Object> deductionItem : deductionsData) {
                SalaryComponentDTO component = new SalaryComponentDTO();
                component.setName((String) deductionItem.get("name"));
                component.setSalaryComponent((String) deductionItem.get("salary_component"));
                component.setAbbreviation((String) deductionItem.get("abbr"));
                component.setType((String) deductionItem.get("type") != null ? (String) deductionItem.get("type") : "Deduction");
                component.setAmount(DataParseUtils.parseDouble(deductionItem.get("amount")));
                component.setDefaultAmount(DataParseUtils.parseDouble(deductionItem.get("default_amount")));
                component.setIsTaxApplicable(DataParseUtils.toBoolean(deductionItem.get("is_tax_applicable")));
                component.setDependsOnLwp(DataParseUtils.toBoolean(deductionItem.get("depends_on_lwp")));
                component.setDoNotIncludeInTotal(DataParseUtils.toBoolean(deductionItem.get("do_not_include_in_total")));
                deductionsList.add(component);
            }
            dto.setDeductions(deductionsList);
        }

        // Si le champ total_deduction n'a pas été renseigné directement, mais que les deductions sont présentes, on peut calculer le total (optionnel, non forcé)
        if (dto.getTotalDeduction() == null && dto.getDeductions() != null) {
            double sum = dto.getDeductions().stream()
                .filter(d -> d.getAmount() != null)
                .mapToDouble(SalaryComponentDTO::getAmount)
                .sum();
            if (sum > 0) dto.setTotalDeduction(sum);
        }
        return dto;
    }

    /**
     * Searches for the base salary within a list of salary components (earnings).
     * This logic is centralized here to be reused.
     * @param components List of salary components (typically earnings).
     * @return The base salary amount, or null if not found.
     */
    public Double findBaseSalaryInSlipComponents(List<SalaryComponentDTO> components) {
        if (components == null) {
            return null;
        }

        String[] baseLabels = {"Salaire de base", "Salaire Base", "Basic", "Base", "BASE", "salaire de base", "salaire base", "basic", "base"};
        for (SalaryComponentDTO component : components) {
            String compName = component.getSalaryComponent();
            String compName2 = component.getName();
            for (String label : baseLabels) {
                if ((compName != null && compName.equalsIgnoreCase(label)) || (compName2 != null && compName2.equalsIgnoreCase(label))) {
                    log.debug("[SALARY-SVC] Base salary found in components: {} -> {}", label, component.getAmount());
                    return component.getAmount();
                }
            }
        }
        return null;
    }
    
    
    
    // CRUD : création, récupération globale, mise à jour, suppression
    public List<SalarySlipDTO> getAllSalarySlips() {
        String url = erpNextApiUtil.buildApiUrl(SALARY_SLIP_DOCTYPE, SALARY_SLIP_FIELDS, null); // Pas de filtres
        return fetchSalarySlips(url);
    }

    public SalarySlipDTO createSalarySlip(SalarySlipDTO salarySlip) {
        if (salarySlip == null) {
            return null;
        }
        try {
            // Convertir l'objet DTO en Map pour l'API
            Map<String, Object> salarySlipData = new HashMap<>();
            salarySlipData.put("doctype", SALARY_SLIP_DOCTYPE);
            salarySlipData.put("employee", salarySlip.getEmployee());
            salarySlipData.put("start_date", salarySlip.getStartDate() != null ? salarySlip.getStartDate().toString() : null);
            salarySlipData.put("end_date", salarySlip.getEndDate() != null ? salarySlip.getEndDate().toString() : null);
            salarySlipData.put("payroll_frequency", salarySlip.getPayrollFrequency());
            salarySlipData.put("salary_structure", salarySlip.getSalaryStructure());
            salarySlipData.put("company", salarySlip.getCompany());
            // Add posting_date if available in DTO
            if (salarySlip.getPostingDate() != null) {
                salarySlipData.put("posting_date", salarySlip.getPostingDate().toString());
            }
            // Add earnings if available
            if (salarySlip.getEarnings() != null && !salarySlip.getEarnings().isEmpty()) {
                List<Map<String, Object>> earningsMaps = new ArrayList<>();
                for (SalaryComponentDTO earning : salarySlip.getEarnings()) {
                    Map<String, Object> earningMap = new HashMap<>();
                    earningMap.put("salary_component", earning.getSalaryComponent());
                    earningMap.put("amount", earning.getAmount());
                    // Add other component fields if ERPNext requires them, e.g., "depends_on_lwp", "is_tax_applicable"
                    earningsMaps.add(earningMap);
                }
                salarySlipData.put("earnings", earningsMaps);
            }
            // Add deductions if available
            if (salarySlip.getDeductions() != null && !salarySlip.getDeductions().isEmpty()) {
                List<Map<String, Object>> deductionsMaps = new ArrayList<>();
                for (SalaryComponentDTO deduction : salarySlip.getDeductions()) {
                    Map<String, Object> deductionMap = new HashMap<>();
                    deductionMap.put("salary_component", deduction.getSalaryComponent());
                    deductionMap.put("amount", deduction.getAmount());
                    // Add other component fields if ERPNext requires them
                    deductionsMaps.add(deductionMap);
                }
                salarySlipData.put("deductions", deductionsMaps);
            }
            // Ajouter le champ base_salary si présent
            if (salarySlip.getBaseSalary() != null) {
                salarySlipData.put("base_salary", salarySlip.getBaseSalary());
            }
            // Créer le bulletin via l'API
            String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + SALARY_SLIP_DOCTYPE;
            Map<String, Object> response = erpNextApiUtil.postData(url, salarySlipData);
            log.error("Réponse brute ERPNext : {}", response);
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String salarySlipId = (String) data.get("name");
                // Récupérer le bulletin créé par son ID et le retourner
                return getSalarySlipById(salarySlipId);
            }
        } catch (Exception e) {
            log.error("Erreur lors de la création du bulletin de salaire", e);
            log.error("Erreur lors de la création du bulletin de salaire: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création du bulletin de salaire: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Met à jour un bulletin de salaire existant
     * @param salarySlipId ID du bulletin à mettre à jour
     * @param salarySlip Nouvelles données du bulletin
     * @return Le bulletin mis à jour, ou null en cas d'erreur
     */
    public SalarySlipDTO updateSalarySlip(String salarySlipId, SalarySlipDTO salarySlip) {
        if (salarySlipId == null || salarySlipId.isEmpty() || salarySlip == null) {
            return null;
        }
        try {
            // Vérifier que le bulletin existe
            SalarySlipDTO existingSalarySlip = getSalarySlipById(salarySlipId);
            if (existingSalarySlip == null) {
                // Log or handle the case where the salary slip to update doesn't exist
                log.warn("Salary slip with ID {} not found for update.", salarySlipId);
                return null;
            }

    Map<String, Object> salarySlipData = new HashMap<>();

    // Add fields from DTO if they are provided for update
    // Basic fields
    if (salarySlip.getEmployee() != null) salarySlipData.put("employee", salarySlip.getEmployee());
    if (salarySlip.getStartDate() != null) salarySlipData.put("start_date", salarySlip.getStartDate());
    if (salarySlip.getEndDate() != null) salarySlipData.put("end_date", salarySlip.getEndDate());
    if (salarySlip.getPayrollFrequency() != null) salarySlipData.put("payroll_frequency", salarySlip.getPayrollFrequency());
    if (salarySlip.getSalaryStructure() != null) salarySlipData.put("salary_structure", salarySlip.getSalaryStructure());
            if (salarySlip.getCompany() != null) salarySlipData.put("company", salarySlip.getCompany());
            
            // New fields from enhanced DTO
            if (salarySlip.getPostingDate() != null) salarySlipData.put("posting_date", salarySlip.getPostingDate());
            if (salarySlip.getStatus() != null) salarySlipData.put("status", salarySlip.getStatus()); // ERPNext might control status changes strictly
            if (salarySlip.getGrossPay() != null) salarySlipData.put("gross_pay", salarySlip.getGrossPay()); // Usually calculated by ERPNext
            if (salarySlip.getNetPay() != null) salarySlipData.put("net_pay", salarySlip.getNetPay());       // Usually calculated by ERPNext
            if (salarySlip.getTotalWorkingDays() != null) salarySlipData.put("total_working_days", salarySlip.getTotalWorkingDays());
            if (salarySlip.getCurrency() != null) salarySlipData.put("currency", salarySlip.getCurrency());

            // Handle earnings (overwrite existing if DTO provides the list)
            // If salarySlip.getEarnings() is null, it means no change to earnings is requested.
            // If salarySlip.getEarnings() is an empty list, it means clear existing earnings.
            if (salarySlip.getEarnings() != null) {
                List<Map<String, Object>> earningsMaps = new ArrayList<>();
                for (SalaryComponentDTO earning : salarySlip.getEarnings()) {
                    Map<String, Object> earningMap = new HashMap<>();
                    earningMap.put("salary_component", earning.getSalaryComponent());
                    earningMap.put("amount", earning.getAmount());
                    // Add other component fields if ERPNext requires/allows them on update
                    earningsMaps.add(earningMap);
                }
                salarySlipData.put("earnings", earningsMaps);
            }

            // Handle deductions (overwrite existing if DTO provides the list)
            if (salarySlip.getDeductions() != null) {
                List<Map<String, Object>> deductionsMaps = new ArrayList<>();
                for (SalaryComponentDTO deduction : salarySlip.getDeductions()) {
                    Map<String, Object> deductionMap = new HashMap<>();
                    deductionMap.put("salary_component", deduction.getSalaryComponent());
                    deductionMap.put("amount", deduction.getAmount());
                    // Add other component fields if ERPNext requires/allows them on update
                    deductionsMaps.add(deductionMap);
                }
                salarySlipData.put("deductions", deductionsMaps);
            }

            if (salarySlipData.isEmpty()) {
                // No updatable fields were provided in the DTO, or only fields that don't map to ERPNext fields for update
                // Log this or return existingSalarySlip if no actual update is to be performed.
                System.out.println("No updatable data provided for salary slip ID: " + salarySlipId);
                return existingSalarySlip; 
            }
            
            // Mettre à jour le bulletin via l'API
            String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + SALARY_SLIP_DOCTYPE + "/" + salarySlipId;
            Map<String, Object> response = erpNextApiUtil.putData(url, salarySlipData);
            
            if (response != null && response.containsKey("data")) {
                // Récupérer le bulletin mis à jour
                return getSalarySlipById(salarySlipId);
            }
        } catch (Exception e) {
            // Log l'erreur
            log.error("Erreur lors de la mise à jour du bulletin de salaire: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Récupère les bulletins pour un employé donné.
     */
    public List<SalarySlipDTO> getSalarySlipsByEmployee(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) {
            return new ArrayList<>();
        }
        String url = erpNextApiUtil.buildApiUrl(
                SALARY_SLIP_DOCTYPE,
                SALARY_SLIP_FIELDS,
                Map.of("employee", employeeId));
        return fetchSalarySlips(url);
    }

    /**
     * Supprime un bulletin de salaire
     * @param salarySlipId ID du bulletin à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean deleteSalarySlip(String salarySlipId) {
        if (salarySlipId == null || salarySlipId.isEmpty()) {
            return false;
        }
        
        try {
            // Vérifier que le bulletin existe
            SalarySlipDTO existingSalarySlip = getSalarySlipById(salarySlipId);
            if (existingSalarySlip == null) {
                return false;
            }
            
            // Supprimer le bulletin via l'API
            String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + SALARY_SLIP_DOCTYPE + "/" + salarySlipId;
            boolean success = erpNextApiUtil.deleteData(url);
            
            return success;
        } catch (Exception e) {
            // Log l'erreur
            log.error("Erreur lors de la suppression du bulletin de salaire: {}", e.getMessage());
            return false;
        }
    }
    public void supprimerSalairesPeriode(String employeeId, LocalDate start, LocalDate end) {
    List<SalarySlipDTO> slips = getSalarySlipsByEmployee(employeeId);

    for (SalarySlipDTO slip : slips) {
        if (slip.getStartDate() != null && !slip.getStartDate().isBefore(start) && !slip.getStartDate().isAfter(end)) {
            erpNextDocumentService.cancelAndDelete("Salary Slip",slip.getName());
        }
    }
    }
    // Calcule la moyenne des bases dans les Salary Structure Assignments (SSA) actifs
    public double calculerMoyenneSalaireBase() {
        // 1. Récupérer les SSA depuis ERPNext
        String[] fields = {"name", "base"};
        String url = erpNextApiUtil.buildApiUrl("Salary Structure Assignment", fields, null);
        Map<String, Object> response = erpNextApiUtil.fetchData(url);
        if (response == null || !response.containsKey("data")) {
            return 0;
        }

        java.util.List<Map<String, Object>> data = (java.util.List<Map<String, Object>>) response.get("data");
        double total = 0;
        int count = 0;
        for (Map<String, Object> item : data) {
            Double base = com.erpnext.utils.DataParseUtils.parseDouble(item.get("base"));
            if (base != null && base > 0) {
                total += base;
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }

    public List<SalarySlipDTO> rechercheSalaireMulti(Double minSalaire, Double maxSalaire, Integer mois, Integer annee) {
        List<SalarySlipDTO> all = getAllSalarySlips();
        return all.stream()
            .filter(slip -> minSalaire == null || (slip.getNetPay() != null && slip.getNetPay() >= minSalaire))
            .filter(slip -> maxSalaire == null || (slip.getNetPay() != null && slip.getNetPay() <= maxSalaire))
            .filter(slip -> mois == null || (slip.getStartDate() != null && slip.getStartDate().getMonthValue() == mois))
            .filter(slip -> annee == null || (slip.getStartDate() != null && slip.getStartDate().getYear() == annee))
            .collect(java.util.stream.Collectors.toList());
        }
        
        
    
}
    


