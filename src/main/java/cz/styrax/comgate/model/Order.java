package cz.styrax.comgate.model;

/**
 * Order with mandatory and voluntary fields for request to Comgate and
 * transaction data
 *
 * @author michal.bokr
 */
public class Order {

    // mandatory
    private String refId;
    private Payer payer;
    private Item item;
    private String method;

    // voluntary  
    private String language;

    // transaction data
    private Transaction transaction;

    public Order(String refId, Payer payer, Item item, String method, String language) {
        this.refId = refId;
        this.payer = payer;
        this.item = item;
        this.method = method;
        this.language = language;
    }

    public Order(String refId, Payer payer, Item item, String method) {
        this.refId = refId;
        this.payer = payer;
        this.item = item;
        this.method = method;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public Payer getPayer() {
        return payer;
    }

    public void setPayer(Payer payer) {
        this.payer = payer;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

}
