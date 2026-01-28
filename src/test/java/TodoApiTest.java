import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;

public class TodoApiTest {
    Playwright playwright;
    APIRequestContext requestContext;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        requestContext = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL("https://jsonplaceholder.typicode.com")
        );
    }

    @Test
    void testGetTodos() throws IOException {
        // Выполнение GET-запроса напрямую через API
        APIResponse response = requestContext.get("/todos/1");
        
        // Проверка статуса
        assertEquals(200, response.status());
        System.out.println("Статус ответа: " + response.status());
        
        // Проверка заголовков
        assertTrue(response.headers().containsKey("content-type"));
        assertEquals("application/json; charset=utf-8", response.headers().get("content-type"));
        System.out.println("Content-Type: " + response.headers().get("content-type"));
        
        // Парсинг JSON
        String responseBody = response.text();
        System.out.println("Полный ответ: " + responseBody);
        
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        
        // Проверка структуры JSON
        assertTrue(jsonNode.has("userId"), "Ответ должен содержать поле userId");
        assertTrue(jsonNode.has("id"), "Ответ должен содержать поле id");
        assertTrue(jsonNode.has("title"), "Ответ должен содержать поле title");
        assertTrue(jsonNode.has("completed"), "Ответ должен содержать поле completed");
        
        // Проверка типов данных
        assertTrue(jsonNode.get("userId").isInt(), "userId должен быть числом");
        assertTrue(jsonNode.get("id").isInt(), "id должен быть числом");
        assertTrue(jsonNode.get("title").isTextual(), "title должен быть строкой");
        assertTrue(jsonNode.get("completed").isBoolean(), "completed должен быть boolean");
        
        // Проверка конкретных значений
        assertEquals(1, jsonNode.get("id").asInt());
        System.out.println("ID задачи: " + jsonNode.get("id").asInt());
        System.out.println("Заголовок: " + jsonNode.get("title").asText());
        System.out.println("Выполнено: " + jsonNode.get("completed").asBoolean());
    }

    @Test
    void testGetAllTodos() throws IOException {
        // Получение всех задач
        APIResponse response = requestContext.get("/todos");
        
        // Проверка статуса
        assertEquals(200, response.status());
        
        // Парсинг JSON массива
        String responseBody = response.text();
        JsonNode jsonArray = objectMapper.readTree(responseBody);
        
        // Проверка что это массив
        assertTrue(jsonArray.isArray(), "Ответ должен быть массивом");
        assertTrue(jsonArray.size() > 0, "Массив не должен быть пустым");
        
        // Проверка первой задачи в массиве
        JsonNode firstTodo = jsonArray.get(0);
        assertNotNull(firstTodo.get("id"));
        assertNotNull(firstTodo.get("title"));
        
        System.out.println("Всего задач: " + jsonArray.size());
        System.out.println("Первая задача: " + firstTodo.toPrettyString());
    }

    @Test
    void testCreateTodo() throws IOException {
        // Создание новой задачи
        Map<String, Object> todoData = new HashMap<>();
        todoData.put("userId", 1);
        todoData.put("title", "Новая задача из теста");
        todoData.put("completed", false);
        
        // Отправляем запрос
        APIResponse response = requestContext.post("/todos",
                RequestOptions.create().setData(todoData));
        
        // Проверка статуса создания (201 Created)
        assertEquals(201, response.status());
        
        // Парсинг ответа
        String responseBody = response.text();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        
        // Проверка структуры созданной задачи
        assertTrue(jsonNode.has("id"));
        assertEquals("Новая задача из теста", jsonNode.get("title").asText());
        assertEquals(false, jsonNode.get("completed").asBoolean());
        
        System.out.println("Создана задача с ID: " + jsonNode.get("id").asInt());
        System.out.println("Ответ: " + jsonNode.toPrettyString());
    }

    @Test
    void testUpdateTodo() throws IOException {
        // Обновление существующей задачи
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("title", "Обновленный заголовок");
        updateData.put("completed", true);
        
        // Отправляем запрос
        APIResponse response = requestContext.put("/todos/1",
                RequestOptions.create().setData(updateData));
        
        // Проверка статуса
        assertEquals(200, response.status());
        
        // Парсинг ответа
        String responseBody = response.text();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        
        // Проверка обновленных данных
        assertEquals("Обновленный заголовок", jsonNode.get("title").asText());
        assertEquals(true, jsonNode.get("completed").asBoolean());
        
        System.out.println("Задача обновлена: " + jsonNode.toPrettyString());
    }

    @Test
    void testDeleteTodo() {
        // Удаление задачи
        APIResponse response = requestContext.delete("/todos/1");
        
        // Проверка статуса
        assertTrue(response.status() == 200 || response.status() == 204,
                "Статус должен быть 200 OK или 204 No Content");
        
        System.out.println("Задача удалена. Статус: " + response.status());
    }

    @Test
    void testInvalidTodo() {
        // Тест на несуществующую задачу
        APIResponse response = requestContext.get("/todos/9999");
        
        // Проверка статуса 404 Not Found
        assertEquals(404, response.status());
        
        System.out.println("Несуществующая задача: статус " + response.status());
    }

    @Test
    void testTodoStructure() throws IOException {
        // Проверка структуры ответа с дополнительными проверками
        APIResponse response = requestContext.get("/todos/1");
        
        // Парсинг JSON
        JsonNode jsonNode = objectMapper.readTree(response.text());
        
        // Проверка всех полей
        assertNotNull(jsonNode.get("userId"));
        assertNotNull(jsonNode.get("id"));
        assertNotNull(jsonNode.get("title"));
        assertNotNull(jsonNode.get("completed"));
        
        // Проверка значений в допустимых диапазонах
        int userId = jsonNode.get("userId").asInt();
        assertTrue(userId >= 1 && userId <= 10, "userId должен быть от 1 до 10");
        
        int id = jsonNode.get("id").asInt();
        assertEquals(1, id, "id должен быть 1 для эндпоинта /todos/1");
        
        String title = jsonNode.get("title").asText();
        assertFalse(title.isEmpty(), "title не должен быть пустым");
        
        System.out.println("Структура JSON проверена успешно!");
        System.out.println("userId: " + userId);
        System.out.println("id: " + id);
        System.out.println("title: " + title);
        System.out.println("completed: " + jsonNode.get("completed").asBoolean());
    }

    @AfterEach
    void tearDown() {
        if (requestContext != null) {
            requestContext.dispose();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}