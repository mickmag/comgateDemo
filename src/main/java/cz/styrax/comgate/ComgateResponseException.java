package cz.styrax.comgate;

/**
 * Exception when Comgate returns error
 * @author michal.bokr
 */
public class ComgateResponseException extends Exception {

    public ComgateResponseException(String string) {
        super(string);
    }
    
}
