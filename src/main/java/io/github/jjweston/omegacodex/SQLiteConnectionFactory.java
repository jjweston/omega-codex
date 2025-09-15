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

import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

class SQLiteConnectionFactory
{
    private final Path workDirectory = Paths.get( "work" );
    private final Path databaseFile  = Paths.get( "omegacodex.db" );

    SQLiteConnectionFactory() {}

    Connection create()
    {
        try { Files.createDirectories( this.workDirectory ); }
        catch ( IOException e ) { throw new OmegaCodexException( "Failed to create work directory.", e ); }

        Path databasePath = this.workDirectory.resolve( this.databaseFile );
        String databaseUrl = "jdbc:sqlite:" + databasePath;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl( databaseUrl );

        try { return dataSource.getConnection(); }
        catch ( SQLException e ) { throw new OmegaCodexException( "Failed to get database connection.", e ); }
    }
}
