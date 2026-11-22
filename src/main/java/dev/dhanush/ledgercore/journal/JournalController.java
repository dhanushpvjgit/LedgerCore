package dev.dhanush.ledgercore.journal;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal-entries")
public class JournalController {
    private final JournalPostingService service;

    public JournalController(JournalPostingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JournalDtos.EntryResponse post(@Valid @RequestBody JournalDtos.PostRequest request, Principal principal) {
        return service.post(request, principal == null ? "system" : principal.getName());
    }

    @GetMapping("/{id}")
    public JournalDtos.EntryResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
