package com.erpnext.controller.rest;

import com.erpnext.model.SalarySlip;
import com.erpnext.service.rest.salary.SalarySlipService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-slips")
public class SalarySlipController {
    @Autowired
    public SalarySlipService salarySlipService;

    @GetMapping
    public List<SalarySlip> getAll() {
        return salarySlipService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalarySlip> getById(@PathVariable Long id) {
        return salarySlipService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public SalarySlip create(@RequestBody SalarySlip slip) {
        return salarySlipService.save(slip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalarySlip> update(@PathVariable Long id, @RequestBody SalarySlip slip) {
        return salarySlipService.findById(id)
                .map(existing -> {
                    slip.setId(id);
                    return ResponseEntity.ok(salarySlipService.save(slip));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (salarySlipService.findById(id).isPresent()) {
            salarySlipService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
} 