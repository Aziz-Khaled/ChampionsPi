package tn.esprit.Champions.test;

import tn.esprit.Champions.models.Negociation;
import tn.esprit.Champions.models.credit;
import tn.esprit.Champions.services.negociationService;
import tn.esprit.Champions.services.creditService;

import java.sql.SQLException;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        negociationService ns = new negociationService();
        creditService cs = new creditService();

        try {
            // --- ÉTAPE PRÉALABLE : RÉCUPÉRER UN CRÉDIT EXISTANT ---
            // On a besoin d'un credit_id valide pour insérer une négociation
            List<credit> listeCredits = cs.SelectAll();

            if (listeCredits.isEmpty()) {
                System.out.println("Erreur : Vous devez d'abord créer un crédit dans la base avant de tester les négociations.");
                return;
            }

            credit creditCible = listeCredits.get(listeCredits.size() - 1);
            int idCreditValide = creditCible.getId();

            // --- 1. TEST INSERTION ---
            System.out.println("--- PHASE 1 : INSERTION NÉGOCIATION ---");
            Negociation nego = new Negociation();
            nego.setCredit_id(idCreditValide);
            nego.setInvestor_id(1); // Assure-toi que l'investisseur avec ID 1 existe dans ta table utilisateur
            nego.setMontant(2000.0);
            nego.setTaux_propose(4.5);

            ns.insertOne(nego);
            System.out.println("Négociation insérée avec succès !");

            // --- 2. TEST LECTURE ET MODIFICATION ---
            System.out.println("\n--- PHASE 2 : MODIFICATION NÉGOCIATION ---");
            List<Negociation> listeNegos = ns.SelectAll();
            if (!listeNegos.isEmpty()) {
                Negociation nModif = listeNegos.get(listeNegos.size() - 1);
                System.out.println("Négociation trouvée pour le crédit ID: " + nModif.getCredit_id());

                // On modifie le montant proposé
                nModif.setMontant(2200.0);
                nModif.setTaux_propose(4.2);

                ns.updateOne(nModif);
                System.out.println("Négociation mise à jour !");
            }

            // --- 3. TEST SUPPRESSION ---
            System.out.println("\n--- PHASE 3 : SUPPRESSION NÉGOCIATION ---");
            /*
            if (!listeNegos.isEmpty()) {
                ns.deleteOne(listeNegos.get(0));
                System.out.println("Négociation supprimée !");
            }
            */

        } catch (SQLException e) {
            System.err.println("Erreur SQL lors du test négociation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}