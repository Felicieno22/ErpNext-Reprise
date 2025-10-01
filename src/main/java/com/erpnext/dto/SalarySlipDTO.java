package com.erpnext.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalarySlipDTO {
    // ... (champs existants)
    // Getters et setters générés pour tous les champs utilisés dans SalaryService et SalaryStatsService
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(Double totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }

    public Double getGrossPay() { return grossPay; }
    public void setGrossPay(Double grossPay) { this.grossPay = grossPay; }

    public Double getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(Double totalDeduction) { this.totalDeduction = totalDeduction; }

    public Double getNetPay() { return netPay; }
    public void setNetPay(Double netPay) { this.netPay = netPay; }

    public String getPayrollFrequency() { return payrollFrequency; }
    public void setPayrollFrequency(String payrollFrequency) { this.payrollFrequency = payrollFrequency; }

    public String getSalaryStructure() { return salaryStructure; }
    public void setSalaryStructure(String salaryStructure) { this.salaryStructure = salaryStructure; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public Double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Double baseSalary) { this.baseSalary = baseSalary; }


    @JsonProperty("earnings")
    public List<SalaryComponentDTO> earnings;
    @JsonProperty("deductions")
    public List<SalaryComponentDTO> deductions;
    
    public List<SalaryComponentDTO> getEarnings() { return earnings; }
    public void setEarnings(List<SalaryComponentDTO> earnings) { this.earnings = earnings; }
    public List<SalaryComponentDTO> getDeductions() { return deductions; }
    public void setDeductions(List<SalaryComponentDTO> deductions) { this.deductions = deductions; }
    public String encodedName;

    public String getEncodedName() {
        return encodedName;
    }
    public void setEncodedName(String encodedName) {
        this.encodedName = encodedName;
    }
    @JsonProperty("base_salary")
    public Double baseSalary;

    public String name;

    @JsonProperty("employee")
    public String employee;

    @JsonProperty("employee_name")
    public String employeeName;

    @JsonProperty("posting_date")
    public LocalDate postingDate;

    @JsonProperty("start_date")
    public LocalDate startDate;

    @JsonProperty("end_date")
    public LocalDate endDate;

    public String status;
    public String currency;

    @JsonProperty("total_working_days")
    public Double totalWorkingDays;

    @JsonProperty("gross_pay")
    public Double grossPay;

    @JsonProperty("total_deduction")
    public Double totalDeduction;

    @JsonProperty("net_pay")
    public Double netPay;

    @JsonProperty("total_in_words")
    public String totalInWords;

    @JsonProperty("payroll_frequency")
    public String payrollFrequency;

    @JsonProperty("salary_structure")
    public String salaryStructure;

    public String company;


    /**
     * Retourne la période du bulletin (startDate - endDate) pour affichage.
     */
    public String getPayPeriod() {
        if (startDate != null && endDate != null) {
            return startDate + " - " + endDate;
        }
        return "";
    }

    /**
     * Affiche le bulletin de salaire dans un format lisible.
     */
    public String displaySalarySlip() {
        StringBuilder slip = new StringBuilder();
        slip.append("Bulletin de Salaire\n");
        slip.append("Nom de l'employé: ").append(employeeName).append("\n");
        slip.append("Période de paie: ").append(getPayPeriod()).append("\n");
        slip.append("Salaire brut: ").append(grossPay).append(" ").append(currency).append("\n");
        slip.append("Total des déductions: ").append(totalDeduction).append(" ").append(currency).append("\n");
        slip.append("Salaire net à payer: ").append(netPay).append(" ").append(currency).append("\n");
        slip.append("Total en lettres: ").append(totalInWords).append("\n");
        slip.append("Statut: ").append(status).append("\n");
        return slip.toString();
    }
}
