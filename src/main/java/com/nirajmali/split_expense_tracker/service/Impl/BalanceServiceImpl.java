package com.nirajmali.split_expense_tracker.service.Impl;

import com.nirajmali.split_expense_tracker.dto.BalanceDTO;
import com.nirajmali.split_expense_tracker.entity.*;
import com.nirajmali.split_expense_tracker.repository.*;
import com.nirajmali.split_expense_tracker.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceServiceImpl implements BalanceService {

    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper= JsonMapper.builder().build();

    private static final String CACHE_KEY_PREFIX = "balance:group:";
    private static final long CACHE_TTL_MINUTES = 10;

    @Override
    public BalanceDTO.GroupBalanceResponse getGroupBalance(Long groupId) {
        String cacheKey= CACHE_KEY_PREFIX + groupId;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null){
                log.info("Cache HIT for group {}", groupId);
                return objectMapper.readValue(
                        cached.toString(),
                        BalanceDTO.GroupBalanceResponse.class
                );
            }
        }catch (Exception e){
            log.warn("Cache read failed for group {}: {}", groupId, e.getMessage());
        }
        log.info("Cache MISS for group {} - calculating from MySQL", groupId);
        BalanceDTO.GroupBalanceResponse response=calculateBalance(groupId);
        cacheBalance(cacheKey,response);
        return response;


    }

    @Transactional
    @Override
    public void refreshGroupBalanceCache(Long groupId) {
        String cacheKey = CACHE_KEY_PREFIX + groupId;
        redisTemplate.delete(cacheKey);
        log.info("Deleted cache for group {}", groupId);

        BalanceDTO.GroupBalanceResponse response =calculateBalance(groupId);
        cacheBalance(cacheKey, response);
        log.info("Refreshed cache for group {}", groupId);

    }


    private void cacheBalance(String cacheKey, BalanceDTO.GroupBalanceResponse response) {
        try{
            String json= objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(
                    cacheKey, json,
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Cache write failed: {}", e.getMessage());
        }
    }

    @Transactional
    private BalanceDTO.GroupBalanceResponse calculateBalance(Long groupId) {
        Group group=groupRepository.findById(groupId).orElseThrow(()->new RuntimeException("Group not found: " + groupId));

        Map<Long, BigDecimal> balanceMap=new HashMap<>();
        Map<Long, String> nameMap= new HashMap<>();

        for (User member: group.getMembers()){
            balanceMap.put(member.getId(), BigDecimal.ZERO);
            nameMap.put(member.getId(), member.getName());
        }

        Pageable allRecords= PageRequest.of(0, Integer.MAX_VALUE);
        List<Expense> expenses=expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId, allRecords).getContent();

        for (Expense expense:expenses){
            Long payerId=expense.getPaidBy().getId();
            BigDecimal amount=expense.getAmount();
            balanceMap.merge(payerId, amount, BigDecimal::add);
        }

        List<ExpenseShare> shares=expenseShareRepository.findAllByGroupId(groupId);
        for (ExpenseShare share : shares){
            Long userId= share.getUser().getId();
            BigDecimal shareAmount= share.getShareAmount();
            balanceMap.merge(userId, shareAmount.negate(), BigDecimal::add);
        }

        List<Settlement> settlements=settlementRepository.findByGroupId(groupId);
        for (Settlement settlement: settlements){
            Long paidById=settlement.getPaidBy().getId();
            Long paidToId=settlement.getPaidTo().getId();
            BigDecimal amount=settlement.getAmount();

            // payer's debt reduces → balance goes up
            balanceMap.merge(paidById, amount, BigDecimal::add);

            // receiver got paid → what's owed to them reduces
            balanceMap.merge(paidToId, amount.negate(), BigDecimal::add);
        }

        List<BalanceDTO.UserBalance> balances=new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry: balanceMap.entrySet()){
            balances.add(BalanceDTO.UserBalance.builder()
                    .userId(entry.getKey())
                    .userName(nameMap.get(entry.getKey()))
                    .netAmount(entry.getValue())
                    .build());
        }

        return BalanceDTO.GroupBalanceResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .balances(balances)
                .build();
    }

    @Transactional
    @Override
    public BalanceDTO.SettlementResponse settlementUp(BalanceDTO.SettleUpRequest request) {
        Group group=groupRepository.findById(request.getGroupId())
                .orElseThrow(()->new RuntimeException("Group not found: "+ request.getGroupId()));

        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(()->new RuntimeException("User not found: " + request.getPaidByUserId()));

        User paidTo= userRepository.findById(request.getPaidToUserId())
                .orElseThrow(()-> new RuntimeException("User not found: "+request.getPaidToUserId()));

        if (paidBy.getId().equals(paidTo.getId())){
            throw new RuntimeException("Cannot settle with yourself");
        }

        Settlement settlement=Settlement.builder()
                .group(group)
                .paidBy(paidBy)
                .paidTo(paidTo)
                .amount(request.getAmount())
                .build();

        settlementRepository.save(settlement);

        String cacheKey = CACHE_KEY_PREFIX + request.getGroupId();
        redisTemplate.delete(cacheKey);
        log.info("Deleted cache after settlement for group {}",
                request.getGroupId());

        return BalanceDTO.SettlementResponse.builder()
                .message("Settlement recorded successfully")
                .paidBy(paidBy.getName())
                .paidTo(paidTo.getName())
                .amount(request.getAmount())
                .build();
    }


}
