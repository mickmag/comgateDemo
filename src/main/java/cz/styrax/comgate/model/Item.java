package cz.styrax.comgate.model;

/**
 * Item with mandatory and voluntary fields for request to Comgate
 * @author michal.bokr
 */
public class Item {

    // mandatory
    private String price;
    private String currency;
    private String label;

    // voluntary
    private String productName;
    private String country;

    public Item(String price, String currency, String label, String productName, String country) {
        this.price = price;
        this.currency = currency;
        this.label = label;
        this.productName = productName;
        this.country = country;
    }

    public Item(String price, String currency, String label) {
        this.price = price;
        this.currency = currency;
        this.label = label;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

}
