package carrental.repository;

import carrental.domain.model.ReservationView;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository {
    boolean isClientAllowed(long clientId);
    long create(long clientId, long carId, LocalDate dateFrom, LocalDate dateTo);
    boolean updateStatus(long rentalId, long clientId, String status);
    List<ReservationView> listByClient(long clientId);
    ReservationView getByIdForClient(long rentalId, long clientId);
}
