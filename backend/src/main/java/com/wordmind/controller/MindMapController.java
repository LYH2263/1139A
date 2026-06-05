package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.MindMapDTO;
import com.wordmind.security.UserPrincipal;
import com.wordmind.service.MindMapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mindmap")
public class MindMapController {
    
    @Autowired
    private MindMapService mindMapService;
    
    @GetMapping("/{wordId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<MindMapDTO.Response> getMindMap(
            @PathVariable Long wordId,
            @RequestParam(defaultValue = "1") int depth) {
        return ApiResponse.success(mindMapService.getMindMap(wordId, depth));
    }
    
    @GetMapping("/{wordId}/learning-path")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<MindMapDTO.LearningPathResponse> getLearningPath(
            @PathVariable Long wordId,
            @RequestParam(defaultValue = "2") int depth,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(mindMapService.getLearningPath(wordId, depth, userPrincipal.getId()));
    }
}
