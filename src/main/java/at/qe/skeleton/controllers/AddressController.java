package at.qe.skeleton.controllers;

import at.qe.skeleton.dtos.AddressCreateDTO;
import at.qe.skeleton.dtos.AddressDTO;
import at.qe.skeleton.dtos.AddressUpdateDTO;
import at.qe.skeleton.mappers.AddressCreateMapper;
import at.qe.skeleton.mappers.AddressMapper;
import at.qe.skeleton.model.Address;
import at.qe.skeleton.services.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final AddressCreateMapper addressCreateMapper;

    public AddressController(AddressService addressService,
                             AddressMapper addressMapper,
                             AddressCreateMapper addressCreateMapper) {
        this.addressService = addressService;
        this.addressMapper = addressMapper;
        this.addressCreateMapper = addressCreateMapper;
    }

    @GetMapping
    public ResponseEntity<List<AddressDTO>> getAll() {
        List<AddressDTO> addresses = addressService.getAll().stream()
                .map(addressMapper::mapTo)
                .toList();
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(addressMapper.mapTo(addressService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<AddressDTO> create(@Valid @RequestBody AddressCreateDTO dto) {
        Address address = addressService.create(addressCreateMapper.mapFrom(dto));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(address.getId())
                .toUri();

        return ResponseEntity.created(location).body(addressMapper.mapTo(address));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AddressDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody AddressUpdateDTO dto) {
        Address updated = addressService.update(id, dto);
        return ResponseEntity.ok(addressMapper.mapTo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
