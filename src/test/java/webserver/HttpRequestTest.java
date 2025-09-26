package webserver;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.junit.Test;

public class HttpRequestTest {

    private String testDirectory = "./src/test/resources/";

    @Test
    public void request_GET() throws Exception {
        InputStream in = new FileInputStream(new File(testDirectory + "Http_Get.txt"));
        HttpRequest httpRequest = new HttpRequest(in);
        assertEquals("GET", httpRequest.getMethod());
        assertEquals("/user/create", httpRequest.getRequestUrl());
        assertEquals("keep-alive", httpRequest.getHeader("Connection"));
        assertEquals("ab", httpRequest.getParameter("userId"));

    }
}