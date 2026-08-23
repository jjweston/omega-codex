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

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.regex.Pattern;

class OpenAiApiCaller
{
    private final int                maxAttempts;
    private final String             apiKeyVarName;
    private final Environment        environment;
    private final HttpRequestBuilder httpRequestBuilder;
    private final HttpClient         httpClient;
    private final Random             random;
    private final OmegaCodexUtil     omegaCodexUtil;
    private final OmegaCodexLogger   omegaCodexLogger;
    private final TaskRunner         taskRunner;
    private final ObjectMapper       objectMapper;
    private final ObjectMapper       yamlObjectMapper;
    private final String             logDividerRequest;
    private final String             logDividerResponseHeaders;
    private final String             logDividerResponse;

    OpenAiApiCaller()
    {
        this( 5,
              "OMEGACODEX_OPENAI_API_KEY",
              new Environment(),
              new HttpRequestBuilder(),
              HttpClient.newHttpClient(),
              new Random(),
              new OmegaCodexUtil(),
              new OmegaCodexLogger(),
              new TaskRunner( 200 ));
    }

    OpenAiApiCaller( int maxAttempts, String apiKeyVarName, Environment environment,
                     HttpRequestBuilder httpRequestBuilder, HttpClient httpClient, Random random,
                     OmegaCodexUtil omegaCodexUtil, OmegaCodexLogger omegaCodexLogger, TaskRunner taskRunner )
    {
        this.maxAttempts        = maxAttempts;
        this.apiKeyVarName      = apiKeyVarName;
        this.environment        = environment;
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpClient         = httpClient;
        this.random             = random;
        this.omegaCodexUtil     = omegaCodexUtil;
        this.omegaCodexLogger   = omegaCodexLogger;
        this.taskRunner         = taskRunner;

        this.objectMapper     = new ObjectMapper();
        this.yamlObjectMapper = YAMLMapper.builder()
                .disable( YAMLWriteFeature.WRITE_DOC_START_MARKER )
                .enable( YAMLWriteFeature.LITERAL_BLOCK_STYLE )
                .enable( YAMLWriteFeature.SPLIT_LINES )
                .build();

        this.logDividerRequest         = "----- Request --------------------------------------------------------";
        this.logDividerResponseHeaders = "----- Response Headers -----------------------------------------------";
        this.logDividerResponse        = "----- Response -------------------------------------------------------";
    }

