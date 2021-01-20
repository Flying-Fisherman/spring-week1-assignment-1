package com.codesoom.assignment.web;

import com.codesoom.assignment.application.TaskApplicationService;
import com.codesoom.assignment.application.TaskJsonTransfer;
import com.codesoom.assignment.domain.Task;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Optional;
import java.util.stream.Collectors;

public class WebHandler implements HttpHandler {
    TaskApplicationService taskApplicationService;
    TaskJsonTransfer transfer;

    public WebHandler(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
        this.transfer = new TaskJsonTransfer();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        System.out.println(path);

        if (method.equals("GET")) {
            if (path.equals("/tasks")) {
                String content = transfer.taskListToJson(taskApplicationService.getAllTasks());
                setJsonToResponseBody(exchange, content, 200);
            } else if (path.contains("/tasks/")) {
                Long taskId = parsePathToTaskId(path);

                Optional<Task> task = taskApplicationService.findTask(taskId);

                if (task.isEmpty()) {
                    sendNotFoundError(exchange);
                    return;
                }
                String content = transfer.taskToJson(task.get());
                setJsonToResponseBody(exchange, content, 200);
            } else {
                exchange.sendResponseHeaders(200, 0);
            }
        } else if (method.equals("POST")) {
            if (path.equals("/tasks")) {
                String requestBody = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()))
                        .lines()
                        .collect(Collectors.joining(""));
                Task requestTask = transfer.jsonStringToTask(requestBody);

                Long taskId = taskApplicationService.createTask(requestTask.getTitle());
                sendUpdatedTaskResult(exchange, taskId, 201);
            }
        } else if (method.equals("PUT")) {
            if (path.contains("/tasks")) {
                Long taskId = parsePathToTaskId(path);
                String requestBody = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody()))
                        .lines()
                        .collect(Collectors.joining(""));
                Task requestTask = transfer.jsonStringToTask(requestBody);

                Optional<Object> result = taskApplicationService.updateTaskTitle(taskId, requestTask.getTitle());

                if (result.isEmpty()) {
                    sendNotFoundError(exchange);
                    return;
                }
                sendUpdatedTaskResult(exchange, taskId, 200);
            }
        } else if (method.equals("DELETE")) {
            if (path.contains("/tasks")) {
                Long taskId = parsePathToTaskId(path);
                System.out.println(taskId);
                Optional<Object> result = taskApplicationService.deleteTask(taskId);

                if (result.isEmpty()) {
                    sendNotFoundError(exchange);
                    return;
                }
                exchange.sendResponseHeaders(204, 0);
            }
        } else {
            sendNotFoundError(exchange);
        }
    }

    private void setJsonToResponseBody(HttpExchange exchange, String content, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, content.getBytes().length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(content.getBytes());
        outputStream.flush();
    }

    private void sendUpdatedTaskResult(HttpExchange exchange, Long taskId, int statusCode) throws IOException {
        Optional<Task> task = taskApplicationService.findTask(taskId);
        if (task.isEmpty()) {
            sendNotFoundError(exchange);
            return;
        }
        String content = transfer.taskToJson(task.get());
        setJsonToResponseBody(exchange, content, statusCode);
    }

    private Long parsePathToTaskId(String path){
        String resourceId = path.split("/")[2];
        return (long) Integer.parseInt(resourceId);
    }

    private void sendNotFoundError(HttpExchange exchange) throws IOException {
        String content = "{\"reason\": \"Not Found\"}";
        setJsonToResponseBody(exchange, content, 404);
    }
}
