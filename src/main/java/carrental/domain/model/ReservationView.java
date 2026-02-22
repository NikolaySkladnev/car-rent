package carrental.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationView(
        long rentalId,
        long clientId,
        String fullName,
        long carId,
        String plateNumber,
        String brand,
        String model,
        LocalDate dateFrom,
        LocalDate dateTo,
        String status,
        BigDecimal dailyRateAtBooking,
        BigDecimal totalAmount,
        BigDecimal penaltyAmount,
        BigDecimal depositAmount
) {}
