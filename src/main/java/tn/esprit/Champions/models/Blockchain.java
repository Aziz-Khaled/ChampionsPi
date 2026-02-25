package tn.esprit.Champions.models;

public class Blockchain {

    private int idBlock;
    private int idTransaction;
    private int blockIndex;


    private String previousHash;
    private String currentHash;

    private Integer walletSource;      // peut être NULL
    private Integer walletDestination;

    private Double montant;

    private String type;

    private Integer idCard;            // NULL si transfert

    // Getters & Setters

    public int getIdBlock() { return idBlock; }
    public void setIdBlock(int idBlock) { this.idBlock = idBlock; }

    public int getIdTransaction() { return idTransaction; }
    public void setIdTransaction(int idTransaction) { this.idTransaction = idTransaction; }

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int blockIndex) { this.blockIndex = blockIndex; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }

    public Integer getWalletSource() { return walletSource; }
    public void setWalletSource(Integer walletSource) { this.walletSource = walletSource; }

    public Integer getWalletDestination() { return walletDestination; }
    public void setWalletDestination(Integer walletDestination) { this.walletDestination = walletDestination; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getIdCard() { return idCard; }
    public void setIdCard(Integer idCard) { this.idCard = idCard; }
}