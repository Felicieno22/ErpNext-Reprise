package com.erpnext.service.rest.imports;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;  
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Calendar;

import com.erpnext.service.rest.salary.SalaryComponentService;
import com.erpnext.service.rest.salary.SalaryStructureService;
import com.erpnext.service.rest.company.CompanyService;
import com.erpnext.service.rest.employe.EmployeService;
import com.erpnext.service.rest.salary.SalaryStructureAssService;
import com.erpnext.service.rest.salary.PayrollService;

@Service
public class ImportService {
    public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImportService.class);

    public final EmployeService employeService;

    public EmployeService getEmployeService() {
        return employeService;
    }
    public final SalaryComponentService salaryComponentService;
    public final SalaryStructureService salaryStructureService;
    public final SalaryStructureAssService salaryStructureAssService;
    public final com.erpnext.utils.ErpNextApiUtil erpNextApiUtil;
    public final PayrollService payrollService;
    public final SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    public final SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public final CompanyService companyService;

    @Autowired
    public ImportService(
            EmployeService employeService,
            SalaryComponentService salaryComponentService,
            SalaryStructureService salaryStructureService,
            SalaryStructureAssService salaryStructureAssService,
            PayrollService payrollService,
            com.erpnext.utils.ErpNextApiUtil erpNextApiUtil,
            CompanyService companyService) {
        this.employeService = employeService;
        this.salaryComponentService = salaryComponentService;
        this.salaryStructureService = salaryStructureService;
        this.salaryStructureAssService = salaryStructureAssService;
        this.payrollService = payrollService;
        this.erpNextApiUtil = erpNextApiUtil;
        this.companyService = companyService;
        this.inputDateFormat.setLenient(false);
    }

    @Transactional
    public Map<String, Object> importFiles(MultipartFile employeeFile, MultipartFile salaryFile, MultipartFile assignmentFile) {
        try {
            // 1. Valider toutes les données avant l'insertion
            validateAllData(employeeFile, salaryFile, assignmentFile);

            // 2. Si la validation est OK, procéder à l'importation
            java.util.List<com.erpnext.dto.EmployeeDTO> employees = importEmployees(employeeFile);
            Map<String, String> refToEmployeeId = new HashMap<>();
            for (com.erpnext.dto.EmployeeDTO emp : employees) {
                refToEmployeeId.put(emp.getEmployeeNumber(), emp.getId());
            }
            importSalaryStructures(salaryFile);
            importAssignmentsAndPayroll(assignmentFile, refToEmployeeId);

            return Map.of(
                "success", true,
                "message", "Import réussi",
                "employeeMapping", refToEmployeeId
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'import: " + e.getMessage());
        }
    }

    public void validateAllData(MultipartFile employeeFile, MultipartFile salaryFile, MultipartFile assignmentFile) throws Exception {
        // Valider le fichier des employés
        validateEmployeeFile(employeeFile);
        
        // Valider le fichier des structures salariales
        validateSalaryStructureFile(salaryFile);
        
        // Valider le fichier des assignations
        validateAssignmentFile(assignmentFile);
    }

    public void validateEmployeeFile(MultipartFile file) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        
        // Skip header
        String line = reader.readLine();
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            try {
                String[] values = line.split(",");
                if (values.length < 6) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Nombre de colonnes insuffisant");
                }

                // Valider la référence
                if (values[0].trim().isEmpty()) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": La référence employé ne peut pas être vide");
                }

                // Valider le nom et prénom
                if (values[1].trim().isEmpty() || values[2].trim().isEmpty()) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Le nom et le prénom sont requis");
                }

                // Valider le genre
                validateGender(values[3]);

                // Valider les dates
                validateDate(values[4], "date d'embauche");
                validateDate(values[5], "date de naissance");

            } catch (Exception e) {
                throw new Exception("Erreur dans le fichier employé, ligne " + lineNumber + ": " + e.getMessage());
            }
        }
    }

    public void validateSalaryStructureFile(MultipartFile file) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        
        // Skip header
        String line = reader.readLine();
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            try {
                String[] values = line.split(",");
                if (values.length < 5) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Nombre de colonnes insuffisant");
                }

                // Valider le nom de la structure
                if (values[0].trim().isEmpty()) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Le nom de la structure ne peut pas être vide");
                }

                // Valider le nom et l'abréviation du composant
                if (values[1].trim().isEmpty() || values[2].trim().isEmpty()) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Le nom et l'abréviation du composant sont requis");
                }

                // Valider le type
                validateComponentType(values[3]);

                // Valider la formule si présente
                if (!values[4].trim().isEmpty()) {
                    // Vérifier que la formule ne contient que des caractères valides
                    if (!values[4].matches("^[\\w\\s+\\-*/(). ]+$")) {
                        throw new IllegalArgumentException("Ligne " + lineNumber + ": La formule contient des caractères invalides");
                    }
                }

            } catch (Exception e) {
                throw new Exception("Erreur dans le fichier des structures salariales, ligne " + lineNumber + ": " + e.getMessage());
            }
        }
    }

    public void validateAssignmentFile(MultipartFile file) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        
        // Skip header
        String line = reader.readLine();
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            try {
                String[] values = line.split(",");
                if (values.length < 4) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Nombre de colonnes insuffisant");
                }

                // Valider la date
                validateDate(values[0], "date d'assignation");

                // Valider la référence employé
                if (values[1].trim().isEmpty()) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": La référence employé ne peut pas être vide");
                }

                // Valider le montant de base
                try {
                    double base = Double.parseDouble(values[2]);
                    validatePositiveNumber(base, "montant de base");
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Le montant de base doit être un nombre valide");
                }

                // Valider le nom de la structure
                if (values[3].trim().isEmpty()) {
                    throw new IllegalArgumentException("Ligne " + lineNumber + ": Le nom de la structure ne peut pas être vide");
                }

            } catch (Exception e) {
                throw new Exception("Erreur dans le fichier des assignations, ligne " + lineNumber + ": " + e.getMessage());
            }
        }
    }

    public java.util.List<com.erpnext.dto.EmployeeDTO> importEmployees(MultipartFile file) throws Exception {
        java.util.List<com.erpnext.dto.EmployeeDTO> employees = new java.util.ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        // Skip header
        String line = reader.readLine();
        int lineNumber = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            try {
                String[] values = line.split(",");
                if (values.length < 6) {
                    throw new IllegalArgumentException("Nombre de colonnes insuffisant");
                }
                // Validation des champs obligatoires
                String firstNameRaw = values[2];
                String dateOfBirthRaw = values[5];
                String firstName = firstNameRaw == null ? "" : firstNameRaw.trim();
                String dateOfBirth = dateOfBirthRaw == null ? "" : dateOfBirthRaw.trim();
                if (firstName.isEmpty() || dateOfBirth.isEmpty()) {
                    logger.error("[Import] Ligne " + lineNumber + ": Champ obligatoire manquant (Prénom ou Date de naissance). Ligne ignorée.");
                    logger.error("  -> firstName='" + firstNameRaw + "', dateOfBirth='" + dateOfBirthRaw + "'");
                    continue;
                }
                com.erpnext.dto.EmployeeDTO employee = new com.erpnext.dto.EmployeeDTO();
                employee.setEmployeeNumber(values[0]); // Ref
                employee.setLastName(values[1].trim()); // Nom
                employee.setFirstName(firstName); // Prenom
                employee.setEmployeeName(firstName + " " + values[1].trim()); // Prénom Nom
                employee.setGender(validateGender(values[3]));
                employee.setDateOfJoining(validateDate(values[4], "date d'embauche"));
                employee.setDateOfBirth(convertDateToIso(validateDate(dateOfBirth, "date de naissance")));
                String companyName = values.length > 6 ? values[6].trim() : "My Company";
                companyService.ensureCompanyExists(companyName);
                employee.setCompany(companyName);
                employee.setStatus("Active");

                // DEBUG : Affiche la date de naissance et le JSON envoyé
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String jsonDebug = mapper.writeValueAsString(employee);
                    logger.info("[Import][DEBUG] Ligne " + lineNumber + ": JSON envoyé à ERPNext : " + jsonDebug);
                } catch (Exception ex) {
                    logger.error("[Import][DEBUG] Erreur lors de la sérialisation JSON: " + ex.getMessage());
                }
                logger.info("[Import][DEBUG] Ligne " + lineNumber + ": Date de naissance envoyée (ISO): '" + employee.getDateOfBirth() + "'");

                // Vérifier si l'employé existe déjà (par numéro)
                com.erpnext.dto.EmployeeDTO existingEmployee = employeService.getEmployeeByNumber(employee.getEmployeeNumber());
                if (existingEmployee != null) {
                    logger.info("[Import][INFO] Employé déjà existant (employee_number=" + employee.getEmployeeNumber() + "), insertion ignorée.");
                } else {
                    employeService.createEmployee(employee); // Persiste dans la base/ERP
                    logger.info("[Import][INFO] Employé inséré (employee_number=" + employee.getEmployeeNumber() + ")");
                }
                employees.add(employee);

            } catch (Exception e) {
                logger.error("Erreur à la ligne " + lineNumber + ": " + e.getMessage());
                logger.error("Contenu de la ligne: " + line);
                throw new Exception("Erreur à la ligne " + lineNumber + ": " + e.getMessage());
            }
        }
        return employees;
    }

    // Convertit une date au format DD/MM/YYYY en YYYY-MM-DD (format ISO attendu par ERPNext)
    public String convertDateToIso(String dateFr) {
        if (dateFr == null) return "";
        String[] parts = dateFr.trim().split("/");
        if (parts.length == 3) {
            return parts[2] + "-" + String.format("%02d", Integer.parseInt(parts[1])) + "-" + String.format("%02d", Integer.parseInt(parts[0]));
        }
        return dateFr; // fallback
    }

    public void importSalaryStructures(MultipartFile file) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        
        // Skip header
        String line = reader.readLine();
        int lineNumber = 1;
        
        // Map to store structures and their components
        Map<String, List<Map<String, Object>>> structureComponents = new HashMap<>();
        // Set to keep track of existing components processed
        Set<String> existingComponents = new HashSet<>();

        // 1. Create all salary components
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            try {
                String[] values = line.split(",");
                if (values.length < 5) {
                    throw new IllegalArgumentException("Nombre de colonnes insuffisant");
                }

                String structureName = values[0].trim();
                String componentName = values[1].trim();
                String componentAbbr = values[2].trim();
                String type = validateComponentType(values[3].trim());
                String formula = values[4].trim();
                String companyName = values.length > 5 ? values[5].trim() : "My Company";
                logger.info("[DEBUG] Champ valeur (formula): " + formula);
                
                if (structureName.isEmpty()) {
                    throw new IllegalArgumentException("Le nom de la structure salariale ne peut pas être vide");
                }

                if (componentName.isEmpty()) {
                    throw new IllegalArgumentException("Le nom du composant salarial ne peut pas être vide");
                }

                Map<String, Object> componentForStructure = new HashMap<>();

                if (!existingComponents.contains(componentName)) {
                    // Check if component exists in ERPNext
                    Map<String, Object> existing = salaryComponentService.getSalaryComponent(componentName);
                    boolean componentExists = (existing != null && existing.containsKey("data"));
                    if (!componentExists) {
                        // Create the component
                        Map<String, Object> componentData = new HashMap<>();
                        componentData.put("doctype", "Salary Component");
                        componentData.put("salary_component", componentName);
                        componentData.put("salary_component_abbr", componentAbbr);
                        componentData.put("type", type);
                        if (!formula.isEmpty()) {
                            componentData.put("formula", formula);
                        }
                        componentData.put("amount_based_on_formula", 1);
                        componentData.put("depends_on_payment_days", 0);
                        companyService.ensureCompanyExists(companyName);
                        componentData.put("company", companyName);

                        Map<String, Object> response = salaryComponentService.insertSalaryComponent(componentData);
                        logger.info("[DEBUG] Réponse API composant: " + response);
                        
                        if (response == null) {
                            logger.error("[Import][ERROR] Échec création composant '" + componentName + "' : réponse nulle. Le composant ne sera pas créé.");
                            throw new Exception("Échec création composant '" + componentName + "' : réponse nulle de l'API.");
                        } else if (response.containsKey("data") && response.get("data") instanceof Map && ((Map<?,?>)response.get("data")).containsKey("name")) {
                            logger.info("[Import][DEBUG] Composant '" + componentName + "' créé/confirmé: " + componentData.get("salary_component"));
                            // Log server messages (warnings, etc.)
                            if (response.containsKey("_server_messages")) {
                                logger.warn("[Import][WARN] Message serveur lors création composant '" + componentName + "': " + response.get("_server_messages"));
                            }
                        } else {
                            // If no "data.name", it's an error
                            String errorMessage = "Erreur création composant '" + componentName + "'. Réponse API: " + response;
                            if (response.containsKey("message") && response.get("message") != null) {
                                errorMessage = "Erreur création composant '" + componentName + "': " + response.get("message");
                            } else if (response.containsKey("exception") && response.get("exception") != null) {
                                errorMessage = "Erreur création composant '" + componentName + "': " + response.get("exception");
                            } else if (response.containsKey("_error_message") && response.get("_error_message") != null) {
                                errorMessage = "Erreur création composant '" + componentName + "': " + response.get("_error_message");
                            }
                            logger.error(errorMessage);
                            throw new Exception(errorMessage);
                        }
                    } else {
                        logger.info("[Import][INFO] Composant '" + componentName + "' déjà existant, insertion ignorée.");
                    }
                    existingComponents.add(componentName);
                }

                // Prepare component for structure grouping
                componentForStructure.put("salary_component", componentName);
                componentForStructure.put("amount_based_on_formula", 1);
                componentForStructure.put("type", type);
                if (!formula.isEmpty()) {
                    componentForStructure.put("formula", formula);
                }
                
                structureComponents.computeIfAbsent(structureName + "||" + companyName, k -> new ArrayList<>())
                    .add(componentForStructure);

            } catch (Exception e) {
                logger.error("Erreur à la ligne " + lineNumber + ": " + e.getMessage());
                logger.error("Contenu de la ligne: " + line);
                throw new Exception("Erreur à la ligne " + lineNumber + ": " + e.getMessage());
            }
        }

        // 2. Create and submit salary structures
        for (Map.Entry<String, List<Map<String, Object>>> entry : structureComponents.entrySet()) {
            String[] keyParts = entry.getKey().split("\\|\\|");
            String structureName = keyParts[0];
            String companyName = keyParts.length > 1 ? keyParts[1] : "My Company";
            companyService.ensureCompanyExists(companyName);
            Map<String, Object> structureData = new HashMap<>();
            structureData.put("doctype", "Salary Structure");
            structureData.put("name", structureName);
            structureData.put("company", companyName);
            structureData.put("is_active", "Yes");
            structureData.put("payroll_frequency", "Monthly");
            structureData.put("currency", "ARS");
            structureData.put("salary_slip_based_on_timesheet", 0);

            List<Map<String, Object>> earnings = new ArrayList<>();
            List<Map<String, Object>> deductions = new ArrayList<>();

            for (Map<String, Object> component : entry.getValue()) {
                Map<String, Object> componentEntry = new HashMap<>();
                componentEntry.put("salary_component", component.get("salary_component"));
                componentEntry.put("amount_based_on_formula", 1);
                if (component.containsKey("formula")) {
                    componentEntry.put("formula", component.get("formula"));
                }

                if ("Earning".equalsIgnoreCase((String)component.get("type"))) {
                    earnings.add(componentEntry);
                } else if ("Deduction".equalsIgnoreCase((String)component.get("type"))) {
                    deductions.add(componentEntry);
                }
            }

            structureData.put("earnings", earnings);
            structureData.put("deductions", deductions);

            // Check if structure already exists
            Map<String, Object> existingStructure = salaryStructureService.getSalaryStructure(structureName);
            logger.info("[Import][DEBUG] Vérification existence structure salariale '" + structureName + "': " + existingStructure);
            boolean structureExists = false;
            if (existingStructure != null && existingStructure.containsKey("data")) {
                Object dataObj = existingStructure.get("data");
                if (dataObj instanceof List) {
                    List<?> structureList = (List<?>) dataObj;
                    if (!structureList.isEmpty()) {
                        Object first = structureList.get(0);
                        if (first instanceof Map) {
                            Object nameObj = ((Map<?, ?>) first).get("name");
                            if (structureName.equals(nameObj)) {
                                structureExists = true;
                            }
                        }
                    }
                }
            }
            if (!structureExists) {
                logger.info("[Import][DEBUG] Structure salariale '" + structureName + "' absente, création en cours...");
                Map<String, Object> response = salaryStructureService.insertSalaryStructure(structureData);
                logger.info("[DEBUG] Réponse API structure: " + response);
                if (response == null) {
                    logger.error("[Import][WARN] Structure '" + structureName + "' création impossible (réponse nulle). Ignorée.");
                    continue;
                } else if (!(response.containsKey("data") && ((Map)response.get("data")).containsKey("name"))) {
                    throw new Exception("Erreur création structure '" + structureName + "': " + response);
                } else {
                    logger.info("[Import][INFO] Structure créée: " + structureData);
                    // Submit the structure automatically
                    Map<String, Object> submitResponse = salaryStructureService.submitSalaryStructure(structureName);
                    logger.info("[Import][INFO] Structure '" + structureName + "' soumise: " + submitResponse);
                    // Check submission success by docstatus
                    boolean submitOk = false;
                    if (submitResponse != null && submitResponse.containsKey("data")) {
                        Object dataObj = submitResponse.get("data");
                        if (dataObj instanceof Map) {
                            Object docstatus = ((Map<?, ?>) dataObj).get("docstatus");
                            submitOk = (docstatus != null && (docstatus.equals(1) || "1".equals(docstatus.toString())));
                        }
                    }
                    if (!submitOk) {
                        logger.error("[Import][ERROR] Soumission de la structure '" + structureName + "' a échoué: " + submitResponse);
                        throw new Exception("Erreur lors de la soumission de la structure " + structureName);
                    }
                    // Confirm final status by re-fetching
                    Map<String, Object> checkAfterCreate = salaryStructureService.getSalaryStructure(structureName);
                    String finalStatus = "UNKNOWN";
                    if (checkAfterCreate != null && checkAfterCreate.containsKey("data")) {
                        Object dataObj = checkAfterCreate.get("data");
                        if (dataObj instanceof List && !((List<?>)dataObj).isEmpty()) {
                            Map<?,?> structMap = (Map<?,?>) ((List<?>)dataObj).get(0);
                            Object docstatus = structMap.get("docstatus");
                            if (docstatus != null) {
                                switch (docstatus.toString()) {
                                    case "1": finalStatus = "Submitted"; break;
                                    case "0": finalStatus = "Draft"; break;
                                    case "2": finalStatus = "Cancelled"; break;
                                    default: finalStatus = "Unknown(" + docstatus + ")";
                                }
                            }
                        }
                    }
                    logger.info("[Import][INFO] Statut final de la structure salariale '" + structureName + "' : " + finalStatus);
                }
            } else {
                logger.info("[Import][INFO] Structure '" + structureName + "' déjà existante, insertion ignorée.");
            }
        }
    }

