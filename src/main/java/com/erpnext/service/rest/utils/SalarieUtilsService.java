package com.erpnext.service.rest.utils;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.erpnext.dto.SalarySlipDTO;
import com.erpnext.service.rest.salary.SalaryService;
import com.erpnext.service.rest.salary.SalaryStructureAssService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional; 

@Service
public class SalarieUtilsService {
    private static final Logger log = LoggerFactory.getLogger(SalarieUtilsService.class);

    @Autowired
    public SalaryService salaryService;

    @Autowired
    public ErpNextDocumentService erpNextDocumentService;

    @Autowired
    public SalaryStructureAssService salaryStructureAssService;

    public SalarySlipDTO salarySlipDTO;

    /**
     * Returns the latest salary slip (by end date) for a given employee.
     */
    public SalarySlipDTO getLastSalarySlipByEmployee(String employeId) {
        List<SalarySlipDTO> slips = salaryService.getSalarySlipsByEmployee(employeId);
        return slips.stream()
            .filter(slip -> slip.getEndDate() != null)
            .max(java.util.Comparator.comparing(SalarySlipDTO::getEndDate))
            .orElse(null);
    }

    public SalarySlipDTO getLatestEmployeeSalaryData(String employeId) {
        // 1. Trouver le dernier bulletin de paie (juste le résumé)
        SalarySlipDTO lastSlipSummary = getLastSalarySlipByEmployee(employeId);

        if (lastSlipSummary != null && lastSlipSummary.getName() != null) {
            log.debug("[SAL-UTIL] Dernier bulletin trouvé (résumé): {}. Récupération des détails.", lastSlipSummary.getName());

            // 2. Récupérer les détails complets de ce bulletin, y compris les composants
            SalarySlipDTO detailedSlip = salaryService.fetchSalarySlipDetailById(lastSlipSummary.getName());

            if (detailedSlip != null) {
                // 3. Utiliser la logique centralisée pour trouver le salaire de base dans les composants
                Double baseSalaryFromComponents = salaryService.findBaseSalaryInSlipComponents(detailedSlip.getEarnings());

                if (baseSalaryFromComponents != null) {
                    log.debug("[SAL-UTIL] Salaire de base trouvé dans les composants: {}. Mise à jour du DTO.", baseSalaryFromComponents);
                    detailedSlip.setBaseSalary(baseSalaryFromComponents);
                } else {
                    log.warn("[SAL-UTIL] Le salaire de base n'a pas été trouvé dans les composants du bulletin {}.", detailedSlip.getName());
                }
                return detailedSlip; // Retourner le bulletin détaillé et corrigé
            }
        }

        // 4. Fallback: si aucun bulletin n'existe, utiliser la SSA active
        log.debug("[SAL-UTIL] Aucun bulletin de paie trouvé. Tentative de création de DTO de référence depuis la SSA.");
        Map<String, Object> activeAssignment = salaryStructureAssService.getActiveAssignmentByEmployee(employeId);
        if (activeAssignment != null) {
            SalarySlipDTO dto = new SalarySlipDTO();
            dto.setEmployee(employeId);
            dto.setBaseSalary((Double) activeAssignment.get("base"));
            dto.setSalaryStructure((String) activeAssignment.get("salary_structure"));
            dto.setCompany((String) activeAssignment.get("company"));
            dto.setPayrollFrequency((String) activeAssignment.get("payroll_frequency"));
            log.debug("[SAL-UTIL] DTO de référence créé depuis la SSA: base={}", dto.getBaseSalary());
            return dto;
        }

        log.warn("[SAL-UTIL] Impossible de trouver des données de salaire de référence pour l'employé {}", employeId);
        return null;
    }

    public Double getLatestEmployeeBaseSalary(String employeId) {
        // Utiliser la méthode principale et fiable pour obtenir les données salariales
        SalarySlipDTO referenceData = getLatestEmployeeSalaryData(employeId);

        if (referenceData != null && referenceData.getBaseSalary() != null && referenceData.getBaseSalary() > 0.0) {
            log.debug("[SAL-UTIL] Salaire de base final déterminé: {}", referenceData.getBaseSalary());
            return referenceData.getBaseSalary();
        }

        log.warn("[SAL-UTIL] Impossible de déterminer le salaire de base pour l'employé {}. Retourne 0.0.", employeId);
        return 0.0;
    }

    private Double getBaseSalaryFromActiveAssignment(String employeeId) {
        Map<String, Object> activeAssignment = salaryStructureAssService.getActiveAssignmentByEmployee(employeeId);
        if (activeAssignment != null && activeAssignment.containsKey("base")) {
            Object baseObj = activeAssignment.get("base");
            if (baseObj instanceof Number) {
                return ((Number) baseObj).doubleValue();
            }
        }
        log.debug("[SSA] Aucune assignation de structure de salaire active ou salaire de base non trouvé pour {}.", employeeId);
        return null;
    }

