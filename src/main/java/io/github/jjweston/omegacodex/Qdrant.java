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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class Qdrant
{
    public static void main( String[] args )
    {
        System.out.println( "Qdrant Proof of Concept" );

        List< String > inputStrings = new LinkedList<>();
        inputStrings.add( "The quick brown fox jumps over the lazy dog." );
        inputStrings.add( "Sally sells sea shells by the sea shore." );
        inputStrings.add( "How much wood would a woodchuck chuck if a woodchuck could chuck wood?" );
        inputStrings.add( "The next sentence is true. The previous sentence is false." );
        inputStrings.add( "It was the best of times. It was the worst of times." );

        String queryString = "What does Sally sell?";

        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory();

        try ( Connection connection = sqLiteConnectionFactory.create();
              QdrantService qdrantService = new QdrantService() )
        {
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService );

            for ( String inputString : inputStrings )
            {
                Embedding inputEmbedding = embeddingService.getEmbedding( inputString );
                qdrantService.upsert( inputEmbedding );
            }

            Embedding queryEmbedding = embeddingService.getEmbedding( queryString );
            List< SearchResult > searchResults = qdrantService.search( queryEmbedding.vector() );

            for (  SearchResult searchResult : searchResults )
            {
                float  score = searchResult.score();
                long   id    = searchResult.id();
                String input = embeddingCacheService.getInput( id );
                System.out.printf( "Score: %.10f, Id: %,d, Input: %s%n", score, id, input );
            }
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to close database connection.", e ); }
    }
}
