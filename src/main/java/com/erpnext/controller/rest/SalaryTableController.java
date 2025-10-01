package com.erpnext.controller.rest;

import com.erpnext.model.SalaryTableEntry;
import com.erpnext.service.rest.utils.SalaryTableService;
import com.erpnext.dto.EmployeeDTO;
import com.erpnext.dto.SalarySlipDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/salaries/table")
@RequiredArgsConstructor
@Slf4j
public class SalaryTableController {
    public final SalaryTableService tableService;

    @GetMapping("")
    public String showSalaryTable(@RequestParam(value = "employee", required = false) String employee,
                                  @RequestParam(value = "month", required = false) String month,
                                  @RequestParam(value = "year", required = false) String year,
                                  Model model) {
        List<EmployeeDTO> allEmployees = tableService.getAllEmployees();
        List<SalarySlipDTO> allSalarySlips = tableService.getAllSalarySlips();
        List<SalaryTableEntry> salaryTableEntries = tableService.getFilteredSalaryTableEntries(employee, month, year);
        Set<String> months = tableService.getAvailableMonths(allSalarySlips);
        Set<String> years = tableService.getAvailableYears(allSalarySlips);
        model.addAttribute("employees", allEmployees);
        model.addAttribute("months", months);
        model.addAttribute("years", years);
        model.addAttribute("currentEmployee", employee);
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentYear", year);
        model.addAttribute("salaryTableEntries", salaryTableEntries != null ? salaryTableEntries : new java.util.ArrayList<>());
        model.addAttribute("totalRow", SalaryTableEntry.totalOf(salaryTableEntries));
        return "salaries/table";
    }
}
