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

import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Hello
{
    public static void main( String[] args ) throws Exception
    {
        Path pythonToolsPath = Paths.get( "python-tools" );
        Path venvPath = pythonToolsPath.resolve( ".venv" );
        Path pythonPath = SystemUtils.IS_OS_WINDOWS ?
                venvPath.resolve( Paths.get( "Scripts", "python.exe" )) :
                venvPath.resolve( Paths.get( "bin", "python" ));
        Path scriptPath = pythonToolsPath.resolve( "hello.py" );

        System.out.println( "Python executable: " + pythonPath );
        System.out.println( "Python script: " + scriptPath );

        ProcessBuilder pb = new ProcessBuilder( pythonPath.toString(), scriptPath.toString() );
        pb.redirectErrorStream( true );
        Process process = pb.start();

        BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ));

        String line;
        while (( line = reader.readLine()) != null ) System.out.println( "Python says: " + line );

        int exitCode = process.waitFor();
        System.out.println( "Python exited with code: " + exitCode );
    }
}
