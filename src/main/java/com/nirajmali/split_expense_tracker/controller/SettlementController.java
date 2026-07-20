package com.nirajmali.split_expense_tracker.controller;

import com.nirajmali.split_expense_tracker.dto.BalanceDTO;
import com.nirajmali.split_expense_tracker.service.BalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SettlementController {

    private final BalanceService balanceService;

    @GetMapping("/balance/{groupId}")
    public ResponseEntity<BalanceDTO.GroupBalanceResponse> getGroupBalance(@PathVariable Long groupId){
        BalanceDTO.GroupBalanceResponse response=balanceService.getGroupBalance(groupId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/settlements")
    public ResponseEntity<BalanceDTO.SettlementResponse> settleUp(@Valid @RequestBody BalanceDTO.SettleUpRequest request){
        BalanceDTO.SettlementResponse response=balanceService.settlementUp(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
