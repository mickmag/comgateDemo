package cz.styrax.comgate;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.afterAfter;
import static spark.Spark.post;
import static spark.Spark.get;
import static spark.Spark.staticFileLocation;

/**
 *
 * @author michal.bokr
 */
public class ServerApp {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
//    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    public void initialize() {

        // Serve static files from src/main/resources/public
        staticFileLocation("/public");

        // Log all requests and responses
        afterAfter(new LoggingFilter());

        post("/pay", (request, response) -> {
//            final String[] queryParamsValues = request.queryParamsValues("method");
//            final String method = queryParamsValues[0];
//            LOGGER.debug("/pay - method: " + method);
            String redirect = postToComgate();
            if (redirect != null) {
                response.redirect(redirect);
                return "";
            }
            // response.redirect("/redirect");
//            response.redirect("https://www.seznam.cz");
            return "Finished";
        });

        get("/redirect", (request, response) -> {
            LOGGER.debug("here");
            return "";
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

    public String postToComgate() {
        String redirectUrl = null;
        try {
            HttpPost post = new HttpPost("https://payments.comgate.cz/v1.0/create");

            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("merchant", "147605"));
            urlParameters.add(new BasicNameValuePair("test", "true"));
//            urlParameters.add(new BasicNameValuePair("country", "CZ")); //N
            urlParameters.add(new BasicNameValuePair("price", "15900"));
            urlParameters.add(new BasicNameValuePair("curr", "CZK"));
            urlParameters.add(new BasicNameValuePair("label", "test produkt"));
            urlParameters.add(new BasicNameValuePair("refId", "referenci ID"));
//            urlParameters.add(new BasicNameValuePair("payerId", "payer ID")); //N
            urlParameters.add(new BasicNameValuePair("method", "ALL"));
//            urlParameters.add(new BasicNameValuePair("account", "ALL")); //N
            urlParameters.add(new BasicNameValuePair("email", "michal.bokr@styrax.cz")); // email platce pro reklamaci
//            urlParameters.add(new BasicNameValuePair("phone", "phone")); //N 
//            urlParameters.add(new BasicNameValuePair("name", "identifikator produktu")); //N
//            urlParameters.add(new BasicNameValuePair("lang", "cs")); //N
            urlParameters.add(new BasicNameValuePair("prepareOnly", "true"));
            urlParameters.add(new BasicNameValuePair("secret", "JiDA57jXvy5FGtcMQ2K5k0iQFifuM96A"));
//            urlParameters.add(new BasicNameValuePair("preauth", "true")); //N
//            urlParameters.add(new BasicNameValuePair("initRecurring", "false")); //N
//            urlParameters.add(new BasicNameValuePair("verification", "false")); //N
//            urlParameters.add(new BasicNameValuePair("eetReport", "false")); //N
//            urlParameters.add(new BasicNameValuePair("eetData", "false")); //N

            post.setEntity(new UrlEncodedFormEntity(urlParameters));

            CloseableHttpClient httpClient = HttpClients.createDefault();
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                final HttpEntity entity = response.getEntity();
                final String responseBody = EntityUtils.toString(entity);
                LOGGER.debug("Comgate response: " + responseBody);
                LOGGER.debug(">>> Print params <<<");
                final Map<String, List<String>> splitQuery = splitQuery(responseBody);
                splitQuery.entrySet().forEach((entry) -> {
                    String key = entry.getKey();
                    List<String> value = entry.getValue();
                    LOGGER.debug(key + " : " + value.get(0));
                });
                               
                final List<String> redirectList = splitQuery.get("redirect");
                if (redirectList != null) {
                    final String redirectParam = redirectList.get(0);
                    if (redirectParam != null && !redirectParam.isEmpty()) {
                        redirectUrl = redirectParam;
                    }
                }
                EntityUtils.consume(entity);                
                return redirectUrl;
            }
        } catch (Exception ex) {
            LOGGER.error("Post entity", ex);
            return redirectUrl;
        }
    }

    public Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
        if (query== null || query.isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(query.split("&"))
                .map(this::splitQueryParameter)
                .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    public SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        try {                    
            final int idx = it.indexOf("=");
            final String key = idx > 0 ? it.substring(0, idx) : it;
            final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
            return new SimpleImmutableEntry<>(
                    URLDecoder.decode(key, "UTF-8"),
                    URLDecoder.decode(value, "UTF-8")
            );
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Decoding problem", e);
        }
        return new SimpleImmutableEntry<>("","");
    }

}
