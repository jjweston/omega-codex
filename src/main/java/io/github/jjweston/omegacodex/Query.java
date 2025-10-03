
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

class Query
{
    private Query() {}

    static void main()
    {
        SQLiteConnectionFactory sqLiteConnectionFactory = new SQLiteConnectionFactory();

        try ( Connection connection = sqLiteConnectionFactory.create();
              QdrantService qdrantService = new QdrantService() )
        {
            OpenAiApiCaller openAiApiCaller = new OpenAiApiCaller();
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            EmbeddingApiService embeddingApiService = new EmbeddingApiService( openAiApiCaller );
            EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService, embeddingApiService );
            ResponseApiService responseApiService =
                    new ResponseApiService( embeddingCacheService, embeddingService, qdrantService, openAiApiCaller );

            Query.processReadme( embeddingService, qdrantService );
            Query.queryLoop( responseApiService );
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to close database connection.", e ); }
    }

    private static void processReadme( EmbeddingService embeddingService, QdrantService qdrantService )
    {
        Path inputFilePath = Paths.get( "readme.md" );
        MarkdownSplitter markdownSplitter = new MarkdownSplitter();
        List< String > chunks = markdownSplitter.split( inputFilePath );
        for ( String chunk : chunks ) qdrantService.upsert( embeddingService.getEmbedding( chunk ));
    }

    private static void queryLoop( ResponseApiService responseApiService )
    {
        System.out.println();
        System.out.println( "Omega Codex - Command-Line Query Interface" );
        System.out.println();
        System.out.println( "Enter your query. Press enter on an empty line when you are finished." );

        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ));
        while ( true )
        {
            System.out.println();
            System.out.print( "> " );

            String query;
            try { query = reader.readLine(); }
            catch ( IOException e ) { throw new OmegaCodexException( "Failed to read query.", e ); }
            System.out.println();

            if ( query == null ) break;
            query = query.trim();
            if ( query.isEmpty() ) break;

            String response = responseApiService.getResponse( query );
            System.out.println();
            System.out.println( "Response:" );
            System.out.println();
            System.out.println( response );
        }

        System.out.println( "Exiting" );
    }
}
