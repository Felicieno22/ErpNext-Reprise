package com.erpnext.controller;

import com.erpnext.model.SalaryDeduction;
import com.erpnext.service.base.SalaryDeductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-deductions")
public class SalaryDeductionController {
    
    @Autowired
    private SalaryDeductionService salaryDeductionService;

    @GetMapping
    public ResponseEntity<List<SalaryDeduction>> getAll() {
        return ResponseEntity.ok(salaryDeductionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryDeduction> getById(@PathVariable Long id) {
        SalaryDeduction deduction = salaryDeductionService.findById(id);
        if (deduction != null) {
            return ResponseEntity.ok(deduction);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<SalaryDeduction> create(@RequestBody SalaryDeduction deduction) {
        SalaryDeduction created = salaryDeductionService.save(deduction);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryDeduction> update(@PathVariable Long id, @RequestBody SalaryDeduction deduction) {
        SalaryDeduction existing = salaryDeductionService.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        deduction.setId(id);
        SalaryDeduction updated = salaryDeductionService.save(deduction);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        SalaryDeduction deduction = salaryDeductionService.findById(id);
        if (deduction == null) {
            return ResponseEntity.notFound().build();
        }
        salaryDeductionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-month")
    public ResponseEntity<List<SalaryDeduction>> getByMonth(@RequestParam String month) {
        List<SalaryDeduction> deductions = salaryDeductionService.findByMonth(month);
        return ResponseEntity.ok(deductions);
    }
}
