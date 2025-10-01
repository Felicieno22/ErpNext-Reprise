package com.erpnext.model;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class SalaryTableEntry {
    public String employeeId;
    public String employeeName;
    public String department;
    public String salarySlipId;
    public String period; // Pour affichage de la période (ex: "01/2024")
    public Map<String, String> components;

    // Champs numériques pour calculs et affichage
    public Double baseSalaryFromComponent;
    public Double primeAmount;
    public Double indemniteAmount;
    public Double grossPay;
    public Double retenueAmount;
    public Double netPay;
    public Double totalDeduction;
    public Double totalAmount;

    public SalaryTableEntry() {
        this.components = new HashMap<>();
    }

    // Getters et setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getSalarySlipId() { return salarySlipId; }
    public void setSalarySlipId(String salarySlipId) { this.salarySlipId = salarySlipId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Map<String, String> getComponents() { return components; }
    public void setComponents(Map<String, String> components) { this.components = components; }
    public Double getBaseSalaryFromComponent() { return baseSalaryFromComponent; }
    public void setBaseSalaryFromComponent(Double baseSalaryFromComponent) { this.baseSalaryFromComponent = baseSalaryFromComponent; }
    public Double getPrimeAmount() { return primeAmount; }
    public void setPrimeAmount(Double primeAmount) { this.primeAmount = primeAmount; }
    public Double getIndemniteAmount() { return indemniteAmount; }
    public void setIndemniteAmount(Double indemniteAmount) { this.indemniteAmount = indemniteAmount; }
    public Double getGrossPay() { return grossPay; }
    public void setGrossPay(Double grossPay) { this.grossPay = grossPay; }
    public Double getRetenueAmount() { return retenueAmount; }
    public void setRetenueAmount(Double retenueAmount) { this.retenueAmount = retenueAmount; }
    public Double getNetPay() { return netPay; }
    public void setNetPay(Double netPay) { this.netPay = netPay; }
    public Double getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(Double totalDeduction) { this.totalDeduction = totalDeduction; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    // Format utilitaire pour affichage (euros, 2 décimales, ou "-")
    public static String formatEuro(Double value) {
        if (value == null) return "-";
        return String.format("%,.2f €", value).replace(',', ' ').replace('.', ',');
    }

    // Génération d'une ligne TOTAL
    public static SalaryTableEntry totalOf(List<SalaryTableEntry> entries) {
        SalaryTableEntry total = new SalaryTableEntry();
        total.setEmployeeName("TOTAL");
        total.setBaseSalaryFromComponent(entries.stream().filter(e -> e.getBaseSalaryFromComponent() != null).mapToDouble(SalaryTableEntry::getBaseSalaryFromComponent).sum());
        total.setPrimeAmount(entries.stream().filter(e -> e.getPrimeAmount() != null).mapToDouble(SalaryTableEntry::getPrimeAmount).sum());
        total.setIndemniteAmount(entries.stream().filter(e -> e.getIndemniteAmount() != null).mapToDouble(SalaryTableEntry::getIndemniteAmount).sum());
        total.setGrossPay(entries.stream().filter(e -> e.getGrossPay() != null).mapToDouble(SalaryTableEntry::getGrossPay).sum());
        total.setRetenueAmount(entries.stream().filter(e -> e.getRetenueAmount() != null).mapToDouble(SalaryTableEntry::getRetenueAmount).sum());
        total.setNetPay(entries.stream().filter(e -> e.getNetPay() != null).mapToDouble(SalaryTableEntry::getNetPay).sum());
        total.setTotalDeduction(entries.stream().filter(e -> e.getTotalDeduction() != null).mapToDouble(SalaryTableEntry::getTotalDeduction).sum());
        total.setTotalAmount(entries.stream().filter(e -> e.getTotalAmount() != null).mapToDouble(SalaryTableEntry::getTotalAmount).sum());
        return total;
    }
}