package com.erpnext.repository;

import com.erpnext.model.SalarySlip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalarySlipRepository extends JpaRepository<SalarySlip, Long> {
    // Méthodes CRUD de base héritées
} 