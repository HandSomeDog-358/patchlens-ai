package com.patchlens.api;

import com.patchlens.dto.CreateUserAccountRequest;
import com.patchlens.dto.UpdateUserAccountRequest;
import com.patchlens.dto.UserAccountDto;
import com.patchlens.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public List<UserAccountDto> list() {
        return userAccountService.list();
    }

    @PostMapping
    public UserAccountDto create(@Valid @RequestBody CreateUserAccountRequest request) {
        return userAccountService.create(request);
    }

    @PutMapping("/{id}")
    public UserAccountDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserAccountRequest request,
            Authentication authentication
    ) {
        return userAccountService.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        userAccountService.delete(id, authentication.getName());
    }
}
