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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddingApiServiceIT
{
    @Test
    void testGetEmbedding_success() throws Exception
    {
        String input = "This is an automated integration test for calling the OpenAI Embedding API.";
        URL resource = this.getClass().getResource( this.getClass().getSimpleName() + ".json" );
        ObjectMapper objectMapper = new ObjectMapper();
        double[] expectedEmbedding = objectMapper.readValue( resource, double[].class );
        EmbeddingApiService embeddingApiService = new EmbeddingApiService();
        double[] actualEmbedding = embeddingApiService.getEmbedding( input );
        assertThat( actualEmbedding ).as( "Embedding" ).containsExactly( expectedEmbedding );
    }
}
