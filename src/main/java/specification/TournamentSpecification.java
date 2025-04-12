package specification;

import com.example.tennis.kz.model.Category;
import com.example.tennis.kz.model.Tournament;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TournamentSpecification {

    public static Specification<Tournament> hasLocation(String location) {
        return (root, query, cb) ->
                location == null ? null : cb.equal(root.get("location"), location);
    }

    public static Specification<Tournament> hasCategory(Category category) {
        return (root, query, cb) ->
                category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Tournament> hasMinLevel(Float minLevel) {
        return (root, query, cb) ->
                minLevel == null ? null : cb.greaterThanOrEqualTo(root.get("minLevel"), minLevel);
    }

    public static Specification<Tournament> hasMaxLevel(Float maxLevel) {
        return (root, query, cb) ->
                maxLevel == null ? null : cb.lessThanOrEqualTo(root.get("maxLevel"), maxLevel);
    }

    public static Specification<Tournament> isBetweenDates(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get("startDate"), start, end);
            } else if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("startDate"), start);
            } else if (end != null) {
                return cb.lessThanOrEqualTo(root.get("startDate"), end);
            } else {
                return null;
            }
        };
    }
}