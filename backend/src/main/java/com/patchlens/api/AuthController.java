package com.patchlens.api;

import com.patchlens.dto.AuthUserDto;
import com.patchlens.dto.ChangePasswordRequest;
import com.patchlens.dto.LoginRequest;
import com.patchlens.service.AuditLogService;
import com.patchlens.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountService userAccountService;
    private final AuditLogService auditLogService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserAccountService userAccountService,
            AuditLogService auditLogService
    ) {
        this.authenticationManager = authenticationManager;
        this.userAccountService = userAccountService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/me")
    public AuthUserDto me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return new AuthUserDto(false, "", "", "");
        }
        return userAccountService.authUser(authentication.getName());
    }

    @PostMapping("/login")
    public AuthUserDto login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
            auditLogService.record("AUTH_LOGIN", "USER", authentication.getName(), "用户登录");
            return userAccountService.authUser(authentication.getName());
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
    }

    @PostMapping("/logout")
    public AuthUserDto logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            auditLogService.record("AUTH_LOGOUT", "USER", authentication.getName(), "用户退出");
        }
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return new AuthUserDto(false, "", "", "");
    }

    @PostMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        userAccountService.changePassword(authentication.getName(), request);
        auditLogService.record("AUTH_PASSWORD_CHANGE", "USER", authentication.getName(), "修改当前账号密码");
    }
}
