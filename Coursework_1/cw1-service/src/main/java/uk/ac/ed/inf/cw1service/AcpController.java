package uk.ac.ed.inf.cw1service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.inf.cw1service.models.Drone;
import uk.ac.ed.inf.cw1service.models.UrlRequest;
import uk.ac.ed.inf.cw1service.repositories.DroneRepository;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/acp") // Requirement: All endpoints start with this
public class AcpController
{
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private DroneRepository droneRepository;
    @PostMapping("/process/dump")
    public ResponseEntity<List<Drone>> processDump(@RequestBody UrlRequest request)
    {
        String targetUrl = request.getUrlPath();
        Drone[] response = restTemplate.getForObject(targetUrl, Drone[].class);
        if (response == null)
        {
            return ResponseEntity.notFound().build();
        }
        List<Drone> droneList = Arrays.asList(response);
        for (Drone drone : droneList)
        {
            var cap = drone.getCapability();
            double cpm = cap.getCpm();
            double ic = cap.getIc();
            double fc = cap.getFc();

            if (Double.isNaN(cpm)) cpm = 0.0;
            if (Double.isNaN(ic)) ic = 0.0;
            if (Double.isNaN(fc)) fc = 0.0;
            double calculatedCost = ic + fc + (cpm * 100);
            drone.setCost100(Math.round(calculatedCost * 100.0) / 100.0);
        }
        droneRepository.saveAll(droneList);
        return ResponseEntity.ok(droneList);
    }
}