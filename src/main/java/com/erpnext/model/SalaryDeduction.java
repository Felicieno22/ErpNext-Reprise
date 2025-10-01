package com.erpnext.model;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Data
@Table(name="deductionSalaire")
public class SalaryDeduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="id")
    Long id;
    @Column(name = "mois")
    String mois;
    @Column(name = "deduction")
    double deduction;
    
    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id=id;
    }
    public String getMois(){
        return mois;
    }
    public void setMois(String mois){
        this.mois=mois;
    }
    public Double getDeduction(){
        return deduction;
    }
    public void setDeduction(double deduction){
        this.deduction=deduction;
    }
}
