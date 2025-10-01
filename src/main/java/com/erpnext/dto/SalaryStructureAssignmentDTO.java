package com.erpnext.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryStructureAssignmentDTO {
    public String name;

    public String employee;

    @JsonProperty("salary_structure")
    public String salaryStructure;

    @JsonProperty("from_date")
    public String fromDate;

    @JsonProperty("base")
    public Double base;

    @JsonProperty("company")
    public String company;

    @JsonProperty("payroll_frequency")
    public String payrollFrequency;

    @JsonProperty("is_active")
    public Boolean isActive;
}
