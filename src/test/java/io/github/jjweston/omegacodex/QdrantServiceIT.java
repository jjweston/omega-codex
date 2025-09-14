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

import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QdrantServiceIT
{
    private final String              collectionName      = "omegacodex_chunks_test";
    private final QdrantClientFactory qdrantClientFactory = new QdrantClientFactory();
    private final TaskRunner          taskRunner          = new TaskRunner( 200 );

    @AfterEach
    void tearDown()
    {
        try( QdrantClient qdrantClient = this.qdrantClientFactory.create() )
        {
            String taskName = "Qdrant Service Integration Test - Delete Collection";

            this.taskRunner.run( taskName, () -> qdrantClient.deleteCollectionAsync( this.collectionName ).get() );
        }
    }

    @Test
    void testSearch() throws Exception
    {
        String className      = this.getClass().getSimpleName();
        int    collectionSize = 1_536;
        int    inputCount     = 5;

        List< SearchResult > expectedResults = new LinkedList<>();
        expectedResults.add( new SearchResult( 2, 0.67939620f ));
        expectedResults.add( new SearchResult( 1, 0.11987578f ));
        expectedResults.add( new SearchResult( 3, 0.11237586f ));
        expectedResults.add( new SearchResult( 4, 0.07297595f ));
        expectedResults.add( new SearchResult( 5, 0.07286711f ));

        try ( QdrantService qdrantService = new QdrantService(
                this.collectionName, collectionSize, this.taskRunner, this.qdrantClientFactory ))
        {
            for ( int i = 0; i < inputCount; i++ )
            {
                String resourceName = String.format( "%s-input-%d.json", className, i );
                long id = i + 1;
                ImmutableDoubleArray inputVector = this.getVector( resourceName );
                qdrantService.upsert( id, inputVector );
            }

            String resourceName = className + "-query.json";
            ImmutableDoubleArray queryVector = this.getVector( resourceName );
            List< SearchResult > actualResults = qdrantService.search( queryVector );
            assertThat( actualResults ).as( "Search Results" ).containsExactlyElementsOf( expectedResults );
        }
    }

    private ImmutableDoubleArray getVector( String resourceName ) throws Exception
    {
        try ( InputStream resourceStream = this.getClass().getResourceAsStream( resourceName ))
        {
            return ImmutableDoubleArray.fromInputStream( resourceStream );
        }
    }
}
