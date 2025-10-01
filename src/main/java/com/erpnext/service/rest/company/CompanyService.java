package com.erpnext.service.rest.company;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.erpnext.utils.ErpNextApiUtil;

/**
 * Service utilitaire pour la gestion de la société (Company) dans ERPNext.
 * Permet principalement de vérifier l'existence d'une société et de la créer si nécessaire
 * en réutilisant une Holiday List existante (par défaut la première trouvée).
 */
@Service
public class CompanyService {

    public static final String COMPANY_DOCTYPE = "Company";
    public static final String HOLIDAY_LIST_DOCTYPE = "Holiday List";

    @Autowired
    public ErpNextApiUtil erpNextApiUtil;

    /**
     * Vérifie si une société existe déjà dans ERPNext
     * @param companyName Nom de la société
     * @return true si elle existe, false sinon
     */
    public boolean companyExists(String companyName) {
        if (companyName == null || companyName.isEmpty()) {
            return false;
        }
        String[] fields = {"name"};
        Map<String, String> filters = new HashMap<>();
        filters.put("name", companyName);
        Map<String, Object> response = erpNextApiUtil.fetchDataWithFilters(COMPANY_DOCTYPE, fields, filters);
        if (response == null || !response.containsKey("data")) {
            return false;
        }
        List<?> dataList = (List<?>) response.get("data");
        return !dataList.isEmpty();
    }

    /**
     * S'assure qu'une société existe. Si ce n'est pas le cas, la crée automatiquement.
     * @param companyName Nom de la société
     */
    public void ensureCompanyExists(String companyName) {
        if (companyName == null || companyName.isEmpty()) return;
        if (!companyExists(companyName)) {
            createCompany(companyName);
        }
    }

    /**
     * Crée une société minimaliste dans ERPNext.
     * Le champ default_holiday_list est renseigné avec la première Holiday List trouvée.
     * @param companyName Nom de la société
     * @return Nom (ID) de la société créée, ou null en cas d'échec
     */
    public String createCompany(String companyName) {
        try {
            String holidayListName = fetchDefaultHolidayList();
            if (holidayListName == null) {
                // Si aucune holiday list trouvée, ERPNext exigera quand même un champ ;
                // on laisse vide pour laisser ERPNext appliquer son propre défaut
                holidayListName = "";
            }

            Map<String, Object> data = new HashMap<>();
            data.put("doctype", COMPANY_DOCTYPE);
            data.put("company_name", companyName);
            data.put("domain", "Services"); // Valeur par défaut générique
            if (!holidayListName.isEmpty()) {
                data.put("default_holiday_list", holidayListName);
            }
            // Certaines versions d'ERPNext nécessitent default_currency
            data.putIfAbsent("default_currency", "ARS");

            String url = erpNextApiUtil.getErpnextUrl() + "/api/resource/" + COMPANY_DOCTYPE;
            Map<String, Object> response = erpNextApiUtil.postData(url, data);
            if (response != null && response.containsKey("data")) {
                Map<String, Object> resData = (Map<String, Object>) response.get("data");
                return (String) resData.get("name");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la société: " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère la première Holiday List disponible.
     * @return Nom de la holiday list
     */
    public String fetchDefaultHolidayList() {
        String[] fields = {"name"};
        Map<String, Object> response = erpNextApiUtil.fetchDataWithFilters(HOLIDAY_LIST_DOCTYPE, fields, null);
        if (response != null && response.containsKey("data")) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
            if (!dataList.isEmpty()) {
                return (String) dataList.get(0).get("name");
            }
        }
        return null;
    }

    public List<Map<String, Object>> getAllCompanies() {
        String[] fields = {"name"};
        Map<String, Object> response = erpNextApiUtil.fetchDataWithFilters(COMPANY_DOCTYPE, fields, null);
        if (response != null && response.containsKey("data")) {
            return (List<Map<String, Object>>) response.get("data");
        }
        return new ArrayList<>();
    }

    public Map<String, Object> getCompanyDetails(String companyName) {
        return erpNextApiUtil.fetchDataById(COMPANY_DOCTYPE, companyName);
    }
}
