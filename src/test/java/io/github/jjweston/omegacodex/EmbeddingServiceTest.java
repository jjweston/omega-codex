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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class EmbeddingServiceTest
{
    private final String   testString    = "Test";
    private final double[] testEmbedding = { -0.75, -0.5, 0.5, 0.75 };

    @Mock private EmbeddingCacheService mockEmbeddingCacheService;
    @Mock private EmbeddingApiService   mockEmbeddingApiService;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp()
    {
        this.embeddingService = new EmbeddingService( this.mockEmbeddingCacheService, this.mockEmbeddingApiService );
    }

    @Test
    void testGetEmbedding_cacheHit()
    {
        when( this.mockEmbeddingCacheService.getEmbedding( this.testString )).thenReturn( this.testEmbedding );
        double[] actualEmbedding = this.embeddingService.getEmbedding( this.testString );
        assertThat( actualEmbedding ).as( "Embedding" ).containsExactly( this.testEmbedding );
    }

    @Test
    void testGetEmbedding_cacheMiss()
    {
        when( this.mockEmbeddingCacheService.getEmbedding( this.testString )).thenReturn( null );
        when( this.mockEmbeddingApiService.getEmbedding( this.testString )).thenReturn( this.testEmbedding );
        double[] actualEmbedding = this.embeddingService.getEmbedding( this.testString );
        assertThat( actualEmbedding ).as( "Embedding" ).containsExactly( this.testEmbedding );
        verify( this.mockEmbeddingCacheService ).setEmbedding( this.testString, this.testEmbedding );
    }
}
