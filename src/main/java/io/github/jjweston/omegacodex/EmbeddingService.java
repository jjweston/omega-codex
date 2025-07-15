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

class EmbeddingService
{
    private final EmbeddingCacheService embeddingCacheService;
    private final EmbeddingApiService   embeddingApiService;

    EmbeddingService()
    {
        this.embeddingCacheService = new EmbeddingCacheService();
        this.embeddingApiService   = new EmbeddingApiService();
    }

    EmbeddingService( EmbeddingCacheService embeddingCacheService, EmbeddingApiService embeddingApiService )
    {
        this.embeddingCacheService = embeddingCacheService;
        this.embeddingApiService   = embeddingApiService;
    }

    double[] getEmbedding( String input )
    {
        double[] embedding = this.embeddingCacheService.getEmbedding( input );

        if ( embedding == null )
        {
            embedding = this.embeddingApiService.getEmbedding( input );
            this.embeddingCacheService.setEmbedding( input, embedding );
        }

        return embedding;
    }
}
