package com.erpnext.service.rest.salary;

import org.springframework.stereotype.Service;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.Normalizer;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import com.erpnext.dto.SalaryComponentDTO;
import com.erpnext.dto.SalarySlipDTO;
import com.erpnext.service.rest.utils.ErpNextDocumentService;
import com.erpnext.service.rest.employe.EmployeService;
import com.erpnext.service.rest.salary.SalaryService;
import com.erpnext.utils.ErpNextApiUtil;

@Service
public class SalaryComponentService {
    public static final Logger log = LoggerFactory.getLogger(SalaryComponentService.class);
    @Autowired
    public ErpNextApiUtil erpNextApiUtil;
    @Autowired
    public EmployeService employeService;
    @Autowired
    public SalaryService salaryService;
    @Autowired
    public ErpNextDocumentService erpNextDocumentService;

    public Map<String, Object> getSalaryComponent(String componentName) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Component/" + componentName;
        return erpNextApiUtil.fetchData(url);
    }

    public Map<String, Object> createSalaryComponent(Map<String, Object> componentData) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Component";
        return erpNextApiUtil.postData(url, componentData);
    }

    public Map<String, Object> insertSalaryComponent(Map<String, Object> componentData) {
        // Ancienne méthode, à migrer vers createSalaryComponent si besoin
        return createSalaryComponent(componentData);
    }

    /**
     * Met à jour le salaire de base pour tous les employés qui ont le composant choisi
     * (ex: Indemnité) avec une valeur supérieure/inférieure au montant donné.
     * Seul le salaire de base est modifié, selon le pourcentage/montant choisi.
     */
    public void updateBaseSalaryForAllEmployees(String salaryComponentName, Double montant, Double pourcentage, String sign, String valeurType) {
        log.info("[SalaireBase] Appel de updateBaseSalaryForAllEmployees avec composant={}, montant={}, pourcentage={}, signe={}, valeurType={}", salaryComponentName, montant, pourcentage, sign, valeurType);
        String[] baseLabels = {"Salaire de base", "Salaire Base", "Basic", "Base", "BASE", "salaire de base", "salaire base", "basic", "base"};
        boolean anyUpdated = false;
        var employees = employeService.getAllEmploye();
        for (var employee : employees) {
            var slips = salaryService.getSalarySlipsByEmployee(employee.getId());
            for (var slip : slips) {
                slip = salaryService.fetchSalarySlipDetailById(slip.getName());
                int docstatus = erpNextDocumentService.getDocStatus("Salary Slip", slip.getName());
                log.info("[DEBUG] Bulletin {}: nb earnings = {} (docstatus={})", slip.getName(), slip.getEarnings() != null ? slip.getEarnings().size() : 0, docstatus);
                Double composantValue = null;
                if (slip.getEarnings() != null) {
                    for (var earning : slip.getEarnings()) {
                        log.info("[DEBUG] Test earning: getSalaryComponent='{}', getName='{}', montant: {}", earning.getSalaryComponent(), earning.getName(), earning.getAmount());
                        String salaryComponentField = earning.getSalaryComponent();
                        String nameField = earning.getName();
                        if (equalsIgnoreAccents(salaryComponentName, salaryComponentField) || equalsIgnoreAccents(salaryComponentName, nameField)) {
                            log.info("[DEBUG] MATCH trouvé sur earning: getSalaryComponent='{}', getName='{}', montant: {}", salaryComponentField, nameField, earning.getAmount());
                            composantValue = earning.getAmount();
                            break;
                        }
                    }
                }
                boolean condition = false;
                if (composantValue != null && montant != null) {
                    if (">".equals(valeurType)) {
                        condition = composantValue > montant;
                    } else if ("<".equals(valeurType)) {
                        condition = composantValue < montant;
                    }
                }
                log.info("[DEBUG] Condition sur composantValue={} et montant={} : {}", composantValue, montant, condition);
                if (condition) {
                    if (docstatus == 1) {
                        log.info("[CANCEL] Bulletin {} est Submitted, on l'annule avant duplication.", slip.getName());
                        erpNextDocumentService.cancelDocument("Salary Slip", slip.getName());
                        docstatus = erpNextDocumentService.getDocStatus("Salary Slip", slip.getName());
                    }
                    // Dupliquer et supprimer l'ancien (qui est maintenant annulé ou draft)
                    log.warn("[DUPLICATION] On va créer un nouveau bulletin pour {} (docstatus={}) et supprimer l'ancien {}.", slip.getEmployeeName(), docstatus, slip.getName());
                    SalarySlipDTO nouveauSlip = new SalarySlipDTO();
                    nouveauSlip.setEmployee(slip.getEmployee());
                    nouveauSlip.setEmployeeName(slip.getEmployeeName());
                    nouveauSlip.setStartDate(slip.getStartDate());
                    nouveauSlip.setEndDate(slip.getEndDate());
                    nouveauSlip.setPayrollFrequency(slip.getPayrollFrequency());
                    nouveauSlip.setSalaryStructure(slip.getSalaryStructure());
                    nouveauSlip.setCompany(slip.getCompany());
                    nouveauSlip.setCurrency(slip.getCurrency());
                    nouveauSlip.setPostingDate(slip.getPostingDate());
                    // Copier earnings/deductions
                    List<SalaryComponentDTO> newEarnings = new ArrayList<>();
                    if (slip.getEarnings() != null) {
                        for (var earning : slip.getEarnings()) {
                            var newEarning = new com.erpnext.dto.SalaryComponentDTO();
                            newEarning.setSalaryComponent(earning.getSalaryComponent());
                            newEarning.setName(earning.getName());
                            newEarning.setAmount(earning.getAmount());
                            newEarning.setAbbreviation(earning.getAbbreviation());
                            newEarning.setType(earning.getType());
                            newEarning.setDefaultAmount(earning.getDefaultAmount());
                            newEarning.setIsTaxApplicable(earning.getIsTaxApplicable());
                            newEarning.setDependsOnLwp(earning.getDependsOnLwp());
                            newEarning.setDoNotIncludeInTotal(earning.getDoNotIncludeInTotal());
                            // Log détaillé pour diagnostic
                            String compName = earning.getSalaryComponent();
                            String compName2 = earning.getName();
                            log.info("[DIAG] Test earning: salaryComponent='{}', name='{}'", compName, compName2);
                            boolean isBase = false;
                            for (String label : baseLabels) {
                                if ((compName != null && compName.equalsIgnoreCase(label)) || (compName2 != null && compName2.equalsIgnoreCase(label))) {
                                    isBase = true;
                                    log.info("[DIAG] MATCH SALAIRE DE BASE sur label='{}' pour composant='{}'", label, compName);
                                    break;
                                }
                            }
                            if (!isBase) {
                                log.info("[DIAG] PAS DE MATCH SALAIRE DE BASE pour composant='{}'", compName);
                            }
                            if (isBase && earning.getAmount() != null) {
                                Double newAmount = montant;
                                if (pourcentage != null) {
                                    double base = earning.getAmount();
                                    double delta = base * (pourcentage / 100.0);
                                    newAmount = "+".equals(sign) ? base + delta : base - delta;
                                }
                                if (newAmount != null) {
                                    newEarning.setAmount(newAmount);
                                    // Mettre à jour le champ baseSalary du bulletin
                                    nouveauSlip.setBaseSalary(newAmount);
                                }
                            }
                            newEarnings.add(newEarning);
                        }
                    }
                    nouveauSlip.setEarnings(newEarnings);
                    // Copier les deductions
                    if (slip.getDeductions() != null) {
                        List<SalaryComponentDTO> newDeductions = new ArrayList<>();
                        for (var deduction : slip.getDeductions()) {
                            var newDeduction = new com.erpnext.dto.SalaryComponentDTO();
                            newDeduction.setSalaryComponent(deduction.getSalaryComponent());
                            newDeduction.setName(deduction.getName());
                            newDeduction.setAmount(deduction.getAmount());
                            newDeduction.setAbbreviation(deduction.getAbbreviation());
                            newDeduction.setType(deduction.getType());
                            newDeduction.setDefaultAmount(deduction.getDefaultAmount());
                            newDeduction.setIsTaxApplicable(deduction.getIsTaxApplicable());
                            newDeduction.setDependsOnLwp(deduction.getDependsOnLwp());
                            newDeduction.setDoNotIncludeInTotal(deduction.getDoNotIncludeInTotal());
                            newDeductions.add(newDeduction);
                        }
                        nouveauSlip.setDeductions(newDeductions);
                    }
                    // Suppression de l'ancien bulletin (avant création du nouveau)
                    try {
                        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Slip/" + slip.getName();
                        erpNextApiUtil.deleteData(url);
                        log.info("[SUPPRESSION] Ancien bulletin {} supprimé.", slip.getName());
                    } catch (Exception ex) {
                        log.error("[SUPPRESSION] Erreur lors de la suppression de l'ancien bulletin {}: {}", slip.getName(), ex.getMessage());
                    }
                    // Création du nouveau bulletin
                    var created = salaryService.createSalarySlip(nouveauSlip);
                    log.info("[DUPLICATION] Nouveau bulletin créé pour {} : {}", slip.getEmployeeName(), created != null ? created.getName() : "(erreur)");
                    if (created == null) {
                        throw new RuntimeException("[DUPLICATION][ERREUR] La création du bulletin a échoué pour " + slip.getEmployeeName());
                    }
                    if (created != null && created.getName() != null) {
                        erpNextDocumentService.submitDocument("Salary Slip", created.getName());
                        log.info("[SUBMIT] Nouveau bulletin {} soumis automatiquement.", created.getName());
                    }
                    anyUpdated = true;
                    continue;
                }
            }
        }
        if (!anyUpdated) {
            throw new RuntimeException("Aucun bulletin de salaire ne correspond à la condition. Aucune mise à jour effectuée.");
        }
    }

    public static boolean equalsIgnoreAccents(String a, String b) {
        if (a == null || b == null) return false;
        String normA = Normalizer.normalize(a, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        String normB = Normalizer.normalize(b, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        return normA.equals(normB);
    }

    public java.util.List<String> getAllComponentNames() {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Component?fields=[\"name\"]&limit_page_length=100";
        java.util.Map<String, Object> response = erpNextApiUtil.fetchData(url);
        java.util.List<String> result = new java.util.ArrayList<>();
        if (response != null && response.containsKey("data")) {
            java.util.List<java.util.Map<String, Object>> dataList = (java.util.List<java.util.Map<String, Object>>) response.get("data");
            for (java.util.Map<String, Object> data : dataList) {
                if (data.containsKey("name")) {
                    result.add((String) data.get("name"));
                }
            }
        }
        return result;
    }
}
