package com.erpnext.controller.rest;

import com.erpnext.dto.EmployeeDTO;
import com.erpnext.service.rest.imports.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImportController.class);
    public final ImportService importService;

    /**
     * Wrapper pour utiliser un File comme MultipartFile sans dépendance de test.
     */
    public static class LocalMultipartFile implements MultipartFile {
        private final File file;
        private final String originalFilename;
        public LocalMultipartFile(File file) {
            this.file = file;
            this.originalFilename = file.getName();
        }
        @Override public String getName() { return originalFilename; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return "text/csv"; }
        @Override public boolean isEmpty() { return file.length() == 0; }
        @Override public long getSize() { return file.length(); }
        @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(file.toPath()); }
        @Override public java.io.InputStream getInputStream() throws IOException { return Files.newInputStream(file.toPath()); }
        @Override public void transferTo(File dest) throws IOException { Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING); }
    }


    @GetMapping
    public String showImportPage(Model model) {
        log.info("Affichage de la page d'importation");
        return "import/index";
    }

    @PostMapping("/upload")
    public String prepareImport(
            @RequestParam(value = "employeeFile", required = false) MultipartFile employeeFile,
            @RequestParam(value = "salaryStructureFile", required = false) MultipartFile salaryStructureFile,
            @RequestParam(value = "salaryMonthlyFile", required = false) MultipartFile salaryMonthlyFile,
            RedirectAttributes redirectAttributes) {

        log.info("Préparation de l'importation et comptage des lignes.");

        if ((employeeFile == null || employeeFile.isEmpty()) &&
                (salaryStructureFile == null || salaryStructureFile.isEmpty()) &&
                (salaryMonthlyFile == null || salaryMonthlyFile.isEmpty())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Aucun fichier n'a été sélectionné pour l'importation.");
            return "redirect:/import";
        }

        try {
            Map<String, String> tempFilePaths = new HashMap<>();
            Map<String, Long> lineCounts = new HashMap<>();
            Map<String, String> originalFileNames = new HashMap<>();
            String tempDir = System.getProperty("java.io.tmpdir") + "/erpnext-import/";
            Files.createDirectories(Paths.get(tempDir));

            if (employeeFile != null && !employeeFile.isEmpty()) {
                String tempFilePath = saveFile(employeeFile, tempDir);
                tempFilePaths.put("employeeFile", tempFilePath);
                lineCounts.put("employeeFile", countLines(employeeFile));
                originalFileNames.put("employeeFile", employeeFile.getOriginalFilename());
            }

            if (salaryStructureFile != null && !salaryStructureFile.isEmpty()) {
                String tempFilePath = saveFile(salaryStructureFile, tempDir);
                tempFilePaths.put("salaryStructureFile", tempFilePath);
                lineCounts.put("salaryStructureFile", countLines(salaryStructureFile));
                originalFileNames.put("salaryStructureFile", salaryStructureFile.getOriginalFilename());
            }

            if (salaryMonthlyFile != null && !salaryMonthlyFile.isEmpty()) {
                String tempFilePath = saveFile(salaryMonthlyFile, tempDir);
                tempFilePaths.put("salaryMonthlyFile", tempFilePath);
                lineCounts.put("salaryMonthlyFile", countLines(salaryMonthlyFile));
                originalFileNames.put("salaryMonthlyFile", salaryMonthlyFile.getOriginalFilename());
            }

            redirectAttributes.addFlashAttribute("tempFilePaths", tempFilePaths);
            redirectAttributes.addFlashAttribute("lineCounts", lineCounts);
            redirectAttributes.addFlashAttribute("originalFileNames", originalFileNames);

            return "redirect:/import/confirm";

        } catch (IOException e) {
            log.error("Erreur lors de la préparation de l'importation", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la préparation de l'importation : " + e.getMessage());
            return "redirect:/import";
        }
    }

    @GetMapping("/confirm")
    public String showConfirmPage(Model model) {
        if (!model.containsAttribute("tempFilePaths")) {
            return "redirect:/import";
        }
        log.info("Affichage de la page de confirmation d'importation");
        return "import/confirm";
    }

    @PostMapping("/execute")
    public String executeFileImport(@RequestParam Map<String, String> params, RedirectAttributes redirectAttributes) {
        log.info("Exécution de l'importation des fichiers");

        String employeeFilePath = params.get("employeeFile");
        String salaryStructureFilePath = params.get("salaryStructureFile");
        String salaryMonthlyFilePath = params.get("salaryMonthlyFile");

        StringBuilder overallSuccessMessage = new StringBuilder();
        StringBuilder overallErrorMessage = new StringBuilder();

        try {
            if (employeeFilePath != null && !employeeFilePath.isEmpty()) {
                try {
                    File file = new File(employeeFilePath);
                    MultipartFile mp = new LocalMultipartFile(file);
                    log.info("Importation du fichier employés: {}", file.getName());
                    List<EmployeeDTO> employees = importService.importEmployees(mp);
                    overallSuccessMessage.append(String.format("Employés importés (%d). ", employees.size()));
                    Files.deleteIfExists(file.toPath());
                } catch (Exception e) {
                    log.error("Erreur import employés {}: {}", employeeFilePath, e.getMessage());
                    overallErrorMessage.append(String.format("Erreur fichier employés (%s): %s. ", new File(employeeFilePath).getName(), e.getMessage()));
                }
            }

            if (salaryStructureFilePath != null && !salaryStructureFilePath.isEmpty()) {
                try {
                    File file = new File(salaryStructureFilePath);
                    MultipartFile mp = new LocalMultipartFile(file);
                    log.info("Importation du fichier structures de salaire: {}", file.getName());
                    importService.importSalaryStructures(mp);
                    overallSuccessMessage.append("Structures de salaire importées. ");
                    Files.deleteIfExists(file.toPath());
                } catch (Exception e) {
                    log.error("Erreur import structures {}: {}", salaryStructureFilePath, e.getMessage());
                    overallErrorMessage.append(String.format("Erreur fichier structures (%s): %s. ", new File(salaryStructureFilePath).getName(), e.getMessage()));
                }
            }

            if (salaryMonthlyFilePath != null && !salaryMonthlyFilePath.isEmpty()) {
                try {
                    File file = new File(salaryMonthlyFilePath);
                    MultipartFile mp = new LocalMultipartFile(file);
                    log.info("Importation du fichier bulletins de salaire: {}", file.getName());
                    List<EmployeeDTO> allEmployees = importService.getEmployeService().getAllEmploye();
                    Map<String, String> refToEmployeeId = new HashMap<>();
                    for (EmployeeDTO emp : allEmployees) {
                        refToEmployeeId.put(emp.getEmployeeNumber(), emp.getId());
                    }
                    importService.importAssignmentsAndPayroll(mp, refToEmployeeId);
                    overallSuccessMessage.append("Bulletins de salaire importés. ");
                    Files.deleteIfExists(file.toPath());
                } catch (Exception e) {
                    log.error("Erreur import bulletins {}: {}", salaryMonthlyFilePath, e.getMessage());
                    overallErrorMessage.append(String.format("Erreur fichier bulletins (%s): %s. ", new File(salaryMonthlyFilePath).getName(), e.getMessage()));
                }
            }

            if (overallSuccessMessage.length() > 0) {
                redirectAttributes.addFlashAttribute("successMessage", overallSuccessMessage.toString());
            }
            if (overallErrorMessage.length() > 0) {
                redirectAttributes.addFlashAttribute("errorMessage", overallErrorMessage.toString());
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'importation des fichiers: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de l'importation: " + e.getMessage());
        }

        return "redirect:/import";
    }

    private String saveFile(MultipartFile file, String dir) throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(dir + filename);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return path.toString();
    }

    private long countLines(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // soustraire 1 pour l'en-tête
            long count = reader.lines().count() - 1;
            return Math.max(0, count);
        }
    }
}
