package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.WordBookDTO;
import com.wordmind.entity.Word;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.WordBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/wordbooks")
public class WordBookController {

    @Autowired
    private WordBookService wordBookService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<WordBookDTO.ListResponse> getAllWordBooks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(wordBookService.getAllWordBooks(keyword, difficulty, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<WordBookDTO.Response> getWordBookById(@PathVariable Long id) {
        return ApiResponse.success(wordBookService.getWordBookById(id));
    }

    @GetMapping("/{id}/words")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<Word>> getWordBookWords(@PathVariable Long id) {
        return ApiResponse.success(wordBookService.getWordBookWords(id));
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> enrollWordBook(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        wordBookService.addWordBookToUser(userId, id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/enroll")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> unenrollWordBook(HttpServletRequest request, @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        wordBookService.removeWordBookFromUser(userId, id);
        return ApiResponse.success();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<WordBookDTO.UserWordBookResponse>> getMyWordBooks(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(wordBookService.getUserWordBooks(userId));
    }

    @GetMapping("/my/progress")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<WordBookDTO.ProgressResponse>> getMyWordBookProgress(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(wordBookService.getWordBookProgress(userId));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
