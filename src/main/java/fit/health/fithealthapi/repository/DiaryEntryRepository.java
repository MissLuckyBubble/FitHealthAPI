package fit.health.fithealthapi.repository;

import fit.health.fithealthapi.model.DiaryEntry;
import fit.health.fithealthapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    List<DiaryEntry> findByUser(User user);
    Optional<DiaryEntry> findByUserAndDate(User user, LocalDate date);
}
