package com.nirajmali.split_expense_tracker.service.Impl;

import com.nirajmali.split_expense_tracker.dto.GroupDTO;
import com.nirajmali.split_expense_tracker.dto.PageResponse;
import com.nirajmali.split_expense_tracker.entity.Group;
import com.nirajmali.split_expense_tracker.entity.User;
import com.nirajmali.split_expense_tracker.repository.GroupRepository;
import com.nirajmali.split_expense_tracker.repository.UserRepository;
import com.nirajmali.split_expense_tracker.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;


    @Transactional
    @Override
    public GroupDTO.GroupResponse createGroup(GroupDTO.CreateGroupRequest request, Long creatorId) {
        User creatorUser=userRepository.findById(creatorId).orElseThrow(()->new RuntimeException("User not found: "+ creatorId));

        Set<User> members=new HashSet<>(userRepository.findAllById(request.getMembersIds()));

        if (members.isEmpty()){
            throw new RuntimeException("No valid members found");
        }
        members.add(creatorUser);

        Group group=Group.builder()
                .name(request.getName())
                .createdBy(creatorUser)
                .members(members)
                .build();

        Group savedGroup=groupRepository.save(group);
        return toResponse(savedGroup);

    }

    @Override
    public PageResponse<GroupDTO.GroupResponse> getGroupsForUser(Long userId, int page, int size) {
        Pageable pageable= PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Group> groupPage=groupRepository.findAllGroupsByMemberId(userId, pageable);

        List<GroupDTO.GroupResponse> content=groupPage.getContent()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<GroupDTO.GroupResponse>builder()
                .content(content)
                .pageNumber(groupPage.getNumber())
                .pageSize(groupPage.getSize())
                .totalElements(groupPage.getTotalElements())
                .totalPages(groupPage.getTotalPages())
                .isLastPage(groupPage.isLast())
                .build();
    }

    @Override
    public GroupDTO.GroupResponse getGroupById(Long groupId) {
        Group group=groupRepository.findById(groupId).orElseThrow(()->new RuntimeException("Group not found: "+ groupId));

        return toResponse(group);
    }

    private GroupDTO.GroupResponse toResponse(Group group) {
        List<GroupDTO.MemberSummary> members=group.getMembers()
                .stream()
                .map(member-> GroupDTO.MemberSummary.builder()
                        .userId(member.getId())
                        .name(member.getName())
                        .email(member.getEmail())
                        .build())
                .collect(Collectors.toList());

        return GroupDTO.GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .createdByName(group.getCreatedBy().getName())
                .members(members)
                .build();
    }

}
