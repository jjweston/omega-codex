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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class EmbeddingApiService
{
    private final String           taskName;
    private final String           apiEndpoint;
    private final String           model;
    private final int              inputLimit;
    private final boolean          logApiSummary;
    private final boolean          logApiDetails;
    private final List< Pattern >  embeddedJsonPatterns;
    private final OpenAiApiCaller  openAiApiCaller;
    private final OmegaCodexLogger omegaCodexLogger;

    EmbeddingApiService( OpenAiApiCaller openAiApiCaller )
    {
        boolean logApiSummary = false;
        boolean logApiDetails = false;

        this( logApiSummary, logApiDetails, openAiApiCaller, new OmegaCodexLogger() );
    }

    EmbeddingApiService( boolean logApiSummary, boolean logApiDetails,
                         OpenAiApiCaller openAiApiCaller, OmegaCodexLogger omegaCodexLogger )
    {
        if ( openAiApiCaller == null ) throw new IllegalArgumentException( "OpenAI API caller must not be null." );

        this.taskName             = "Embedding API Call";
        this.apiEndpoint          = "https://api.openai.com/v1/embeddings";
        this.model                = "text-embedding-3-small";
        this.inputLimit           = 20_000;
        this.logApiSummary        = logApiSummary;
        this.logApiDetails        = logApiDetails;
        this.embeddedJsonPatterns = List.of();
        this.openAiApiCaller      = openAiApiCaller;
        this.omegaCodexLogger     = omegaCodexLogger;
    }

    ImmutableDoubleArray getEmbeddingVector( String input )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        if ( input.length() > this.inputLimit )
        {
            String message = String.format(
                    "Input length must not be greater than %,d. Actual Length: %,d", this.inputLimit, input.length() );
            throw new IllegalArgumentException( message );
        }

        String startMessage = String.format( "Input Length: %,d", input.length() );

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put( "model", this.model );
        requestNode.put( "input", input );

        Map< String, Integer > arraysToTrim = Map.of();

        JsonNode responseNode = this.openAiApiCaller.getResponse(
                this.taskName, this.apiEndpoint, requestNode, startMessage,
                this.logApiSummary, this.logApiDetails,
                this.embeddedJsonPatterns, arraysToTrim );

        if ( this.logApiSummary )
        {
            int totalTokens = responseNode.path( "usage" ).path( "total_tokens" ).intValue();
            this.omegaCodexLogger.println( String.format( "%s, Tokens: %,d", this.taskName, totalTokens ));
        }

        JsonNode embeddingNode = responseNode.path( "data" ).get( 0 ).path( "embedding" );
        double[] vector = embeddingNode.valueStream().mapToDouble( JsonNode::asDouble ).toArray();
        return new ImmutableDoubleArray( vector );
    }
}
