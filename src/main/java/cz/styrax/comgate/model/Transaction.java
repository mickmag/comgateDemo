package cz.styrax.comgate.model;

/**
 * Transaction data
 * @author michal.bokr
 */
public class Transaction {
    private String transId;
    private String status;
    // fee or 'unknown'
    private String fee;

    public Transaction(String transId, String status, String fee) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFee() {
        return fee;
    }

    public void setFee(String fee) {
        this.fee = fee;
    }

    
}
