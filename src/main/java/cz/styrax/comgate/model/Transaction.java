package cz.styrax.comgate.model;

/**
 * Transaction data
 * @author michal.bokr
 */
public class Transaction {
    private String transId;
    private Status status;
    // fee or 'unknown'
    private String fee;
    
    public Transaction(String transId) {
        this.transId = transId;
    }

    public Transaction(String transId, Status status, String fee) {
        this.transId = transId;
        this.status = status;
        this.fee = fee;
    }

    public String getTransId() {
        return transId;
    }

    public void setTransId(String transId) {
        this.transId = transId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }
    
    
    public enum Status {
        PAID("Zaplaceno"),
        CANCELLED("Stornováno"),
        AUTHORIZED("Autorizováno");
        
        String translation;
        
        Status(String translation) {
            this.translation = translation;
        }
        
        public String getTranslation() {
            return translation;
        }
    }
    
}
