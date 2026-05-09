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
import tools.jackson.databind.node.ObjectNode;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Pattern;

public class OpenAiApiCaller
{
    private final String             apiKeyVarName;
    private final Environment        environment;
    private final HttpRequestBuilder httpRequestBuilder;
    private final HttpClient         httpClient;
    private final OmegaCodexUtil     omegaCodexUtil;
    private final TaskRunner         taskRunner;
    private final ObjectMapper       objectMapper;

    OpenAiApiCaller()
    {
        this( "OMEGACODEX_OPENAI_API_KEY",
              new Environment(),
              new HttpRequestBuilder(),
              HttpClient.newHttpClient(),
              new OmegaCodexUtil(),
              new TaskRunner( 200 ));
    }

    OpenAiApiCaller( String apiKeyVarName, Environment environment, HttpRequestBuilder httpRequestBuilder,
                     HttpClient httpClient, OmegaCodexUtil omegaCodexUtil, TaskRunner taskRunner )
    {
        this.apiKeyVarName      = apiKeyVarName;
        this.environment        = environment;
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpClient         = httpClient;
        this.omegaCodexUtil     = omegaCodexUtil;
        this.taskRunner         = taskRunner;
        this.objectMapper       = new ObjectMapper();
    }

    JsonNode getResponse( String taskName, String apiEndpoint, ObjectNode requestNode, String startMessage,
                          boolean logApiSummary, boolean logApiDetails, List< Pattern > embeddedJsonPatterns )
    {
        if ( taskName == null ) throw new IllegalArgumentException( "Task name must not be null." );
        if ( apiEndpoint == null ) throw new IllegalArgumentException( "API endpoint must not be null." );
        if ( requestNode == null ) throw new IllegalArgumentException( "Request node must not be null." );

        String requestString = this.objectMapper.writeValueAsString( requestNode );

        if ( logApiDetails )
        {
            String debugRequestString = this.expandEmbeddedJson(
                    JsonPointer.compile( "/request" ), requestNode, embeddedJsonPatterns ).toPrettyString();

            omegaCodexUtil.println( "----------------------------------------------------------------------" );
            omegaCodexUtil.println( "Request:" );
            omegaCodexUtil.println( debugRequestString );
            omegaCodexUtil.println( "----------------------------------------------------------------------" );
        }

        HttpRequest request = this.httpRequestBuilder.reset()
                .uri( apiEndpoint )
                .header( "Content-Type", "application/json" )
                .header( "Authorization", "Bearer " + this.environment.getString( this.apiKeyVarName ))
                .POST( requestString )
                .build();

        HttpResponse< String > response = this.taskRunner.get( taskName, startMessage, logApiSummary,
                () -> this.httpClient.send( request, HttpResponse.BodyHandlers.ofString() ));

        int statusCode = response.statusCode();
        String responseString = response.body();

        JsonNode responseNode;
        try { responseNode = this.objectMapper.readTree( responseString ); }
        catch ( JacksonException e )
        {
            throw new OmegaCodexException(
                    String.format( "%s, Failed to deserialize response. Status Code: %d, Response:%n%s",
                                   taskName, statusCode, responseString ), e );
        }

        if ( logApiDetails )
        {
            String debugResponseString = this.expandEmbeddedJson(
                    JsonPointer.compile( "/response" ), responseNode, embeddedJsonPatterns ).toPrettyString();

            omegaCodexUtil.println( "----------------------------------------------------------------------" );
            omegaCodexUtil.println( "Status Code: " + statusCode );
            omegaCodexUtil.println( "Response:" );
            omegaCodexUtil.println( debugResponseString );
            omegaCodexUtil.println( "----------------------------------------------------------------------" );
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

    private JsonNode expandEmbeddedJson( JsonPointer path, JsonNode node, List< Pattern > embeddedJsonPatterns )
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
                    this.omegaCodexUtil.println(
                            "Failed to deserialize embedded JSON for path: " + pathString + ", JSON: " + nodeString );
                }
            }
        }

        if ( node.isObject() )
        {
            ObjectNode copy = this.objectMapper.createObjectNode();

            for ( String name : node.propertyNames() )
            {
                copy.set( name, this.expandEmbeddedJson(
                        path.appendProperty( name ), node.path( name ), embeddedJsonPatterns ));
            }

            return copy;
        }

        if ( node.isArray() )
        {
            ArrayNode copy = this.objectMapper.createArrayNode();

            for ( int i = 0; i < node.size(); i++ )
            {
                copy.add( this.expandEmbeddedJson( path.appendIndex( i ), node.get( i ), embeddedJsonPatterns ));
            }

            return copy;
        }

        return node;
    }
}
