package kz.kimep.ticketing.identity.service;

import kz.kimep.ticketing.identity.domain.User;
import kz.kimep.ticketing.identity.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDirectory {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public String findEmail(Long userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse("unknown");
    }
}
