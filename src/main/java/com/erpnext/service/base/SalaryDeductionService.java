package com.erpnext.service.base;

import com.erpnext.model.SalaryDeduction;
import com.erpnext.repository.SalaryDeductionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SalaryDeductionService {
    
    @Autowired
    private SalaryDeductionRepository salaryDeductionRepository;

    public List<SalaryDeduction> findAll() {
        return salaryDeductionRepository.findAll();
    }

    public SalaryDeduction findById(Long id) {
        Optional<SalaryDeduction> optional = salaryDeductionRepository.findById(id);
        return optional.orElse(null);
    }

    public SalaryDeduction save(SalaryDeduction deduction) {
        return salaryDeductionRepository.save(deduction);
    }

    public List<SalaryDeduction> findByMonth(String month) {
        return salaryDeductionRepository.findByMois(month);
    }

    public void delete(Long id) {
        salaryDeductionRepository.deleteById(id);
    }
}
