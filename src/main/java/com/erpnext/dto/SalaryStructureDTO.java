package com.erpnext.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryStructureDTO {
    public String name;

    @JsonProperty("salary_structure_name")
    public String salaryStructureName;

    @JsonProperty("is_active")
    public Boolean isActive;

    public String company;

    @JsonProperty("payroll_frequency")
    public String payrollFrequency;

    public List<SalaryComponentDTO> earnings;
    public List<SalaryComponentDTO> deductions;

    @JsonProperty("from_date")
    public String fromDate;

    @JsonProperty("to_date")
    public String toDate;
}
