package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.WasteIncinerationFactorDTO;
import com.example.carbon.emission.model.service.WasteIncinerationFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waste-incineration-factors")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class WasteIncinerationFactorController {

    @Autowired
    private WasteIncinerationFactorService service;

    @GetMapping
    public ResponseEntity<List<WasteIncinerationFactorDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WasteIncinerationFactorDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<WasteIncinerationFactorDTO> getByName(@PathVariable String name) {
        return ResponseEntity.ok(service.findByEmissionFactorName(name));
    }

    @PostMapping
    public ResponseEntity<WasteIncinerationFactorDTO> create(@RequestBody WasteIncinerationFactorDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WasteIncinerationFactorDTO> update(@PathVariable Long id, @RequestBody WasteIncinerationFactorDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
