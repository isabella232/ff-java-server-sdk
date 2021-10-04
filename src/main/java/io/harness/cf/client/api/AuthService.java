package io.harness.cf.client.api;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.util.concurrent.AbstractScheduledService;
import io.harness.cf.ApiException;
import io.harness.cf.api.DefaultApi;
import io.harness.cf.client.dto.AuthenticationRequestBuilder;
import io.harness.cf.model.AuthenticationRequest;
import io.harness.cf.model.AuthenticationResponse;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthService extends AbstractScheduledService {

    protected final String apiKey;
    protected final CfClient cfClient;
    protected final DefaultApi defaultApi;
    protected final int pollIntervalInSec;

    public AuthService(

            final DefaultApi defaultApi,
            final String apiKey,
            final CfClient cfClient,
            final int pollIntervalInSec
    ) {

        this.defaultApi = defaultApi;
        this.apiKey = apiKey;
        this.cfClient = cfClient;
        this.pollIntervalInSec = pollIntervalInSec;
    }

    @Override
    protected void runOneIteration() throws Exception {

        if (isNullOrEmpty(apiKey)) {

            throw new CfClientException("SDK key cannot be empty");
        }

        try {

            final AuthenticationRequest request = AuthenticationRequestBuilder
                    .anAuthenticationRequest()
                    .apiKey(apiKey)
                    .build();

            final AuthenticationResponse authResponse = defaultApi.authenticate(request);

            String jwtToken = authResponse.getAuthToken();
            cfClient.setJwtToken(jwtToken);
            cfClient.init();
            log.info("Stopping Auth service");

            this.stopAsync();

        } catch (ApiException apiException) {

            log.error("Failed to get auth token {}", apiException.getMessage());
            if (apiException.getCode() == 401 || apiException.getCode() == 403) {

                String errorMsg = String.format("Invalid apiKey %s. Serving default value. ", apiKey);
                log.error(errorMsg);
                throw new CfClientException(errorMsg);
            }
        }
    }

    @Override
    protected Scheduler scheduler() {

        return Scheduler.newFixedDelaySchedule(0L, pollIntervalInSec, TimeUnit.SECONDS);
    }
}
