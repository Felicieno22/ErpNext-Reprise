package com.erpnext.service.rest.utils;

import java.util.List;
import java.util.Map;
import com.erpnext.dto.SalarySlipDTO;
import com.erpnext.service.rest.salary.SalaryService;
import com.erpnext.dto.SalaryComponentDTO;

import com.erpnext.dto.SalarySlipDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SalaryStatsService {
    public final SalaryService salaryService;

    public SalaryStatsService(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    /**
     * Retourne les statistiques mensuelles pour une année donnée
     */
    public List<MonthlySalaryStats> getMonthlyStats(int year) {
        List<SalarySlipDTO> all = salaryService.getAllSalarySlips();
        Map<Integer, List<SalarySlipDTO>> byMonth = all.stream()
                .filter(slip -> slip.getStartDate() != null && slip.getStartDate().getYear() == year)
                .collect(Collectors.groupingBy(slip -> slip.getStartDate().getMonthValue()));
        List<MonthlySalaryStats> stats = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            List<SalarySlipDTO> slips = byMonth.getOrDefault(m, new ArrayList<>());
            // Remplacement : fetch detail pour chaque slip
            List<SalarySlipDTO> detailedSlips = slips.stream()
                .map(slip -> salaryService.fetchSalarySlipDetailById(slip.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            stats.add(MonthlySalaryStats.fromSalarySlips(year, m, detailedSlips));
        }
        return stats;
    }

    /**
     * Calcule les totaux annuels pour une année donnée
     */
    public MonthlySalaryStats getYearlyStats(int year) {
        List<SalarySlipDTO> all = salaryService.getAllSalarySlips();
        List<SalarySlipDTO> yearSlips = all.stream()
                .filter(slip -> slip.getStartDate() != null && slip.getStartDate().getYear() == year)
                .map(slip -> salaryService.fetchSalarySlipDetailById(slip.getName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return MonthlySalaryStats.fromSalarySlips(year, 0, yearSlips);
    }

    @Data
    public static class MonthlySalaryStats {
        public int year;
        public int month; // 1-12, 0 = année
        public double totalGross;
        public double totalNet;
        public double totalBase;
        public double totalPrime;
        public double totalIndemnite;
        public double totalRetenue;
        public int count;
        public List<SalarySlipDTO> slips;

        public MonthlySalaryStats(int year, int month, double totalGross, double totalNet, double totalBase, double totalPrime, double totalIndemnite, double totalRetenue, int count, List<SalarySlipDTO> slips) {
            this.year = year;
            this.month = month;
            this.totalGross = totalGross;
            this.totalNet = totalNet;
            this.totalBase = totalBase;
            this.totalPrime = totalPrime;
            this.totalIndemnite = totalIndemnite;
            this.totalRetenue = totalRetenue;
            this.count = count;
            this.slips = slips;
        }

        public int getMonth() {
            return month;
        }

        public double getTotalGross() { // Already effectively present via @Data, but good to be aware
            return totalGross;
        }

        public double getTotalNet() {
            return totalNet;
        }

        public double getTotalBase() {
            return totalBase;
        }

        public double getTotalPrime() { // Already effectively present via @Data
            return totalPrime;
        }

        public double getTotalIndemnite() { // Already effectively present via @Data
            return totalIndemnite;
        }

        public double getTotalRetenue() {
            return totalRetenue;
        }

        public int getCount() { // Already effectively present via @Data
            return count;
        }

        public List<SalarySlipDTO> getSlips() { // Already effectively present via @Data
            return slips;
        }

        public static MonthlySalaryStats fromSalarySlips(int year, int month, List<SalarySlipDTO> slips) {
            double totalGross = 0, totalNet = 0, totalBase = 0, totalPrime = 0, totalIndemnite = 0, totalRetenue = 0;
            for (SalarySlipDTO slip : slips) {
                totalGross += slip.getGrossPay() != null ? slip.getGrossPay() : 0;
                totalNet += slip.getNetPay() != null ? slip.getNetPay() : 0;
                totalBase += slip.getBaseSalary() != null ? slip.getBaseSalary() : 0;
                if (slip.getEarnings() != null) {
                    totalPrime += slip.getEarnings().stream().filter(e -> e.getSalaryComponent() != null && e.getSalaryComponent().toLowerCase().contains("prime")).mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0).sum();
                    totalIndemnite += slip.getEarnings().stream().filter(e -> e.getSalaryComponent() != null && e.getSalaryComponent().toLowerCase().contains("indemn")).mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0).sum();
                }
                if (slip.getDeductions() != null) {
                    totalRetenue += slip.getDeductions().stream().mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0).sum();
                }
            }
            return new MonthlySalaryStats(year, month, totalGross, totalNet, totalBase, totalPrime, totalIndemnite, totalRetenue, slips.size(), slips);
        }
    }
}
