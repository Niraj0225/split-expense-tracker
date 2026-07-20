package com.nirajmali.split_expense_tracker.controller;

import com.nirajmali.split_expense_tracker.dto.GroupDTO;
import com.nirajmali.split_expense_tracker.dto.PageResponse;
import com.nirajmali.split_expense_tracker.entity.User;
import com.nirajmali.split_expense_tracker.repository.UserRepository;
import com.nirajmali.split_expense_tracker.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<GroupDTO.GroupResponse> createGroup(
            @Valid @RequestBody GroupDTO.CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails){

        User creator = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
        System.out.println("user id by creator id:"+ creator.getId());
        GroupDTO.GroupResponse response= groupService.createGroup(request, creator.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/user/my-groups")
    public ResponseEntity<PageResponse<GroupDTO.GroupResponse>> getMyGroups(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        User user=userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()->new RuntimeException("User not found"));


        PageResponse<GroupDTO.GroupResponse> response=
                groupService.getGroupsForUser(user.getId(),page,size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDTO.GroupResponse> getGroupById(@PathVariable Long groupId){
        GroupDTO.GroupResponse response=groupService.getGroupById(groupId);
        return ResponseEntity.ok(response);
    }
}
