package com.erpnext.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.erpnext.service.rest.salary.SalaryComponentService;

@Controller
public class HomeController {

    public final SalaryComponentService salaryComponentService;

    public HomeController(SalaryComponentService salaryComponentService) {
        this.salaryComponentService = salaryComponentService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login"; // Redirige la racine vers la page de connexion
    }

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("salaryComponents", salaryComponentService.getAllComponentNames());
        return "home"; // Charge la page home.html
    }
}
