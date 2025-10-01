package com.erpnext.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.erpnext.service.rest.salary.SalaryStructureService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping("/api/salary-structure")
public class SalaryStructureRestController {
    public static final Logger logger = LoggerFactory.getLogger(SalaryStructureRestController.class);

    @Autowired
    public SalaryStructureService salaryStructureService;

    /**
     * Vérifie le statut docstatus d'une Salary Structure par son nom
     * @param name nom de la structure
     * @return statut et docstatus
     */
    @GetMapping("/status/{name}")
    public Map<String, Object> getSalaryStructureStatus(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> structure = salaryStructureService.getSalaryStructure(name);
        response.put("name", name);
        if (structure != null && structure.containsKey("data")) {
            Object dataObj = structure.get("data");
            if (dataObj instanceof List && !((List<?>)dataObj).isEmpty()) {
                Map<?,?> structMap = (Map<?,?>) ((List<?>)dataObj).get(0);
                Object docstatus = structMap.get("docstatus");
                response.put("docstatus", docstatus);
                if (docstatus != null && (docstatus.equals(1) || docstatus.equals("1"))) {
                    response.put("status", "Submitted");
                } else if (docstatus != null && (docstatus.equals(0) || docstatus.equals("0"))) {
                    response.put("status", "Draft");
                } else if (docstatus != null && (docstatus.equals(2) || docstatus.equals("2"))) {
                    response.put("status", "Cancelled");
                } else {
                    response.put("status", "Unknown");
                }
            } else {
                response.put("status", "NotFound");
            }
        } else {
            response.put("status", "NotFound");
        }
        return response;
    }
}
