/*

Copyright 2025-2026 Jeffrey J. Weston <jjweston@gmail.com>

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
public class OpenAIApiCallerTest
{
    private final String testTaskName      = "OpenAIApiCallerTest";
    private final String testApiEndpoint   = "https://example.org/v1/test";
    private final String testApiKeyVarName = "OMEGACODEX_TEST_API_KEY";

    @Mock private Environment            mockEnvironment;
    @Mock private HttpRequestBuilder     mockHttpRequestBuilder;
    @Mock private HttpClient             mockHttpClient;
    @Mock private OmegaCodexUtil         mockOmegaCodexUtil;
    @Mock private OmegaCodexLogger       mockOmegaCodexLogger;
    @Mock private HttpResponse< String > mockHttpResponse;

    @Captor private ArgumentCaptor< String > requestBodyCaptor;

    @Test
    void testGetResponse_nullTaskName()
    {
        OpenAiApiCaller openAiApiCaller = this.createOpenAiApiCaller();
        ObjectMapper    objectMapper    = new ObjectMapper();
        ObjectNode      requestNode     = objectMapper.createObjectNode();

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> openAiApiCaller.getResponse(
                        null, this.testApiEndpoint, requestNode, null, false, false, List.of() ));

        assertEquals( "Task name must not be null.", exception.getMessage() );
    }

    @Test
    void testGetResponse_nullApiEndpoint()
    {
        OpenAiApiCaller openAiApiCaller = this.createOpenAiApiCaller();
        ObjectMapper    objectMapper    = new ObjectMapper();
        ObjectNode      requestNode     = objectMapper.createObjectNode();

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> openAiApiCaller.getResponse(
                        this.testTaskName, null, requestNode, null, false, false, List.of() ));

        assertEquals( "API endpoint must not be null.", exception.getMessage() );
    }

    @Test
    void testGetResponse_nullRequestNode()
    {
        OpenAiApiCaller openAiApiCaller = this.createOpenAiApiCaller();

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> openAiApiCaller.getResponse(
                        this.testTaskName, this.testApiEndpoint, null, null, false, false, List.of() ));

        assertEquals( "Request node must not be null.", exception.getMessage() );
    }

    @Test
    void testGetResponse_error_noMessage() throws Exception
    {
        OpenAiApiCaller openAiApiCaller = this.createOpenAiApiCaller();
        ObjectMapper    objectMapper    = new ObjectMapper();
        ObjectNode      requestNode     = objectMapper.createObjectNode();
        int             statusCode      = 500;

        String response =
                """
                {
                }
                """;

        this.mockApiCall( statusCode, response );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> openAiApiCaller.getResponse(
                        this.testTaskName, this.testApiEndpoint, requestNode, null, false, false, List.of() ));

        assertEquals( "OpenAIApiCallerTest, Error Returned, Status Code: 500", exception.getMessage() );
    }

    @Test
    void testGetResponse_error_withMessage() throws Exception
    {
        OpenAiApiCaller openAiApiCaller = this.createOpenAiApiCaller();
        ObjectMapper    objectMapper    = new ObjectMapper();
        ObjectNode      requestNode     = objectMapper.createObjectNode();
        int             statusCode      = 401;

        String response =
                """
                {
                    "error":
                    {
                        "message": "Invalid API key provided.",
                        "type": "invalid_request_error",
                        "param": null,
                        "code": "invalid_api_key"
                    }
                }
                """;

        this.mockApiCall( statusCode, response );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> openAiApiCaller.getResponse(
                        this.testTaskName, this.testApiEndpoint, requestNode, null, false, false, List.of() ));

        String expectedMessage =
                "OpenAIApiCallerTest, Error Returned, Status Code: 401, Error Message: Invalid API key provided.";

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void testGetResponse_invalidResponse() throws Exception
    {
        OpenAiApiCaller openAiApiCaller = this.createOpenAiApiCaller();
        ObjectMapper    objectMapper    = new ObjectMapper();
        ObjectNode      requestNode     = objectMapper.createObjectNode();
        int             statusCode      = 402;

        String responseString = "This is not valid JSON.";

        this.mockApiCall( statusCode, responseString );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> openAiApiCaller.getResponse(
                        this.testTaskName, this.testApiEndpoint, requestNode, null, false, false, List.of() ));

        String expectedMessage =
                "OpenAIApiCallerTest, Failed to deserialize response. Status Code: 402, Response:" +
                System.lineSeparator() +
                "This is not valid JSON.";

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    void testGetResponse_success() throws Exception
    {
        OpenAiApiCaller openAiApiCaller = this.createOpenAiApiCaller();
        ObjectMapper    objectMapper    = new ObjectMapper();
        int             statusCode      = 200;

        String responseString =
                """
                {
                  "adjective": "frozen",
                  "noun": "yogurt"
                }
                """;

        ObjectNode expectedRequestNode = objectMapper.createObjectNode()
                .put( "query", "What is your favorite food?" );

        JsonNode expectedResponseNode = JsonNodeFactory.instance.objectNode()
                .put( "adjective", "frozen" )
                .put( "noun", "yogurt" );

        this.mockApiCall( statusCode, responseString );

        JsonNode actualResponseNode = openAiApiCaller.getResponse(
                this.testTaskName, this.testApiEndpoint, expectedRequestNode, "Start Message", true, false, List.of() );

        String actualRequestString = this.requestBodyCaptor.getValue();
        JsonNode actualRequestNode = objectMapper.readTree( actualRequestString );

        assertEquals( expectedRequestNode, actualRequestNode );
        assertEquals( expectedResponseNode, actualResponseNode );

        InOrder inOrder = inOrder( this.mockOmegaCodexLogger );

        inOrder.verify( this.mockOmegaCodexLogger ).println( "OpenAIApiCallerTest, Starting, Start Message" );
        inOrder.verify( this.mockOmegaCodexLogger ).println( "OpenAIApiCallerTest, Complete, Duration: 0 ms" );

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegaCodexLogger );
    }

    private OpenAiApiCaller createOpenAiApiCaller()
    {
        TaskRunner testTaskRunner = new TaskRunner( 0, this.mockOmegaCodexUtil, this.mockOmegaCodexLogger );

        return new OpenAiApiCaller( this.testApiKeyVarName, this.mockEnvironment, this.mockHttpRequestBuilder,
                                    this.mockHttpClient, this.mockOmegaCodexLogger, testTaskRunner );
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
