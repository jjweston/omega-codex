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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
public class ResponseApiServiceTest
{
    private final int testIterationLimit = 5;

    @Mock private OpenAiApiCaller       mockOpenAiApiCaller;
    @Mock private EmbeddingCacheService mockEmbeddingCacheService;
    @Mock private EmbeddingService      mockEmbeddingService;
    @Mock private QdrantService         mockQdrantService;
    @Mock private OmegaCodexUtil        mockOmegaCodexUtil;

    @Captor private ArgumentCaptor< ObjectNode > requestNodeCaptor;

    @Test
    void testConstructor_nullEmbeddingCacheService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false,
                        null, this.mockEmbeddingService, this.mockQdrantService,
                        this.mockOpenAiApiCaller, this.mockOmegaCodexUtil ));

        assertEquals( "Embedding cache service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullEmbeddingService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false,
                        this.mockEmbeddingCacheService, null, this.mockQdrantService,
                        this.mockOpenAiApiCaller, this.mockOmegaCodexUtil ));

        assertEquals( "Embedding service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullQdrantService()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false,
                        this.mockEmbeddingCacheService, this.mockEmbeddingService, null,
                        this.mockOpenAiApiCaller, this.mockOmegaCodexUtil ));

        assertEquals( "Qdrant service must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullOpenAiCaller()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> new ResponseApiService(
                        this.testIterationLimit, false, false, false,
                        this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                        null, this.mockOmegaCodexUtil ));

        assertEquals( "OpenAI API caller must not be null.", exception.getMessage() );
    }

    @Test
    void getResponse_nullQuery()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> responseApiService.getResponse( null ));

        assertEquals( "Query must not be null.", exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void getResponse_iterationLimit()
    {
        int iterationLimit = 1024;

        ResponseApiService responseApiService = new ResponseApiService(
                iterationLimit, true, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String userQuery = "What is a problem with an infinite solution?";
        String functionQuery = "What is infinity plus one?";
        ImmutableDoubleArray queryVector = new ImmutableDoubleArray( new double[] { 0.5, 0.4, 0.3, 0.2, 0.1 } );
        Embedding queryEmbedding = new Embedding( 42, queryVector );
        SearchResult searchResult = new SearchResult( 7, 0.5f );
        List< SearchResult > searchResults = List.of( searchResult );
        String testSearchResult = "Infinity";

        String responseString = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type" : "function_call",
                      "arguments" : "{\\"query\\":\\"%s\\"}",
                      "call_id" : "test_call_id",
                      "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """, functionQuery );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller
                .getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        when( this.mockEmbeddingService.getEmbedding( functionQuery )).thenReturn( queryEmbedding );
        when( this.mockQdrantService.search( queryVector )).thenReturn( searchResults );
        when( this.mockEmbeddingCacheService.getInput( searchResult.id() )).thenReturn( testSearchResult );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( userQuery ));

        assertEquals( "Failed to get response within 1,024 iterations.", exception.getMessage() );

        InOrder inOrder = inOrder( this.mockOmegaCodexUtil );

        for  ( int i = 0; i < iterationLimit; i++ )
        {
            inOrder.verify( this.mockOmegaCodexUtil ).println( String.format(
                    "Response API Call, Iteration: %,d, " +
                    "Input Tokens: 2,000, Output Tokens: 1,000, Total Tokens: 3,000", i + 1 ));
        }

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegaCodexUtil );
    }

    @Test
    void getResponse_success()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, true, false, true,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String expectedUserQuery         = "What is your quest?";
        String expectedUserResponse      = "To seek the Holy Grail!";
        String expectedFunctionQuery     = "What is my quest?";
        String expectedFunctionResponse1 = "Quest Objective: Holy Grail";
        String expectedFunctionResponse2 = "Favorite Color: Blue";
        String expectedCallId            = "test_call_id";
        long   expectedSearchResultId1   = 7;
        long   expectedSearchResultId2   = 1_024;

        ImmutableDoubleArray queryVector = new ImmutableDoubleArray( new double[] { 0.5, 0.4, 0.3, 0.2, 0.1 } );
        Embedding queryEmbedding = new Embedding( 42, queryVector );

        List< SearchResult > searchResults = List.of(
                new SearchResult( expectedSearchResultId1, 0.50f ),
                new SearchResult( expectedSearchResultId2, 0.25f ));

        String responseString1 = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type" : "function_call",
                      "arguments" : "{\\"query\\":\\"%s\\"}",
                      "call_id" : "%s",
                      "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """, expectedFunctionQuery, expectedCallId );

        String responseString2 = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "%s"
                        }
                      ],
                      "role": "assistant"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2500,
                    "output_tokens": 1500,
                    "total_tokens": 4000
                  }
                }
                """, expectedUserResponse );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode1 = objectMapper.readTree( responseString1 );
        JsonNode responseNode2 = objectMapper.readTree( responseString2 );

        when( this.mockOpenAiApiCaller
                .getResponse( any(), any(), this.requestNodeCaptor.capture(), any(), anyBoolean() ))
                .thenReturn( responseNode1, responseNode2 );

        when( this.mockEmbeddingService.getEmbedding( expectedFunctionQuery )).thenReturn( queryEmbedding );
        when( this.mockQdrantService.search( queryVector )).thenReturn( searchResults );
        when( this.mockEmbeddingCacheService.getInput( expectedSearchResultId1 ))
                .thenReturn( expectedFunctionResponse1 );
        when( this.mockEmbeddingCacheService.getInput( expectedSearchResultId2 ))
                .thenReturn( expectedFunctionResponse2 );

        String actualUserResponse = responseApiService.getResponse( expectedUserQuery );

        assertEquals( expectedUserResponse, actualUserResponse );

        InOrder inOrder = inOrder( this.mockOmegaCodexUtil );

        inOrder.verify( this.mockOmegaCodexUtil ).println(
                "Response API Call, Iteration: 1, Input Tokens: 2,000, Output Tokens: 1,000, Total Tokens: 3,000" );

        inOrder.verify( this.mockOmegaCodexUtil ).println( "Response API Call, Search Readme: What is my quest?" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( "Chunk:     7, Score: 0.5000000000" );
        inOrder.verify( this.mockOmegaCodexUtil ).println( "Chunk: 1,024, Score: 0.2500000000" );

        inOrder.verify( this.mockOmegaCodexUtil ).println(
                "Response API Call, Iteration: 2, Input Tokens: 2,500, Output Tokens: 1,500, Total Tokens: 4,000" );

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockOmegaCodexUtil );

        List< ObjectNode > requestNodeList = this.requestNodeCaptor.getAllValues();
        assertEquals( 2, requestNodeList.size() );

        String actualUserQuery = requestNodeList.getFirst().path( "input" ).path( 1 ).path( "content" ).asString();
        assertEquals( expectedUserQuery, actualUserQuery );

        String actualCallId = requestNodeList.getLast().path( "input" ).path( 3 ).path( "call_id" ).asString();
        assertEquals( expectedCallId, actualCallId );

        String functionResponseJson = requestNodeList.getLast().path( "input" ).path( 3 ).path( "output" ).asString();
        JsonNode functionResponseNode = objectMapper.readTree( functionResponseJson );
        assertEquals( 2, functionResponseNode.size() );

        assertEquals( expectedSearchResultId1, functionResponseNode.path( 0 ).path( "id" ).asLong() );
        assertEquals( expectedSearchResultId2, functionResponseNode.path( 1 ).path( "id" ).asLong() );

        assertEquals( expectedFunctionResponse1, functionResponseNode.path( 0 ).path( "text" ).asString() );
        assertEquals( expectedFunctionResponse2, functionResponseNode.path( 1 ).path( "text" ).asString() );
    }

    @Test
    void handleOutput_noMessage_and_noFunctionCall()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Failed to find response message or function call:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_message_and_functionCall()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";
        String testQuery = "Test Query";
        ImmutableDoubleArray queryVector = new ImmutableDoubleArray( new double[] { 0.5, 0.4, 0.3, 0.2, 0.1 } );
        Embedding queryEmbedding = new Embedding( 42, queryVector );
        SearchResult searchResult = new SearchResult( 7, 0.5f );
        List< SearchResult > searchResults = List.of( searchResult );
        String testSearchResult = "Test Search Result";

        String responseString = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "Test Message"
                        }
                      ],
                      "role": "assistant"
                    },
                    {
                        "type" : "function_call",
                        "arguments" : "{\\"query\\":\\"%s\\"}",
                        "call_id" : "test_call_id",
                        "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """, testQuery );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        when( this.mockEmbeddingService.getEmbedding( testQuery )).thenReturn( queryEmbedding );
        when( this.mockQdrantService.search( queryVector )).thenReturn( searchResults );
        when( this.mockEmbeddingCacheService.getInput( searchResult.id() )).thenReturn( testSearchResult );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Received response message with function call:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_noAssistantRole()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "Test Message"
                        }
                      ],
                      "role": "broken"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Found response message from unexpected role:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_multipleMessages()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "First Message"
                        }
                      ],
                      "role": "assistant"
                    },
                    {
                      "type": "message",
                      "content":
                      [
                        {
                          "text": "Second Message"
                        }
                      ],
                      "role": "assistant"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Found more than one response message:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleOutput_multipleContentElements()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";

        ArrayNode contentNode = JsonNodeFactory.instance.arrayNode();
        for ( int i = 0; i < 1_024; i++ ) contentNode.add( JsonNodeFactory.instance.objectNode().put( "text", "foo" ));

        String responseString = String.format(
                """
                {
                  "output":
                  [
                    {
                      "type": "message",
                      "content": %s,
                      "role": "assistant"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """, contentNode );

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Expected 1 content element, but received 1,024:" +
                System.lineSeparator() +
                responseNode.path( "output" ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleFunctionCall_invalidArguments()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                        "type" : "function_call",
                        "arguments" : "This is not valid JSON.",
                        "call_id" : "test_call_id",
                        "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Failed to deserialize arguments:" +
                System.lineSeparator() +
                "This is not valid JSON.";

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleFunctionCall_invalidFunction()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                        "type" : "function_call",
                        "arguments" : "{\\"query\\":\\"Test Query\\"}",
                        "call_id" : "test_call_id",
                        "name" : "invalid_function"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        OmegaCodexException exception = assertThrowsExactly( OmegaCodexException.class,
                () -> responseApiService.getResponse( queryString ));

        String expectedMessage =
                "Unrecognized function:" +
                System.lineSeparator() +
                responseNode.path( "output" ).path( 0 ).toPrettyString();

        assertEquals( expectedMessage, exception.getMessage() );
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void handleSearchReadme_emptyQuery()
    {
        ResponseApiService responseApiService = new ResponseApiService(
                this.testIterationLimit, false, false, false,
                this.mockEmbeddingCacheService, this.mockEmbeddingService, this.mockQdrantService,
                this.mockOpenAiApiCaller, this.mockOmegaCodexUtil );

        String queryString = "What is your quest?";

        String responseString =
                """
                {
                  "output":
                  [
                    {
                        "type" : "function_call",
                        "arguments" : "{\\"query\\":\\"\\"}",
                        "call_id" : "test_call_id",
                        "name" : "search_readme"
                    }
                  ],
                  "usage":
                  {
                    "input_tokens": 2000,
                    "output_tokens": 1000,
                    "total_tokens": 3000
                  }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree( responseString );

        when( this.mockOpenAiApiCaller.getResponse( any(), any(), any(), any(), anyBoolean() ))
                .thenReturn( responseNode );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> responseApiService.getResponse( queryString ));

        assertEquals( "Query must not be empty.", exception.getMessage() );
    }
}
