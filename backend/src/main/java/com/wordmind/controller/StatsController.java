package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.StatsDTO;
import com.wordmind.dto.StudyPlanDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StatsController {
    
    @Autowired
    private StatsService statsService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @GetMapping("/stats/me")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<StatsDTO.Response> getMyStats(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(statsService.getUserStats(userId));
    }

    @PostMapping("/stats/report")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<StatsDTO.ReportResponse> generateReport(
            HttpServletRequest request,
            @Valid @RequestBody StatsDTO.ReportRequest reportRequest) {
        Long userId = getUserIdFromRequest(request);
        LocalDateTime[] dateRange = parseDateRange(reportRequest);
        return ApiResponse.success(statsService.generateReport(userId, dateRange[0], dateRange[1]));
    }

    @PostMapping("/stats/share")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<StatsDTO.ShareResponse> createShare(
            HttpServletRequest request,
            @Valid @RequestBody StatsDTO.ShareRequest shareRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(statsService.createShareToken(userId, shareRequest));
    }

    @GetMapping("/public/report/{token}")
    public ApiResponse<StatsDTO.PublicReportResponse> getPublicReport(@PathVariable String token) {
        return ApiResponse.success(statsService.getPublicReport(token));
    }
    
    @PostMapping("/study-plans")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<StudyPlanDTO.Response> createStudyPlan(
            HttpServletRequest request,
            @Valid @RequestBody StudyPlanDTO.CreateRequest createRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(statsService.createStudyPlan(userId, createRequest));
    }
    
    @GetMapping("/study-records")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<StudyPlanDTO.Response>> getStudyRecords(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(statsService.getUserStudyPlans(userId));
    }

    private LocalDateTime[] parseDateRange(StatsDTO.ReportRequest request) {
        LocalDateTime startDate;
        LocalDateTime endDate;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        switch (request.getTimeRange()) {
            case "WEEK":
                LocalDate now = LocalDate.now();
                startDate = LocalDateTime.of(now.with(java.time.DayOfWeek.MONDAY), LocalTime.MIN);
                endDate = LocalDateTime.of(now.with(java.time.DayOfWeek.SUNDAY), LocalTime.MAX);
                break;
            case "MONTH":
                LocalDate monthNow = LocalDate.now();
                startDate = LocalDateTime.of(monthNow.withDayOfMonth(1), LocalTime.MIN);
                endDate = LocalDateTime.of(monthNow.withDayOfMonth(monthNow.lengthOfMonth()), LocalTime.MAX);
                break;
            case "CUSTOM":
                if (request.getStartDate() == null || request.getEndDate() == null) {
                    throw new IllegalArgumentException("自定义时间范围需要提供开始和结束日期");
                }
                startDate = LocalDateTime.of(LocalDate.parse(request.getStartDate(), dateFormatter), LocalTime.MIN);
                endDate = LocalDateTime.of(LocalDate.parse(request.getEndDate(), dateFormatter), LocalTime.MAX);
                break;
            default:
                throw new IllegalArgumentException("不支持的时间范围类型");
        }

        return new LocalDateTime[]{startDate, endDate};
    }
    
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
