package com.erpnext.service.rest.salary;

import com.erpnext.dto.EmployeeDTO;
import com.erpnext.dto.SalarySlipDTO;
import com.erpnext.service.rest.employe.EmployeService;
import com.erpnext.dto.SalaryComponentDTO;
import org.springframework.stereotype.Service;

@Service
public class SalaryDetailsService {
    public final SalaryService salaryService;
    public final EmployeService employeService;

    public SalaryDetailsService(SalaryService salaryService, EmployeService employeService) {
        this.salaryService = salaryService;
        this.employeService = employeService;
    }

    /**
     * Retourne le composant 'Salaire Base' s'il existe.
     */
    public SalaryComponentDTO getBaseSalaryComponent(SalarySlipDTO slip) {
        if (slip != null && slip.getEarnings() != null) {
            return slip.getEarnings().stream()
                .filter(e -> "Salaire Base".equals(e.getSalaryComponent()))
                .findFirst().orElse(null);
        }
        return null;
    }

    /**
     * Retourne la première déduction s'il y en a.
     */
    public SalaryComponentDTO getFirstDeductionComponent(SalarySlipDTO slip) {
        if (slip != null && slip.getDeductions() != null && !slip.getDeductions().isEmpty()) {
            return slip.getDeductions().get(0);
        }
        return null;
    }

    public SalarySlipDTO getSalarySlipDetail(String salarySlipId) {
        if (salarySlipId == null || salarySlipId.isEmpty()) return null;
        return salaryService.fetchSalarySlipDetailById(salarySlipId);
    }

    public EmployeeDTO getEmployeeForSlip(SalarySlipDTO slip) {
        if (slip == null || slip.getEmployee() == null) return null;
        return employeService.getEmployeeById(slip.getEmployee());
    }
}
