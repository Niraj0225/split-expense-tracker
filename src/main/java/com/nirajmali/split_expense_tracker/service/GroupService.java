package com.nirajmali.split_expense_tracker.service;

import com.nirajmali.split_expense_tracker.dto.GroupDTO;
import com.nirajmali.split_expense_tracker.dto.PageResponse;

public interface GroupService {

    public GroupDTO.GroupResponse createGroup(GroupDTO.CreateGroupRequest request, Long creatorId);

    public PageResponse<GroupDTO.GroupResponse> getGroupsForUser(Long userId, int page, int size);

    public GroupDTO.GroupResponse getGroupById(Long groupId);


}
