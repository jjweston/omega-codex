/*

Copyright 2025 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package io.github.jjweston.omegacodex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class EmbeddingApiServiceTest
{
    private final String testApiEndpoint = "https://example.org/v1/embeddings";
    private final String testApiKeyName  = "OMEGACODEX_TEST_API_KEY";

    private final String testModel       = "test-embedding";
    private final int    testInputLimit  = 1_000;

    @Mock private Dotenv                 mockDotenv;
    @Mock private HttpClient             mockHttpClient;
    @Mock private HttpRequestBuilder     mockHttpRequestBuilder;
    @Mock private HttpResponse< String > mockHttpResponse;
    @Mock private OmegaCodexLogger       mockOmegaCodexLogger;

    @Captor private ArgumentCaptor< String > requestBodyCaptor;

    private EmbeddingApiService embeddingApiService;

    @BeforeEach
    void setUp()
    {
        this.embeddingApiService = new EmbeddingApiService(
                this.testApiEndpoint, this.testApiKeyName, this.testModel, this.testInputLimit,
                this.mockDotenv, this.mockHttpRequestBuilder, this.mockHttpClient, this.mockOmegaCodexLogger );
    }

    @AfterEach
    void tearDown()
    {
        verifyNoMoreInteractions( this.mockHttpClient );
    }

    @Test
    void testGetEmbedding_nullInput()
    {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.embeddingApiService.getEmbedding( null ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_emptyInput()
    {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.embeddingApiService.getEmbedding( "" ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_longInput()
    {
        String input = "a".repeat( this.testInputLimit + 1 );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.embeddingApiService.getEmbedding( input ));

        String message =
                String.format( "Input exceeds maximum allowed length of %,d characters.", this.testInputLimit );
        assertEquals( message, exception.getMessage() );
    }

    @Test
    void testGetEmbedding_missingApiKey()
    {
        IllegalStateException exception = assertThrowsExactly(
                IllegalStateException.class, () -> this.embeddingApiService.getEmbedding( "Test" ));

        assertEquals( "Missing required environment variable: " + this.testApiKeyName, exception.getMessage() );
    }

    @Test
    void testGetEmbedding_success() throws Exception
    {
        String input = "This is a test with \"quote\" characters included in it.";
        int statusCode = 200;
        double[] expectedEmbedding = { -0.75, -0.5, 0.5, 0.75 };

        ObjectMapper objectMapper = new ObjectMapper();

        String response = String.format( """
                {
                  "object" : "list",
                  "data" : [ {
                    "object" : "embedding",
                    "index" : 0,
                    "embedding" : %s
                  } ],
                  "model" : "%s",
                  "usage" : {
                    "prompt_tokens" : 10,
                    "total_tokens" : 10
                  }
                }
                """, objectMapper.writeValueAsString( expectedEmbedding ), this.testModel );

        this.mockApiCall( statusCode, response );

        double[] actualEmbedding = this.embeddingApiService.getEmbedding( input );

        Map< String, String > expectedRequestMap = new HashMap<>();
        expectedRequestMap.put( "model", this.testModel );
        expectedRequestMap.put( "input", input );

        String actualRequestString = this.requestBodyCaptor.getValue();
        TypeReference< HashMap< String, String >> typeRef = new TypeReference<>() {};
        Map< String, String > actualRequestMap = objectMapper.readValue( actualRequestString, typeRef );

        assertThat( actualRequestMap ).as( "Request Map" ).containsExactlyInAnyOrderEntriesOf( expectedRequestMap );
        assertThat( actualEmbedding ).as( "Embedding" ).containsExactly( expectedEmbedding );
    }

    @Test
    void testGetEmbedding_error() throws Exception
    {
        String input = "Test Input";
        int statusCode = 401;
        String message = "Invalid API key provided.";

        String response = String.format( """
                {
                    "error":
                    {
                        "message": "%s",
                        "type": "invalid_request_error",
                        "param": null,
                        "code": "invalid_api_key"
                    }
                }
                """, message );

        this.mockApiCall( statusCode, response );

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class, () -> this.embeddingApiService.getEmbedding( input ));

        String expectedMessage =
                "Error returned from embedding API. Status Code: " + statusCode + ", Message: " + message;
        assertEquals( expectedMessage, exception.getMessage() );
    }

    private void mockApiCall( int statusCode, String response ) throws Exception
    {
        String testApiKey = "Test API Key";

        when( this.mockDotenv.get( this.testApiKeyName )).thenReturn( testApiKey );
        when( this.mockHttpRequestBuilder.reset() ).thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.uri( this.testApiEndpoint )).thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.header( "Content-Type", "application/json" ))
                .thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.header( "Authorization", "Bearer " + testApiKey ))
                .thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpRequestBuilder.POST( this.requestBodyCaptor.capture() ))
                .thenReturn( this.mockHttpRequestBuilder );
        when( this.mockHttpClient.< String >send( any(), any() )).thenReturn( this.mockHttpResponse );
        when( this.mockHttpResponse.statusCode() ).thenReturn( statusCode );
        when( this.mockHttpResponse.body() ).thenReturn( response );
    }
}
