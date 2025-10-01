package com.erpnext.controller.rest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.erpnext.service.rest.employe.EmployeService;
import com.erpnext.service.rest.salary.SalaryService;
import com.erpnext.service.rest.utils.SalarieUtilsService;
import com.erpnext.service.rest.utils.SalaryTableService;
import com.erpnext.service.rest.salary.SalaryStructureAssService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.erpnext.dto.EmployeeDTO;
import com.erpnext.dto.SalarySlipDTO;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/employees") // Base path for both web views and API
public class EmployeeController {

    @Autowired
    public EmployeService employeService;
    
    @Autowired
    public SalaryService salaryService;
    @Autowired
    public SalaryTableService salaryTableService;
    
    @Autowired
    public SalarieUtilsService salarieUtilsService;

    @Autowired
    public SalaryStructureAssService salaryStructureAssService;    
    // ======= WEB VIEWS ENDPOINTS =======
    
    @GetMapping("/list")
    public String listEmployees(
            @RequestParam(required = false) String searchTerm,
            Model model) {
        List<EmployeeDTO> employees = employeService.getEmployesByFilters(
                null, null, null, null, searchTerm);
        model.addAttribute("employees", employees);
        model.addAttribute("currentSearchTerm", searchTerm);
        return "employees/list";
    }
    
    @GetMapping("/{employeeId}/details")
    public String employeeDetails(@PathVariable String employeeId, Model model) {
        EmployeeDTO employee = employeService.getEmployeeById(employeeId);
        if (employee == null) {
            return "redirect:/employees/list?error=Employee+not+found";
        }
        List<SalarySlipDTO> salarySlips = salaryTableService.getSalarySlipsByEmployee(employeeId);
        model.addAttribute("employee", employee);
        model.addAttribute("salarySlips", salarySlips);
        return "employees/details";
    }
    
    @GetMapping("/{employeeId}/presences")
    public String employeePresences(@PathVariable String employeeId, Model model) {
        EmployeeDTO employee = employeService.getEmployeeById(employeeId);
        if (employee == null) {
            return "redirect:/employees/list?error=Employee+not+found";
        }
        // Just an example placeholder
        return "redirect:/employees/" + employeeId + "/details?info=Presences+en+cours+de+développement";
    }
    
    @GetMapping({"/{employeeId}/edit", "/{employeeId}/update"})
    public String editEmployeeForm(@PathVariable String employeeId, Model model) {
        EmployeeDTO employee = employeService.getEmployeeById(employeeId);
        if (employee == null) {
            return "redirect:/employees/list?error=Employee+not+found";
        }
        List<String> departments = employeService.getDistinctValues("department");
        List<String> companies = employeService.getDistinctValues("company");
        List<String> designations = employeService.getDistinctValues("designation");
        List<String> statuses = employeService.getDistinctValues("status");
        
        model.addAttribute("employee", employee);
        model.addAttribute("departments", departments);
        model.addAttribute("companies", companies);
        model.addAttribute("designations", designations);
        model.addAttribute("statuses", statuses);
        
        return "employees/edit";
    }
    
    @PostMapping("/{employeeId}/update")
    public String updateEmployee(
            @PathVariable String employeeId,
            @ModelAttribute EmployeeDTO employee,
            Model model) {
        EmployeeDTO existingEmployee = employeService.getEmployeeById(employeeId);
        if (existingEmployee == null) {
            return "redirect:/employees/list?error=Employee+not+found";
        }
        employee.setId(employeeId);
        EmployeeDTO updatedEmployee = employeService.updateEmployee(employeeId, employee);
        if (updatedEmployee == null) {
            return "redirect:/employees/" + employeeId + "/edit?error=Update+failed";
        }
        return "redirect:/employees/" + employeeId + "/details?success=Employee+updated+successfully";
    }
    
