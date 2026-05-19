package com.patchlens.service;

import com.patchlens.domain.UserAccount;
import com.patchlens.dto.AuthUserDto;
import com.patchlens.dto.ChangePasswordRequest;
import com.patchlens.dto.CreateUserAccountRequest;
import com.patchlens.dto.UpdateUserAccountRequest;
import com.patchlens.dto.UserAccountDto;
import com.patchlens.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserAccountService implements UserDetailsService, ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${patchlens.security.admin-username}") String bootstrapUsername,
            @Value("${patchlens.security.admin-password}") String bootstrapPassword
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new User(
                account.getUsername(),
                account.getPasswordHash(),
                account.isEnabled(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole()))
        );
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountRepository.count() > 0) {
            return;
        }
        UserAccount account = new UserAccount();
        account.setUsername(bootstrapUsername);
        account.setDisplayName("系统管理员");
        account.setRole("ADMIN");
        account.setEnabled(true);
        account.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        userAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<UserAccountDto> list() {
        return userAccountRepository.findAll().stream()
                .map(UserAccountDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuthUserDto authUser(String username) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new AuthUserDto(true, account.getUsername(), account.getDisplayName(), account.getRole());
    }

    @Transactional
    public UserAccountDto create(CreateUserAccountRequest request) {
        String username = request.username().trim();
        String displayName = request.displayName().trim();
        validatePassword(request.password());
        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("账号已存在");
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setDisplayName(displayName);
        account.setRole("ADMIN");
        account.setEnabled(request.enabled());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        return UserAccountDto.from(userAccountRepository.save(account));
    }

    @Transactional
    public UserAccountDto update(Long id, UpdateUserAccountRequest request, String actorUsername) {
        UserAccount account = getAccount(id);
        if (account.getUsername().equals(actorUsername) && !request.enabled()) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        if (account.isEnabled() && !request.enabled()) {
            ensureAnotherEnabledAccount();
        }
        account.setDisplayName(request.displayName().trim());
        account.setEnabled(request.enabled());
        if (StringUtils.hasText(request.password())) {
            validatePassword(request.password());
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        account.markUpdated();
        return UserAccountDto.from(account);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        validatePassword(request.newPassword());
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        account.markUpdated();
    }

    @Transactional
    public void delete(Long id, String actorUsername) {
        UserAccount account = getAccount(id);
        if (account.getUsername().equals(actorUsername)) {
            throw new IllegalArgumentException("不能删除当前登录账号");
        }
        if (account.isEnabled()) {
            ensureAnotherEnabledAccount();
        }
        userAccountRepository.delete(account);
    }

    private UserAccount getAccount(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private void ensureAnotherEnabledAccount() {
        if (userAccountRepository.countByEnabledTrue() <= 1) {
            throw new IllegalArgumentException("至少需要保留一个启用账号");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new IllegalArgumentException("密码长度至少 8 位");
        }
    }
}
