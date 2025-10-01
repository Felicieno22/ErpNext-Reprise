package com.erpnext.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.erpnext.dto.EmployeeDTO;
import com.erpnext.dto.SalarySlipDTO;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Component
public class PdfGeneratorUtil {

    public static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
    public static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
    public static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
    public static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    public static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
    public static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);
    
    public static final BaseColor HEADER_COLOR = new BaseColor(52, 58, 64); // #343a40
    public static final BaseColor LIGHT_GRAY = new BaseColor(248, 249, 250); // #f8f9fa
    
    /**
     * Génère un PDF pour un bulletin de salaire (version simplifiée)
     * @param salarySlip Informations du bulletin de salaire
     * @return Tableau d'octets contenant le PDF généré
     * @throws IOException Si une erreur survient lors de la génération du PDF
     */
    public byte[] generateSalarySlipPdf(SalarySlipDTO salarySlip) throws IOException {
        // Créer un EmployeeDTO simplifié à partir des informations du bulletin
        EmployeeDTO employee = new EmployeeDTO();
        employee.setId(salarySlip.getEmployee());
        // employee.setEmployeeName(salarySlip.getEmployeeName()); // Field removed from SalarySlipDTO
        employee.setCompany(salarySlip.getCompany());
        
        // Générer le PDF en utilisant la méthode complète
        ByteArrayInputStream pdfStream = generateSalarySlipPdf(employee, salarySlip);
        
        // Convertir le ByteArrayInputStream en byte[]
        byte[] pdfBytes = new byte[pdfStream.available()];
        pdfStream.read(pdfBytes);
        return pdfBytes;
    }
    
    /**
     * Génère un PDF pour un bulletin de salaire
     * @param employee Informations de l'employé
     * @param salarySlip Informations du bulletin de salaire
     * @return Stream contenant le PDF généré
     * @throws IOException Si une erreur survient lors de la génération du PDF
     */
    public ByteArrayInputStream generateSalarySlipPdf(EmployeeDTO employee, SalarySlipDTO salarySlip) throws IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Titre du document
            addTitle(document, "BULLETIN DE SALAIRE");
            
            // Informations de l'entreprise
            addCompanyInfo(document, salarySlip.getCompany());
            
            // Informations de l'employé
            addEmployeeInfo(document, employee, salarySlip);
            
            // Détails du salaire
            addSalaryDetails(document, salarySlip);
            
            // Récapitulatif
            addSummary(document, salarySlip);
            
            document.close();
            
        } catch (DocumentException e) {
            throw new IOException("Erreur lors de la génération du PDF: " + e.getMessage());
        }
        
        return new ByteArrayInputStream(out.toByteArray());
    }
    
    public void addTitle(Document document, String title) throws DocumentException {
        Paragraph titleParagraph = new Paragraph(title, TITLE_FONT);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(20);
        document.add(titleParagraph);
    }
    
    public void addCompanyInfo(Document document, String company) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setPadding(10);
        
        Paragraph companyName = new Paragraph(company, SUBTITLE_FONT);
        companyName.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(companyName);
        
        table.addCell(cell);
        table.setSpacingAfter(20);
        
        document.add(table);
    }
    
    public void addEmployeeInfo(Document document, EmployeeDTO employee, SalarySlipDTO salarySlip) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        
        // Informations de l'employé
        addInfoCell(table, "Employé:", employee.getEmployeeName());
        addInfoCell(table, "ID Employé:", employee.getId());
        // addInfoCell(table, "Département:", employee.getDepartment()); // Field removed from EmployeeDTO
        // addInfoCell(table, "Poste:", employee.getDesignation()); // Field removed from EmployeeDTO
        addInfoCell(table, "Département:", "N/A");
        addInfoCell(table, "Poste:", "N/A");
        
        // Informations de la période
        addInfoCell(table, "Période:", formatDate(salarySlip.getStartDate() != null ? salarySlip.getStartDate().toString() : null) + " - " + formatDate(salarySlip.getEndDate() != null ? salarySlip.getEndDate().toString() : null));
        // addInfoCell(table, "Date de paiement:", formatDate(salarySlip.getPostingDate())); // Field removed from SalarySlipDTO
        // addInfoCell(table, "Jours travaillés:", formatNumber(salarySlip.getPaymentDays())); // Field removed from SalarySlipDTO
        // addInfoCell(table, "Heures travaillées:", formatNumber(salarySlip.getTotalWorkingHours())); // Field removed from SalarySlipDTO
        addInfoCell(table, "Date de paiement:", "N/A");
        addInfoCell(table, "Jours travaillés:", "N/A");
        addInfoCell(table, "Heures travaillées:", "N/A");
        
        table.setSpacingAfter(20);
        document.add(table);
    }
    
    public void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
    
    public void addSalaryDetails(Document document, SalarySlipDTO salarySlip) throws DocumentException {
        // Titre de la section
        Paragraph sectionTitle = new Paragraph("DÉTAILS DU SALAIRE", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);
        
        // Tableau des gains
        PdfPTable earningsTable = new PdfPTable(2);
        earningsTable.setWidthPercentage(100);
        addHeaderCell(earningsTable, "GAINS", 2);

        // Afficher chaque composant de gain
        double totalEarnings = 0.0;
        if (salarySlip.getEarnings() != null && !salarySlip.getEarnings().isEmpty()) {
            for (com.erpnext.dto.SalaryComponentDTO earning : salarySlip.getEarnings()) {
                addDetailRow(earningsTable, earning.getSalaryComponent(), formatCurrency(earning.getAmount(), salarySlip.getCurrency()));
                if (earning.getAmount() != null) totalEarnings += earning.getAmount();
            }
        } else {
            addDetailRow(earningsTable, "Aucun gain", "N/A");
        }
        addTotalRow(earningsTable, "Total des gains", formatCurrency(salarySlip.getGrossPay(), salarySlip.getCurrency()));
        document.add(earningsTable);

        // Tableau des déductions
        PdfPTable deductionsTable = new PdfPTable(2);
        deductionsTable.setWidthPercentage(100);
        deductionsTable.setSpacingBefore(20);
        addHeaderCell(deductionsTable, "DÉDUCTIONS", 2);

        double totalDeductions = 0.0;
        if (salarySlip.getDeductions() != null && !salarySlip.getDeductions().isEmpty()) {
            for (com.erpnext.dto.SalaryComponentDTO deduction : salarySlip.getDeductions()) {
                addDetailRow(deductionsTable, deduction.getSalaryComponent(), formatCurrency(deduction.getAmount(), salarySlip.getCurrency()));
                if (deduction.getAmount() != null) totalDeductions += deduction.getAmount();
            }
        } else {
            addDetailRow(deductionsTable, "Aucune déduction", "N/A");
        }
        addTotalRow(deductionsTable, "Total des déductions", formatCurrency(salarySlip.getTotalDeduction(), salarySlip.getCurrency()));
        document.add(deductionsTable);
    }
    
    public void addSummary(Document document, SalarySlipDTO salarySlip) throws DocumentException {
        // Tableau récapitulatif
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(30);

        // Salaire brut
        PdfPCell grossPayLabelCell = new PdfPCell(new Phrase("SALAIRE BRUT", TOTAL_FONT));
        grossPayLabelCell.setBorder(Rectangle.TOP);
        grossPayLabelCell.setPadding(5);
        summaryTable.addCell(grossPayLabelCell);

        PdfPCell grossPayValueCell = new PdfPCell(new Phrase(formatCurrency(salarySlip.getGrossPay(), salarySlip.getCurrency()), TOTAL_FONT));
        grossPayValueCell.setBorder(Rectangle.TOP);
        grossPayValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        grossPayValueCell.setPadding(5);
        summaryTable.addCell(grossPayValueCell);

        // Total des déductions
        PdfPCell deductionLabelCell = new PdfPCell(new Phrase("TOTAL DÉDUCTIONS", TOTAL_FONT));
        deductionLabelCell.setBorder(Rectangle.TOP);
        deductionLabelCell.setPadding(5);
        summaryTable.addCell(deductionLabelCell);

        PdfPCell deductionValueCell = new PdfPCell(new Phrase(formatCurrency(salarySlip.getTotalDeduction(), salarySlip.getCurrency()), TOTAL_FONT));
        deductionValueCell.setBorder(Rectangle.TOP);
        deductionValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        deductionValueCell.setPadding(5);
        summaryTable.addCell(deductionValueCell);

        // Salaire net
        PdfPCell netPayLabelCell = new PdfPCell(new Phrase("SALAIRE NET", TOTAL_FONT));
        netPayLabelCell.setBorder(Rectangle.TOP);
        netPayLabelCell.setPadding(5);
        summaryTable.addCell(netPayLabelCell);

        PdfPCell netPayValueCell = new PdfPCell(new Phrase(formatCurrency(salarySlip.getNetPay(), salarySlip.getCurrency()), TOTAL_FONT));
        netPayValueCell.setBorder(Rectangle.TOP);
        netPayValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netPayValueCell.setPadding(5);
        summaryTable.addCell(netPayValueCell);

        document.add(summaryTable);
    }
    
    public void addHeaderCell(PdfPTable table, String text, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(HEADER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setColspan(colspan);
        table.addCell(cell);
    }
    
    public void addDetailRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setPadding(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
    
    public void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setPadding(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, BOLD_FONT));
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
    
    public String formatNumber(Double number) {
        if (number == null) {
            return "0";
        }
        return NumberFormat.getNumberInstance(Locale.FRANCE).format(number);
    }
    
    public String formatCurrency(Double amount, String currency) {
        if (amount == null) {
            amount = 0.0;
        }
        
        if (currency == null || currency.isEmpty()) {
            currency = "EUR";
        }
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        return formatter.format(amount);
    }
    
    public String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }
        
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
