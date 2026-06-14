package com.tertech.tkenlightment.membership.member;

import com.tertech.tkenlightment.membership.member.domain.models.MemberId;
import com.tertech.tkenlightment.membership.member.domain.models.MemberStatus;
import com.tertech.tkenlightment.membership.shared.domain.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MemberRepository extends JpaRepository<MemberEntity, MemberId> {

    boolean existsByEmail(String email);

    Optional<MemberEntity> findByEmail(String email);

    Page<MemberEntity> findByStatus(MemberStatus status, Pageable pageable);

    @Query("""
            SELECT m FROM MemberEntity m
            WHERE (:query IS NULL
                OR LOWER(CAST(m.firstName AS String)) LIKE LOWER(CONCAT('%', CAST(:query AS String), '%'))
                OR LOWER(CAST(m.lastName AS String)) LIKE LOWER(CONCAT('%', CAST(:query AS String), '%'))
                OR LOWER(CAST(m.membershipNumber AS String)) LIKE LOWER(CONCAT('%', CAST(:query AS String), '%'))
                OR LOWER(CAST(m.email AS String)) LIKE LOWER(CONCAT('%', CAST(:query AS String), '%')))
            AND (:status IS NULL OR m.status = :status)
            """)
    Page<MemberEntity> search(
            @Param("query") String query,
            @Param("status") MemberStatus status,
            Pageable pageable);

    @Query("SELECT m FROM MemberEntity m WHERE m.status = 'ACTIVE'")
    List<MemberEntity> findActiveMembers();

    List<MemberEntity> findByStatusNot(MemberStatus status);

    default MemberEntity getById(MemberId id) {
        return findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Member not found with id: " + id.value()));
    }
}
