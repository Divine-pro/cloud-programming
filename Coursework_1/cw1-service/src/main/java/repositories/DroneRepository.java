package uk.ac.ed.inf.cw1service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ed.inf.cw1service.models.Drone;

@Repository
public interface DroneRepository extends JpaRepository<Drone, String>
{
}