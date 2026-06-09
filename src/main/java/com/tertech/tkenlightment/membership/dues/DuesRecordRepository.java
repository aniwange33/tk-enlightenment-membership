package com.tertech.tkenlightment.membership.dues;

import com.tertech.tkenlightment.membership.dues.domain.models.DuesRecordId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DuesRecordRepository extends JpaRepository<DuesRecordEntity, DuesRecordId> {

    List<DuesRecordEntity> findByMemberIdOrderByYearDesc(String memberId);

    Optional<DuesRecordEntity> findByMemberIdAndYear(String memberId, int year);

    @Query("""
            SELECT d FROM DuesRecordEntity d
            WHERE d.year = :year AND d.paid = false
            """)
    List<DuesRecordEntity> findUnpaidForYear(@Param("year") int year);
}
