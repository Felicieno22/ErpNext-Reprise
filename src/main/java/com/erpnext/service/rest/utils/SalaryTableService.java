package com.erpnext.service.rest.utils;

import com.erpnext.model.SalaryTableEntry;
import com.erpnext.service.rest.employe.EmployeService;
import com.erpnext.service.rest.salary.SalaryService;
import com.erpnext.dto.EmployeeDTO;
import com.erpnext.dto.SalarySlipDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryTableService {
    public final SalaryService salaryService;
    public final EmployeService employeService;

    public SalaryTableService(SalaryService salaryService, EmployeService employeService) {
        this.salaryService = salaryService;
        this.employeService = employeService;
    }

    public List<EmployeeDTO> getAllEmployees() {
        return employeService.getAllEmploye();
    }

    public List<SalarySlipDTO> getAllSalarySlips() {
        return salaryService.getAllSalarySlips();
    }

    /**
     * Retourne la liste des bulletins de salaire pour un employé donné
     */
    public List<SalarySlipDTO> getSalarySlipsByEmployee(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) return Collections.emptyList();
        return getAllSalarySlips().stream()
            .filter(slip -> employeeId.equals(slip.getEmployee()))
            .map(slip -> salaryService.fetchSalarySlipDetailById(slip.getName()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<SalaryTableEntry> getFilteredSalaryTableEntries(String employee, String month, String year) {
        List<SalarySlipDTO> allSalarySlips = getAllSalarySlips();
        List<SalaryTableEntry> entries = new ArrayList<>();
        for (SalarySlipDTO slip : allSalarySlips) {
            if ((employee == null || employee.isEmpty() || employee.equals(slip.getEmployee())) &&
                (month == null || month.isEmpty() || (slip.getStartDate() != null && slip.getStartDate().getMonthValue() == Integer.parseInt(month))) &&
                (year == null || year.isEmpty() || (slip.getStartDate() != null && slip.getStartDate().getYear() == Integer.parseInt(year)))) {
                SalarySlipDTO detailed = salaryService.fetchSalarySlipDetailById(slip.getName());
                if (detailed != null) {
                    SalaryTableEntry entry = new SalaryTableEntry();
                    entry.setEmployeeId(detailed.getEmployee());
                    entry.setEmployeeName(detailed.getEmployeeName());
                    entry.setSalarySlipId(detailed.getName());
                    // Période sous forme "MM/YYYY" ou fallback
                    if (detailed.getStartDate() != null && detailed.getEndDate() != null) {
                        entry.setPeriod(String.format("%02d/%d", detailed.getStartDate().getMonthValue(), detailed.getStartDate().getYear()));
                    } else {
                        entry.setPeriod("-");
                    }
                    // Calculs numériques
                    if (detailed.getEarnings() != null) {
                        entry.setBaseSalaryFromComponent(
                            detailed.getEarnings().stream()
                                .filter(e -> e.getSalaryComponent() != null && e.getSalaryComponent().toLowerCase().contains("base"))
                                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0).sum()
                        );
                        entry.setPrimeAmount(
                            detailed.getEarnings().stream()
                                .filter(e -> e.getSalaryComponent() != null && e.getSalaryComponent().toLowerCase().contains("prime"))
                                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0).sum()
                        );
                        entry.setIndemniteAmount(
                            detailed.getEarnings().stream()
                                .filter(e -> e.getSalaryComponent() != null && e.getSalaryComponent().toLowerCase().contains("indemn"))
                                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0).sum()
                        );
                    }
                    if (detailed.getDeductions() != null) {
                        entry.setRetenueAmount(
                            detailed.getDeductions().stream()
                                .mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0).sum()
                        );
                    }
                    entry.setGrossPay(detailed.getGrossPay());
                    entry.setNetPay(detailed.getNetPay());
                    entry.setTotalDeduction(detailed.getTotalDeduction());
                    Double brut = detailed.getGrossPay() != null ? detailed.getGrossPay() : 0;
                    Double retenue = entry.getRetenueAmount() != null ? entry.getRetenueAmount() : 0;
                    entry.setTotalAmount(brut - retenue);
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    public Set<String> getAvailableMonths(List<SalarySlipDTO> slips) {
        Set<String> months = new TreeSet<>();
        for (SalarySlipDTO slip : slips) {
            if (slip.getStartDate() != null) {
                months.add(String.format("%02d", slip.getStartDate().getMonthValue()));
            }
        }
        return months;
    }

    public Set<String> getAvailableYears(List<SalarySlipDTO> slips) {
        Set<String> years = new TreeSet<>();
        for (SalarySlipDTO slip : slips) {
            if (slip.getStartDate() != null) {
                years.add(String.valueOf(slip.getStartDate().getYear()));
            }
        }
        if (years.isEmpty()) {
            years.add(String.valueOf(LocalDate.now().getYear()));
        }
        return years;
    }
}
