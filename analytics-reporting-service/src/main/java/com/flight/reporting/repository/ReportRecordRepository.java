package com.flight.reporting.repository;

import com.flight.reporting.model.ReportRecord;
import com.flight.reporting.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRecordRepository extends JpaRepository<ReportRecord, Long> {

    Optional<ReportRecord> findByReportReference(String reference);

    List<ReportRecord> findByReportTypeOrderByGeneratedAtDesc(ReportType type);

    List<ReportRecord> findAllByOrderByGeneratedAtDesc();

    boolean existsByReportReference(String reference);
}
