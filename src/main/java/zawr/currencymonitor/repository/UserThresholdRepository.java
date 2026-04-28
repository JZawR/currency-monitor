package zawr.currencymonitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zawr.currencymonitor.entity.UserThreshold;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserThresholdRepository extends JpaRepository<UserThreshold, Long> {
    Optional<UserThreshold> findByTelegramChatId(String telegramChatId);

    List<UserThreshold> findAllByEnabledTrue();
}