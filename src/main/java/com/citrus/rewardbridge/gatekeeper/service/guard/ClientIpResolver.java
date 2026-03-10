package com.citrus.rewardbridge.gatekeeper.service.guard;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String resolvedIp = forwardedFor.split(",")[0].trim();
            log.info("Client IP resolved from X-Forwarded-For. resolvedIp={}, headerValue={}", resolvedIp, forwardedFor);
            return resolvedIp;
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            String resolvedIp = realIp.trim();
            log.info("Client IP resolved from X-Real-IP. resolvedIp={}", resolvedIp);
            return resolvedIp;
        }

        String resolvedIp = request.getRemoteAddr();
        log.info("Client IP resolved from remote address. resolvedIp={}", resolvedIp);
        return resolvedIp;
    }
}
