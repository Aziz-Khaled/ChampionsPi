package tn.esprit.Champions.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.Champions.models.currency;
import tn.esprit.Champions.models.typeCurrency;
import tn.esprit.Champions.services.CurrencyService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class admin_wallet_controller {

    @FXML private ComboBox<String> boxCode;
    @FXML private ComboBox<String> boxType;
    @FXML private Label lblName;
    @FXML private CheckBox chkIsTrading;
    @FXML private Button add_currency;
    @FXML private TableView<currency> table;
    @FXML
    private Button btnSignOut;

    private ObservableList<currency> assetList = FXCollections.observableArrayList();
    private CurrencyService currencyService = new CurrencyService();
    private typeCurrency currentType;

    private List<String> cryptoSymbols = new ArrayList<>();
    private List<String> fiatCodes = new ArrayList<>();
    private boolean cryptoLoaded = false;
    private boolean fiatLoaded = false;
    private currency selectedCurrency = null;


    @FXML
    public void initialize() {

        TableColumn<currency, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));

        TableColumn<currency, String> colNom = new TableColumn<>("Nom");
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));

        TableColumn<currency, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type_currency"));

        TableColumn<currency, Boolean> colTrading = new TableColumn<>("Trading");
        colTrading.setCellValueFactory(new PropertyValueFactory<>("is_trading"));

        table.getColumns().clear();
        table.getColumns().addAll(colCode, colNom, colType, colTrading);
        table.setItems(assetList);


        loadCurrenciesFromDB();


        boxType.setItems(FXCollections.observableArrayList("FIAT", "CRYPTO"));
        boxType.setDisable(true);


        fetchFiatList();
        fetchCryptoList();

        boxCode.setOnAction(event -> handleCodeSelection());
        add_currency.setOnAction(event -> handleAdd());
        // Détecter la sélection d'une ligne
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedCurrency = newSelection;
                // Mettre à jour le formulaire avec les informations de la ligne sélectionnée
                boxCode.setValue(selectedCurrency.getCode());
                lblName.setText(selectedCurrency.getNom());
                boxType.setValue(selectedCurrency.getType_currency() == typeCurrency.crypto ? "CRYPTO" : "FIAT");
                chkIsTrading.setSelected(selectedCurrency.isIs_trading());

                // Activer ou désactiver le checkbox selon le type
                chkIsTrading.setDisable(selectedCurrency.getType_currency() == typeCurrency.fiat);
            }
        });
        btnSignOut.setOnAction(event -> handleSignOut());
    }

    private void loadCurrenciesFromDB() {
        new Thread(() -> {
            try {
                List<currency> list = currencyService.SelectAll();
                Platform.runLater(() -> {
                    assetList.clear();
                    assetList.addAll(list);
                });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur BD", "Impossible de charger les currencies."));
            }
        }).start();
    }

    private void fetchFiatList() {
        new Thread(() -> {
            try {
                URL url = new URL("https://open.er-api.com/v6/latest/USD");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                con.disconnect();

                JSONObject json = new JSONObject(sb.toString());
                JSONObject rates = json.getJSONObject("rates");
                fiatCodes.clear();
                fiatCodes.addAll(rates.keySet());

                Platform.runLater(() -> {
                    boxCode.getItems().clear();
                    boxCode.getItems().addAll(fiatCodes);
                });

                fiatLoaded = true;
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur API", "Impossible de récupérer les devises FIAT."));
            }
        }).start();
    }

    private void fetchCryptoList() {
        new Thread(() -> {
            try {
                URL urlCrypto = new URL("https://api.coingecko.com/api/v3/coins/list");
                HttpURLConnection conCrypto = (HttpURLConnection) urlCrypto.openConnection();
                conCrypto.setRequestMethod("GET");

                BufferedReader brCrypto = new BufferedReader(new InputStreamReader(conCrypto.getInputStream()));
                StringBuilder sbCrypto = new StringBuilder();
                String line;
                while ((line = brCrypto.readLine()) != null) sbCrypto.append(line);
                brCrypto.close();
                conCrypto.disconnect();

                JSONArray cryptoArray = new JSONArray(sbCrypto.toString());
                cryptoSymbols.clear();
                for (int i = 0; i < cryptoArray.length(); i++) {
                    cryptoSymbols.add(cryptoArray.getJSONObject(i).getString("symbol").toUpperCase());
                }

                cryptoLoaded = true;
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur API", "Impossible de récupérer la liste des cryptos."));
            }
        }).start();
    }

    @FXML
    private void handleCodeSelection() {
        String selectedCode = boxCode.getValue();
        if (selectedCode == null || selectedCode.isEmpty()) return;

        lblName.setText(selectedCode);

        if (!fiatLoaded || !cryptoLoaded) {
            showAlert("Patience", "Chargement des données en cours, réessayez dans quelques secondes.");
            return;
        }

        determineCurrencyType(selectedCode);
    }

    private void determineCurrencyType(String code) {
        if (cryptoSymbols.contains(code.toUpperCase())) {
            currentType = typeCurrency.crypto;
            boxType.setValue("CRYPTO");
            chkIsTrading.setDisable(false);
            add_currency.setDisable(false);
        } else if (fiatCodes.contains(code)) {
            currentType = typeCurrency.fiat;
            boxType.setValue("FIAT");
            chkIsTrading.setSelected(false);
            chkIsTrading.setDisable(true);
            add_currency.setDisable(false);
        } else {
            currentType = null;
            boxType.setValue(null);
            chkIsTrading.setDisable(true);
            add_currency.setDisable(true);
            showAlert("Erreur", "Code non reconnu comme FIAT ou CRYPTO");
        }
    }

    @FXML
    private void handleAdd() {
        String code = boxCode.getValue();
        String name = lblName.getText();
        boolean isTrading = chkIsTrading.isSelected();

        if (code == null || code.isEmpty() || currentType == null) {
            showAlert("Erreur", "Veuillez sélectionner un code valide !");
            return;
        }

        currency newCurrency = new currency(0, code, name, currentType, isTrading);

        try {
            currencyService.insertOne(newCurrency);


            assetList.add(newCurrency);

            showAlert("Succès", "Currency ajoutée : " + name + " (" + currentType + ")");
        } catch (SQLException ex) {
            showAlert("Erreur", ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleDelete() {
        if (selectedCurrency == null) {
            showAlert("Erreur", "Veuillez sélectionner une currency à supprimer !");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer la currency : " + selectedCurrency.getNom() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {

                    currencyService.deleteOne(selectedCurrency);


                    assetList.remove(selectedCurrency);

                    showAlert("Succès", "Currency supprimée avec succès !");


                    boxCode.setValue(null);
                    lblName.setText("");
                    boxType.setValue(null);
                    chkIsTrading.setSelected(false);
                    chkIsTrading.setDisable(true);

                    selectedCurrency = null;
                } catch (SQLException ex) {

                    showAlert("Erreur", ex.getMessage());
                }
            }
        });
    }
    @FXML
    private void handleModify() {
        if (selectedCurrency == null) {
            showAlert("Erreur", "Veuillez sélectionner une currency à modifier !");
            return;
        }

        if (selectedCurrency.getType_currency() == typeCurrency.fiat) {
            showAlert("Info", "Tu n’as rien à changer dans une currency de type FIAT.");
            return;
        }


        boolean newIsTrading = chkIsTrading.isSelected();
        selectedCurrency.setIs_trading(newIsTrading);

        try {
            currencyService.updateOne(selectedCurrency);


            table.refresh();

            showAlert("Succès", "Currency mise à jour avec succès !");
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }
    @FXML
    private void handleSignOut() {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DashboardWalletClient.fxml"));
            Parent root = loader.load();


            btnSignOut.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le Dashboard client !");
        }
    }
}