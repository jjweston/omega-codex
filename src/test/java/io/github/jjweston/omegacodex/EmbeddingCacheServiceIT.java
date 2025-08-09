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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class EmbeddingCacheServiceIT
{
    @Test
    void testCache() throws Exception
    {
        String testInput = "This is a test input.";
        double[] testEmbedding = { -0.75, -0.5, 0.5, 0.75 };

        String databaseUrl = "jdbc:sqlite::memory:";
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl( databaseUrl );

        try ( Connection connection = dataSource.getConnection() )
        {
            EmbeddingCacheService embeddingCacheService = new EmbeddingCacheService( connection );
            Assertions.assertNull( embeddingCacheService.getEmbedding( testInput ));
            embeddingCacheService.setEmbedding( testInput, testEmbedding );
            double[] cachedEmbedding = embeddingCacheService.getEmbedding( testInput );
            assertThat( cachedEmbedding ).as( "Embedding" ).containsExactly( testEmbedding );

            IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                    () -> embeddingCacheService.setEmbedding( testInput, testEmbedding ));

            assertEquals( "Input must not be a duplicate.", exception.getMessage() );
        }
    }
}
