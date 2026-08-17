package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.ThermalEmissionFactorDTO;
import com.example.carbon.emission.model.service.ThermalEmissionFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thermal-emission-factors")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class ThermalEmissionFactorController {

    @Autowired
    private ThermalEmissionFactorService service;

    @GetMapping
    public ResponseEntity<List<ThermalEmissionFactorDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThermalEmissionFactorDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<ThermalEmissionFactorDTO> getByName(@PathVariable String name) {
        return ResponseEntity.ok(service.findByEmissionFactorName(name));
    }

    @PostMapping
    public ResponseEntity<ThermalEmissionFactorDTO> create(@RequestBody ThermalEmissionFactorDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThermalEmissionFactorDTO> update(@PathVariable Long id, @RequestBody ThermalEmissionFactorDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
