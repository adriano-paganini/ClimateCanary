package at.qe.skeleton.services;

import at.qe.skeleton.models.Userx;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Override
    public void sendUserInvitationEmail(Userx user) {

    }
}
