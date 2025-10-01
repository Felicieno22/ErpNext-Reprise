package com.erpnext.controller.rest;

import com.erpnext.dto.EmployeeDTO;
import com.erpnext.dto.SalarySlipDTO;
import com.erpnext.service.rest.employe.EmployeService;
import com.erpnext.service.rest.salary.SalaryComponentService;
import com.erpnext.service.rest.salary.SalaryDetailsService;
import com.erpnext.service.rest.salary.SalaryService;
import com.erpnext.service.rest.utils.SalaryTableService;
import com.erpnext.utils.PdfGeneratorUtil;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des bulletins de salaire
 */
@Controller
@RequestMapping("/salaries")
@RequiredArgsConstructor
public class SalaryController {
    public static final Logger log = LoggerFactory.getLogger(SalaryController.class);
    
    public final SalaryService salaryService;
    public final EmployeService employeService;
    public final PdfGeneratorUtil pdfGeneratorUtil;
    public final SalaryDetailsService salaryDetailsService;
    public final SalaryTableService salaryTableService;
    public final SalaryComponentService salaryComponentService;

    /**
     * Retourne la liste complète des bulletins de salaire (API).
     */
    @GetMapping("/api/salary-slips")
    @ResponseBody
    public ResponseEntity<List<SalarySlipDTO>> getAllSalarySlipsApi() {
        List<SalarySlipDTO> slips = salaryService.getAllSalarySlips();
        return ResponseEntity.ok(slips != null ? slips : new ArrayList<>());
    }

    /**
     * Retourne un bulletin de salaire détaillé par son identifiant (API).
     */
    @GetMapping("/api/salary-slips/{id}")
    @ResponseBody
    public ResponseEntity<SalarySlipDTO> getSalarySlipByIdApi(@PathVariable String id) {
        SalarySlipDTO salarySlipDTO = salaryDetailsService.getSalarySlipDetail(id);
        return salarySlipDTO != null ? ResponseEntity.ok(salarySlipDTO) : ResponseEntity.notFound().build();
    }

