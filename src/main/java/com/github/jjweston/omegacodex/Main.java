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

package com.github.jjweston.omegacodex;

import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Main
{
    public static void main( String[] args ) throws Exception
    {
        String apiKeyName = "OMEGACODEX_OPENAI_API_KEY";
        String apiEndPoint = "https://api.openai.com/v1/embeddings";
        String model = "text-embedding-3-small";
        String input = "Omega Codex is an AI assistant for developers.";

        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get( apiKeyName );
        if ( apiKey == null ) throw new IllegalStateException( "Missing required environment variable: " + apiKeyName );

        String requestBody = String.format( "{ \"model\": \"%s\", \"input\": \"%s\" }", model, input );

        HttpRequest request = HttpRequest.newBuilder()
                .uri( URI.create( apiEndPoint ))
                .header( "Content-Type", "application/json" )
                .header( "Authorization", "Bearer " + apiKey )
                .POST( HttpRequest.BodyPublishers.ofString( requestBody, StandardCharsets.UTF_8 ))
                .build();

        try ( HttpClient client = HttpClient.newHttpClient() )
        {
            HttpResponse< String > response = client.send( request, HttpResponse.BodyHandlers.ofString() );
            System.out.println( "Status Code: "  + response.statusCode() );
            System.out.println( "Response Body:" );
            System.out.println( response.body() );
        }
    }
}
