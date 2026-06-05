package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.WordBookDTO;
import com.wordmind.service.WordBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/wordbooks")
public class AdminWordBookController {

    @Autowired
    private WordBookService wordBookService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WordBookDTO.Response> createWordBook(@Valid @RequestBody WordBookDTO.CreateRequest request) {
        return ApiResponse.success(wordBookService.createWordBook(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WordBookDTO.Response> updateWordBook(
            @PathVariable Long id,
            @Valid @RequestBody WordBookDTO.UpdateRequest request) {
        return ApiResponse.success(wordBookService.updateWordBook(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteWordBook(@PathVariable Long id) {
        wordBookService.deleteWordBook(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/words")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> addWordsToWordBook(
            @PathVariable Long id,
            @RequestBody WordBookDTO.WordBatchRequest request) {
        wordBookService.addWordsToWordBook(id, request.getWordIds());
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/words")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> removeWordsFromWordBook(
            @PathVariable Long id,
            @RequestBody WordBookDTO.WordBatchRequest request) {
        wordBookService.removeWordsFromWordBook(id, request.getWordIds());
        return ApiResponse.success();
    }
}
