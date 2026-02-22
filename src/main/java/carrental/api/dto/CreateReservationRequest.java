package carrental.api.dto;

public record CreateReservationRequest(
        long carId,
        String dateFrom,
        String dateTo
) {}
