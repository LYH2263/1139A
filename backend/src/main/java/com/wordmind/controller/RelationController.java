package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.MindMapDTO;
import com.wordmind.security.UserPrincipal;
import com.wordmind.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/relations")
public class RelationController {
    
    @Autowired
    private RelationService relationService;
    
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<MindMapDTO.RelationEdge> createRelation(
            @Valid @RequestBody MindMapDTO.RelationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ApiResponse.success(relationService.createRelationByUser(request, userPrincipal.getId()));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> deleteRelation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        relationService.deleteRelationByUser(id, userPrincipal.getId());
        return ApiResponse.success();
    }
}
