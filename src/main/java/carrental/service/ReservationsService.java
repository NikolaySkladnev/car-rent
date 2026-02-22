package carrental.service;

import carrental.domain.error.DomainException;
import carrental.domain.error.ErrorCode;
import carrental.domain.model.ReservationView;
import carrental.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.List;

public final class ReservationsService {
    private final ReservationRepository res;

    public ReservationsService(ReservationRepository res) {
        this.res = res;
    }

    public ReservationView createAndConfirm(long clientId, long carId, LocalDate dateFrom, LocalDate dateTo) {
        if (carId <= 0 || dateFrom == null || dateTo == null) {
            throw new DomainException(ErrorCode.VALIDATION, "validation error");
        }
        if (!dateTo.isAfter(dateFrom)) {
            throw new DomainException(ErrorCode.VALIDATION, "date_to must be greater than date_from");
        }

        boolean allowed = res.isClientAllowed(clientId);
        if (!allowed) throw new DomainException(ErrorCode.CLIENT_BLOCKED, "client blocked");

        long id = res.create(clientId, carId, dateFrom, dateTo);

        boolean ok = res.updateStatus(id, clientId, "confirmed");
        if (!ok) throw new DomainException(ErrorCode.INTERNAL, "internal error");

        return res.getByIdForClient(id, clientId);
    }

    public List<ReservationView> listMine(long clientId) {
        return res.listByClient(clientId);
    }

    public ReservationView getMine(long rentalId, long clientId) {
        return res.getByIdForClient(rentalId, clientId);
    }

    public void cancel(long rentalId, long clientId) {
        boolean ok = res.updateStatus(rentalId, clientId, "canceled");
        if (!ok) throw new DomainException(ErrorCode.NOT_FOUND, "reservation not found");
    }
}
