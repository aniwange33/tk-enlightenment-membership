package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.dues.rest.dtos.DuesRecordResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DuesAPI {

    private final DuesService duesService;

    DuesAPI(DuesService duesService) {
        this.duesService = duesService;
    }

    public DuesRecordResponse markPaid(String memberId, int year) {
        DuesRecordEntity record = duesService.markPaid(memberId, year);
        return toResponse(record);
    }

    public List<DuesRecordResponse> getDuesHistory(String memberId) {
        return duesService.getDuesHistory(memberId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void createDuesRecord(String memberId, int year) {
        duesService.createDuesRecord(memberId, year);
    }

    public void generateDuesForYear(int year) {
        duesService.generateDuesForYear(year);
    }

    public void inactivateUnpaidMembers(int year) {
        duesService.inactivateUnpaidMembers(year);
    }

    private DuesRecordResponse toResponse(DuesRecordEntity entity) {
        return new DuesRecordResponse(
                entity.getId().value(),
                entity.getMemberId(),
                entity.getYear(),
                entity.getPaid(),
                entity.getPaidDate());
    }
}
