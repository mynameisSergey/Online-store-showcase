package com.example.online_shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.http.HttpHeaders;
import java.util.function.Function;

@Service
@Slf4j
public class PaymentsService extends DefaultApi {

    @Autowired
    private ReactiveOAuth2AuthorizedClientManager auth2AuthorizedClientManager;

    @Value("${payments.server.url}")
    private String apiPath;

    @Value("${client.registration.id}")
    private String clientRegistrationId;

    public Mono<ApiClient> callApi() {
        log.info("Start getBalance");
        return auth2AuthorizedClientManager.authorize(OAuth2AuthorizeRequest
                        .withClientRegistrationId(clientRegistrationId)
                        .principal("N/A")
                        .build())
                .map(client -> client.getAccessToken().getTokenValue())
                .map(accessToken -> {
                    ApiClient apiClient = super.getApiClient();
                    apiClient.setBasePath(apiPath);
                    apiClient.addDefaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                    super.setApiClient(apiClient);
                    return super.getApiClient();
                });
    }

    public Mono<BigDecimal> getBalance() {
        return callApi()
                .map(apiClient -> super.apiBalanceGet())
                .flatMap(Function.identity())
                .log()
                .onErrorReturn(BigDecimal.ZERO);
    }

    public Mono<Boolean> createPayment(BigDecimal amount) {
        return callApi()
                .map(apiClient -> super.apiBalancePost(amount))
                .flatMap(Function.identity())
                .log()
                .onErrorReturn(false);
    }
}
