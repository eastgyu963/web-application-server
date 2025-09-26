package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {

    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            InputStreamReader r = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(r);
            String firstLine = br.readLine();
            log.info("first line:{}", firstLine);

            String[] split = firstLine.split(" ");
            String method = split[0];
            String requestUrl = split[1];
            log.info("method:{}", method);
            log.info("request url:{}", requestUrl);

            if (method.equals("GET")) {
                if (requestUrl.equals("/index.html")) {
                    String indexHtml = readIndexHtml();

                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = indexHtml.getBytes();
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else if (requestUrl.equals("/user/form.html")) {
                    String userForm = readUserForm();

                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = userForm.getBytes();
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else {
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = "Hello World".getBytes();
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                }
            } else if (method.equals("POST")) {
                if (requestUrl.contains("/user/create")) {
                    String contentLength = "";
                    while (true) {
                        String line = br.readLine();
                        log.info("line:{}", line);
                        if (line.isEmpty()) {
                            break;
                        }
                        if (line.contains("Content-Length")) {
                            int i = line.indexOf(":");
                            contentLength = line.substring(i + 2);
                        }
                    }
                    String body = IOUtils.readData(br, Integer.parseInt(contentLength));
                    log.info("body:{}", body);
                    Map<String, String> stringMap = HttpRequestUtils.parseQueryString(body);
                    User user = new User(stringMap.get("userId"), stringMap.get("password"),
                            stringMap.get("name"),
                            stringMap.get("email"));
                    log.info("user:{}", user);
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String readIndexHtml() {
        try {
            FileInputStream fis = new FileInputStream(
                    "C:\\study\\web-application-server\\webapp\\index.html");
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

    private String readUserForm() {
        try {
            FileInputStream fis = new FileInputStream(
                    "C:\\study\\web-application-server\\webapp\\user\\form.html");
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
}
