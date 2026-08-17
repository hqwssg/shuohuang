package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.ElectricityCarbonEmissionFactorDTO;
import com.example.carbon.emission.model.service.ElectricityCarbonEmissionFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/electricity-carbon-factors")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class ElectricityCarbonEmissionFactorController {
    
    @Autowired
    private ElectricityCarbonEmissionFactorService service;
    
    @GetMapping
    public ResponseEntity<List<ElectricityCarbonEmissionFactorDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ElectricityCarbonEmissionFactorDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ElectricityCarbonEmissionFactorDTO>> search(@RequestParam String factorName) {
        return ResponseEntity.ok(service.searchByFactorName(factorName));
    }
    
    @PostMapping
    public ResponseEntity<ElectricityCarbonEmissionFactorDTO> create(@RequestBody ElectricityCarbonEmissionFactorDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ElectricityCarbonEmissionFactorDTO> update(@PathVariable Long id, @RequestBody ElectricityCarbonEmissionFactorDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}