    // === METHOD TO GENERATE SALARY SLIPS WITH REFERENCE DATA ===
    public List<SalarySlipDTO> genererBulletinSalaire(String employeId,
                                                         LocalDate dateDebut, // Changed to LocalDate
                                                         LocalDate dateFin,   // Changed to LocalDate
                                                         double baseSalaire,
                                                         SalarySlipDTO donneesReference) {
        List<SalarySlipDTO> bulletinsCrees = new ArrayList<>();
        if (dateDebut == null || dateFin == null || dateFin.isBefore(dateDebut)) {
            log.warn("Invalid date range provided for employee {}. No salary slips will be generated.", employeId);
            return bulletinsCrees;
        }

        if (donneesReference == null) {
            log.debug("[SSA] Aucune donnée de référence fournie pour l'employé {}. Génération impossible.", employeId);
            return bulletinsCrees;
        }

        // Retrieve existing salary slips
        List<SalarySlipDTO> bulletinsExistants = salaryService.getSalarySlipsByEmployee(employeId);

        YearMonth start = YearMonth.from(dateDebut);
        YearMonth end = YearMonth.from(dateFin);
        YearMonth current = start;

        while (!current.isAfter(end)) {
            final YearMonth currentMonth = current;
            boolean existe = bulletinsExistants.stream().anyMatch(slip -> {
                LocalDate slipStart = slip.getStartDate();
                return slipStart != null && YearMonth.from(slipStart).equals(currentMonth);
            });

            if (!existe) { // Inverted the condition
                if (donneesReference.getSalaryStructure() != null && donneesReference.getCompany() != null) {
                    // Use the base salary passed as a parameter directly
                    Double basePourSSA = baseSalaire;
                    LocalDate startOfMonth = current.atDay(1);
                    LocalDate endOfMonth = current.atEndOfMonth();

                    // 1. Create a temporary SSA
                    Map<String, Object> assignmentData = new HashMap<>();
                    assignmentData.put("employee", employeId);
                    assignmentData.put("salary_structure", donneesReference.getSalaryStructure());
                    assignmentData.put("from_date", startOfMonth.toString());
                    assignmentData.put("to_date", endOfMonth.toString());
                    assignmentData.put("base", basePourSSA);
                    assignmentData.put("company", donneesReference.getCompany());
                    if (donneesReference.getPayrollFrequency() != null)
                        assignmentData.put("payroll_frequency", donneesReference.getPayrollFrequency());
                    assignmentData.put("is_active", true);

                    Map<String, Object> ssaCreated = salaryStructureAssService.createSalaryStructureAssignment(assignmentData);
                    String ssaNameTemp = null;
                    if (ssaCreated != null && ssaCreated.get("data") instanceof Map<?, ?> dataMap) { // Improved type check
                        ssaNameTemp = (String) dataMap.get("name");
                    }

                    if (ssaNameTemp != null) {
                        // 2. Submit the temporary SSA
                        salaryStructureAssService.submitSalaryStructureAssignment(
                            salaryStructureAssService.getSubmitAssignmentUrl(ssaNameTemp)
                        );
                        log.debug("[SSA] SSA temporaire soumise: {}", ssaNameTemp);
                    }

                    // 3. Generate the salary slip
                    SalarySlipDTO nouveau = new SalarySlipDTO();
                    nouveau.setEmployee(employeId);
                    nouveau.setStartDate(startOfMonth);
                    nouveau.setEndDate(endOfMonth);
                    nouveau.setBaseSalary(basePourSSA);
                    nouveau.setSalaryStructure(donneesReference.getSalaryStructure());
                    nouveau.setCompany(donneesReference.getCompany());
                    nouveau.setCurrency(donneesReference.getCurrency());
                    nouveau.setPayrollFrequency(donneesReference.getPayrollFrequency());

                    SalarySlipDTO created = salaryService.createSalarySlip(nouveau);
                    if (created != null && created.getName() != null) {
                        log.debug("[BULLETIN] Bulletin créé: {}", created.getName());
                        erpNextDocumentService.submitDocument("Salary Slip", created.getName());
                        log.info("[SUBMIT] Nouveau bulletin {} soumis automatiquement.", created.getName()); // Using log.info
                    }
                    // Ajouter le bulletin persisté à la liste finale si la création a réussi
                    if (created != null) {
                        bulletinsCrees.add(created);
                    } else {
                        bulletinsCrees.add(nouveau); // fallback très improbable
                    }

                    // 4. Delete the temporary SSA
                    if (ssaNameTemp != null) {
                        try {
                            erpNextDocumentService.cancelAndDelete("Salary Structure Assignment", ssaNameTemp);
                            log.debug("[DEBUG] SSA temporaire supprimée: {}", ssaNameTemp); // Using log.debug
                        } catch (Exception ex) {
                            log.error("[ERREUR] Impossible de supprimer la SSA temporaire: {} - {}", ssaNameTemp, ex.getMessage()); // Using log.error
                        }
                    }
                }
            } else {
                log.debug("[BULLETIN] Un bulletin existe déjà pour le mois {}. Génération sautée.", current);
            }
            current = current.plusMonths(1);
        }
        return bulletinsCrees;
    }

    public double resolveBaseSalary(Double valeur, Boolean moyenneBase, SalarySlipDTO referenceData) {
    if (valeur != null && valeur != 0.0) {
        System.out.println("[DEBUG][CONTROLLER] Using provided 'valeur' as base salary: " + valeur);
        return valeur;
    } else if (Boolean.TRUE.equals(moyenneBase)) {
        double moyenne = salaryService.calculerMoyenneSalaireBase();
        System.out.println("[DEBUG][CONTROLLER] Using global average base salary: " + moyenne);
        return moyenne;
    } else if (referenceData != null && referenceData.getBaseSalary() != null && referenceData.getBaseSalary() != 0.0) {
        System.out.println("[DEBUG][CONTROLLER] Using last known employee base salary: " + referenceData.getBaseSalary());
        return referenceData.getBaseSalary();
    }
    return 0.0;
}

}