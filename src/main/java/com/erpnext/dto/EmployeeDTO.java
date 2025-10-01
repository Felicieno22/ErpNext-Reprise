package com.erpnext.dto;

// Utiliser String pour les dates pour correspondre au format JSON "YYYY-MM-DD"
// import java.util.Date; 

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmployeeDTO {
    @JsonProperty("first_name")
    public String firstName;
    @JsonProperty("last_name")
    public String lastName;

    public String name;              // name de l'enregistrement dans ERPNext
    @JsonProperty("employee_name")
    public String employeeName;    // Correspond à "employee_name"
    @JsonProperty("employee_number")
    public String employeeNumber;  // Correspond à "employee_number"
    @JsonProperty("date_of_joining")
    public String dateOfJoining;   // Correspond à "date_of_joining"
    @JsonProperty("date_of_birth")
    public String dateOfBirth;    // Correspond à "date_of_birth"
    public String company;
    public String gender;
    public String status;
    public String department;
    public String designation;

    // Constructeur par défaut
    public EmployeeDTO() {
    }

    // Constructeur avec les champs pour la création (sans name interne 'name')
    public EmployeeDTO(String employeeName, String employeeNumber, String dateOfJoining, String company, String gender, String status, String department, String designation) {
        this.employeeName = employeeName;
        this.employeeNumber = employeeNumber;
        this.dateOfJoining = dateOfJoining;
        this.company = company;
        this.gender = gender;
        this.status = status;
        this.department = department;
        this.designation = designation;
    }

    // Ancien constructeur pour compatibilité
    public EmployeeDTO(String employeeName, String employeeNumber, String dateOfJoining, String company, String gender, String status) {
        this.employeeName = employeeName;
        this.employeeNumber = employeeNumber;
        this.dateOfJoining = dateOfJoining;
        this.company = company;
        this.gender = gender;
        this.status = status;
    }

    // Getters et Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getId() {
        return name;
    }

    public void setId(String id) {
        this.name = id;
    }

    // Existing getname/setname methods for backward compatibility
    public String getname() {
        return name;
    }

    public void setname(String name) {
        this.name = name;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(String dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}

