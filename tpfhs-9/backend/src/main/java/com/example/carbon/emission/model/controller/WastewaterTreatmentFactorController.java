package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.WastewaterTreatmentFactorDTO;
import com.example.carbon.emission.model.service.WastewaterTreatmentFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wastewater-treatment-factors")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class WastewaterTreatmentFactorController {

    @Autowired
    private WastewaterTreatmentFactorService service;

    @GetMapping
    public ResponseEntity<List<WastewaterTreatmentFactorDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WastewaterTreatmentFactorDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<WastewaterTreatmentFactorDTO> getByName(@PathVariable String name) {
        return ResponseEntity.ok(service.findByEmissionFactorName(name));
    }

    @PostMapping
    public ResponseEntity<WastewaterTreatmentFactorDTO> create(@RequestBody WastewaterTreatmentFactorDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WastewaterTreatmentFactorDTO> update(@PathVariable Long id, @RequestBody WastewaterTreatmentFactorDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
