package de.tum.in.www1.artemis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.leaderboard.tutor.TutorLeaderboardComplaintsView;

@Repository
public interface TutorLeaderboardComplaintsViewRepository extends JpaRepository<TutorLeaderboardComplaintsView, Long> {

    List<TutorLeaderboardComplaintsView> findAllByCourseId(long courseId);

    // TODO: add when examId is added to the view: List<TutorLeaderboardComplaintsView> findAllByExamId(long examId);

    List<TutorLeaderboardComplaintsView> findAllByLeaderboardId_ExerciseId(long exerciseId);
}
