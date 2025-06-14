package com.atuhome.ragdemo.repository;

import com.atuhome.ragdemo.model.entity.QAHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface QAHistoryRepository extends JpaRepository<QAHistory, UUID> {
    
    Page<QAHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<QAHistory> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, 
            LocalDateTime endDate
    );
    
    @Query("SELECT qa FROM QAHistory qa WHERE qa.question LIKE %:searchTerm% OR qa.answer LIKE %:searchTerm%")
    List<QAHistory> findByQuestionOrAnswerContaining(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT AVG(qa.responseTimeMs) FROM QAHistory qa WHERE qa.responseTimeMs IS NOT NULL")
    Double getAverageResponseTime();
    
    @Query("SELECT COUNT(qa) FROM QAHistory qa WHERE qa.feedbackRating IS NOT NULL")
    long countWithFeedback();
    
    @Query("SELECT AVG(CAST(qa.feedbackRating AS double)) FROM QAHistory qa WHERE qa.feedbackRating IS NOT NULL")
    Double getAverageFeedbackRating();
}