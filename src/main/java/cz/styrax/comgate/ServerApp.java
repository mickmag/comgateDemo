package cz.styrax.comgate;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.afterAfter;
import static spark.Spark.post;
import static spark.Spark.get;
import static spark.Spark.staticFileLocation;

//import static spark.Spark.afterAfter;
//import static spark.Spark.post;
//import static spark.Spark.staticFileLocation;

/**
 *
 * @author michal.bokr
 */
public class ServerApp {

    final static Logger logger = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) {

        // Serve static files from src/main/resources/public
        staticFileLocation("/public");

        // Log all requests and responses
        afterAfter(new LoggingFilter());

        
        post("/pay", (request, response) -> {
            System.out.println(request.body());      
            final String[] queryParamsValues = request.queryParamsValues("method");
            final String method = queryParamsValues[0];           
//            response.header("redirect", "https://www.seznam.cz/");_
//            response.redirect("/redirect");
//            return "";
            return "redirect: https://www.seznam.cz";
        });
        
        get("/pay", (request, response) -> {
            System.out.println(request.body());      
//            response.header('location: https://www.seznam.cz/');
//            final String[] queryParamsValues = request.queryParamsValues("method");
//            final String method = queryParamsValues[0];           
//            response.header("redirect", "https://www.seznam.cz/");_
//            response.redirect("/redirect");
//            return "";
//            return "Zvolená metoda " + method;

//              response.header("redirectUrl", "https://www.seznam.cz/");
              return "redirect: https://www.seznam.cz";
        });

    }
}
