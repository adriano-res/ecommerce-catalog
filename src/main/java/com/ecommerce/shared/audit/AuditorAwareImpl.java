package com.ecommerce.shared.audit;

import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

  public static final String MDC_USER_KEY = "actor";
  private static final String DEFAULT_AUDITOR = "system";

  @Override
  public Optional<String> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)
        && authentication.getName() != null
        && !authentication.getName().isBlank()) {
      return Optional.of(authentication.getName());
    }

    String actor = MDC.get(MDC_USER_KEY);
    if (actor != null && !actor.isBlank()) {
      return Optional.of(actor);
    }
    return Optional.of(DEFAULT_AUDITOR);
  }
}
