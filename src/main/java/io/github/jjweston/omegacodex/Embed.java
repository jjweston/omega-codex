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
import com.fasterxml.jackson.databind.ObjectMapper;

public class Embed
{
    public static void main( String[] args )
    {
        int embeddingStringLimit = 50;
        String input = "Omega Codex is an AI assistant for developers.";
        System.out.println( "Input: " + input );

        EmbeddingService embeddingService = new EmbeddingService();
        double[] embedding = embeddingService.getEmbedding( input );

        String embeddingString;
        ObjectMapper objectMapper = new ObjectMapper();
        try { embeddingString = objectMapper.writeValueAsString( embedding ); }
        catch ( JsonProcessingException e ) { throw new OmegaCodexException( e ); }

        if ( embeddingString.length() > embeddingStringLimit )
        {
            embeddingString = embeddingString.substring( 0, embeddingStringLimit ) + "...";
        }

        System.out.println( "Embedding: " + embeddingString );
    }
}
