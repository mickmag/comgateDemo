package cz.styrax.comgate.model;

/**
 * Payer with mandatory and voluntary fields for request to Comgate
 * @author michal.bokr
 */
public class Payer {
    // mandatory
    private String email;
    
    // voluntary
    private String phone;
    private String payerId;

    public Payer(String email, String phone, String payerId) {
        this.email = email;
        this.phone = phone;
        this.payerId = payerId;
    }
    
    public Payer(String email) {
        this.email = email;
    }
        
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }
    
    
}
