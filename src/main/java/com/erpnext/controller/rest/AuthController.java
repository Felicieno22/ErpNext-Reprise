package com.erpnext.controller.rest;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.erpnext.service.rest.auth.AuthService;

import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    public AuthService authService;

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login"; // Affiche la page de connexion
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            authService.setApiKey("a498e6aff5faf03:51f40a5c4ebc99d");
            Map<String, Object> response = authService.login(username, password);

            // LOG : Afficher la réponse brute de l'API ERPNext
            System.out.println("Réponse brute ERPNext : " + response);

            if (response != null && response.containsKey("message")) {
                session.setAttribute("username", username);
                session.setAttribute("token", response.get("message"));
                session.setAttribute("apiKey", "a498e6aff5faf03:51f40a5c4ebc99d");

                // Redirige vers la page d'accueil après une connexion réussie
                return "redirect:/home"; 
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Identifiants invalides : " + e.getMessage());
        }
        return "auth/login"; // Si échec, rester sur la page de login
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Appel au service pour déconnecter l'utilisateur
        authService.setApiKey((String) session.getAttribute("apiKey"));
        authService.logout(); 
        session.invalidate();
        return "redirect:/login"; // Redirige vers la page de login après la déconnexion
    }
}