    /**
     * Crée un nouveau bulletin de salaire (API).
     */
    @PostMapping("/api/salary-slips")
    @ResponseBody
    public ResponseEntity<SalarySlipDTO> createSalarySlipApi(@RequestBody SalarySlipDTO salarySlipDTO) {
        SalarySlipDTO createdSlip = salaryService.createSalarySlip(salarySlipDTO);
        return createdSlip != null ? new ResponseEntity<>(createdSlip, HttpStatus.CREATED) : new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    /**
     * Met à jour un bulletin de salaire existant (API).
     */
    @PutMapping("/api/salary-slips/{id}")
    @ResponseBody
    public ResponseEntity<SalarySlipDTO> updateSalarySlipApi(@PathVariable String id, @RequestBody SalarySlipDTO salarySlipDTO) {
        SalarySlipDTO updatedSlip = salaryService.updateSalarySlip(id, salarySlipDTO);
        return updatedSlip != null ? ResponseEntity.ok(updatedSlip) : ResponseEntity.notFound().build();
    }

    /**
     * Supprime un bulletin de salaire par son identifiant (API).
     */
    @DeleteMapping("/api/salary-slips/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteSalarySlipApi(@PathVariable String id) {
        boolean deleted = salaryService.deleteSalarySlip(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Télécharge un bulletin de salaire en PDF (API).
     */
    @GetMapping("/api/salary-slips/{id}/pdf")
    public ResponseEntity<InputStreamResource> downloadSalarySlipPdfApi(@PathVariable String id) {
        log.info("API request to download PDF for salary slip ID: {}", id);
        SalarySlipDTO salarySlip = salaryDetailsService.getSalarySlipDetail(id);
        if (salarySlip == null) {
            log.warn("Salary slip with ID {} not found for PDF generation.", id);
            return ResponseEntity.notFound().build();
        }

        EmployeeDTO employee = employeService.getEmployeeById(salarySlip.getEmployee());
        if (employee == null) {
            log.warn("Employee with ID {} not found for salary slip {}", salarySlip.getEmployee(), id);
            return ResponseEntity.notFound().build();
        }

        try {
            ByteArrayInputStream bis = pdfGeneratorUtil.generateSalarySlipPdf(employee, salarySlip);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=salary_slip_" + id.replace("/", "_") + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));
        } catch (IOException e) {
            log.error("Error generating PDF for salary slip ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Affiche la liste des bulletins de salaire avec filtres (vue web).
     */
    @GetMapping("/list")
    public String listSalarySlips(
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String year,
            Model model) {
        
        log.info("Affichage de la liste des bulletins de salaire avec filtres: employee={}, month={}, year={}", employee, month, year);
        
        List<EmployeeDTO> allEmployees = employeService.getAllEmploye();
        List<SalarySlipDTO> allSalarySlips = new ArrayList<>();
        
        for (EmployeeDTO emp : allEmployees) {
            List<SalarySlipDTO> employeeSlips = salaryTableService.getSalarySlipsByEmployee(emp.getId());
            if (employeeSlips != null) {
                allSalarySlips.addAll(employeeSlips);
            }
        }
        
        List<SalarySlipDTO> filteredSlips = allSalarySlips.stream()
                .filter(slip -> employee == null || employee.isEmpty() || slip.getEmployee().equals(employee))
                .filter(slip -> {
                    if (month == null || month.isEmpty()) return true;
                    if (slip.getStartDate() == null) return false;
                    try {
                        return slip.getStartDate().getMonthValue() == Integer.parseInt(month);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .filter(slip -> {
                    if (year == null || year.isEmpty()) return true;
                    if (slip.getStartDate() == null) return false;
                    try {
                        return slip.getStartDate().getYear() == Integer.parseInt(year);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
        // Ajout de l'encodage du nom pour chaque bulletin
        java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
        for (SalarySlipDTO slip : filteredSlips) {
            try {
                slip.setEncodedName(java.net.URLEncoder.encode(slip.getName(), utf8));
            } catch (Exception e) {
                slip.setEncodedName(slip.getName()); // fallback
            }
        }
        
        Set<String> months = new TreeSet<>();
        for (int i = 1; i <= 12; i++) {
            months.add(String.format("%02d", i)); // 01, 02, ..., 12
        }
        
        Set<String> years = new TreeSet<>();
        for (SalarySlipDTO slip : allSalarySlips) {
            if (slip.getStartDate() != null) {
                years.add(String.valueOf(slip.getStartDate().getYear()));
            }
        }
        if (years.isEmpty()) {
            years.add(String.valueOf(LocalDate.now().getYear()));
        }
        
        // Ajout de la liste des composants salariaux pour le formulaire
        List<String> salaryComponents = salaryComponentService.getAllComponentNames();
        model.addAttribute("salaryComponents", salaryComponents);
        
        model.addAttribute("salarySlips", filteredSlips);
        model.addAttribute("employees", allEmployees);
        model.addAttribute("months", months);
        model.addAttribute("years", years);
        model.addAttribute("currentEmployee", employee);
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentYear", year);
        
        return "salaries/list";
    }

    /**
     * Génère et télécharge un PDF pour un bulletin de salaire spécifique.
     */
    @GetMapping("/pdf/{salarySlipId}")
    public ResponseEntity<?> generateSalarySlipPdf(@PathVariable String salarySlipId) {
        return generatePdf(salarySlipId);
    }

    /**
     * Gère les identifiants complexes contenant des slashes pour le téléchargement PDF.
     */
    @GetMapping("/pdf/**")
    public ResponseEntity<?> generateSalarySlipPdfWithComplexId(HttpServletRequest request) {
        String path = request.getRequestURI();
        String prefix = "/salaries/pdf/";
        
        if (path.startsWith(prefix) && path.length() > prefix.length()) {
            String salarySlipId = path.substring(prefix.length());
            if (!salarySlipId.contains("/")) {
                return null; // Laisser la méthode simple gérer ce cas
            }
            try {
                if (salarySlipId.contains("%")) {
                    salarySlipId = java.net.URLDecoder.decode(salarySlipId, "UTF-8");
                }
            } catch (Exception e) {
                log.error("Erreur lors du décodage de l'ID du bulletin pour PDF: {}", e.getMessage());
            }
            log.info("Génération du PDF pour le bulletin de salaire (ID complexe): {}", salarySlipId);
            return generatePdf(salarySlipId);
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> generatePdf(String salarySlipId) {
        SalarySlipDTO salarySlip = salaryDetailsService.getSalarySlipDetail(salarySlipId);
        if (salarySlip == null) {
            log.warn("Bulletin de salaire non trouvé pour PDF: {}", salarySlipId);
            return ResponseEntity.notFound().build();
        }

        EmployeeDTO employee = employeService.getEmployeeById(salarySlip.getEmployee());
        if (employee == null) {
            log.warn("Employé non trouvé pour le PDF du bulletin: {}", salarySlip.getEmployee());
            return ResponseEntity.notFound().build();
        }

        try {
            ByteArrayInputStream bis = pdfGeneratorUtil.generateSalarySlipPdf(employee, salarySlip);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=salary_slip_" + salarySlipId.replace("/", "_") + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));
        } catch (IOException e) {
            log.error("Erreur lors de la génération du PDF pour le bulletin de salaire ID: {}", salarySlipId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/update-base")
    public String showUpdateBaseSalaryForm(Model model) {
        model.addAttribute("salaryComponents", salaryComponentService.getAllComponentNames());
        return "components/update-base-salary";
    }

    @PostMapping("/update-base")
    public String updateBaseSalary(
            @RequestParam(name = "salaryComponent", required = false) String salaryComponent,
            @RequestParam(name = "montant", required = false) Double montant,
            @RequestParam(name = "pourcentage", required = false) Double pourcentage,
            @RequestParam(name = "sign", required = false) String sign,
            @RequestParam(name = "valeurType", required = false) String valeurType,
            Model model
    ) {
        log.info("[updateBaseSalary] Reçu : composant={}, montant={}, pourcentage={}, signe={}, valeurType={}", salaryComponent, montant, pourcentage, sign, valeurType);
        try {
            salaryComponentService.updateBaseSalaryForAllEmployees(salaryComponent, montant, pourcentage, sign, valeurType);
            log.info("[updateBaseSalary] Mise à jour du salaire de base terminée.");
            model.addAttribute("successMessage", "Mise à jour du salaire de base effectuée avec succès.");
        } catch (RuntimeException ex) {
            log.error("[updateBaseSalary] Erreur : {}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("existingSalarySlips", salaryService.getAllSalarySlips());
        }
        model.addAttribute("salaryComponents", salaryComponentService.getAllComponentNames());
        return "components/update-base-salary";
    }
    @GetMapping("/recherche")
    public String rechercheSalaire(
            @RequestParam(value = "minSalaire", required = false) Double minSalaire,
            @RequestParam(value = "maxSalaire", required = false) Double maxSalaire,
            @RequestParam(value = "mois", required = false) Integer mois,
            @RequestParam(value = "annee", required = false) Integer annee,
            Model model) {
        List<SalarySlipDTO> salarySlips = salaryService.rechercheSalaireMulti(minSalaire, maxSalaire, mois, annee);
        model.addAttribute("salarySlips", salarySlips);
        model.addAttribute("minSalaire", minSalaire);
        model.addAttribute("maxSalaire", maxSalaire);
        model.addAttribute("mois", mois);
        model.addAttribute("annee", annee);
        return "salaries/recherche";
    }

}

