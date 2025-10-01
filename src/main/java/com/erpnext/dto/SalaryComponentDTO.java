package com.erpnext.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalaryComponentDTO {
    public String name;

    @JsonProperty("salary_component")
    public String salaryComponent;

    public String abbreviation;
    public String type;

    @JsonProperty("amount")
    public Double amount;

    @JsonProperty("default_amount")
    public Double defaultAmount;

    @JsonProperty("is_tax_applicable")
    public Boolean isTaxApplicable;

    @JsonProperty("depends_on_lwp")
    public Boolean dependsOnLwp;

    @JsonProperty("do_not_include_in_total")
    public Boolean doNotIncludeInTotal;

    // Getters and setters for all fields
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSalaryComponent() { return salaryComponent; }
    public void setSalaryComponent(String salaryComponent) { this.salaryComponent = salaryComponent; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getDefaultAmount() { return defaultAmount; }
    public void setDefaultAmount(Double defaultAmount) { this.defaultAmount = defaultAmount; }

    public Boolean getIsTaxApplicable() { return isTaxApplicable; }
    public void setIsTaxApplicable(Boolean isTaxApplicable) { this.isTaxApplicable = isTaxApplicable; }

    public Boolean getDependsOnLwp() { return dependsOnLwp; }
    public void setDependsOnLwp(Boolean dependsOnLwp) { this.dependsOnLwp = dependsOnLwp; }

    public Boolean getDoNotIncludeInTotal() { return doNotIncludeInTotal; }
    public void setDoNotIncludeInTotal(Boolean doNotIncludeInTotal) { this.doNotIncludeInTotal = doNotIncludeInTotal; }
}

