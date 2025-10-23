package com.example.bankcards.controller;

import com.example.bankcards.dto.common.PageResponse;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.TransferResponse;
import com.example.bankcards.security.AuthenticatedUser;
import com.example.bankcards.service.TransferService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards/transfers")
@RequiredArgsConstructor
@Validated
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')")
    public TransferResponse transfer(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TransferRequest request) {
        UUID userId = user.id();
        return transferService.transferBetweenOwnCards(userId, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public PageResponse<TransferResponse> history(
            @AuthenticationPrincipal AuthenticatedUser user,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return transferService.getUserTransfers(user.id(), pageable);
    }
}
