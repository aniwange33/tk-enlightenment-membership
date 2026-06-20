package com.tertech.tkenlightment.membership.auth.rest.controllers;

import com.tertech.tkenlightment.membership.auth.AuthPrincipal;
import com.tertech.tkenlightment.membership.auth.rest.dtos.MeResponse;
import com.tertech.tkenlightment.membership.auth.rest.dtos.UpdateMeRequest;
import com.tertech.tkenlightment.membership.dues.DuesAPI;
import com.tertech.tkenlightment.membership.dues.rest.dtos.DuesRecordResponse;
import com.tertech.tkenlightment.membership.member.MemberAPI;
import com.tertech.tkenlightment.membership.member.domain.services.UpdateMemberProfileCmd;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self-service endpoints for the authenticated member; admins (no linked member) get 404. */
@RestController
@RequestMapping("/api/me")
class MeController {

    private final MemberAPI memberAPI;
    private final DuesAPI duesAPI;

    MeController(MemberAPI memberAPI, DuesAPI duesAPI) {
        this.memberAPI = memberAPI;
        this.duesAPI = duesAPI;
    }

    @GetMapping
    MeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return MeResponse.from(memberAPI.getMember(requireMemberId(principal)));
    }

    @PutMapping
    MeResponse updateMe(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestBody UpdateMeRequest request) {
        var cmd = new UpdateMemberProfileCmd(
                requireMemberId(principal), request.phone(), request.address());
        return MeResponse.from(memberAPI.updateProfile(cmd));
    }

    @GetMapping("/dues")
    List<DuesRecordResponse> myDues(@AuthenticationPrincipal AuthPrincipal principal) {
        return duesAPI.getDuesHistory(requireMemberId(principal));
    }

    private String requireMemberId(AuthPrincipal principal) {
        if (principal.memberId() == null) {
            throw new ResourceNotFoundException("No member is linked to this account");
        }
        return principal.memberId();
    }
}
