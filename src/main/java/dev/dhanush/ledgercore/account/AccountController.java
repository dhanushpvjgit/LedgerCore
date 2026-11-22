package dev.dhanush.ledgercore.account;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDtos.Response create(@Valid @RequestBody AccountDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<AccountDtos.Response> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public AccountDtos.Response get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/deactivate")
    public AccountDtos.Response deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }
}
