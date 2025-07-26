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

import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Hello
{
    public static void main( String[] args ) throws Exception
    {
        Path pythonToolsPath = Paths.get( "python-tools" );
        Path scriptPath = Paths.get( "hello.py" );

        System.out.println( "Running Python script: " + scriptPath );

        ProcessBuilder processBuilder = new ProcessBuilder( "poetry", "run", "python", scriptPath.toString() );
        processBuilder.directory( pythonToolsPath.toFile() );
        Process process = processBuilder.start();

        // synchronization is unnecessary; these lists are only accessed by one thread at a time
        List< String > stdoutLines = new LinkedList<>();
        List< String > stderrLines = new LinkedList<>();

        AtomicReference< Exception > stdoutExceptionReference = new AtomicReference<>();
        AtomicReference< Exception > stderrExceptionReference = new AtomicReference<>();

        Thread stdoutThread = Hello.readLines( process.inputReader(), stdoutLines, stdoutExceptionReference );
        Thread stderrThread = Hello.readLines( process.errorReader(), stderrLines, stderrExceptionReference );

        int exitCode = process.waitFor();
        stdoutThread.join();
        stderrThread.join();

        List< OmegaCodexException > exceptions = new LinkedList<>();
        Exception stdoutException = stdoutExceptionReference.get();
        Exception stderrException = stderrExceptionReference.get();

        if ( stdoutException != null )
        {
            exceptions.add(
                    new OmegaCodexException( "Exception occurred while reading standard input.", stdoutException ));
        }

        if ( stderrException != null )
        {
            exceptions.add(
                    new OmegaCodexException( "Exception occurred while reading standard error.", stderrException ));
        }

        if ( !exceptions.isEmpty() )
        {
            if ( exceptions.size() == 1 ) throw exceptions.getFirst();

            OmegaCodexException exception = new OmegaCodexException( "Exceptions occurred while running Python." );
            for ( Exception e : exceptions ) exception.addSuppressed( e );
            throw exception;
        }

        System.out.println( "Python exited with code: " + exitCode );
        for ( String line : stdoutLines ) System.out.println( "Standard Out: " + line );
        for ( String line : stderrLines ) System.out.println( "Standard Err: " + line );
    }

    private static Thread readLines(
            BufferedReader reader, List< String> lines, AtomicReference< Exception > exception )
    {
        Thread thread = new Thread( () ->
        {
            try
            {
                String line;
                while (( line = reader.readLine() ) != null ) lines.add( line );
            }
            catch ( Exception e ) { exception.set( e ); }
        } );

        thread.start();
        return thread;
    }
}
