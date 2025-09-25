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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            String firstLine = br.readLine(); //첫번째 라인만 읽기
            log.info("first line:{}", firstLine);
            if (firstLine != null) {
                String[] split = firstLine.split(" ");
                log.info("request url:{}", split[1]);
                if (split[1].equals("/index.html")) {
                    log.info("파일스트림 생성 시작");
                    FileInputStream fis = new FileInputStream(
                            "C:\\study\\web-application-server\\webapp\\index.html");
                    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                    BufferedReader br2 = new BufferedReader(isr);
                    log.info("파일스트림 생성 완료");
                    String s;
                    String indexFile = "";
                    while ((s = br2.readLine()) != null) {
                        log.info("s:{}", s);
                        indexFile = indexFile + s;
                    }
                    log.info("indexFile:{}", indexFile);
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] body = indexFile.getBytes();
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                }
            } else {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = "Hello World".getBytes();
                response200Header(dos, body.length);
                responseBody(dos, body);
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
}
