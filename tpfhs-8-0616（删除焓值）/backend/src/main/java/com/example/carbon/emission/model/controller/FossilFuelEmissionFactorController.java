package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.FossilFuelEmissionFactorDTO;
import com.example.carbon.emission.model.service.FossilFuelEmissionFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fossil-fuel-factors")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class FossilFuelEmissionFactorController {
    
    @Autowired
    private FossilFuelEmissionFactorService service;
    
    @GetMapping
    public ResponseEntity<List<FossilFuelEmissionFactorDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<FossilFuelEmissionFactorDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<FossilFuelEmissionFactorDTO>> search(@RequestParam String fuelType) {
        return ResponseEntity.ok(service.searchByFuelType(fuelType));
    }
    
    @PostMapping
    public ResponseEntity<FossilFuelEmissionFactorDTO> create(@RequestBody FossilFuelEmissionFactorDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<FossilFuelEmissionFactorDTO> update(@PathVariable Long id, @RequestBody FossilFuelEmissionFactorDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}