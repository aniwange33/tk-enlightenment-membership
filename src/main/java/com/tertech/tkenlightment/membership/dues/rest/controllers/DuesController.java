package com.tertech.tkenlightment.membership.dues.rest.controllers;

import com.tertech.tkenlightment.membership.dues.DuesAPI;
import com.tertech.tkenlightment.membership.dues.rest.dtos.DuesRecordResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members/{memberId}/dues")
class DuesController {

    private final DuesAPI duesAPI;

    DuesController(DuesAPI duesAPI) {
        this.duesAPI = duesAPI;
    }

    @PostMapping("/{year}/pay")
    ResponseEntity<DuesRecordResponse> markPaid(
            @PathVariable String memberId, @PathVariable int year) {
        var response = duesAPI.markPaid(memberId, year);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    List<DuesRecordResponse> getDuesHistory(@PathVariable String memberId) {
        return duesAPI.getDuesHistory(memberId);
    }
}
