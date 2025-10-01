package com.erpnext.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
public class SalarySlip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;
    public String employee;
    public String employeeName;
    public LocalDate postingDate;
    public LocalDate startDate;
    public LocalDate endDate;
    public String status;
    public String currency;
    public Double totalWorkingDays;
    public Double grossPay;
    public Double totalDeduction;
    public Double netPay;
    public String payrollFrequency;
    public String salaryStructure;
    public String company;
    public Double baseSalary;

    public void setId(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    
} 