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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class EmbeddingCacheService
{
    private final Connection     connection;
    private final OmegaCodexUtil omegaCodexUtil;

    EmbeddingCacheService( Connection connection )
    {
        this( connection, new OmegaCodexUtil() );
    }

    EmbeddingCacheService( Connection connection, OmegaCodexUtil omegaCodexUtil )
    {
        this.connection     = connection;
        this.omegaCodexUtil = omegaCodexUtil;
        this.init();
    }

    Embedding getEmbedding( String input )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        try
        {
            PreparedStatement statement = this.connection.prepareStatement(
                    "SELECT Id, Vector FROM Embedding WHERE Input = ?" );
            statement.setString( 1, input );
            ResultSet result = statement.executeQuery();

            if ( result.next() )
            {
                long id = result.getLong( "Id" );
                String vectorString = result.getString( "Vector" );
                return new Embedding( id, new ImmutableDoubleArray( vectorString ));
            }
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to get embedding.", e ); }

        return null;
    }

    long setEmbedding( String input, ImmutableDoubleArray vector )
    {
        if ( input == null ) throw new IllegalArgumentException( "Input must not be null." );
        if ( input.isEmpty() ) throw new IllegalArgumentException( "Input must not be empty." );

        if ( vector == null ) throw new IllegalArgumentException( "Vector must not be null." );
        if ( vector.length() == 0 ) throw new IllegalArgumentException( "Vector must not be empty." );

        try
        {
            PreparedStatement statement = this.connection.prepareStatement(
                    "INSERT OR IGNORE INTO Embedding ( Input, Vector ) VALUES ( ?, ? )",
                    Statement.RETURN_GENERATED_KEYS );
            statement.setString( 1, input );
            statement.setString( 2, vector.toString() );

            if ( statement.executeUpdate() == 0 )
            {
                throw new IllegalArgumentException( "Input must not be a duplicate." );
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if ( generatedKeys.next() )
            {
                long id = generatedKeys.getLong( 1 );
                this.omegaCodexUtil.println( String.format( "Cache New Embedding, ID: %,d", id ));
                return id;
            }
            else throw new OmegaCodexException( "Failed to get ID of added embedding." );
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to insert into Embeddings table.", e ); }
    }

    String getInput( long id )
    {
        try
        {
            PreparedStatement statement = this.connection.prepareStatement(
                    "SELECT Input FROM Embedding WHERE Id = ?" );
            statement.setLong( 1, id );
            ResultSet result = statement.executeQuery();

            if ( result.next() ) return result.getString( "Input" );
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to get input.", e ); }

        throw new OmegaCodexException( String.format( "Unable to find embedding with id: %,d", id ));
    }

    private void init()
    {
        try
        {
            PreparedStatement statement = this.connection.prepareStatement( """
                    CREATE TABLE IF NOT EXISTS Embedding
                    (
                        Id     INTEGER PRIMARY KEY AUTOINCREMENT,
                        Input  TEXT    UNIQUE NOT NULL,
                        Vector TEXT           NOT NULL
                    )
                    """ );
            statement.execute();
        }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to create Embeddings table.", e ); }
    }
}