    JsonNode getResponse( String taskName, String apiEndpoint, ObjectNode requestNode, String startMessage,
                          boolean logSummary, boolean logRequest, boolean logResponseHeaders, boolean logResponse,
                          List< Pattern > embeddedJsonPatterns, Map< String, Integer > arraysToTrim )
    {
        if ( taskName == null ) throw new IllegalArgumentException( "Task name must not be null." );
        if ( apiEndpoint == null ) throw new IllegalArgumentException( "API endpoint must not be null." );
        if ( requestNode == null ) throw new IllegalArgumentException( "Request node must not be null." );

        String requestString = this.objectMapper.writeValueAsString( requestNode );

        if ( logRequest )
        {
            String debugRequestString = this.yamlObjectMapper.writer().writeValueAsString(
                    this.prepareJsonForLogging(
                            JsonPointer.compile( "/request" ),
                            requestNode, embeddedJsonPatterns, arraysToTrim )).trim();

            this.omegaCodexLogger.println( this.logDividerRequest );
            this.omegaCodexLogger.println( debugRequestString );
            this.omegaCodexLogger.println( this.logDividerRequest );
        }

        HttpRequest request = this.httpRequestBuilder.reset()
                .uri( apiEndpoint )
                .header( "Content-Type", "application/json" )
                .header( "Authorization", "Bearer " + this.environment.getString( this.apiKeyVarName ))
                .POST( requestString )
                .build();

        int statusCode = 0;
        JsonNode responseNode = JsonNodeFactory.instance.objectNode();

        for ( int attempt = 1; attempt <= this.maxAttempts; attempt++ )
        {
            HttpResponse< String > response = this.taskRunner.get( taskName, startMessage, logSummary,
                    () -> this.httpClient.send( request, HttpResponse.BodyHandlers.ofString() ));

            statusCode = response.statusCode();
            String responseString = response.body();

            if ( logResponseHeaders )
            {
                String debugResponseHeadersString =
                        this.yamlObjectMapper.writer().writeValueAsString( response.headers().map() ).trim();

                this.omegaCodexLogger.println( this.logDividerResponseHeaders );
                this.omegaCodexLogger.println( debugResponseHeadersString );
                this.omegaCodexLogger.println( this.logDividerResponseHeaders );
            }

            try { responseNode = this.objectMapper.readTree( responseString ); }
            catch ( JacksonException e )
            {
                throw new OmegaCodexException(
                        String.format( "%s, Failed to deserialize response. Status Code: %d, Response:%n%s",
                                taskName, statusCode, responseString ), e );
            }

            if ( logResponse )
            {
                String debugResponseString = this.yamlObjectMapper.writer().writeValueAsString(
                        this.prepareJsonForLogging(
                                JsonPointer.compile( "/response" ),
                                responseNode, embeddedJsonPatterns, arraysToTrim )).trim();

                this.omegaCodexLogger.println( this.logDividerResponse );
                this.omegaCodexLogger.println( "Status Code: " + statusCode );
                this.omegaCodexLogger.println( "Response:" );
                this.omegaCodexLogger.println( debugResponseString );
                this.omegaCodexLogger.println( this.logDividerResponse );
            }

            if ( statusCode != 429 ) break;

            OptionalLong retryDelayHeader = response.headers().firstValueAsLong( "retry-after-ms" );
            if ( retryDelayHeader.isEmpty() ) break;

            long retryDelay = retryDelayHeader.getAsLong();
            if ( retryDelay <= 0 ) break;

            if ( attempt == this.maxAttempts ) break;

            float jitter = this.random.nextFloat() / 10 + 1.1f;
            long sleepDelay = (long) ( retryDelay * jitter );

            if ( logSummary )
            {
                this.omegaCodexLogger.println( String.format(
                        "%s, Rate Limit Exceeded, Attempt: %,d, Retry Delay: %,d ms, Jitter: %.3f, Sleeping: %,d ms",
                        taskName, attempt, retryDelay, jitter, sleepDelay ));
            }

            try { this.omegaCodexUtil.sleepThread( sleepDelay ); }
            catch ( InterruptedException e )
            {
                this.omegaCodexUtil.interruptThread();
                throw new OmegaCodexException( taskName + ", Retry Sleep Interrupted", e );
            }
        }

        if ( statusCode != 200 )
        {
            String errorMessage = responseNode.path( "error" ).path( "message" ).asString();
            String exceptionMessage = taskName + ", Error Returned, Status Code: " + statusCode;
            if ( !errorMessage.isEmpty() ) exceptionMessage += ", Error Message: " + errorMessage;
            throw new OmegaCodexException( exceptionMessage );
        }

        return responseNode;
    }

    private JsonNode prepareJsonForLogging(
            JsonPointer path, JsonNode node, List< Pattern > embeddedJsonPatterns, Map< String, Integer > arraysToTrim )
    {
        String pathString = path.toString();
        if ( embeddedJsonPatterns.stream().anyMatch( pattern -> pattern.matcher( pathString ).matches() ))
        {
            if ( node.isString() )
            {
                String nodeString = node.asString();
                try
                {
                    node = this.objectMapper.readTree( nodeString );
                }
                catch ( JacksonException _ )
                {
                    this.omegaCodexLogger.println(
                            "Failed to deserialize embedded JSON for path: " + pathString + ", JSON: " + nodeString );
                }
            }
        }

        if ( node.isObject() )
        {
            ObjectNode copy = this.objectMapper.createObjectNode();

            for ( String name : node.propertyNames() )
            {
                copy.set( name, this.prepareJsonForLogging(
                        path.appendProperty( name ), node.path( name ), embeddedJsonPatterns, arraysToTrim ));
            }

            return copy;
        }

        if ( node.isArray() )
        {
            ArrayNode copy = this.objectMapper.createArrayNode();

            int startIndex = 0;

            if ( arraysToTrim.containsKey( pathString ))
            {
                startIndex = arraysToTrim.get( pathString );
                if ( startIndex > 0 )
                {
                    copy.add( this.objectMapper.createObjectNode()
                            .put( "type", "debugging" )
                            .put( "message", "Elements Trimmed" )
                            .put( "count", startIndex ));
                }
            }

            for ( int i = startIndex; i < node.size(); i++ )
            {
                copy.add( this.prepareJsonForLogging(
                        path.appendIndex( i ), node.get( i ), embeddedJsonPatterns, arraysToTrim ));
            }

            return copy;
        }

        return node;
    }
}
