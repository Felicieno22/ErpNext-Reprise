package com.erpnext.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe générique pour les réponses API
 * @param <T> Type de données contenues dans la réponse
 */
@Data
@NoArgsConstructor
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    /**
     * Crée une réponse de succès
     * @param data Données à inclure dans la réponse
     * @param message Message de succès
     * @return Réponse API
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        try {
            return new ApiResponse<T>(true, message, data);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<T>(false, "Erreur lors de la création de la réponse: " + e.getMessage(), null);
        }
    }
    
    /**
     * Crée une réponse d'erreur
     * @param message Message d'erreur
     * @return Réponse API
     */
    public static <T> ApiResponse<T> error(String message) {
        try {
            return new ApiResponse<T>(false, message, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<T>(false, "Erreur lors de la création de la réponse d'erreur: " + e.getMessage(), null);
        }
    }
}
