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

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
public class EnvironmentTest
{
    @Mock private Dotenv mockDotenv;

    private Environment environment;

    @BeforeEach
    void setUp()
    {
        this.environment = new Environment( this.mockDotenv );
    }

    @Test
    void getString_nullName()
    {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> this.environment.getString( null ));

        assertEquals( "Name must not be null.", exception.getMessage() );
    }

    @Test
    void getString_missingVariable()
    {
        String name = "test";
        String message = "Missing required environment variable. Name: " + name;

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class, () -> this.environment.getString( name ));

        assertEquals( message, exception.getMessage() );
    }

    @Test
    void getString_success()
    {
        String name = "test";
        String value = "Test Value";

        when( this.mockDotenv.get( name )).thenReturn( value );

        assertEquals( value, this.environment.getString( name ));
    }

    @Test
    void getInt_invalid()
    {
        String name = "test";
        String value = "Test Value";
        String message = "Cannot convert environment variable to integer. Name: " + name + ", Value: " + value;

        when( this.mockDotenv.get( name )).thenReturn( value );

        OmegaCodexException exception = assertThrowsExactly(
                OmegaCodexException.class, () -> this.environment.getInt( name ));

        assertEquals( message, exception.getMessage() );
    }

    @Test
    void getInt_success()
    {
        String name = "test";
        String stringValue = "42";
        int intValue = 42;

        when( this.mockDotenv.get( name )).thenReturn( stringValue );

        assertEquals( intValue, this.environment.getInt( name ));
    }
}
