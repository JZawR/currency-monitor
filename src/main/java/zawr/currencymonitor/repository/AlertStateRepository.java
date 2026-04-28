package zawr.currencymonitor.repository;

import zawr.currencymonitor.model.AlertState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AlertStateRepository extends MongoRepository<AlertState, String> {
    Optional<AlertState> findByKey(String key);
}