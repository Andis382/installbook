package io.github.andis382.installbook.dashboard;

import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.dashboard.DashboardService.Dashboard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboard;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService dashboard, CurrentUser currentUser) {
        this.dashboard = dashboard;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/dashboard")
    public Dashboard today() {
        return dashboard.build(currentUser.organization());
    }
}
