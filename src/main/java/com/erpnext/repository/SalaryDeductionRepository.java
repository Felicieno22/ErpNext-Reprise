package com.erpnext.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.erpnext.model.SalaryDeduction;

@Repository
public interface SalaryDeductionRepository extends JpaRepository<SalaryDeduction, Long> {
    
    List<SalaryDeduction> findByMois(String mois);
}
