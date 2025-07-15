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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class EmbeddingCacheServiceTest
{
    private final double[] testEmbedding = { -0.75, -0.5, 0.5, 0.75 };

    @Mock private DirectoryCreator  mockDirectoryCreator;
    @Mock private SQLiteDataSource  mockDataSource;
    @Mock private OmegaCodexLogger  mockOmegaCodexLogger;
    @Mock private Connection        mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private ResultSet         mockResultSet;

    private EmbeddingCacheService embeddingCacheService;

    @BeforeEach
    void setUp() throws Exception
    {
        when( this.mockDataSource.getConnection() ).thenReturn( this.mockConnection );
        when( this.mockConnection.prepareStatement( any() )).thenReturn( this.mockPreparedStatement );

        this.embeddingCacheService =
                new EmbeddingCacheService( mockDirectoryCreator, mockDataSource, mockOmegaCodexLogger );
    }

    @Test
    void testGetEmbedding_nullInput()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.embeddingCacheService.getEmbedding( null ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_emptyInput()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.embeddingCacheService.getEmbedding( "" ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testGetEmbedding_cacheHit() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String embeddingString = objectMapper.writeValueAsString( this.testEmbedding );

        when( this.mockPreparedStatement.executeQuery() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( true );
        when ( this.mockResultSet.getString( "Embedding" )).thenReturn( embeddingString );

        double[] actualEmbedding = this.embeddingCacheService.getEmbedding( "Test" );

        assertThat( actualEmbedding ).as( "Embedding" ).containsExactly( this.testEmbedding );
    }

    @Test
    void testGetEmbedding_cacheMiss() throws Exception
    {
        when( this.mockPreparedStatement.executeQuery() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( false );

        double[] actualEmbedding = this.embeddingCacheService.getEmbedding( "Test" );

        assertNull( actualEmbedding );
    }

    @Test
    void testSetEmbedding_nullInput()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.embeddingCacheService.setEmbedding( null, this.testEmbedding ));

        assertEquals( "Input must not be null.", exception.getMessage() );
    }

    @Test
    void testSetEmbedding_emptyInput()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.embeddingCacheService.setEmbedding( "", this.testEmbedding ));

        assertEquals( "Input must not be empty.", exception.getMessage() );
    }

    @Test
    void testSetEmbedding_nullEmbedding()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.embeddingCacheService.setEmbedding( "Test", null ));

        assertEquals( "Embedding must not be null.", exception.getMessage() );
    }

    @Test
    void testSetEmbedding_emptyEmbedding()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.embeddingCacheService.setEmbedding( "Test", new double[] {} ));

        assertEquals( "Embedding must not be empty.", exception.getMessage() );
    }

    @Test
    void testSetEmbedding_newEmbedding() throws Exception
    {
        //noinspection MagicConstant
        when( this.mockConnection.prepareStatement( any(), anyInt() )).thenReturn( this.mockPreparedStatement );
        when( this.mockPreparedStatement.executeUpdate() ).thenReturn( 1 );
        when( this.mockPreparedStatement.getGeneratedKeys() ).thenReturn( this.mockResultSet );
        when( this.mockResultSet.next() ).thenReturn( true );
        when( this.mockResultSet.getLong( 1 )).thenReturn( 42L );
        this.embeddingCacheService.setEmbedding( "Test", this.testEmbedding );
    }

    @Test
    void testSetEmbedding_duplicateEmbedding() throws Exception
    {
        //noinspection MagicConstant
        when( this.mockConnection.prepareStatement( any(), anyInt() )).thenReturn( this.mockPreparedStatement );
        when( this.mockPreparedStatement.executeUpdate() ).thenReturn( 0 );

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> this.embeddingCacheService.setEmbedding( "Test", this.testEmbedding ));

        assertEquals( "Input must not be a duplicate.", exception.getMessage() );
    }
}
