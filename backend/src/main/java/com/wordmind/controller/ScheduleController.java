package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.ScheduleDTO;
import com.wordmind.security.UserPrincipal;
import com.wordmind.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.Response> createSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ScheduleDTO.CreateRequest request) {
        return ApiResponse.success(scheduleService.createSchedule(userPrincipal.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.Response> updateSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody ScheduleDTO.UpdateRequest request) {
        return ApiResponse.success(scheduleService.updateSchedule(userPrincipal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> deleteSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        scheduleService.deleteSchedule(userPrincipal.getId(), id);
        return ApiResponse.success(null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.ListResponse> getSchedules(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(scheduleService.getSchedules(userPrincipal.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.DetailResponse> getScheduleDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ApiResponse.success(scheduleService.getScheduleDetail(userPrincipal.getId(), id));
    }

    @GetMapping("/{id}/today")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.TodayResponse> getTodaySchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ApiResponse.success(scheduleService.getTodaySchedule(userPrincipal.getId(), id));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<ScheduleDTO.TodayResponse>> getTodayAllSchedules(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(scheduleService.getTodayAllSchedules(userPrincipal.getId()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.TodayResponse> completeToday(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestBody ScheduleDTO.CompleteRequest request) {
        return ApiResponse.success(scheduleService.completeToday(userPrincipal.getId(), id, request));
    }
}
