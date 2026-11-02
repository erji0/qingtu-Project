package com.qingtu.controller;

import com.qingtu.common.RequireRole;
import com.qingtu.common.Result;
import com.qingtu.common.Roles;
import com.qingtu.dto.SuggestionVO;
import com.qingtu.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dispatch")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminDispatchController {

    private final DispatchService dispatchService;

    @PostMapping("/generate")
    public Result<Integer> generate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dispatchService.generate(date));
    }

    @GetMapping("/suggestions")
    public Result<List<SuggestionVO>> suggestions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dispatchService.list(status, date));
    }

    @PutMapping("/suggestions/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        dispatchService.confirm(id);
        return Result.ok();
    }

    @PutMapping("/suggestions/{id}/ignore")
    public Result<Void> ignore(@PathVariable Long id) {
        dispatchService.ignore(id);
        return Result.ok();
    }
}
