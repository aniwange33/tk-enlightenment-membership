package com.tertech.tkenlightment.membership.member;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MembershipNumberSequenceRepository extends JpaRepository<MembershipNumberSequenceEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM MembershipNumberSequenceEntity s WHERE s.year = :year")
    Optional<MembershipNumberSequenceEntity> findByYearForUpdate(@Param("year") Integer year);
}
