package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.dues.domain.models.DuesRecordId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface DuesRecordRepository extends JpaRepository<DuesRecordEntity, DuesRecordId> {

    List<DuesRecordEntity> findByMemberIdOrderByYearDesc(String memberId);

    Optional<DuesRecordEntity> findByMemberIdAndYear(String memberId, int year);

    Page<DuesRecordEntity> findByYearAndPaidFalse(int year, Pageable pageable);
}
