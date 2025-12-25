package com.planify.booking_service.repository;

import com.planify.booking_service.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByActiveTrueOrderByNameAsc();
}
