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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

class EmbeddingApiService
{
    private final String             apiEndPoint;
    private final String             apiKeyVarName;
    private final String             model;
    private final int                inputLimit;
    private final Environment        environment;
    private final HttpRequestBuilder httpRequestBuilder;
    private final HttpClient         httpClient;
    private final OmegaCodexUtil     omegaCodexUtil;
    private final TaskRunner         taskRunner;

    EmbeddingApiService()
    {
        this.apiEndPoint        = "https://api.openai.com/v1/embeddings";
        this.apiKeyVarName      = "OMEGACODEX_OPENAI_API_KEY";
        this.model              = "text-embedding-3-small";
        this.inputLimit         = 20_000;
        this.environment        = new Environment();
        this.httpRequestBuilder = new HttpRequestBuilder();
        this.httpClient         = HttpClient.newHttpClient();
        this.omegaCodexUtil     = new OmegaCodexUtil();
        this.taskRunner         = new TaskRunner( 200 );
    }

    EmbeddingApiService(
            String apiEndPoint, String apiKeyVarName, String model, int inputLimit, Environment environment,
            HttpRequestBuilder httpRequestBuilder, HttpClient httpClient, OmegaCodexUtil omegaCodexUtil,
            TaskRunner taskRunner )
    {
        this.apiEndPoint        = apiEndPoint;
        this.apiKeyVarName      = apiKeyVarName;
        this.model              = model;
        this.inputLimit         = inputLimit;
        this.environment        = environment;
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpClient         = httpClient;
        this.omegaCodexUtil     = omegaCodexUtil;
        this.taskRunner         = taskRunner;
    }

    double[] getEmbeddingVector( String input )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        if ( input.length() > this.inputLimit )
        {
            String message =
                    String.format( "Input exceeds maximum allowed length of %,d characters.", this.inputLimit );
            throw new IllegalArgumentException( message );
        }

        String taskName = "Embedding API Call";
        String startMessage = String.format( "Input Length: %,d", input.length() );

        ObjectMapper objectMapper = new ObjectMapper();

        Map< String, String > requestMap = new HashMap<>();
        requestMap.put( "model", this.model );
        requestMap.put( "input", input );

        String requestString;
        try { requestString = objectMapper.writeValueAsString( requestMap ); }
        catch ( JsonProcessingException e ) { throw new OmegaCodexException( e ); }

        HttpRequest request = this.httpRequestBuilder.reset()
                .uri( this.apiEndPoint )
                .header( "Content-Type", "application/json" )
                .header( "Authorization", "Bearer " + this.environment.getString( this.apiKeyVarName ))
                .POST( requestString )
                .build();

        HttpResponse< String > response = this.taskRunner.get( taskName, startMessage, () ->
                this.httpClient.send( request, HttpResponse.BodyHandlers.ofString() ));

        int statusCode = response.statusCode();
        String responseBody = response.body();

        if ( statusCode != 200 )
        {
            String message;
            try { message = objectMapper.readTree( responseBody ).path( "error" ).path( "message" ).asText(); }
            catch ( JsonProcessingException e ) { throw new OmegaCodexException( e ); }

            String exceptionMessage = "Error returned from embedding API. Status Code: " + statusCode;
            if ( !message.isEmpty() ) exceptionMessage += ", Message: " + message;
            throw new OmegaCodexException( exceptionMessage );
        }

        JsonNode rootNode;
        try { rootNode = objectMapper.readTree( responseBody ); }
        catch ( JsonProcessingException e ) { throw new OmegaCodexException( e ); }

        int totalTokens = rootNode.path( "usage" ).path( "total_tokens" ).intValue();
        this.omegaCodexUtil.println( String.format( "%s, Tokens: %,d", taskName, totalTokens ));

        JsonNode embeddingNode = rootNode.path( "data" ).get( 0 ).path( "embedding" );
        double[] vector = new double[ embeddingNode.size() ];
        for ( int i = 0; i < embeddingNode.size(); i++ ) vector[ i ] = embeddingNode.get( i ).asDouble();
        return vector;
    }
}
