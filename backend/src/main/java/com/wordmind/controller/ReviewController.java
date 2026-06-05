package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.ReviewDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ReviewDTO.TodayResponse> getTodayReviews(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(reviewService.getTodayReviews(userId));
    }
    
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ReviewDTO.SubmitResponse> submitReview(
            HttpServletRequest request,
            @Valid @RequestBody ReviewDTO.SubmitRequest submitRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(reviewService.submitReview(userId, submitRequest));
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ReviewDTO.SessionStats> getSessionStats(
            HttpServletRequest request,
            @RequestParam String sessionId) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(reviewService.getSessionStats(userId, sessionId));
    }
    
    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ReviewDTO.SettingsResponse> getSettings(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(reviewService.getSettings(userId));
    }
    
    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ReviewDTO.SettingsResponse> saveSettings(
            HttpServletRequest request,
            @Valid @RequestBody ReviewDTO.SettingsRequest settingsRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(reviewService.saveSettings(userId, settingsRequest));
    }
    
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
