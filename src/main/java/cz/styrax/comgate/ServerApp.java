package cz.styrax.comgate;

import cz.styrax.comgate.model.Item;
import cz.styrax.comgate.model.Order;
import cz.styrax.comgate.model.Payer;
import cz.styrax.comgate.model.Transaction;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 *
 * @author michal.bokr
 */
public class ServerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
    private static final String ERROR_HTML_FORMAT = "<div class=\"container\"><h1>Chyba</h1><div>Výpis chyby: {0}</div><a href=\"index.html\">Další platba</a></div>";

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
        // Initialize FreeMarkerEngine templates
        Locale csLocale = new Locale("cs", "CZ");
        Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_26);
        freeMarkerConfiguration.setEncoding(csLocale, "UTF-8");
        freeMarkerConfiguration.setTemplateLoader(new ClassTemplateLoader(ServerApp.class, "/templates/"));
        freeMarkerEngine = new FreeMarkerEngine(freeMarkerConfiguration);

        Locale.setDefault(csLocale);
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
                LOGGER.error("Error during communication with Comgate: ", ex);
                errorMsg = ex.getMessage();
            }
            response.type("text/html; charset=UTF-8");
            response.status(500);
            return MessageFormat.format(ERROR_HTML_FORMAT, errorMsg);
        });

        post("/status", (request, response) -> {
            LOGGER.debug(">>> Status - print params <<<");
            final Map<String, List<String>> queryParams = splitQuery(request.body());
            printQueryParams(queryParams);

            String refId = getParamFromQueryMap(queryParams, "refId");
            if (refId != null) {
                Order order = orders.get(refId);
                if (order != null) {
                    String transId = getParamFromQueryMap(queryParams, "transId");
                    String s = getParamFromQueryMap(queryParams, "status");
                    Transaction.Status status = Transaction.Status.valueOf(s.toUpperCase());
                    String fee = getParamFromQueryMap(queryParams, "fee");
                    Transaction transaction = order.getTransaction();
                    transaction.setFee(fee);
                    transaction.setStatus(status);
                    if (!isEmptyString(transId)) {
                        transaction.setTransId(transId);
                    }
                }
            }
            return "";
        });

        get("/result", (request, response) -> {
            Map<String, Object> attributes = new HashMap<>();

            String refId = request.queryParams("refId");
            if (!isEmptyString(refId)) {
                Order order = orders.get(refId);
                if (order != null) {
                    Transaction tran = order.getTransaction();
                    if (tran != null) {
                        attributes.put("status", tran.getStatus() != null ? tran.getStatus().getTranslation() : null);
                        attributes.put("tranId", tran.getTransId());
                    }
                    double price = Double.parseDouble(order.getItem().getPrice()) / 100;
                    attributes.put("price", MessageFormat.format("{0, number,#.00}", price));
                    attributes.put("curr", order.getItem().getCurrency());
                }
            }
            response.status(200);
            response.type("text/html; charset=UTF-8");
            return freeMarkerEngine.render(new ModelAndView(attributes, "result.html"));
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
                final Map<String, List<String>> queryMap = splitQuery(responseBody);
                printQueryParams(queryMap);
                String code = getParamFromQueryMap(queryMap, "code");
                String message = getParamFromQueryMap(queryMap, "message");
                if (code == null || !code.equals("0")) {
                    throw new ComgateResponseException(MessageFormat.format("Error in Comgate response: {0} {1}", code, message));
                }
                String transId = getParamFromQueryMap(queryMap, "transId");
                order.setTransaction(new Transaction(transId));
                redirectUrl = getParamFromQueryMap(queryMap, "redirect");
            }
            return redirectUrl;
        } finally {
            httpClient.close();
        }
    }

    public Map<String, List<String>> splitQuery(String query) {
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
        final Item item = new Item("15950", "CZK", "custom label");
        final Order order = new Order(refId, payer, item, "ALL");
        return order;
    }

    private void printQueryParams(Map<String, List<String>> splitQuery) {
        splitQuery.entrySet().forEach((entry) -> {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            LOGGER.debug(key + " : " + value.get(0));
        });
    }

    boolean isEmptyString(String string) {
        return string == null || string.isEmpty();
    }

    String getParamFromQueryMap(Map<String, List<String>> queryParams, String keyParam) {
        String result = null;
        if (queryParams.get(keyParam) != null) {
            final String param = queryParams.get(keyParam).get(0);
            if (!isEmptyString(param)) {
                result = param;
            }
        }
        return result;
    }
}
