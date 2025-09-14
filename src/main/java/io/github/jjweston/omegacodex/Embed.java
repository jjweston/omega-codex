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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class Embed
{
    public static void main( String[] args )
    {
        int embeddingStringLimit = 50;
        String input = "Omega Codex is an AI assistant for developers.";
        System.out.println( "Input: " + input );

        double[] vector = Embed.getEmbeddingVector( input );
        String vectorString;
        ObjectMapper objectMapper = new ObjectMapper();
        try { vectorString = objectMapper.writeValueAsString( vector ); }
        catch ( JsonProcessingException e )
        {
            throw new OmegaCodexException( "Failed to serialize vector: " + Arrays.toString( vector ), e );
        }

        if ( vectorString.length() > embeddingStringLimit )
        {
            vectorString = vectorString.substring( 0, embeddingStringLimit ) + "...";
        }

        System.out.println( "Vector: " + vectorString );
    }

    private static double[] getEmbeddingVector( String input )
    {
        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory();
        try ( Connection connection = sqLiteConnectionFactory.create() )
        {
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService );
            return embeddingService.getEmbedding( input ).vector();
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to close database connection.", e ); }
    }
}
