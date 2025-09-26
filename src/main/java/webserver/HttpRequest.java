package webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class HttpRequest {

    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);
    private final InputStream is;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> parameter = new HashMap<>();
    private String method;
    private String requestUrl;
    private String body;

    public HttpRequest(InputStream is) {
        this.is = is;
        try {
            parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void parse() throws IOException {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        log.info(
                "\n****************************************************************************************************************");
        String firstLine = br.readLine();
        log.info("firstLine: {}", firstLine);

        String[] split = firstLine.split(" ");
        method = split[0];
        requestUrl = split[1];
        log.info("method: {}", method);
        log.info("requestUrl: {}", requestUrl);

        String header = "";
        while (!(header = br.readLine()).isEmpty()) {
            log.info("header: {}", header);
            int i = header.indexOf(":");
            String key = header.substring(0, i);
            String value = header.substring(i + 2);
            headers.put(key, value);
        }
        log.info("headers: {}", headers);
        if (method.equals("POST")) {
            body = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
            parameter = HttpRequestUtils.parseQueryString(body);
            requestUrl = split[1];
            log.info("post parameter: {}", parameter);
        } else if (method.equals("GET")) {
            int i = requestUrl.indexOf("?");
            if (i != -1) {
                String url = requestUrl.substring(0, i);
                String queryString = requestUrl.substring(i + 1);
                log.info("url: {}", url);
                log.info("queryString: {}", queryString);
                requestUrl = url;
                parameter = HttpRequestUtils.parseQueryString(queryString);
                log.info("get parameter: {}", parameter);
            } else {
                log.info("no queryparameter");
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getBody() {
        return body;
    }

    public String getHeader(String headerKey) {
        return headers.get(headerKey);
    }

    public String getParameter(String parameterKey) {
        return parameter.get(parameterKey);
    }
}
