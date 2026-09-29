package com.zh.controller;

import com.zh.domain.dto.Result;
import com.zh.service.IDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板汇总 前端控制器
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private IDashboardService dashboardService;

    @GetMapping("/summary")
    public Result summary() {
        return dashboardService.getSummary();
    }
}
