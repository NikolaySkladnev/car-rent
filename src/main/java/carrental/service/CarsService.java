package carrental.service;

import carrental.domain.model.Car;
import carrental.repository.CarRepository;

import java.time.LocalDate;
import java.util.List;

public final class CarsService {
    private final CarRepository cars;

    public CarsService(CarRepository cars) {
        this.cars = cars;
    }

    public List<Car> listAvailable(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) {
            LocalDate today = LocalDate.now();
            return cars.listAvailable(today, today.plusDays(1));
        }
        return cars.listAvailable(dateFrom, dateTo);
    }

    public Car get(long id) {
        return cars.getById(id);
    }
}
