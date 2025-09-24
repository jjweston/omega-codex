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
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResponseApiServiceIT
{
    private final String              collectionName      = "omegacodex_chunks_test";
    private final QdrantClientFactory qdrantClientFactory = new QdrantClientFactory();
    private final TaskRunner          taskRunner          = new TaskRunner( 200 );

    @AfterEach
    void tearDown()
    {
        try( QdrantClient qdrantClient = this.qdrantClientFactory.create() )
        {
            String taskName = "Response API Service Integration Test - Delete Collection";

            this.taskRunner.run( taskName, () -> qdrantClient.deleteCollectionAsync( this.collectionName ).get() );
        }
    }

    @Test
    @SuppressWarnings( "ExtractMethodRecommender" )
    void testGetResponse() throws Exception
    {
        int collectionSize = 1_536;

        String query =
                """
                This is an automated integration test verifying successful integration with the OpenAI Responses API. \
                In order for this test to pass \
                you must respond with the correct answer to the question I ask you below as follows:

                Answer: [answer]

                Replace `[answer]` with your answer. \
                Do not add anything else to your response otherwise the test will fail.

                What is your name?\
                """;

        String databaseUrl = "jdbc:sqlite::memory:";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl( databaseUrl );

        try ( Connection connection = dataSource.getConnection();
              QdrantService qdrantService = new QdrantService(
                      this.collectionName, collectionSize, this.taskRunner, this.qdrantClientFactory ))
        {
            OpenAiApiCaller openAiApiCaller = new OpenAiApiCaller();
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            EmbeddingApiService embeddingApiService = new EmbeddingApiService( openAiApiCaller );
            EmbeddingService embeddingService = new EmbeddingService( embeddingCacheService, embeddingApiService );
            ResponseApiService responseApiService =
                    new ResponseApiService( embeddingCacheService, embeddingService, qdrantService, openAiApiCaller );

            String actualResponse = responseApiService.getResponse( query );

            assertEquals( "Answer: Omega Codex", actualResponse );
        }
    }
}
