package webserver;

import db.DataBase;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

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
            HttpRequest httpRequest = new HttpRequest(in);
            HttpResponse httpResponse = new HttpResponse(out);

            String method = httpRequest.getMethod();
            String requestUrl = httpRequest.getRequestUrl();
            String cookie = httpRequest.getHeader("Cookie");
            String strBody = httpRequest.getBody();

            if (method.equals("GET")) {
                if (requestUrl.contains("/user/list")) {
                    Map<String, String> cookies = HttpRequestUtils.parseCookies(cookie);
                    if (cookies.get("logined").equals("true")) {
                        String userListHtml = makeUserListHtml(DataBase.findAll());
                        httpResponse.forwardStr(userListHtml);
                    } else {
                        httpResponse.forward("/user/login.html");
                    }
                } else if (requestUrl.contains(".html")) {
                    httpResponse.forward(requestUrl);
                } else if (requestUrl.contains(".css")) {
                    httpResponse.forward(requestUrl);
                } else {
                    httpResponse.forwardStr("hello world");
                }
            } else if (method.equals("POST")) {
                if (requestUrl.contains("/user/create")) {
                    log.info("body:{}", strBody);
                    User user = new User(httpRequest.getParameter("userId"),
                            httpRequest.getParameter("password"),
                            httpRequest.getParameter("name"),
                            httpRequest.getParameter("email"));
                    log.info("user:{}", user);
                    DataBase.addUser(user);
                    httpResponse.sendRedirect("http://localhost:8080/index.html");
                } else if (requestUrl.contains("/user/login")) {
                    log.info("body:{}", strBody);
                    String userId = httpRequest.getParameter("userId");
                    String password = httpRequest.getParameter("password");
                    User userById = DataBase.findUserById(userId);
                    if (userById != null && userById.getPassword().equals(password)) {
                        log.info("login success");
                        httpResponse.addHeader("Set-Cookie", "logined=true");
                        httpResponse.sendRedirect("http://localhost:8080/index.html");
                    } else {
                        log.info("login fail");
                        httpResponse.addHeader("Set-Cookie", "logined=false");
                        httpResponse.sendRedirect("http://localhost:8080/user/login_failed.html");
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


}
