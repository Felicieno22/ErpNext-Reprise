package com.erpnext.controller.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.erpnext.service.rest.utils.SalaryStatsService;
import com.erpnext.service.rest.utils.SalaryStatsService.MonthlySalaryStats;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/salaries/stats")
@RequiredArgsConstructor
@Slf4j
public class SalaryStatsController {
    public final SalaryStatsService statsService;

    @GetMapping("")
    public String showStats(@RequestParam(value = "year", required = false) Integer year, Model model) {
        int selectedYear = (year != null) ? year : LocalDate.now().getYear();
        List<MonthlySalaryStats> monthlyStats = statsService.getMonthlyStats(selectedYear);
        MonthlySalaryStats yearlyStats = statsService.getYearlyStats(selectedYear);
        model.addAttribute("monthlyStats", monthlyStats);
        model.addAttribute("yearlyStats", yearlyStats);
        model.addAttribute("selectedYear", selectedYear);
        // Génération des labels de mois en français pour Chart.js
        List<String> labels = new java.util.ArrayList<>();
        java.time.format.TextStyle style = java.time.format.TextStyle.SHORT;
        java.util.Locale locale = java.util.Locale.FRENCH;
        for (SalaryStatsService.MonthlySalaryStats stat : monthlyStats) {
            int m = stat.getMonth();
            if (m == 0) {
                labels.add("Année");
            } else {
                labels.add(java.time.Month.of(m).getDisplayName(style, locale));
            }
        }
        model.addAttribute("labels", labels);
        // Préparation des datasets pour Chart.js
        List<Double> monthlyStatsBase = monthlyStats.stream().map(SalaryStatsService.MonthlySalaryStats::getTotalBase).collect(java.util.stream.Collectors.toList());
        List<Double> monthlyStatsNet = monthlyStats.stream().map(SalaryStatsService.MonthlySalaryStats::getTotalNet).collect(java.util.stream.Collectors.toList());
        List<Double> monthlyStatsRetenue = monthlyStats.stream().map(SalaryStatsService.MonthlySalaryStats::getTotalRetenue).collect(java.util.stream.Collectors.toList());
        model.addAttribute("monthlyStatsBase", monthlyStatsBase);
        model.addAttribute("monthlyStatsNet", monthlyStatsNet);
        model.addAttribute("monthlyStatsRetenue", monthlyStatsRetenue);
        return "salaries/stats";
    }
}