public void importAssignmentsAndPayroll(MultipartFile file, Map<String, String> refToEmployeeId) throws Exception {
    BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
    
    // Skip header
    String line = reader.readLine();
    int lineNumber = 1;

    Map<String, List<Map<String, Object>>> entriesByMonth = new HashMap<>();
    
    while ((line = reader.readLine()) != null) {
        logger.info("[DEBUG] Traitement CSV3 ligne " + lineNumber + " : " + line);
        lineNumber++;
        try {
            String[] values = line.split(",");
            if (values.length < 4) {
                logger.error("[Import][ERROR] Nombre de colonnes insuffisant à la ligne " + lineNumber);
                throw new IllegalArgumentException("Nombre de colonnes insuffisant");
            }

            String monthStr = values[0].trim(); // Format: dd/MM/yyyy
            String employeeRef = values[1].trim();
            String base = values[2].trim();
            String structureName = values[3].trim();

            // Verify employee existence
            String employeeId = refToEmployeeId.get(employeeRef);
            if (employeeId == null) {
                logger.error("[Import][ERROR] Référence employé non trouvée: " + employeeRef + " à la ligne " + lineNumber);
                throw new IllegalArgumentException("Référence employé non trouvée: " + employeeRef);
            }

            // Convert and get last day of month
            LocalDate fromDate = LocalDate.parse(monthStr, dateFormatter);
            YearMonth yearMonth = YearMonth.from(fromDate);
            LocalDate endDate = yearMonth.atEndOfMonth();

            double baseValue = Double.parseDouble(base);
            validatePositiveNumber(baseValue, "base");

            // Check if salary structure exists and submit if draft
            Map<String, Object> structureDetails = erpNextApiUtil.fetchData(
                erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Structure/" + structureName);
            String structureStatus = "";
            if (structureDetails != null && structureDetails.containsKey("data")) {
                Object dataDetails = structureDetails.get("data");
                if (dataDetails instanceof Map && ((Map<?, ?>)dataDetails).get("status") != null) {
                    structureStatus = ((Map<?, ?>)dataDetails).get("status").toString();
                }
            }
            if (structureStatus.equalsIgnoreCase("Draft")) {
                logger.info("[Import][DEBUG] Soumission automatique de la Salary Structure '" + structureName + "' (était Draft)");
                Map<String, Object> submitResponse = salaryStructureService.submitSalaryStructure(structureName);
                logger.info("[Import][DEBUG] Réponse de soumission de la Salary Structure '" + structureName + "': " + submitResponse);
            }

            Map<String, Object> assignmentData = new HashMap<>();
            assignmentData.put("employee", employeeId);
            assignmentData.put("salary_structure", structureName);
            assignmentData.put("from_date", fromDate.format(DateTimeFormatter.ISO_DATE));
            assignmentData.put("base", baseValue);
            String companyName = values.length > 4 ? values[4].trim() : "My Company";
            companyService.ensureCompanyExists(companyName);
            assignmentData.put("company", companyName);

            logger.info("[Import][DEBUG] Assignation de structure salariale: " + assignmentData);

            // Pre-check existing assignment
            Map<String, String> filters = new HashMap<>();
            filters.put("employee", employeeId);
            filters.put("salary_structure", structureName);
            filters.put("from_date", fromDate.format(DateTimeFormatter.ISO_DATE));
            String[] fields = new String[] {"name"};
            Map<String, Object> existingAssignmentResp = erpNextApiUtil.fetchDataWithFilters(
                "Salary Structure Assignment", fields, filters
            );

            boolean shouldCreate = true;
            if (existingAssignmentResp != null && existingAssignmentResp.containsKey("data")) {
                Object dataObj = existingAssignmentResp.get("data");
                if (dataObj instanceof java.util.List && !((java.util.List<?>) dataObj).isEmpty()) {
                    Map<?, ?> existing = (Map<?, ?>) ((java.util.List<?>) dataObj).get(0);
                    String assignmentName = existing.get("name").toString();

                    // GET details to check status
                    String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Structure Assignment/" + assignmentName;
                    Map<String, Object> details = erpNextApiUtil.fetchData(url);
                    String status = "";
                    if (details != null && details.containsKey("data")) {
                        Object dataDetails = details.get("data");
                        if (dataDetails instanceof Map && ((Map<?,?>)dataDetails).get("status") != null) {
                            status = ((Map<?,?>)dataDetails).get("status").toString();
                        }
                    }
                    if ("Draft".equalsIgnoreCase(status)) {
                        // Update the draft assignment
                        Map<String, Object> updateResp = erpNextApiUtil.putData(url, assignmentData);
                        logger.info("[Import][DEBUG] Mise à jour assignation existante (Draft): " + updateResp);
                        shouldCreate = false;
                    } else {
                        logger.info("[Import][DEBUG] Assignation existante non Draft, création ignorée (" + status + ")");
                        shouldCreate = false;
                        continue;
                    }
                }
            }

            Map<String, Object> assignmentResponse = null;
            if (shouldCreate) {
                assignmentResponse = salaryStructureAssService.createSalaryStructureAssignment(assignmentData);
            }

            if (assignmentResponse == null) {
                logger.error("[Import][CSV3][ERREUR] L'appel API assignation a échoué (réponse nulle) pour EmployéRef="
                        + employeeRef + ", Structure=" + structureName);
                throw new Exception("L'appel API assignation a échoué (réponse nulle) pour EmployéRef="
                        + employeeRef + ", Structure=" + structureName
                        + ". Vérifiez l'existence de la structure salariale et la connexion à ERPNext.");
            }

            Object exc = assignmentResponse.get("exception");
            Object excType = assignmentResponse.get("_error_message");
            String excStr = exc != null ? exc.toString() : (excType != null ? excType.toString() : "");
            if (excStr.contains("DuplicateAssignment")) {
                logger.warn("[Import][CSV3][DUPLICATE] Assignation déjà existante pour EmployéRef="
                        + employeeRef + ", Structure=" + structureName + " (doublon ignoré)");
                continue;
            }

            logger.info("[DEBUG] Réponse API assignation: " + assignmentResponse);
            Object successObj = assignmentResponse.get("success");
            String assignmentName = null;
            Map<String, Object> dataMap = null;
            if (assignmentResponse.get("data") instanceof Map) {
                dataMap = (Map<String, Object>) assignmentResponse.get("data");
                assignmentName = (String) dataMap.get("name");
            }
            if (successObj != null) {
                if (!(Boolean.TRUE.equals(successObj))) {
                    logger.error("[Import][CSV3][ERREUR] Assignation échouée pour EmployéRef=" + employeeRef
                            + ", Structure=" + structureName + ": " + assignmentResponse.get("message"));
                    throw new Exception((String) assignmentResponse.get("message"));
                } else {
                    logger.info("[Import][CSV3] Assignation réussie pour EmployéRef=" + employeeRef
                            + ", Structure=" + structureName);
                }
            } else if (assignmentResponse.get("data") != null) {
                logger.info("[Import][CSV3] Assignation réussie (data présent) pour EmployéRef=" + employeeRef
                        + ", Structure=" + structureName);
            } else {
                logger.error("[Import][CSV3][ERREUR] Assignation échouée (ni success ni data) pour EmployéRef="
                        + employeeRef + ", Structure=" + structureName);
                throw new Exception("Réponse API assignation inattendue : " + assignmentResponse);
            }

            // Auto-submit the assignment if its docstatus is 0 (Draft)
            if (assignmentName != null && !assignmentName.isEmpty() && dataMap != null && dataMap.containsKey("docstatus")) {
                Object docStatusObj = dataMap.get("docstatus");
                boolean isDraft = false;
                if (docStatusObj instanceof Integer && ((Integer) docStatusObj) == 0) {
                    isDraft = true;
                } else if (docStatusObj instanceof String && "0".equals(docStatusObj.toString())) {
                    isDraft = true;
                } else if (docStatusObj instanceof Double && ((Double) docStatusObj).intValue() == 0) {
                    isDraft = true;
                }

                if (isDraft) {
                    logger.info("[Import][CSV3] Assignation '" + assignmentName + "' créée avec docstatus=0 (Draft). Tentative de soumission...");
                    try {
                        String urlForSubmission = erpNextApiUtil.getErpnextUrl() + "/api/resource/Salary Structure Assignment/" + assignmentName;
                        Map<String, Object> submitResponse = salaryStructureAssService.submitSalaryStructureAssignment(urlForSubmission);
                        logger.info("[Import][CSV3] Soumission automatique de l'assignation '" + assignmentName + "' : " + submitResponse);
                        if (submitResponse.containsKey("data")) {
                            Map<String, Object> submittedData = (Map<String, Object>) submitResponse.get("data");
                            if (submittedData.containsKey("docstatus")) {
                                logger.info("[Import][CSV3] Docstatus de l'assignation '" + assignmentName + "' après soumission: " + submittedData.get("docstatus"));
                            }
                        }
                    } catch (Exception submitEx) {
                        logger.error("[Import][CSV3][ERREUR] Soumission de l'assignation '" + assignmentName + "' échouée : " + submitEx.getMessage(), submitEx);
                    }
                } else {
                    logger.info("[Import][CSV3] Assignation '" + assignmentName + "' créée avec docstatus=" + docStatusObj + ". Pas de soumission automatique nécessaire.");
                }
            } else if (assignmentName != null && !assignmentName.isEmpty()) {
                logger.warn("[Import][CSV3] Impossible de déterminer le docstatus pour l'assignation '" + assignmentName + "' à partir de la réponse de création. La soumission automatique est ignorée.");
            }

            // Gather payroll info
            Map<String, Object> payrollEntry = new HashMap<>();
            payrollEntry.put("employee", employeeId);
            payrollEntry.put("employeeName", employeeId); // Using employee ID temporarily

            // Group by month for payroll creation
            entriesByMonth.computeIfAbsent(monthStr, k -> new ArrayList<>()).add(payrollEntry);
        } catch (IllegalArgumentException e) {
            logger.error("[Import][CSV3][ERREUR_LIGNE] Erreur de traitement à la ligne " + (lineNumber -1) + " (contenu: '" + line + "'): " + e.getMessage());
            // Poursuivre avec la ligne suivante, ou ajouter une gestion d'erreur plus robuste si nécessaire
        } catch (Exception e) {
            logger.error("[Import][CSV3][ERREUR_LIGNE_INATTENDUE] Erreur inattendue à la ligne " + (lineNumber-1) + " (contenu: '" + line + "'): " + e.getMessage(), e);
            // Poursuivre avec la ligne suivante, ou ajouter une gestion d'erreur plus robuste si nécessaire
        }
        }

        // Create payroll slips per month, per employee
        for (Map.Entry<String, List<Map<String, Object>>> monthEntry : entriesByMonth.entrySet()) {
            String monthStr = monthEntry.getKey();
            List<Map<String, Object>> employeesInMonth = monthEntry.getValue();

            LocalDate startDate = LocalDate.parse(monthStr, dateFormatter);
            YearMonth yearMonth = YearMonth.from(startDate);
            LocalDate endDate = yearMonth.atEndOfMonth();

            for (Map<String, Object> employeeMap : employeesInMonth) {
                String employeeId = (String) employeeMap.get("employee"); // Ou "employeeId" selon la structure exacte
                if (employeeId == null) {
                    logger.warn("[Import][CSV3] employeeId manquant dans employeeMap pour le mois " + monthStr + ", map: " + employeeMap);
                    continue;
                }
                String companyName = employeeMap.containsKey("company") ? (String) employeeMap.get("company") : "My Company";
                Map<String, Object> payrollDataForOneEmployee = new HashMap<>();
                payrollDataForOneEmployee.put("company", companyName);
                payrollDataForOneEmployee.put("posting_date", startDate.format(DateTimeFormatter.ISO_DATE));
                payrollDataForOneEmployee.put("payroll_frequency", "Monthly");
                payrollDataForOneEmployee.put("start_date", startDate.format(DateTimeFormatter.ISO_DATE));
                payrollDataForOneEmployee.put("end_date", endDate.format(DateTimeFormatter.ISO_DATE));
                payrollDataForOneEmployee.put("employee", employeeId);
                // La liste "employees" ne contient maintenant qu'un seul employé, avec seulement l'ID de l'employé
                List<Map<String, Object>> singleEmployeeList = new ArrayList<>();
                Map<String, Object> slipEmployeeDetail = new HashMap<>();
                slipEmployeeDetail.put("employee", employeeId); // employeeId est déjà correctement extrait
                singleEmployeeList.add(slipEmployeeDetail);
                payrollDataForOneEmployee.put("employees", singleEmployeeList);
                payrollDataForOneEmployee.put("exchange_rate", 1.0);

                logger.info("[Import][CSV3] Tentative de création de fiche de paie pour l'employé " + employeeId + " pour le mois " + monthStr + ": " + payrollDataForOneEmployee);

                Map<String, Object> existingSlip = payrollService.getSalarySlip(payrollDataForOneEmployee);
                boolean slipExists = (existingSlip != null && existingSlip.containsKey("data") && 
                                      existingSlip.get("data") instanceof List && !((List<?>)existingSlip.get("data")).isEmpty());

                if (!slipExists) {
                    Map<String, Object> payrollResponse = payrollService.insertPayroll(payrollDataForOneEmployee);
                    logger.info("[Import][CSV3] Réponse création fiche de paie pour employé " + employeeId + " mois " + monthStr + ": " + payrollResponse);

                    String payrollName = null;
                    boolean isDraft = false;
                    boolean creationConsideredSuccess = false;

                    if (payrollResponse != null) {
                        // Check for explicit success flag from wrapper/util if any
                        if (Boolean.TRUE.equals(payrollResponse.get("success"))) {
                            creationConsideredSuccess = true;
                        }

                        // Independently, check ERPNext's typical success response structure (document in data)
                        if (payrollResponse.get("data") instanceof Map) {
                            Map<?, ?> dataMap = (Map<?, ?>) payrollResponse.get("data");
                            if (dataMap.containsKey("name") && dataMap.get("name") != null) {
                                payrollName = (String) dataMap.get("name");
                                Object docStatusObj = dataMap.get("docstatus");
                                // Check for various ways docstatus=0 (Draft) might be represented
                                if (docStatusObj instanceof Integer && ((Integer) docStatusObj) == 0) {
                                    isDraft = true;
                                } else if (docStatusObj instanceof String && "0".equals(docStatusObj.toString())) {
                                    isDraft = true;
                                } else if (docStatusObj instanceof Double && ((Double) docStatusObj).intValue() == 0) {
                                    isDraft = true;
                                } else if (docStatusObj != null) {
                                    logger.warn("[Import][CSV3] Docstatus pour fiche '" + payrollName + "' est '" + docStatusObj + "' (type: " + docStatusObj.getClass().getSimpleName() + "), pas considéré comme Draft pour soumission auto.");
                                }

                                // If we got a document name and it's a draft, consider creation successful for our purposes
                                if (payrollName != null && isDraft) {
                                    creationConsideredSuccess = true;
                                }
                            } else {
                                logger.warn("[Import][CSV3] Réponse de création de fiche de paie reçue, mais 'data.name' est manquant. Response: " + payrollResponse);
                            }
                        } else if (!creationConsideredSuccess) { // Only log if not already deemed success by 'success:true'
                           logger.warn("[Import][CSV3] Réponse de création de fiche de paie reçue, mais 'data' n'est pas une Map ou est manquante. Response: " + payrollResponse);
                        }
                    } else {
                        logger.error("[Import][CSV3][ERREUR] Réponse NULLE lors de la tentative de création de la fiche de paie pour employé " + employeeId + " mois " + monthStr);
                    }

                    if (!creationConsideredSuccess) {
                        String errorMessage = (payrollResponse != null && payrollResponse.get("message") != null) ? String.valueOf(payrollResponse.get("message")) : "Détails non fournis ou réponse nulle.";
                        logger.error("[Import][CSV3][ERREUR] Création fiche de paie interprétée comme ÉCHOUÉE pour employé " + employeeId + " mois " + monthStr + ". Message: " + errorMessage + ". Full Response: " + payrollResponse);
                    } else {
                        logger.info("[Import][CSV3] Fiche de paie (Name: " + payrollName + ", IsDraft: " + isDraft + ") traitée avec succès pour employé " + employeeId + " mois " + monthStr);
                        if (isDraft && payrollName != null) {
                            try {
                                logger.info("[Import][CSV3] Fiche de paie '" + payrollName + "' est en Draft. Tentative de soumission...");
                                Map<String, Object> submitResponse = payrollService.submitPayroll(payrollName);
                                logger.info("[Import][CSV3] Réponse de soumission automatique pour la fiche de paie '" + payrollName + "' : " + submitResponse);
                            } catch (Exception submitEx) {
                                logger.error("[Import][CSV3][ERREUR] Exception lors de la tentative de soumission de la fiche de paie '" + payrollName + "' : " + submitEx.getMessage(), submitEx);
                            }
                        } else if (payrollName == null) {
                            logger.error("[Import][CSV3][ERREUR_CRITIQUE] Création considérée comme un succès MAIS payrollName est null. Impossible de soumettre. Response: " + payrollResponse);
                        } else { // Not a draft, or name missing
                            logger.info("[Import][CSV3] Fiche de paie '" + payrollName + "' n'est pas en Draft (docstatus non égal à 0 ou nom manquant), soumission automatique ignorée.");
                        }
                    }
                } else {
                    logger.info("[Import][INFO] Bulletin déjà existant pour l'employé " + employeeId + " pour le mois " + monthStr + ", insertion ignorée.");
                }
            }
        }
    }

    /**
     * Valide et convertit le genre
     */
    public String validateGender(String gender) {
        if (gender.equalsIgnoreCase("Masculin") || gender.equalsIgnoreCase("M")) {
            return "Male";
        } else if (gender.equalsIgnoreCase("Feminin") || gender.equalsIgnoreCase("F")) {
            return "Female";
        } else {
            throw new IllegalArgumentException(
                String.format("Genre invalide: '%s'. Valeurs acceptées: Masculin, M, Feminin, F", gender)
            );
        }
    }

    /**
     * Valide et convertit une date du format dd/MM/yyyy vers yyyy-MM-dd
     * Vérifie aussi si la date est dans une plage raisonnable
     */
    public String validateDate(String date, String fieldName) throws Exception {
        try {
            // Convertir la date
            Date parsedDate = inputDateFormat.parse(date.trim());
            
            // Vérifier si la date est dans une plage raisonnable (entre 1950 et 2100)
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);
            
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1; // Les mois commencent à 0
            
            if (year < 1950 || year > 2100) {
                throw new IllegalArgumentException(
                    String.format("Année invalide pour %s: %d. L'année doit être entre 1950 et 2100", fieldName, year)
                );
            }
            
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException(
                    String.format("Mois invalide pour %s: %d. Le mois doit être entre 1 et 12", fieldName, month)
                );
            }
            
            // Vérifier si le jour est valide pour ce mois
            int day = cal.get(Calendar.DAY_OF_MONTH);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            if (day < 1 || day > maxDay) {
                throw new IllegalArgumentException(
                    String.format("Jour invalide pour %s: %d. Le jour doit être entre 1 et %d pour ce mois", fieldName, day, maxDay)
                );
            }
            
            return outputDateFormat.format(parsedDate);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Format de date invalide pour %s: '%s'. Format attendu: JJ/MM/AAAA", fieldName, date)
            );
        }
    }

    public void validatePositiveNumber(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                String.format("La valeur pour %s doit être positive. Valeur reçue: %f", fieldName, value)
            );
        }
    }

    /**
     * Valide et convertit le type de composant salarial
     */
    public String validateComponentType(String type) {
        if (type.equalsIgnoreCase("earning")) {
            return "Earning";
        } else if (type.equalsIgnoreCase("deduction")) {
            return "Deduction";
        } else {
            throw new IllegalArgumentException(
                String.format("Type de composant invalide: '%s'. Valeurs acceptées: earning, deduction", type)
            );
        }
    }
}
