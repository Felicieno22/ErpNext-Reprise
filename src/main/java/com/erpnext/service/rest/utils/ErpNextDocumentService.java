package com.erpnext.service.rest.utils;

import com.erpnext.utils.ErpNextApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service métier de haut niveau pour gérer les opérations sur les documents ERPNext.
 * Permet d'enchaîner cancel → update → submit de manière sûre et centralisée.
 */
@Service
public class ErpNextDocumentService {

    public static final Logger logger = LoggerFactory.getLogger(ErpNextDocumentService.class);

    @Autowired
    public ErpNextApiUtil erpNextApiUtil;

    /**
     * Annule, met à jour puis soumet un document ERPNext.
     *
     * @param doctype       Le type du document (ex: "Salary Slip", "Purchase Invoice")
     * @param name          L'identifiant unique du document (ex: "SAL-SLIP-2025-0001")
     * @param updateFields  Les champs à mettre à jour
     * @return Message indiquant le succès
     */
    public String cancelUpdateAndSubmit(String doctype, String name, Map<String, Object> updateFields) {
        try {
            int docstatus = getDocStatus(doctype, name);
            logger.info("Statut du document {} / {} : docstatus={}", doctype, name, docstatus);
            if (docstatus == 0) { // Draft
                logger.info("Document en Draft : update + submit");
                this.updateDocument(doctype, name, updateFields);
                this.callMethod(doctype, name, "submit");
            } else if (docstatus == 1) { // Submitted
                logger.info("Document soumis : cancel + update + submit");
                this.callMethod(doctype, name, "cancel");
                this.updateDocument(doctype, name, updateFields);
                this.callMethod(doctype, name, "submit");
            } else {
                throw new RuntimeException("Impossible de modifier un document annulé ou dans un état inconnu (docstatus=" + docstatus + ")");
            }
            return "✅ Document " + name + " mis à jour et soumis avec succès.";
        } catch (Exception e) {
            logger.error("❌ Échec de cancel → update → submit sur {} / {} : {}", doctype, name, e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour complète du document : " + name, e);
        }
    }

    /**
     * Supprime un document ERPNext après l'avoir annulé s'il est soumis.
     *
     * @param doctype Le type du document
     * @param name    L'identifiant du document
     * @return Message de confirmation
     */
    public String cancelAndDelete(String doctype, String name) {
        try {
            logger.info("Annulation avant suppression : {} / {}", doctype, name);
            this.callMethod(doctype, name, "cancel");

            logger.info("Suppression du document : {}", name);
            this.deleteDocument(doctype, name);

            return "🗑️ Document " + name + " annulé et supprimé avec succès.";
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la suppression de {} / {} : {}", doctype, name, e.getMessage());
            throw new RuntimeException("Erreur lors de l'annulation ou suppression : " + name, e);
        }
    }

    /**
     * Appelle une méthode (submit, cancel, etc.) sur un document ERPNext via l'API REST.
     */
    public Map<String, Object> callMethod(String doctype, String name, String method) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + doctype + "/" + name;
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("run_method", method);
        return erpNextApiUtil.postData(url, body);
    }

    /**
     * Met à jour un document ERPNext via l'API REST.
     */
    public Map<String, Object> updateDocument(String doctype, String name, Map<String, Object> updateFields) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + doctype + "/" + name;
        return erpNextApiUtil.putData(url, updateFields);
    }

    /**
     * Supprime un document ERPNext via l'API REST.
     */
    public boolean deleteDocument(String doctype, String name) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + doctype + "/" + name;
        return erpNextApiUtil.deleteData(url);
    }

    public int getDocStatus(String doctype, String name) {
        String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + doctype + "/" + name;
        Map<String, Object> response = erpNextApiUtil.fetchData(url);
        if (response != null && response.containsKey("data")) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Object docstatusObj = data.get("docstatus");
            if (docstatusObj instanceof Number) {
                return ((Number) docstatusObj).intValue();
            } else if (docstatusObj instanceof String) {
                try {
                    return Integer.parseInt((String) docstatusObj);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public void cancelDocument(String doctype, String name) {
        callMethod(doctype, name, "cancel");
    }

    public void submitDocument(String doctype, String name) {
        callMethod(doctype, name, "submit");
    }
}
