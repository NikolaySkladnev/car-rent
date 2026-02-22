package carrental.repository;

import carrental.domain.model.Car;

import java.time.LocalDate;
import java.util.List;

public interface CarRepository {
    List<Car> listAvailable(LocalDate dateFrom, LocalDate dateTo);
    Car getById(long id);
}
