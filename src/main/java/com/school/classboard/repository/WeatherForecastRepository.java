package com.school.classboard.repository;

import com.school.classboard.domain.WeatherForecast;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, Long> {

    List<WeatherForecast> findByForecastDateGreaterThanEqualOrderByForecastDateAsc(LocalDate date);

    Optional<WeatherForecast> findByForecastDate(LocalDate date);

    void deleteByForecastDateBefore(LocalDate date);
}
