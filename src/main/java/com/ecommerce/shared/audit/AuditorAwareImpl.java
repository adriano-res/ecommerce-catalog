package com.ecommerce.shared.audit;

import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

  public static final String MDC_USER_KEY = "actor";
  private static final String DEFAULT_AUDITOR = "system";

  @Override
  public Optional<String> getCurrentAuditor() {
    String actor = MDC.get(MDC_USER_KEY);
    if (actor != null && !actor.isBlank()) {
      return Optional.of(actor);
    }
    return Optional.of(DEFAULT_AUDITOR);
  }
}
