package carrental.domain.model;

import java.math.BigDecimal;

public record Car(
        long carId,
        String plateNumber,
        String brand,
        String model,
        String status,
        BigDecimal dailyCost,
        BigDecimal insuranceCost,
        Integer prodYear
) {}
