package com.erpnext.dto;

import java.time.LocalDate;

public class PayrollDTO {
// Classe renommée pour cohérence (anciennement PayrolDTO)

    public String employeeId; // ID de l'employé
    public String employeeName; // Nom de l'employé
    public String salaryStructure; // Structure salariale associée
    public LocalDate startDate; // Date de début de la période de paie
    public LocalDate endDate; // Date de fin de la période de paie
    public double grossPay; // Montant brut à payer
    public double netPay; // Montant net à payer
    public String company; // Nom de l'entreprise
    public String payrollFrequency; // Fréquence de paie (mensuelle, hebdomadaire, etc.)
    public LocalDate postingDate; // Date de publication de la fiche de paie

    // Getters et Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getSalaryStructure() {
        return salaryStructure;
    }

    public void setSalaryStructure(String salaryStructure) {
        this.salaryStructure = salaryStructure;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(double grossPay) {
        this.grossPay = grossPay;
    }

    public double getNetPay() {
        return netPay;
    }

    public void setNetPay(double netPay) {
        this.netPay = netPay;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPayrollFrequency() {
        return payrollFrequency;
    }

    public void setPayrollFrequency(String payrollFrequency) {
        this.payrollFrequency = payrollFrequency;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }
}
