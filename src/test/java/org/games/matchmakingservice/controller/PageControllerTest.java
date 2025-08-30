package org.games.matchmakingservice.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PageControllerTest {

    private final PageController controller = new PageController();

    @Test
    void home_ReturnsDemoPage() {
        String result = controller.home();
        assertEquals("demo.html", result);
    }

    @Test
    void login_ReturnsLoginPage() {
        String result = controller.login();
        assertEquals("login.html", result);
    }
}
