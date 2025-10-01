package com.erpnext.service.rest.salary;

import com.erpnext.model.SalarySlip;
import com.erpnext.repository.SalarySlipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SalarySlipService {
    @Autowired
    public SalarySlipRepository salarySlipRepository;

    public List<SalarySlip> findAll() {
        return salarySlipRepository.findAll();
    }

    public Optional<SalarySlip> findById(Long id) {
        return salarySlipRepository.findById(id);
    }

    public SalarySlip save(SalarySlip slip) {
        return salarySlipRepository.save(slip);
    }

    public void deleteById(Long id) {
        salarySlipRepository.deleteById(id);
    }
} 