package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponse {

    private static final Logger log = LoggerFactory.getLogger(HttpResponse.class);
    private final OutputStream out;
    private final DataOutputStream dos;
    private Map<String, String> header = new HashMap<>();

    public HttpResponse(OutputStream out) {
        this.out = out;
        this.dos = new DataOutputStream(out);
    }

    public void forward(String url) {
        String strFile = readFile(url);
        if (url.contains(".html")) {
            byte[] body = strFile.getBytes();
            response200Header(body.length);
            responseBody(body);
        } else if (url.contains(".css")) {
            byte[] body = strFile.getBytes();
            response200CssHeader(body.length);
            responseBody(body);
        } else if (url.contains(".js")) {
            byte[] body = strFile.getBytes();
            response200CssHeader(body.length);
            responseBody(body);
        }
    }

    public void forwardStr(String str) {
        byte[] body = str.getBytes();
        response200Header(body.length);
        responseBody(body);
    }

    public void sendRedirect(String redirectUrl) {
        DataOutputStream dos = new DataOutputStream(out);
        response302Header(redirectUrl);
    }

    public void addHeader(String key, String value) {
        header.put(key, value);
    }

    private String readFile(String url) {
        try {
            FileInputStream fis = new FileInputStream(
                    "C:\\study\\web-application-server\\webapp" + url);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String s;
            String result = "";
            while ((s = br.readLine()) != null) {
                result = result + s;
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private void responseHeader() {
        try {
            for (String key : header.keySet()) {
                String value = header.get(key);
                dos.writeBytes(key + ": " + value + "\r\n");
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            responseHeader();
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200CssHeader(int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            responseHeader();

            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + url + "\r\n");
            responseHeader();

            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


}
