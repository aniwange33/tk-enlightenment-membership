package com.tertech.tkenlightment.membership.member.rest.controllers;

import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.member.domain.services.ChangeStatusCmd;
import com.tertech.tkenlightment.membership.member.domain.services.RegisterMemberCmd;
import com.tertech.tkenlightment.membership.member.domain.services.UpdateMemberProfileCmd;
import com.tertech.tkenlightment.membership.member.rest.dtos.ChangeStatusRequest;
import com.tertech.tkenlightment.membership.member.rest.dtos.MemberResponse;
import com.tertech.tkenlightment.membership.member.rest.dtos.RegisterMemberRequest;
import com.tertech.tkenlightment.membership.member.rest.dtos.UpdateMemberProfileRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
class MemberController {

    private final MemberAPI memberAPI;

    MemberController(MemberAPI memberAPI) {
        this.memberAPI = memberAPI;
    }

    @PostMapping
    ResponseEntity<MemberResponse> register(@Valid @RequestBody RegisterMemberRequest request) {
        var cmd = new RegisterMemberCmd(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.email(),
                request.phone(),
                request.address());

        var result = memberAPI.registerMember(cmd);
        var response = MemberResponse.from(result);

        return ResponseEntity.created(URI.create("/api/members/" + response.id()))
                .body(response);
    }

    @GetMapping
    Page<MemberResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MemberStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return memberAPI.listMembers(query, status, pageable).map(MemberResponse::from);
    }

    @GetMapping("/{memberId}")
    MemberResponse getById(@PathVariable String memberId) {
        return MemberResponse.from(memberAPI.getMember(memberId));
    }

    @PatchMapping("/{memberId}/status")
    MemberResponse changeStatus(
            @PathVariable String memberId, @Valid @RequestBody ChangeStatusRequest request) {
        var cmd = new ChangeStatusCmd(memberId, request.status());
        return MemberResponse.from(memberAPI.changeStatus(cmd));
    }

    @PutMapping("/{memberId}/profile")
    MemberResponse updateProfile(
            @PathVariable String memberId, @RequestBody UpdateMemberProfileRequest request) {
        var cmd = new UpdateMemberProfileCmd(memberId, request.phone(), request.address());
        return MemberResponse.from(memberAPI.updateProfile(cmd));
    }
}
