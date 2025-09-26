package webserver;

import db.DataBase;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
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
            log.info("\n******************************************************");
            log.info("first line:{}", firstLine);
            if (firstLine == null) {
                return;
            }
            String[] split = firstLine.split(" ");
            String method = split[0];
            String requestUrl = split[1];
            log.info("method: {}", method);
            log.info("request url: {}", requestUrl);

            //헤더 읽는 부분
            String header = "";
            int contentLength = 0;
            String cookie = "";
            while (!(header = br.readLine()).isEmpty()) {
                log.info("header: {}", header);
                if (header.contains("Content-Length")) {
                    int i = header.indexOf(":");
                    contentLength = Integer.parseInt(header.substring(i + 2));
                } else if (header.contains("Cookie")) {
                    int i = header.indexOf(":");
                    cookie = header.substring(i + 2);
                }
            }
            log.info("contentLength: {}", contentLength);
            log.info("cookie: {}", cookie);
            //헤더 읽기 끝

            if (method.equals("GET")) {
                if (requestUrl.contains(".html")) {
                    String html = readHtml(requestUrl);

                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = html.getBytes();
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else if (requestUrl.contains(".css")) {
                    String css = readCss(requestUrl);

                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = css.getBytes();
                    response200CssHeader(dos, body.length);
                    responseBody(dos, body);
                } else if (requestUrl.contains("/user/list")) {
                    Map<String, String> stringMap = HttpRequestUtils.parseCookies(cookie);

                    if (stringMap.get("logined").equals("true")) {
                        String userListHtml = makeUserListHtml(DataBase.findAll());
                        byte[] body = userListHtml.toString().getBytes();
                        DataOutputStream dos = new DataOutputStream(out);
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    } else {
                        String loginForm = readHtml("/user/login.html");
                        byte[] body = loginForm.getBytes();
                        DataOutputStream dos = new DataOutputStream(out);
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    }

                } else {
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = "Hello World".getBytes();
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                }
            } else if (method.equals("POST")) {
                if (requestUrl.contains("/user/create")) {
                    String body = IOUtils.readData(br, contentLength);
                    log.info("body:{}", body);
                    Map<String, String> stringMap = HttpRequestUtils.parseQueryString(body);
                    User user = new User(stringMap.get("userId"), stringMap.get("password"),
                            stringMap.get("name"),
                            stringMap.get("email"));
                    log.info("user:{}", user);
                    DataBase.addUser(user);
                    DataOutputStream dos = new DataOutputStream(out);
                    response302Header(dos, "http://localhost:8080/index.html");
                } else if (requestUrl.contains("/user/login")) {
                    String body = IOUtils.readData(br, contentLength);
                    log.info("body:{}", body);
                    Map<String, String> stringMap = HttpRequestUtils.parseQueryString(body);
                    String userId = stringMap.get("userId");
                    String password = stringMap.get("password");
                    User userById = DataBase.findUserById(userId);
                    if (userById != null && userById.getPassword().equals(password)) {
                        log.info("login success");
                        DataOutputStream dos = new DataOutputStream(out);
                        response302HeaderLoginSuccess(dos, "http://localhost:8080/index.html");
                    } else {
                        log.info("login fail");
                        DataOutputStream dos = new DataOutputStream(out);
                        response302HeaderLoginFail(dos,
                                "http://localhost:8080/user/login_failed.html");

                    }
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String makeUserListHtml(Collection<User> users) {
        StringBuilder listHtml = new StringBuilder();
        listHtml.append(
                "<!DOCTYPE html><html><head><title>user list</title></head><body>");
        listHtml.append("<table><tr><th>userId</th><th>name</th></tr>");
        Collection<User> Users = DataBase.findAll();
        for (User user : Users) {
            log.info("user:{}", user);
            listHtml.append(
                    "<tr><td>" + user.getUserId() + "</td>" +
                            "<td>" + user.getName() + "</td></tr>");
        }
        listHtml.append("</table></body></html>");
        return listHtml.toString();
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

    private void response200CssHeader(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + url + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderLoginSuccess(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + url + "\r\n");
            dos.writeBytes("Set-Cookie: logined=true");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderLoginFail(DataOutputStream dos, String url) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + url + "\r\n");
            dos.writeBytes("Set-Cookie: logined=false");
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

    private String readHtml(String url) {
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

    private String readCss(String url) {
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
}
