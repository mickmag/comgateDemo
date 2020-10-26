package cz.styrax.comgate;

import cz.styrax.comgate.model.Item;
import cz.styrax.comgate.model.Order;
import cz.styrax.comgate.model.Payer;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import static spark.Spark.afterAfter;
import static spark.Spark.post;
import static spark.Spark.get;
import static spark.Spark.staticFileLocation;
import spark.template.freemarker.FreeMarkerEngine;

/**
 * Main application to comunicate with Comgate pay wall
 * @author michal.bokr
 */
public class ServerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
    private static final String ERROR_HTML_FORMAT = "<div class=\"container\"><h1>Chyba</h1><div>Výpis chyby: {0}</div><a href=\"index.html\">Další platba</a></div>";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, String> conf;
    private final ResponseHandler<String> responseHandler;
    private final Map<String, Order> orders;
    private final FreeMarkerEngine freeMarkerEngine;

    public ServerApp() {
        this.conf = new HashMap<>();
        this.orders = new HashMap<>();
        this.responseHandler = (HttpResponse response) -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };
        // Initialize FreeMarkerEngine
        Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_26);
        freeMarkerConfiguration.setTemplateLoader(new ClassTemplateLoader(ServerApp.class, "/templates/"));
        freeMarkerEngine = new FreeMarkerEngine(freeMarkerConfiguration);
    }

    public void initialize() {
        loadConfiguration();

        // Serve static files from src/main/resources/public
        staticFileLocation("/public");

        // Log all requests and responses
        afterAfter(new LoggingFilter());

        post("/pay", (request, response) -> {
//            final String[] queryParamsValues = request.queryParamsValues("method");
//            final String method = queryParamsValues[0];
//            LOGGER.debug("/pay - method: " + method);
            String errorMsg = "Chyba při placení";
            try {
                String redirect = postToComgate();
                if (redirect != null) {
                    response.redirect(redirect);
                    return "";
                }
            } catch (Exception ex) {
                LOGGER.error("Post entity", ex);
                errorMsg = ex.getMessage();
            }
            response.type("text/html; charset=UTF-8");
            response.status(500);
            return MessageFormat.format(ERROR_HTML_FORMAT, errorMsg);
        });
		

        post("/status", (request, response) -> {
            // TODO get data from request body as query params
            return "status joooo";
        });

        get("/result", (request, response) -> {     
            // TODO get data from query
            response.status(200);
            response.type("text/html; charset=UTF-8");
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("data", "hello world");
            return freeMarkerEngine.render(new ModelAndView(attributes, "test.html"));
        });
    }

    public String postToComgate() throws Exception {
        String redirectUrl = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost post = new HttpPost("https://payments.comgate.cz/v1.0/create");
            final Order order = createNewOrder();
            orders.put(order.getRefId(), order);
            
            
            // https://www.comgate.cz/cz/protokol-api
            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("merchant", conf.get("MERCHANT")));
            urlParameters.add(new BasicNameValuePair("test", conf.get("TEST")));
//            urlParameters.add(new BasicNameValuePair("country", "CZ")); //N
            urlParameters.add(new BasicNameValuePair("price", order.getItem().getPrice()));
            urlParameters.add(new BasicNameValuePair("curr", order.getItem().getCurrency()));
            urlParameters.add(new BasicNameValuePair("label", order.getItem().getLabel()));
            urlParameters.add(new BasicNameValuePair("refId", order.getRefId()));
//            urlParameters.add(new BasicNameValuePair("payerId", "payer ID")); //N
            urlParameters.add(new BasicNameValuePair("method", order.getMethod()));
//            urlParameters.add(new BasicNameValuePair("account", "ALL")); //N
            urlParameters.add(new BasicNameValuePair("email", order.getPayer().getEmail())); // email platce pro reklamaci
//            urlParameters.add(new BasicNameValuePair("phone", "phone")); //N 
//            urlParameters.add(new BasicNameValuePair("name", "identifikator produktu")); //N
//            urlParameters.add(new BasicNameValuePair("lang", "cs")); //N
            urlParameters.add(new BasicNameValuePair("prepareOnly", "true"));
            urlParameters.add(new BasicNameValuePair("secret", conf.get("SECRET")));
//            urlParameters.add(new BasicNameValuePair("preauth", "true")); //N
//            urlParameters.add(new BasicNameValuePair("initRecurring", "false")); //N
//            urlParameters.add(new BasicNameValuePair("verification", "false")); //N
//            urlParameters.add(new BasicNameValuePair("eetReport", "false")); //N
//            urlParameters.add(new BasicNameValuePair("eetData", "false")); //N

            post.setEntity(new UrlEncodedFormEntity(urlParameters));
            String responseBody = httpClient.execute(post, responseHandler);
            if (responseBody != null) {
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
            }
//                EntityUtils.consume(entity);
            return redirectUrl;
        } finally {
            httpClient.close();
        }
    }

    public Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
        if (query == null || query.isEmpty()) {
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
        return new SimpleImmutableEntry<>("", "");
    }

    public static BigInteger getNewRefId() {
        return new BigInteger(60, RANDOM);
    }

    private void loadConfiguration() {
        Dotenv dotenv = Dotenv.configure().directory("./conf/.env").load();
        conf.put("MERCHANT", dotenv.get("MERCHANT"));
        conf.put("TEST", dotenv.get("TEST"));
        conf.put("SECRET", dotenv.get("SECRET"));
    }
    
    private Order createNewOrder() {
        UUID uuid = UUID.randomUUID();
        String refId = uuid.toString();
        final Payer payer = new Payer("michal.bokr@styrax.cz");
        final Item item = new Item("15900", "CZK", "custom label");
        final Order order = new Order(refId, payer, item, "ALL");
        return order;                
    }
}
