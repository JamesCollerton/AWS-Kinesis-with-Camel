package com.example.kinesiscamel.initialization;

import com.example.kinesiscamel.model.TwitterApiCreateRequest;
import com.example.kinesiscamel.model.TwitterApiCreateRule;
import com.example.kinesiscamel.model.TwitterApiSearchResult;
import com.example.kinesiscamel.model.TwitterApiSearchRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TwitterInitialization implements ApplicationListener<ContextRefreshedEvent> {

  @Value("${twitter-api.search-url}")
  private String twitterApiSearchUrl;

  @Value("${twitter-api.rule-path}")
  private String twitterApiRulePath;

  private static final String queryString = "to:BBC";

  @Autowired private RestTemplate restTemplate;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    log.info("Searching for tweets, composing URI");

    String uriString =
        UriComponentsBuilder.fromHttpUrl(
                String.format("%s%s", twitterApiSearchUrl, twitterApiRulePath))
            .toUriString();

    log.info("Composed URI {}", uriString);

    ResponseEntity<TwitterApiSearchResult<TwitterApiSearchRule>>
        twitterApiSearchResultResponseEntity =
            restTemplate.exchange(
                uriString, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

    TwitterApiSearchResult<TwitterApiSearchRule> rules =
        twitterApiSearchResultResponseEntity.getBody();

    log.info("Retrieved list of rules {}", rules);

    boolean ruleDoesNotExist =
        Optional.ofNullable(rules)
            .map(TwitterApiSearchResult::getData)
            .map(List::stream)
            .map(s -> s.map(TwitterApiSearchRule::getValue).noneMatch(queryString::equals))
            .orElse(true);

    if (ruleDoesNotExist) {

      log.info("Rule missing {}, adding rule.", queryString);

      TwitterApiCreateRequest twitterApiCreateRequest =
          TwitterApiCreateRequest.builder()
              .add(
                  Collections.singletonList(
                      TwitterApiCreateRule.builder().value(queryString).build()))
              .build();

      ResponseEntity<TwitterApiSearchResult<TwitterApiSearchRule>>
          twitterApiCreateResultResponseEntity =
              restTemplate.exchange(
                  uriString,
                  HttpMethod.POST,
                  new HttpEntity<>(twitterApiCreateRequest),
                  new ParameterizedTypeReference<>() {});
    }
  }
}
