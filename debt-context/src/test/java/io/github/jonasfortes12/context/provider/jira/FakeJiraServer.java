package io.github.jonasfortes12.context.provider.jira;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** Local stand-in for a Jira site: answers every request with one fixed status and body. */
final class FakeJiraServer implements AutoCloseable {

    private final HttpServer server;
    private final int status;
    private final String response;
    private volatile HttpExchange exchange;

    FakeJiraServer(int status, String response) throws IOException {
        this.status = status;
        this.response = response;
        this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        this.server.createContext("/", this::handle);
        this.server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        this.exchange = exchange;
        if (status >= 300 && status < 400) {
            exchange.getResponseHeaders().add("Location", endpoint() + "/login.jsp");
        }
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    String endpoint() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    String requestPath() {
        return exchange.getRequestURI().getPath();
    }

    String query() {
        return exchange.getRequestURI().getQuery();
    }

    String header(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
