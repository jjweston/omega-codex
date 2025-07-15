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
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class EmbeddingCacheService
{
    private final Path workDirectory = Paths.get( "work" );
    private final Path databaseFile  = Paths.get( "omegacodex.db" );

    private final DirectoryCreator directoryCreator;
    private final SQLiteDataSource dataSource;
    private final OmegaCodexLogger omegaCodexLogger;

    EmbeddingCacheService()
    {
        this.directoryCreator = new DirectoryCreator();
        this.dataSource       = new SQLiteDataSource();
        this.omegaCodexLogger = new OmegaCodexLogger();

        this.init();
    }

    EmbeddingCacheService(
            DirectoryCreator directoryCreator, SQLiteDataSource dataSource, OmegaCodexLogger omegaCodexLogger )
    {
        this.directoryCreator = directoryCreator;
        this.dataSource       = dataSource;
        this.omegaCodexLogger = omegaCodexLogger;

        this.init();
    }

    double[] getEmbedding( String input )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        try
        {
            Connection connection = this.dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT Embedding FROM Embeddings WHERE input = ?" );
            statement.setString( 1, input );
            ResultSet result = statement.executeQuery();

            if ( result.next() )
            {
                String embeddingString = result.getString( "Embedding" );
                ObjectMapper objectMapper = new ObjectMapper();
                try { return objectMapper.readValue( embeddingString, double[].class ); }
                catch ( JsonProcessingException e ) { throw new OmegaCodexException( e ); }
            }
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to query Embeddings table.", e ); }

        return null;
    }

    void setEmbedding( String input, double[] embedding )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        if ( embedding == null ) throw new IllegalArgumentException( "Embedding must not be null." );
        if ( embedding.length == 0 ) throw new IllegalArgumentException( "Embedding must not be empty." );

        String embeddingString;
        ObjectMapper objectMapper = new ObjectMapper();
        try { embeddingString = objectMapper.writeValueAsString( embedding ); }
        catch ( JsonProcessingException e ) { throw new OmegaCodexException( e ); }

        try
        {
            Connection connection = this.dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR IGNORE INTO Embeddings ( Input, Embedding ) VALUES ( ?, ? )",
                    Statement.RETURN_GENERATED_KEYS );
            statement.setString( 1, input );
            statement.setString( 2, embeddingString );

            if ( statement.executeUpdate() == 0 )
            {
                throw new IllegalArgumentException( "Input must not be a duplicate." );
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if ( generatedKeys.next() )
            {
                long id = generatedKeys.getLong( 1 );
                this.omegaCodexLogger.log( String.format( "Cache New Embedding, ID: %,d", id ));
            }
            else throw new OmegaCodexException( "Failed to get Id of added embedding." );
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to insert into Embeddings table.", e ); }
    }

    private void init()
    {
        this.directoryCreator.create( this.workDirectory );

        Path databasePath = this.workDirectory.resolve( this.databaseFile );
        String databaseUrl = "jdbc:sqlite:" + databasePath;
        this.dataSource.setUrl( databaseUrl );

        try
        {
            Connection connection = this.dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS Embeddings
                    (
                        Id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        Input     TEXT    UNIQUE NOT NULL,
                        Embedding TEXT           NOT NULL
                    )
                    """ );
            statement.execute();
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to create Embeddings table.", e ); }
    }
}
