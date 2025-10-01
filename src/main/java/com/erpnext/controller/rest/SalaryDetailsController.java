package com.erpnext.controller.rest;

import com.erpnext.dto.SalarySlipDTO;
import com.erpnext.service.rest.salary.SalaryDetailsService;
import com.erpnext.dto.EmployeeDTO;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/salaries/details")
@RequiredArgsConstructor
public class SalaryDetailsController {
    public static final Logger log = LoggerFactory.getLogger(SalaryDetailsController.class);
    public final SalaryDetailsService detailsService;

    @GetMapping("/{salarySlipId:.+}")
    public String salarySlipDetails(@PathVariable String salarySlipId, Model model) {
        // Décodage manuel pour supporter les %2F (slashs encodés)
        try {
            salarySlipId = java.net.URLDecoder.decode(salarySlipId, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.warn("Erreur lors du décodage de l'identifiant du bulletin: {}", e.getMessage());
        }
        return showSalarySlipDetails(salarySlipId, model);
    }

    @GetMapping("")
    public String salarySlipDetailsWithParam(@RequestParam("id") String salarySlipId, Model model) {
        return showSalarySlipDetails(salarySlipId, model);
    }

    public String showSalarySlipDetails(String salarySlipId, Model model) {
        SalarySlipDTO salarySlip = detailsService.getSalarySlipDetail(salarySlipId);
        if (salarySlip == null) {
            model.addAttribute("errorMessage", "Le bulletin de salaire '" + salarySlipId + "' n'existe pas.");
            model.addAttribute("errorTitle", "Bulletin non trouvé");
            model.addAttribute("returnUrl", "/salaries/table");
            return "error/not-found";
        }
        EmployeeDTO employee = detailsService.getEmployeeForSlip(salarySlip);
        // Extraction du salaire de base depuis les earnings (composant "Salaire Base")
        Double baseSalaryFromComponent = null;
        
if (salarySlip.getEarnings() != null) {
    for (var earning : salarySlip.getEarnings()) {
        
        String comp = earning.getSalaryComponent();
        if (comp != null && comp.toLowerCase().replace(" ", "").contains("salairebase")) {
            baseSalaryFromComponent = earning.getAmount();
            
            break;
        }
    }
}
        model.addAttribute("salarySlip", salarySlip);
        model.addAttribute("employee", employee);
        model.addAttribute("baseSalary", salarySlip.getBaseSalary());
        model.addAttribute("baseSalaryFromComponent", baseSalaryFromComponent);
        model.addAttribute("earnings", salarySlip.getEarnings());
        model.addAttribute("deductions", salarySlip.getDeductions());
        model.addAttribute("grossPay", salarySlip.getGrossPay());
        model.addAttribute("totalDeduction", salarySlip.getTotalDeduction());
        return "salaries/details";
    }
}
