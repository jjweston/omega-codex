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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class EmbeddingApiServiceTest
{
    private final String testApiEndpoint   = "https://example.org/v1/embeddings";
    private final String testApiKeyVarName = "OMEGACODEX_TEST_API_KEY";
    private final String testModel         = "test-embedding";

    @Mock private Environment            mockEnvironment;
    @Mock private HttpClient             mockHttpClient;
    @Mock private HttpRequestBuilder     mockHttpRequestBuilder;
    @Mock private HttpResponse< String > mockHttpResponse;
    @Mock private OmegaCodexUtil         mockOmegaCodexUtil;

    @Captor private ArgumentCaptor< String > requestBodyCaptor;

    private EmbeddingApiService embeddingApiService;

    @BeforeEach
    void setUp()
    {
        int testInputLimit = 5_000;
        TaskRunner taskRunner = new TaskRunner( 0, this.mockOmegaCodexUtil );

        this.embeddingApiService = new EmbeddingApiService(
                this.testApiEndpoint, this.testApiKeyVarName, this.testModel, testInputLimit, this.mockEnvironment,
                this.mockHttpRequestBuilder, this.mockHttpClient, this.mockOmegaCodexUtil, taskRunner );
    }

    @AfterEach
    void tearDown()
    {
        verifyNoMoreInteractions( this.mockHttpClient );
    }

    @Test
    void testGetEmbeddingVector_nullInput()
    {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.embeddingApiService.getEmbeddingVector( null ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbeddingVector_emptyInput()
    {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.embeddingApiService.getEmbeddingVector( "" ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testGetEmbeddingVector_longInput()
    {
        String input = "a".repeat( 8_192 );

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.embeddingApiService.getEmbeddingVector( input ));

        assertEquals( "Input length must not be greater than 5,000. Actual Length: 8,192", exception.getMessage() );
    }

    @Test
    void testGetEmbeddingVector_error() throws Exception
    {
        String input = "Test Input";
        int statusCode = 401;
        String errorMessage = "Invalid API key provided.";

        String response = String.format(
                """
                {
                    "error":
                    {
                        "message": "%s",
                        "type": "invalid_request_error",
                        "param": null,
                        "code": "invalid_api_key"
                    }
                }
                """, errorMessage );

        this.mockApiCall( statusCode, response );

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class, () -> this.embeddingApiService.getEmbeddingVector( input ));

        String expectedMessage =
                "Embedding API Call, Error Returned, Status Code: " + statusCode + ", Error Message: " + errorMessage;
        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void testGetEmbeddingVector_success() throws Exception
    {
        String input = "This is a test with \"quote\" characters included in it. ".repeat( 20 ).trim();
        int statusCode = 200;
        ImmutableDoubleArray expectedVector = new ImmutableDoubleArray( new double[] { -0.75, -0.5, 0.5, 0.75 } );

        String response = String.format(
                """
                {
                  "object" : "list",
                  "data" : [ {
                    "object" : "embedding",
                    "index" : 0,
                    "embedding" : %s
                  } ],
                  "model" : "%s",
                  "usage" : {
                    "prompt_tokens" : 1024,
                    "total_tokens" : 1024
                  }
                }
                """, expectedVector, this.testModel );

        this.mockApiCall( statusCode, response );

        ImmutableDoubleArray actualVector = this.embeddingApiService.getEmbeddingVector( input );

        Map< String, String > expectedRequestMap = new HashMap<>();
        expectedRequestMap.put( "model", this.testModel );
        expectedRequestMap.put( "input", input );

        String actualRequestString = this.requestBodyCaptor.getValue();
        TypeReference< HashMap< String, String >> typeRef = new TypeReference<>() {};
        ObjectMapper objectMapper = new ObjectMapper();
        Map< String, String > actualRequestMap = objectMapper.readValue( actualRequestString, typeRef );

        assertThat( actualRequestMap ).as( "Request Map" ).containsExactlyInAnyOrderEntriesOf( expectedRequestMap );
        assertEquals( expectedVector, actualVector );

        verify( this.mockOmegaCodexUtil ).println( "Embedding API Call, Starting, Input Length: 1,099" );
        verify( this.mockOmegaCodexUtil ).println( "Embedding API Call, Tokens: 1,024" );
    }

    private void mockApiCall( int statusCode, String response ) throws Exception
    {
        String testApiKey = "Test API Key";

        when( this.mockEnvironment.getString( this.testApiKeyVarName )).thenReturn( testApiKey );
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
