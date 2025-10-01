package com.erpnext.controller.rest;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrôleur pour gérer les erreurs HTTP et les afficher dans l'interface utilisateur
 */
@Controller
public class CustomErrorController implements ErrorController {
    public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Récupérer le code d'erreur
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = 500; // Par défaut
        
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }
        
        // Récupérer le message d'erreur
        String errorMessage = "Une erreur s'est produite";
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (message != null && !message.toString().isEmpty()) {
            errorMessage = message.toString();
        }
        
        // Récupérer l'exception
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (throwable != null) {
            // Journaliser l'erreur
            log.error("Erreur {} : {}", statusCode, errorMessage, throwable);
        } else {
            log.error("Erreur {} : {}", statusCode, errorMessage);
        }
        
        // Ajouter les informations au modèle
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("timestamp", System.currentTimeMillis());
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        
        // Retourner la page d'erreur appropriée
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "error/404";
        } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "error/403";
        } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return "error/500";
        }
        
        return "error/general";
    }
}
