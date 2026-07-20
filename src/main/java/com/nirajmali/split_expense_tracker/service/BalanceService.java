package com.nirajmali.split_expense_tracker.service;

import com.nirajmali.split_expense_tracker.dto.BalanceDTO;

public interface BalanceService {

    public BalanceDTO.GroupBalanceResponse getGroupBalance(Long groupId);

    public BalanceDTO.SettlementResponse settlementUp(BalanceDTO.SettleUpRequest  request);

    public void refreshGroupBalanceCache(Long groupId);
}