   @PostMapping("/{employeeId}/generate-salary")
public String generateSalarySlips(@PathVariable String employeeId,
                                  @RequestParam("startDate") String startDateStr,
                                  @RequestParam("endDate") String endDateStr,
                                  @RequestParam(value = "valeur", required = false) Double valeur,
                                  @RequestParam(value = "ecraser", required = false) Boolean ecraser,
                                  @RequestParam(value = "moyenneBase", required = false) Boolean moyenneBase,
                                  Model model,
                                  HttpServletRequest request) {

    System.out.println("==== Paramètres reçus dans la requête ====");
    request.getParameterMap().forEach((k, v) -> System.out.println(k + " = " + java.util.Arrays.toString(v)));
    System.out.println("==== Fin des paramètres ====");
    System.out.println("valeur=" + valeur + ", ecraser=" + ecraser + ", moyenneBase=" + moyenneBase);

    LocalDate startDate;
    LocalDate endDate;
    try {
        startDate = LocalDate.parse(startDateStr);
        endDate = LocalDate.parse(endDateStr);
    } catch (Exception e) {
        model.addAttribute("error", "Invalid date format. Please use YYYY-MM-DD.");
        EmployeeDTO employee = employeService.getEmployeeById(employeeId);
        List<SalarySlipDTO> salarySlips = salaryTableService.getSalarySlipsByEmployee(employeeId);
        model.addAttribute("employee", employee);
        model.addAttribute("salarySlips", salarySlips);
        return "employees/details";
    }

    // 🔁 Récupérer les données de référence salariale les plus fiables AVANT toute suppression
    // Cette méthode va maintenant chercher dans la SSA si le bulletin n'a pas le salaire de base.
    SalarySlipDTO referenceData = salarieUtilsService.getLatestEmployeeSalaryData(employeeId);
    System.out.println("[DEBUG][CONTROLLER] Reference data found before erasure: " + referenceData);

    // 🗑️ Supprimer les anciennes fiches si l'option est cochée
    if (Boolean.TRUE.equals(ecraser)) {
        salaryService.supprimerSalairesPeriode(employeeId, startDate, endDate);
        salaryStructureAssService.deleteAssignmentsInPeriod(employeeId, startDate, endDate);
        System.out.println("[DEBUG][CONTROLLER] Existing salary slips deleted for period.");
    }

    // ✅ Calculer le salaire de base final à utiliser
    double baseSalaryToUse = salarieUtilsService.resolveBaseSalary(valeur, moyenneBase, referenceData);

    if (baseSalaryToUse == 0.0) {
        System.out.println("[WARN][CONTROLLER] Base salary resolved to 0.0 – ensure this is intended.");
    }

    //  Génération
    salarieUtilsService.genererBulletinSalaire(employeeId, startDate, endDate, baseSalaryToUse, referenceData);

    // 🔄 Rechargement de la vue
    EmployeeDTO employee = employeService.getEmployeeById(employeeId);
    List<SalarySlipDTO> salarySlips = salaryTableService.getSalarySlipsByEmployee(employeeId);
    model.addAttribute("employee", employee);
    model.addAttribute("salarySlips", salarySlips);
    model.addAttribute("success", "Salary slips generated successfully!");
    return "employees/details";
}


    // ======= REST API ENDPOINTS =======
    // Pour bien séparer les API REST, on utilise un préfixe "/api" dans l'URL,
    // on ajoute @ResponseBody sur toutes les méthodes REST (ou on pourrait utiliser @RestController)
    
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<EmployeeDTO>> getAllEmployeesApi() {
        List<EmployeeDTO> employees = employeService.getAllEmploye();
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }
    
    @GetMapping("/api/filter")
    @ResponseBody
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByFiltersApi(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String searchTerm) {
        List<EmployeeDTO> employees = employeService.getEmployesByFilters(
                status, department, company, designation, searchTerm);
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }
    
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeDTO> getEmployeeByIdApi(@PathVariable String id) {
        EmployeeDTO employee = employeService.getEmployeeById(id);
        if (employee == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(employee, HttpStatus.OK);
    }
    
    @GetMapping("/api/distinct/{fieldName}")
    @ResponseBody
    public ResponseEntity<List<String>> getDistinctValuesApi(@PathVariable String fieldName) {
        List<String> values = employeService.getDistinctValues(fieldName);
        return new ResponseEntity<>(values, HttpStatus.OK);
    }
    
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<EmployeeDTO> createEmployeeApi(@RequestBody EmployeeDTO employee) {
        EmployeeDTO createdEmployee = employeService.createEmployee(employee);
        if (createdEmployee == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(createdEmployee, HttpStatus.CREATED);
    }
    
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<EmployeeDTO> updateEmployeeApi(
            @PathVariable String id,
            @RequestBody EmployeeDTO employee) {
        EmployeeDTO updatedEmployee = employeService.updateEmployee(id, employee);
        if (updatedEmployee == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(updatedEmployee, HttpStatus.OK);
    }
    
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteEmployeeApi(@PathVariable String id) {
        boolean deleted = employeService.deleteEmployee(id);
        if (!deleted) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
