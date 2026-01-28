import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;

public class DynamicLoadingTest {
    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @Test
    void testDynamicLoading() throws ExecutionException, InterruptedException {
        // Создаем Playwright
        playwright = Playwright.create();
        
        // Запускаем браузер
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext();
        
        // Начинаем запись трассировки
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        
        page = context.newPage();
        
        // Создаем Future для проверки сетевого запроса
        CompletableFuture<Boolean> requestCompleted = new CompletableFuture<>();
        
        // Перехватываем сетевые запросы
        page.onResponse(response -> {
            String url = response.url();
            if (url.contains("dynamic_loading")) {
                int status = response.status();
                System.out.println("Запрос перехвачен: " + url + " - Статус: " + status);
                
                if (status == 200) {
                    requestCompleted.complete(true);
                }
            }
        });
        
        // Открываем страницу
        page.navigate("https://the-internet.herokuapp.com/dynamic_loading/1");
        
        // Нажимаем кнопку Start
        page.click("button");
        
        // Ждем появления текста "Hello World!"
        page.waitForSelector("#finish", new Page.WaitForSelectorOptions().setTimeout(10000));

        // Получаем текст
        Locator finishText = page.locator("#finish");
        String actualText = finishText.textContent();
        
        // Проверяем текст
        Assertions.assertEquals("Hello World!", actualText.trim());
        System.out.println("Текст найден: " + actualText.trim());
        
        // Проверяем, что сетевой запрос завершился успешно
        try {
            boolean requestSuccess = requestCompleted.get();
            Assertions.assertTrue(requestSuccess, "Запрос к /dynamic_loading не завершился успешно");
            System.out.println("Сетевой запрос успешно завершен со статусом 200");
        } catch (Exception e) {
            System.out.println("Предупреждение: Не удалось дождаться сетевого запроса");
        }
        
        // Сохраняем трассировку
        context.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("trace/trace-success.zip")));
        System.out.println("Трассировка сохранена в trace/trace-success.zip");
    }

    @AfterEach
    void tearDown() {
        if (page != null) {
            page.close();
        }
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}